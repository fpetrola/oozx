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
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.se.StackListener;
import com.fpetrola.z80.spy.ExecutionListener;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.*;
import java.util.function.Function;

import static com.fpetrola.z80.registers.RegisterName.SP;

public class StackAnalyzer {
  public record Entry(int value, int pc, boolean returnAddress) {
  }

  private State state;
  private Function<StackListener, Boolean> lastEvent;
  private boolean initialized;
  private StackAsRepositoryState stackAsRepository = new StackAsRepositoryState();
  private StackListener stackListener;
  public MultiValuedMap<Integer, Integer> dynamicInvocation = new HashSetValuedHashMap<>();
  public final Map<Integer, Integer> callContinuations = new HashMap<>();
  public final Set<Integer> shiftedReturns = new HashSet<>();
  public final MultiValuedMap<Integer, Integer> dataConsumedBy = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> dataOnTopAt = new HashSetValuedHashMap<>();
  public final Set<Integer> poppedCallSites = new HashSet<>();
  public final Set<Integer> returnsConsumedBy = new HashSet<>();
  public final MultiValuedMap<Integer, Integer> pushedValues = new HashSetValuedHashMap<>();
  public static boolean collecting;
  private int pcValue;
  private final List<Integer> simulatedRets = new ArrayList<>();
  private final List<Integer> simulatedCallsPcs = new ArrayList<>();
  private final Map<Integer, Entry> entries = new HashMap<>();
  private final Map<Integer, Entry> consumedReturns = new HashMap<>();
  private final MemoryWriteListener forgetOverwritten = (address, value) -> {
    entries.remove(address);
    entries.remove((address - 1) & 0xFFFF);
  };

  public StackAnalyzer(State state) {
    reset(state);
  }

  public List<Integer> getSimulatedCallsPcs() {
    return simulatedCallsPcs;
  }

  public void setCollecting(boolean collecting) {
    StackAnalyzer.collecting = collecting;
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

  public Entry[] copyEntries(int from, int length) {
    Entry[] copy = new Entry[length];
    for (int i = 0; i < length; i++)
      copy[i] = entries.get(from + i);
    return copy;
  }

  public void restoreEntries(int from, Entry[] saved) {
    for (int i = 0; i < saved.length; i++)
      if (saved[i] == null)
        entries.remove(from + i);
      else
        entries.put(from + i, saved[i]);
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
        if (entry != null && entry.returnAddress())
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
          int sp = state.getRegisterSP().read();
          if (sp >= 16384) {
            Entry entry = entries.get(sp);
            int top = state.getMemory().read16Bits(sp);
            if ((entry == null || !entry.returnAddress()) && Math.abs(top - pcValue) < 20) {
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
        if (source instanceof Register register && register.getName().equals(SP.name()))
          stackAsRepository.spReadAt = pcValue;

        if (target instanceof Register register && register.getName().equals(SP.name())) {
          int newSpAddress = source.read();
          int oldSpAddress = register.read();
          if (distance(oldSpAddress, newSpAddress) > 2000) {
            if (distance(stackAsRepository.spReadAt, pcValue) < 2000)
              usingStackAsRepository(newSpAddress, oldSpAddress);
          } else if (distance(oldSpAddress, newSpAddress) < 200)
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
        Entry entry = entryAtSp();
        if (entry == null)
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
    if (continuation == null)
      return false;
    callContinuations.put(consumedReturn.pc(), continuation);
    shiftedReturns.add(pcValue);
    lastEvent = l -> l.returnShifted(instruction, pcValue, continuation, consumedReturn.pc());
    return true;
  }

  private void addDynamicInvocationData(int address) {
    if (collecting && address >= 16384)
      dynamicInvocation.put(pcValue, address);
  }

  private int distance(int oldSpAddress, int newSpAddress) {
    return Math.abs(oldSpAddress - newSpAddress) & 0xffff;
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
        if (ret instanceof RetN || nextPC == -1)
          return false;
        Entry consumedReturn = consumedReturns.get(poppedSlot());
        Entry entry = entries.remove(poppedSlot());
        if (entry != null && entry.returnAddress())
          returnsConsumedBy.add(pcValue);
        if (entry != null && !entry.returnAddress()) {
          if (entry.pc() != -1)
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
            poppedCallSites.add(entry.pc());
        }
      }

      private int poppedSlot() {
        return state.getRegisterSP().read() - 2 & 0xFFFF;
      }
    });
  }


  private void remember(boolean returnAddress) {
    int sp = state.getRegisterSP().read();
    Entry entry = new Entry(state.getMemory().read16Bits(sp), state.getPc().read(), returnAddress);
    entries.put(sp, entry);
    if (!returnAddress)
      pushedValues.put(entry.pc(), entry.value());
  }

  public void forgetLearned() {
    shiftedReturns.clear();
    dataConsumedBy.clear();
    dataOnTopAt.clear();
    poppedCallSites.clear();
    returnsConsumedBy.clear();
    pushedValues.clear();
  }

  public void learnFrom(StackAnalyzer recorded) {
    dynamicInvocation.putAll(recorded.dynamicInvocation);
    callContinuations.putAll(recorded.callContinuations);
    shiftedReturns.addAll(recorded.shiftedReturns);
    dataConsumedBy.putAll(recorded.dataConsumedBy);
    dataOnTopAt.putAll(recorded.dataOnTopAt);
    poppedCallSites.addAll(recorded.poppedCallSites);
    returnsConsumedBy.addAll(recorded.returnsConsumedBy);
    pushedValues.putAll(recorded.pushedValues);
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
