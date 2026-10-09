/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.routines.CodeVersions;
import com.fpetrola.z80.se.StackListener;
import com.fpetrola.z80.spy.ExecutionListener;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.*;
import java.util.function.Function;

import static com.fpetrola.z80.registers.RegisterName.SP;

public class StackAnalyzer implements java.io.Serializable {
  public record Entry(int value, int pc, boolean returnAddress) {
  }

  private transient State state;
  private transient Function<StackListener, Boolean> lastEvent;
  private boolean initialized;
  private transient StackAsRepositoryState stackAsRepository = new StackAsRepositoryState();
  private transient StackListener stackListener;
  public MultiValuedMap<Integer, Integer> dynamicInvocation = new HashSetValuedHashMap<>();
  public final Map<Integer, Integer> callContinuations = new HashMap<>();
  public final MultiValuedMap<Integer, Integer> shiftedReturns = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> dataConsumedBy = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> dataOnTopAt = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> poppedCallSites = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> returnsConsumedBy = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> pushedValues = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> calledThrough = new HashSetValuedHashMap<>();
  public final Set<Integer> jumpTableSites = new HashSet<>(), recordedPops = new HashSet<>(), pushesTakenByPops = new HashSet<>();
  public final Map<Integer, Integer> stackSwitches = new HashMap<>();
  private transient int[] leaving, reentered;
  private final transient Map<Integer, int[]> leftStacks = new HashMap<>();
  private transient int switchHomeSp = -1, lastStorePlace = -1;
  private transient boolean callSinceLoad;
  public final MultiValuedMap<Integer, Integer> nonLocalRets = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> returnSlots = new HashSetValuedHashMap<>();
  public static boolean collecting;
  private boolean learnedFromRecording;
  public CodeVersions codeVersions = new CodeVersions();
  private int pcValue;
  private int stackResetTo = -1;
  private boolean returnsDropped;
  public boolean knowsWholeStack = true;
  private final transient List<Integer> simulatedRets = new ArrayList<>();
  private final transient List<Integer> simulatedCallsPcs = new ArrayList<>();
  private final transient Map<Integer, Entry> entries = new HashMap<>();
  private final transient Map<Integer, Entry> consumedReturns = new HashMap<>();
  private final transient MemoryWriteListener forgetOverwritten = (address, value) -> {
    entries.remove(address);
    entries.remove((address - 1) & 0xFFFF);
  };

  public StackAnalyzer(State state) {
    reset(state);
  }

  public List<Integer> getSimulatedCallsPcs() {
    return simulatedCallsPcs;
  }

  public void reset(State state) {
    if (this.state != null)
      this.state.getMemory().removeMemoryWriteListener(forgetOverwritten);
    this.state = state;
    if (state != null)
      state.getMemory().addMemoryWriteListener(forgetOverwritten);
    entries.clear();
    consumedReturns.clear();
    lastEvent = null;
    stackAsRepository = new StackAsRepositoryState();
    pcValue = -1;
    initialized = false;
  }


  private int placeOf(Object reference) {
    int[] memory = state.getMemory().getData();
    if (reference instanceof IndirectMemory16BitReference indirect && indirect.getTarget() instanceof Memory16BitReference operand)
      return memory[pcValue + operand.getDelta() & 0xffff] | memory[pcValue + operand.getDelta() + 1 & 0xffff] << 8;
    if (reference instanceof Memory16BitReference operand)
      return pcValue + operand.getDelta() & 0xffff;
    return -1;
  }

  private void confirmSwitch(int[] left) {
    stackSwitches.put(left[3], left[0]);
    nonLocalRets.remove(left[3]);
    shiftedReturns.remove(left[3]);
    returnsConsumedBy.remove(left[3]);
    dataConsumedBy.remove(left[3]);
    if (collecting)
      dynamicInvocation.put(left[3], left[4]);
  }

  public int homeStackPointer() {
    return switchHomeSp;
  }

  public RegisterName trampolineRegister(int address) {
    int opcode = state.getMemory().read(address, 0);
    if (opcode == 0xE9)
      return RegisterName.HL;
    if ((opcode == 0xDD || opcode == 0xFD) && state.getMemory().read(address + 1 & 0xffff, 0) == 0xE9)
      return opcode == 0xDD ? RegisterName.IX : RegisterName.IY;
    return null;
  }

  public void forgetStack() {
    entries.clear();
    consumedReturns.clear();
    lastEvent = null;
    stackAsRepository = new StackAsRepositoryState();
    leftStacks.clear();
    leaving = reentered = null;
    lastStorePlace = -1;
  }

  public void init() {
    int start = state.getRegisterSP().read();
    for (int count = 0; start > 0 && count < 20; count += 2) {
      int address = start - count;
      int value = state.getMemory().read16Bits(address);
      if (value > 23296)
        entries.put(address, new Entry(value, -1, true));
    }
    initialized = true;
  }

  public record Saved(Entry[] window, Map<Integer, Entry> consumedReturns) {
  }

  public Saved copyEntries(int from, int length) {
    Entry[] copy = new Entry[length];
    for (int i = 0; i < length; i++)
      copy[i] = entries.get(from + i);
    return new Saved(copy, new HashMap<>(consumedReturns));
  }

  public void restoreEntries(int from, Saved saved) {
    for (int i = 0; i < saved.window().length; i++)
      if (saved.window()[i] == null)
        entries.remove(from + i);
      else
        entries.put(from + i, saved.window()[i]);
    consumedReturns.clear();
    consumedReturns.putAll(saved.consumedReturns());
  }

  private Entry entryAtSp() {
    return entries.get(state.getRegisterSP().read());
  }

  public void beforeExecution(Instruction instruction) {
    pcValue = state.getPc().read();
    if (!initialized)
      init();
    lastEvent = null;
    Entry top = entryAtSp();
    if (top != null && !top.returnAddress() && top.pc() != -1)
      dataOnTopAt.put(pcValue, top.pc());
    instruction.accept(new InstructionVisitor<>() {
      public void visitingPop(Pop pop) {
        Entry entry = entryAtSp();
        if (collecting && entry != null && !entry.returnAddress() && entry.pc() != -1) {
          recordedPops.add(pcValue);
          pushesTakenByPops.add(entry.pc());
        }
        if (entry != null && entry.returnAddress() && entry.pc() != -1 && (!recordedPops.contains(pcValue) || poppedCallSites.containsValue(pcValue)))
          lastEvent = l -> l.returnAddressPopped(pcValue, entry.value(), entry.pc());
      }

      public boolean visitingJP(JP jp) {
        if (jp.getPositionOpcodeReference() instanceof Register register) {
          int jumpAddress = register.read();
          int consumedSlot = state.getRegisterSP().read() - 2 & 0xffff;
          if (consumedReturns.containsKey(consumedSlot) && returningShifted(jp, jumpAddress, consumedReturns.get(consumedSlot))) {
            consumedReturns.remove(consumedSlot);
            return true;
          }
          addDynamicInvocationData(jumpAddress);
          Entry caller = entryAtSp();
          if (collecting && caller != null && caller.returnAddress() && state.getMemory().read16Bits(caller.pc() + 1 & 0xffff) == pcValue)
            calledThrough.put(caller.pc(), jumpAddress);
          int sp = state.getRegisterSP().read();
          if (sp >= 16384) {
            Entry entry = entries.get(sp);
            int top = state.getMemory().read16Bits(sp);
            if (entry != null && !entry.returnAddress() && Math.abs(top - pcValue) < 20) {
              lastEvent = l -> l.simulatedCall(pcValue, jumpAddress, getInvocationsSet(pcValue), top);
              simulatedCallsPcs.add(pcValue);
              simulatedRets.add(top);
            }
            return true;
          }
        }
        return false;
      }

      public void visitingLd(Ld ld) {
        ImmutableOpcodeReference source = ld.getSource();
        OpcodeReference target = ld.getTarget();
        if (source instanceof Register register && register.getName().equals(SP.name())) {
          stackAsRepository.spReadAt = pcValue;
          lastStorePlace = placeOf(target);
        }

        if (target instanceof Register register && register.getName().equals(SP.name())) {
          int newSpAddress = source.read();
          int oldSpAddress = register.read();
          int place = placeOf(source);
          int[] left = place == -1 ? null : leftStacks.remove(place);
          reentered = left != null && left[3] != -1 ? left : null;
          boolean repointing = lastStorePlace != -1 && place != lastStorePlace;
          leaving = collecting && repointing ? new int[]{pcValue, lastStorePlace, newSpAddress, -1, -1} : null;
          lastStorePlace = -1;
          callSinceLoad = false;
          if (stackSwitches.containsValue(pcValue)) {
            switchHomeSp = oldSpAddress;
            return;
          }
          if (distance(oldSpAddress, newSpAddress) > 2000) {
            if (distance(stackAsRepository.spReadAt, pcValue) < 2000)
              usingStackAsRepository(newSpAddress, oldSpAddress);
            stackResetTo = newSpAddress;
            returnsDropped = false;
          } else if (distance(oldSpAddress, newSpAddress) < 200 && !repointing)
            droppingReturnAddresses(oldSpAddress, newSpAddress);
        }
      }

      private void droppingReturnAddresses(int oldSpAddress, int newSpAddress) {
        Entry outermost = null;
        for (int address = oldSpAddress; address < newSpAddress; address += 2) {
          Entry entry = entries.get(address);
          if (entry != null && entry.returnAddress() && entry.pc() != -1)
            outermost = entry;
        }
        Entry dropped = outermost;
        if (dropped != null) {
          stackResetTo = newSpAddress;
          returnsDropped = true;
        }
        if (dropped != null)
          lastEvent = l -> l.droppingReturnValues(pcValue, newSpAddress, oldSpAddress, dropped);
      }

      private void usingStackAsRepository(int newSpAddress, int oldSpAddress) {
        if (!stackAsRepository.active) {
          stackAsRepository.active = true;
          stackAsRepository.lastSP = oldSpAddress;
          lastEvent = l -> l.beginUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
        } else if (distance(newSpAddress, stackAsRepository.lastSP) < 200) {
          int restoredSP = stackAsRepository.lastSP;
          lastEvent = l -> l.endUsingStackAsRepository(pcValue, restoredSP, oldSpAddress);
          stackAsRepository.clear();
        }
      }

      public boolean visitingRet(Ret ret) {
        if (ret instanceof RetN)
          return false;
        if (collecting && !ret.getCondition().conditionMet(ret))
          return true;
        lastStorePlace = -1;
        if (leaving != null && leaving[3] == -1 && !callSinceLoad && state.getRegisterSP().read() == leaving[2]) {
          int[] memory = state.getMemory().getData();
          leaving[3] = pcValue;
          leaving[4] = memory[leaving[2]] | memory[leaving[2] + 1 & 0xffff] << 8;
          leftStacks.put(leaving[1], leaving);
          leaving = null;
          if (reentered != null)
            confirmSwitch(reentered);
          reentered = null;
        }
        if (stackSwitches.containsKey(pcValue))
          return true;
        Entry entry = entryAtSp();
        boolean afterStackReset = state.getRegisterSP().read() == stackResetTo;
        stackResetTo = -1;
        Entry popped = consumedReturns.get(state.getRegisterSP().read() - 2 & 0xffff);
        boolean pastPoppedReturn = entry != null && entry.returnAddress() && popped != null && codeVersions.isVersioned(popped.pc());
        if (collecting && (pastPoppedReturn || afterStackReset && (returnsDropped ? entry != null && entry.returnAddress() : entry == null && knowsWholeStack)))
          nonLocalRets.put(pcValue, state.getRegisterSP().read());
        if (entry == null && afterStackReset && knowsWholeStack)
          jumpingUsingRet(ret, state.getMemory().read16Bits(state.getRegisterSP().read()), null);
        else if (entry == null)
          lastEvent = l -> l.returningToUnknownAddress(pcValue);
        else if (!entry.returnAddress() && !simulatedRets.contains(entry.value()))
          jumpingUsingRet(ret, entry.value(), consumedReturns.get(state.getRegisterSP().read()));
        return true;
      }
    });

    if (lastEvent != null && stackListener != null)
      lastEvent.apply(stackListener);
  }

  private void jumpingUsingRet(Ret ret, int target, Entry consumedReturn) {
    if (consumedReturn != null && returningShifted(ret, target, consumedReturn))
      return;
    addDynamicInvocationData(target);
    Set<Integer> targets = getInvocationsSet(pcValue);
    if (!targets.isEmpty())
      lastEvent = l -> l.jumpUsingRet(ret, pcValue, targets);
  }

  private boolean returningShifted(Instruction instruction, int target, Entry consumedReturn) {
    Integer continuation = collecting ? ((target - consumedReturn.value() & 0xffff) < 256 ? target : null) : callContinuations.get(consumedReturn.pc());
    if (continuation == null || !learnContinuation(pcValue, consumedReturn.pc(), continuation))
      return false;
    lastEvent = l -> l.returnShifted(instruction, pcValue, continuation, consumedReturn.pc());
    return true;
  }

  private boolean learnContinuation(int returnPc, int callSite, int continuation) {
    Integer known = callContinuations.get(callSite);
    if (known != null && known != continuation)
      jumpTableAt(callSite);
    if (jumpTableSites.contains(callSite))
      return false;
    callContinuations.put(callSite, continuation);
    shiftedReturns.put(returnPc, callSite);
    return true;
  }

  private void jumpTableAt(int callSite) {
    if (!jumpTableSites.add(callSite))
      return;
    Integer known = callContinuations.remove(callSite);
    shiftedReturns.entries().stream().filter(e -> e.getValue() == callSite).toList().forEach(e -> {
      shiftedReturns.removeMapping(e.getKey(), e.getValue());
      if (known != null)
        dynamicInvocation.put(e.getKey(), known);
    });
  }

  private void addDynamicInvocationData(int address) {
    if (collecting && (address >= 16384 || pcValue < 16384))
      dynamicInvocation.put(pcValue, address);
  }

  private int distance(int oldSpAddress, int newSpAddress) {
    return Math.abs(oldSpAddress - newSpAddress);
  }

  public void afterExecution(Instruction instruction) {
    instruction.accept(new InstructionVisitor<>() {
      public boolean visitingCall(Call call) {
        if (call.getNextPC() != -1)
          remember(true);
        return true;
      }

      public void visitPush(Push push) {
        remember(false);
      }

      public void visitEx(Ex ex) {
        if (!(ex.getTarget() instanceof Register))
          remember(false);
      }

      public boolean visitingRet(Ret ret) {
        int nextPC = ret.getNextPC();
        if (ret instanceof RetN || nextPC == -1 || stackSwitches.containsKey(pcValue))
          return false;
        Entry consumedReturn = consumedReturns.get(poppedSlot());
        Entry entry = entries.remove(poppedSlot());
        if (entry != null && entry.returnAddress())
          returnsConsumedBy.put(pcValue, entry.pc());
        if (entry != null && !entry.returnAddress()) {
          if (entry.pc() != -1 && (collecting || !learnedFromRecording))
            dataConsumedBy.put(pcValue, entry.pc());
          consumedReturns.remove(poppedSlot());
          if (!simulatedRets.contains(nextPC))
            jumpingUsingRet(ret, nextPC, consumedReturn);
        }
        return true;
      }

      public void visitingPop(Pop pop) {
        int slot = poppedSlot();
        Entry entry = entries.remove(slot);
        if (entry != null && entry.returnAddress()) {
          consumedReturns.put(slot, entry);
          if (entry.pc() != -1)
            poppedCallSites.put(entry.pc(), pcValue);
        }
      }

      private int poppedSlot() {
        return state.getRegisterSP().read() - 2 & 0xFFFF;
      }
    });
  }

  private void remember(boolean returnAddress) {
    callSinceLoad |= returnAddress;
    int sp = state.getRegisterSP().read();
    Entry entry = new Entry(state.getMemory().read16Bits(sp), state.getPc().read(), returnAddress);
    entries.put(sp, entry);
    consumedReturns.remove(sp - 2 & 0xffff);
    if (returnAddress && collecting)
      returnSlots.put(sp, entry.pc());
    if (!returnAddress)
      pushedValues.put(entry.pc(), entry.value());
  }

  public boolean returnPoppedBelow(int sp) {
    return consumedReturns.containsKey(sp - 2 & 0xffff);
  }

  public void forgetLearned() {
    dynamicInvocation.clear();
    stackSwitches.clear();
    leftStacks.clear();
    leaving = reentered = null;
    lastStorePlace = -1;
    callContinuations.clear();
    shiftedReturns.clear();
    dataConsumedBy.clear();
    dataOnTopAt.clear();
    poppedCallSites.clear();
    returnsConsumedBy.clear();
    pushedValues.clear();
    calledThrough.clear();
    jumpTableSites.clear();
    pushesTakenByPops.clear();
    nonLocalRets.clear();
    returnSlots.clear();
    learnedFromRecording = false;
    codeVersions = new CodeVersions();
  }

  public void learnFrom(StackAnalyzer recorded) {
    learnedFromRecording = true;
    codeVersions.addAll(recorded.codeVersions);
    dynamicInvocation.putAll(recorded.dynamicInvocation);
    recorded.jumpTableSites.forEach(this::jumpTableAt);
    recorded.shiftedReturns.entries().forEach(e -> learnContinuation(e.getKey(), e.getValue(), recorded.callContinuations.get(e.getValue())));
    recordedPops.addAll(recorded.recordedPops);
    pushesTakenByPops.addAll(recorded.pushesTakenByPops);
    stackSwitches.putAll(recorded.stackSwitches);
    dataConsumedBy.putAll(recorded.dataConsumedBy);
    dataOnTopAt.putAll(recorded.dataOnTopAt);
    poppedCallSites.putAll(recorded.poppedCallSites);
    returnsConsumedBy.putAll(recorded.returnsConsumedBy);
    pushedValues.putAll(recorded.pushedValues);
    calledThrough.putAll(recorded.calledThrough);
    nonLocalRets.putAll(recorded.nonLocalRets);
    returnSlots.putAll(recorded.returnSlots);
  }

  public void learnFromForks(StackAnalyzer forked, Set<Integer> recordedSites) {
    forked.dynamicInvocation.keySet().removeAll(recordedSites);
    forked.dataConsumedBy.keySet().removeAll(recordedSites);
    learnFrom(forked);
  }

  public Set<Integer> getInvocationsSet(int pcValue1) {
    return new HashSet<>(dynamicInvocation.get(pcValue1));
  }

  public boolean listenEvents(StackListener stackListener) {
    return lastEvent != null && lastEvent.apply(stackListener);
  }

  public void addExecutionListener(InstructionExecutor instructionExecutor) {
    instructionExecutor.setExecutionListener(new ExecutionListener() {
      public void beforeExecution(Instruction instruction) {
        StackAnalyzer.this.beforeExecution(instruction);
      }

      public void afterExecution(Instruction instruction) {
        StackAnalyzer.this.afterExecution(instruction);
      }
    });
  }

  public void addEventListener(StackListener stackListener) {
    this.stackListener = stackListener;
  }
}
