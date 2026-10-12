/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.routines.CodeVersions;
import com.fpetrola.z80.routines.RoutineManager;

import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.bytecode.generators.helpers.*;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.tstates.UncontendedTiming;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineVisitor;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.MultiSet;
import org.cojen.maker.Field;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.*;
import java.util.stream.Stream;
import java.util.function.Supplier;

public class RoutineBytecodeGenerator {
  public final BytecodeGenerationContext context;
  public Map<String, Variable> registers = new HashMap<>();
  public Field memory;
  public MethodMaker mm;
  private Label start, unwound;
  private Set<Integer> landings;
  private ControlFlowStructuring structuring;
  /** The call being generated: an unwinding to its return address goes on after it. */
  public Integer resumeAddress;
  public Label resumeLabel;
  /** Call sites whose unwinding is being handled: code there is outside what it handles, as a catch is outside its try. */
  private final Set<Integer> handling = new HashSet<>();
  /** Where the handling of an unwinding at a call site starts: code run by that handling that unwinds again to the same call site goes back there. */
  private final Map<Integer, Label> handlerStarts = new HashMap<>();
  private final Map<java.lang.Integer, Label> labels = new HashMap<>();
  protected final Set<java.lang.Integer> positionedLabels = new HashSet<>();
  public final Routine routine;
  public Register lastMemPc = new Plain16BitRegister("lastMemPc");

  public Map<String, Variable> variables = new HashMap<>();
  public Map<String, Register> registerByVariable = new HashMap<>();
  public Map<Register, Variable> variablesByRegister = new HashMap<>();
  protected final Map<java.lang.Integer, Label> insertLabels = new HashMap<>();
  public Instruction currentInstruction;
  public Register currentRegister;

  public static <S> S getRealVariable(S variable) {
    Object variable1 = variable;
    if (variable1 instanceof VariableDelegator)
      variable1 = ((Variable) variable1).get();
    return (S) variable1;
  }

  public RoutineBytecodeGenerator(BytecodeGenerationContext context, Routine routine) {
    this.context = context;
    this.routine = routine;
  }

  public static String createLabelName(int label) {
    return "$" + Helper.formatAddress(label);
  }

  public void generate() {
    mm = getMethod(routine.getEntryPoint());
    addInstructions();
    returnFromMethod();
  }

  protected void addVariables() {
    Arrays.stream(RegisterName.values()).forEach(n -> addField(n.name()));
  }

  private void addInstructions() {
    List<InstructionGenerator> generators = new ArrayList<>();

    addVariables();
    addField("nextAddress");
    memory = mm.field("mem");
    registers.put("mem", memory);

    List<Routine> routines = routine.getRoutineManager().getRoutines();

    routine.accept(new RoutineVisitor<java.lang.Integer>() {
      public void visitInstruction(int address, Instruction instruction) {
        {
          boolean contains = routine.contains(address);
          if (contains) {
            context.pc.write(address);
            int firstAddress = address;

            Runnable scopeAdjuster = () -> {
              context.pc.write(address);
//                new InstructionActionExecutor<>(r -> r.adjustRegisterScope()).executeAction(instruction);
            };

            Runnable labelGenerator = () -> {
              context.pc.write(address);
              JumpLabelVisitor jumpLabelVisitor1 = new JumpLabelVisitor();
              instruction.accept(jumpLabelVisitor1);
              addLabel(address);
            };
            Runnable instructionGenerator = () -> {
              context.pc.write(address);

              if (address == 0xB94E)
                System.out.print("");

              currentInstruction = instruction;
              generateInstruction(address, instruction, firstAddress);

              int nextAddress = continuation(address);
              if (fallsThrough(address, instruction) && routine.contains(nextAddress) && getLabel(nextAddress) == null) {
                mm.invoke("untranslated", nextAddress);
                returnFromMethod();
              }
              if (fallsThrough(address, instruction) && routine.contains(nextAddress) && getLabel(nextAddress) != null && (structuring.headerFor(address, nextAddress) != -1 || nextAddress <= address || java.util.stream.IntStream.range(address + 1, nextAddress).anyMatch(a -> routine.contains(a) && context.routineManager.getInstructionAt(a) != null && context.routineManager.getInstructionAt(a) != instruction)))
                goTo(address, nextAddress);
              Routine continuationOwner = context.routineManager.findRoutineAt(nextAddress);
              if (fallsThrough(address, instruction) && !routine.contains(nextAddress) && continuationOwner != null && (continuationOwner.getEntryPoint() == nextAddress ? !continuationOwner.isVirtual() : context.routineManager.isEnteredFromOutside(continuationOwner, nextAddress)))
                tailJump(nextAddress, address);
              if (fallsThrough(address, instruction) && routines.stream().anyMatch(routine1 -> routine1.isVirtual() && routine1 != routine && routine1.getEntryPoint() == nextAddress))
                tailJump(nextAddress, address);
              if (fallsThrough(address, instruction) && !(instruction instanceof Call) && !routine.contains(nextAddress) && continuationOwner == null) {
                mm.invoke("untranslated", nextAddress);
                returnFromMethod();
              }

            };

            generators.add(new InstructionGenerator(scopeAdjuster, labelGenerator, instructionGenerator));
          }
        }
      }

      private void generateInstruction(int address, Instruction instruction, int firstAddress) {
        lastMemPc.write(address);

        int label = -1;
        if (getLabel(address) != null) {
          label = firstAddress;
          hereLabel(label);
        }
        ControlFlowStructuring.Region region = structuring.regions().get(address);
        if (region != null)
          region.entries().forEach(entry -> mm.invoke("isNextPC", entry).ifTrue(getLabel(entry)::goto_));

        List<RoutineManager.CodeVariant> variants = context.routineManager.codeVariantsAt(address);
        if (!variants.isEmpty()) {
          Variable hash = mm.invoke("codeHash", variants.get(0).variableStart(), variants.get(0).variableBytes().length);
          variants.forEach(v -> hash.ifEq(v.hash(), () -> tailJump(v.relocated(address), -1)));
          mm.invoke("unknownCodeVariant", address, variants.get(0).variableStart(), variants.get(0).variableBytes().length);
        }
        List<int[]> versions = codeVersions().instructionVersions(address);
        if (!versions.isEmpty()) {
          invokePcUncharged(address);
          Variable hash = mm.invoke("codeHash", address, instruction.getLength());
          Label done = mm.label();
          versions.forEach(bytes -> hash.ifEq(Arrays.hashCode(bytes), () -> {
            Instruction version = codeVersions().decode(address, bytes);
            chargeTstates(UncontendedTiming.costOf(bytes)[version instanceof RepeatingInstruction ? 1 : 0]);
            mm.invoke("fetched", ((AbstractInstruction) version).getRDelta());
            version.accept(new InstructionsBytecodeGenerator(mm, RoutineBytecodeGenerator.this, address));
            if (RoutineManager.fallsThrough(version))
              done.goto_();
          }));
          executeMutantCode(address, instruction);
          done.here();
        } else if (mutantCodeInInstruction(instruction, address)) {
          invokePcUncharged(address);
          executeMutantCode(address, instruction);
        } else if (routine.getVirtualPop().containsKey(address) && routine.getVirtualPop().get(address) == address) {
          throwAtVirtualPop(address);
          returnFromMethod();
        } else {
          invokePc(address);
          InstructionsBytecodeGenerator instructionsBytecodeGenerator = new InstructionsBytecodeGenerator(mm, RoutineBytecodeGenerator.this, address);
          instruction.accept(instructionsBytecodeGenerator);

          if (!instructionsBytecodeGenerator.incPopsAdded && routine.getVirtualPop().containsKey(address)) {
            throwAfterVirtualPop(address);
            returnFromMethod();
          }
        }
      }

    });

    mm.nop();
    start = mm.label();
    start.here();
    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.labelGenerator().run());

    List<Integer> entries = new ArrayList<>(labels.keySet()).stream().filter(address -> routine.contains(address) && address != routine.getEntryPoint() && (context.routineManager.isEnteredFromOutside(routine, address) || context.routineManager.callers.get(address).stream().anyMatch(caller -> !routine.contains(caller)) || landings().contains(address))).toList();
    context.dispatchedEntries.addAll(entries);
    structuring = new ControlFlowStructuring(routine, context.routineManager, entries, this::continuation);
    entries.forEach(address -> mm.invoke("isNextPC", address).ifTrue(() -> goTo(-1, address)));

    Label label = getLabel(routine.getEntryPoint());
    if (label != null)
      label.goto_();
    generators.forEach(g -> g.instructionGenerator().run());

    positionedLabels.forEach(l -> labels.get(l).here());
    returnFromMethod();
    emitUnwound();


  }

  private boolean mutantCodeInInstruction(Instruction instruction, int address) {
    Set<java.lang.Integer> mutantAddress = codeVersions().mutant();
    Set<java.lang.Integer> operands = CodeVersions.operandOffsets(instruction);
    return java.util.stream.IntStream.range(0, instruction.getLength()).anyMatch(i -> mutantAddress.contains(address + i) && !operands.contains(i));
  }

  private void executeMutantCode(int address, Instruction instruction) {
    Variable executedUpTo = mm.invoke("executeMutantCode", address);
    afterInvoke();
    int next = address + instruction.getLength() & 0xffff;
    Runnable leave = () -> {
      for (int inside = address + 1; inside < address + instruction.getLength(); inside++)
        if (getLabel(inside) != null) {
          Label overlapping = getLabel(inside);
          executedUpTo.ifEq(inside, overlapping::goto_);
        }
      int target = instruction instanceof Call ? -1 : RoutineManager.fixedJumpTarget(instruction);
      if (target != -1 && getLabel(target) != null)
        executedUpTo.ifEq(target, getLabel(target)::goto_);
      executedUpTo.ifNe(-1, () -> {
        mm.invoke("goOnFrom", executedUpTo);
        afterInvoke();
      });
      returnFromMethod();
    };
    if (fallsThrough(address, instruction))
      executedUpTo.ifNe(next, leave);
    else {
      if (getLabel(next) != null)
        executedUpTo.ifEq(next, getLabel(next)::goto_);
      leave.run();
    }
  }

  private CodeVersions codeVersions() {
    return stackAnalyzer().codeVersions;
  }

  protected void addField(String name) {
    // cm.addField(int.class, name).private_().static_();
    Variable field = mm.field(name);
    registers.put(name, field);

    if (name.length() == 2 || name.equals("R") || true) field = new Composed16BitRegisterVariable(mm, name, context);

    variables.put(name, field);
  }

  public Label addLabel(int labelLine) {
    Label label = labels.get(labelLine);
    if (label == null) {
      label = mm.label();
      labels.put(labelLine, label);
      positionedLabels.add(labelLine);

      if (insertLabels.get(labelLine) == null) {
        insertLabels.put(labelLine, mm.label());
      }
    }

    return label;
  }

  public Label getLabel(int i) {
    return labels.get(i);
  }

  public void hereLabel(int labelName) {
    Label insertLabel = insertLabels.get(labelName);
    if (insertLabel != null) insertLabel.here();

    Label label = getLabel(labelName);
    if (label == null) {
      label = addLabel(labelName);
    }

    label.here();
    positionedLabels.remove(labelName);

  }

  public MethodMaker getMethod(int address) {
    return createMethod(address);
  }

  public MethodMaker createMethod(int address) {
    return findOrCreateMethodAt(address);
  }

  public MethodMaker findOrCreateMethodAt(int address) {
    String methodName = createLabelName(address);
    MethodMaker methodMaker = context.methods.get(methodName);

    if (methodMaker == null) {
      methodMaker = createMethod(address, methodName);
    }

    context.methods.put(methodName, methodMaker);

    return methodMaker;
  }

  protected MethodMaker createMethod(int address, String methodName) {
    Routine owner = context.routineManager.findRoutineAt(address);
    return context.cm.addMethod(isJumpMember(owner) && owner.getEntryPoint() == address ? int.class : void.class, methodName).public_();
  }

  public  Variable getField(String name) {
    return registers.get(name);
  }

  public Variable setVariable(String name, Supplier<Object> value) {
    Variable var = mm.var(int.class);
    var.name(name);
    Variable set = doSetValue(value, var);
    return set;
  }

  protected Variable doSetValue(Supplier<Object> value, Variable var) {
    Variable set = var;
    Object value1 = getRealVariable(value.get());
    if (value1 != null) set = var.set(value1);
    return set;
  }

  public  Variable getExistingVariable(Register register) {
    String registerName = register.getName().replace(",", "");

    Variable variable1 = variables.get(registerName);
    if (variable1 instanceof VariableDelegator variable) {
      variable.setRegister(register);
    }
    currentRegister = register;
    return variable1;
  }

  public Variable getVariableFromMemory(Object variable, String bits) {
    List<Object> params = new ArrayList<>();
    params.add(getRealVariable(variable));
    params.add(lastMemPc.read());
    addOtherMemSyncParameters(params);
    return mm.invoke("mem" + bits, params.toArray());
  }

  public void writeVariableToMemory(Object o, Object variable, String bits) {
    List<Object> params = new ArrayList<>();
    params.add(getRealVariable(variable));
    params.add(getRealVariable(o));
    params.add(lastMemPc.read());
    addOtherMemSyncParameters(params);
    mm.invoke("wMem" + bits, params.toArray());
  }

  protected void addOtherMemSyncParameters(List<Object> params) {
  }

  public Variable getExistingVariable(String hl) {
    return variables.get(hl);
  }

  public void jumpInto(int target) {
    inPagedBank(target, this::enter);
  }

  /** From code outside the paged window, a target where another bank has code is reached at that bank's copy while the bank is paged; code relocated out of a bank always reaches its own copy. */
  public void inPagedBank(int target, java.util.function.IntConsumer reach) {
    Label done = mm.label();
    List<RoutineManager.CodeVariant> banked = inOwnBank(target) == target && routine.getEntryPoint() < com.fpetrola.z80.memory.MemoryBanks.WINDOW ? context.routineManager.bankVariantsAt(target) : List.of();
    Variable bank = banked.isEmpty() ? null : mm.invoke("bank");
    banked.forEach(variant -> bank.ifEq(variant.bank(), () -> {
      reach.accept(variant.relocated(target));
      done.goto_();
    }));
    reach.accept(inOwnBank(target));
    done.here();
  }

  private void enter(int address) {
    Routine owner = context.routineManager.findRoutineAt(address);
    if (owner != null && context.routineManager.isEnteredFromOutside(owner, address)) {
      mm.invoke("setNextAddress", address);
      invokeTransformedMethod(owner.getEntryPoint());
    } else if (owner != null)
      invokeTransformedMethod(address);
    else if (stackAnalyzer().trampolineRegister(address) != null)
      throughTrampoline(address, stackAnalyzer().trampolineRegister(address), stackAnalyzer().dynamicInvocation.get(address));
    else
      mm.invoke("untranslated", address);
  }

  /** An instruction that only jumps through a register (the ROM's JP (HL) at 006F, CALL-JUMP's at 162C) goes on at the targets recorded for it. */
  public void throughTrampoline(int trampoline, RegisterName register, Collection<Integer> targets) {
    invokePc(trampoline, register == RegisterName.HL ? 1 : 2, register == RegisterName.HL ? 4 : 8);
    dispatch(mm.invoke(register.name()), targets.stream());
  }

  private void dispatch(Variable address, Stream<Integer> knownTargets) {
    Label reached = mm.label();
    knownTargets.distinct().sorted().forEach(target -> address.ifEq(target, () -> {
      jumpInto(target);
      reached.goto_();
    }));
    mm.invoke("jump", address);
    afterInvoke();
    reached.here();
  }

  private int inOwnBank(int address) {
    return context.routineManager.bankVariantsAt(address).stream().filter(v -> routine.getEntryPoint() >= v.relocatedAt() && routine.getEntryPoint() < v.relocated(v.end())).findFirst().map(v -> v.relocated(address)).orElse(address);
  }

  public void tailJump(int target, int site) {
    int address = inOwnBank(target);
    Routine owner = context.routineManager.findRoutineAt(address);
    if (owner != null && context.routineManager.callersWhoseReturnIsTaken(routine, stackAnalyzer()).contains(owner) && context.ancestorJumps().goesBack(routine, owner)) {
      mm.invoke("unwind", address);
      returnFromMethod();
    } else if (isJumpMember(routine) && isJumpMember(owner) && (site == -1 || context.routineManager.ownDataLeftForOthers(site, routine, stackAnalyzer()).isEmpty())) {
      if (owner.getEntryPoint() != address)
        mm.invoke("setNextAddress", address);
      mm.return_(owner.getEntryPoint());
    } else {
      jumpInto(address);
      if (site != -1)
        leaveWithOwnData(site);
      returnFromMethod();
    }
  }

  private boolean isJumpMember(Routine candidate) {
    return candidate != null && context.routinesInJumpCycles().contains(candidate);
  }

  public Variable invokeTransformedMethod(int jumpLabel) {
    String labelName = createLabelName(jumpLabel);
    Variable invoke = null;
    try {
      Routine target = context.routineManager.findRoutineAt(jumpLabel);
      int entry = jumpLabel;
      if (target != null && target.getEntryPoint() != jumpLabel && !context.methods.containsKey(labelName)) {
        mm.invoke("setNextAddress", jumpLabel);
        entry = target.getEntryPoint();
        labelName = createLabelName(entry);
        context.dispatchedEntries.add(jumpLabel);
      }
      boolean chain = isJumpMember(target) && target.getEntryPoint() == entry;
      invoke = chain ? mm.invoke("runJumps", entry) : mm.invoke(labelName);
      if (chain || context.mayUnwind(target))
        afterInvoke();
    } catch (Exception e) {
      System.out.println("not found: " + labelName + " (" + e + ")" + " from " + routine + " at " + Helper.formatAddress(context.pc.read()) + " callers " + context.routineManager.callers.get(jumpLabel) + " owner " + context.routineManager.findRoutineAt(jumpLabel));
    }
    return invoke;
  }

  private boolean fallsThrough(int address, Instruction instruction) {
    return RoutineManager.fallsThrough(instruction) || codeVersions().instructionVersions(address).stream().anyMatch(bytes -> RoutineManager.fallsThrough(codeVersions().decode(address, bytes)));
  }

  private void invokePc(int address) {
    if (context.routineManager.getInstructionAt(address) instanceof AbstractInstruction instruction)
      invokePc(address, instruction.getRDelta());
  }

  public void invokePc(int address, int rDelta) {
    invokePc(address, rDelta, costOf(address)[context.routineManager.getInstructionAt(address) instanceof RepeatingInstruction ? 1 : 0]);
  }

  public void invokePc(int address, int rDelta, int cost) {
    if (!context.direct)
      mm.invoke("pc", context.routineManager.originalAddress(address), rDelta, cost);
  }

  public void chargeTstates(int tstates) {
    if (!context.direct && tstates != 0)
      mm.invoke("tstates", tstates);
  }

  /** The site charges nothing itself: whichever version or fallback runs there charges its own cost. */
  private void invokePcUncharged(int address) {
    invokePc(address, 0, 0);
  }

  /** The T-states of the instruction at the address on its cheapest and dearest path, measured on the emulator's own timing model. */
  public int[] costOf(int address) {
    Instruction instruction = context.routineManager.getInstructionAt(address);
    if (instruction == null)
      return new int[]{0, 0};
    int[] bytes = new int[instruction.getLength()];
    for (int i = 0; i < bytes.length; i++)
      bytes[i] = context.symbolicExecutionAdapter.state.getMemory().read(address + i & 0xffff, 0);
    return UncontendedTiming.costOf(bytes);
  }

  public boolean virtualPopOnBranch(int address) {
    Integer pop = routine.getVirtualPop().get(address);
    return pop != null && pop != context.routineManager.addressAfter(address);
  }

  private void loadPoppedReturnAddress(int returnPoint, int callSite) {
    for (int length = 1; length <= 2; length++)
      if (context.routineManager.getInstructionAt(returnPoint - length) instanceof Pop pop && pop.getLength() == length)
        getExistingVariable((Register) pop.getTarget()).set(pushesReturnAddress(callSite) ? mm.invoke("pop") : context.routineManager.originalAddress(context.routineManager.addressAfter(callSite)));
  }

  /** After code that may have left this frame: an unwinding that stops here goes on where it says, any other leaves this routine too. */
  public void afterInvoke() {
    mm.invoke("unwinding").ifTrue(this::stopUnwindingHere);
  }

  private void stopUnwindingHere() {
    int key = context.pc.read();
    Collection<Integer> points = catchPoints().get(key);
    if (!points.isEmpty() && handling.contains(key))
      handlerStarts.get(key).goto_();
    else if (!points.isEmpty()) {
      handling.add(key);
      Label handlerStart = mm.label().here();
      handlerStarts.put(key, handlerStart);
      Variable own = mm.invoke("ownAddress", addresses(points));
      points.forEach(i -> own.ifEq(i, () -> {
        loadPoppedReturnAddress(i, key);
        Label target = getLabel(i);
        if (target != null)
          target.goto_();
        else if (context.routineManager.getInstructionAt(i) instanceof Ret ret && ret.getCondition() instanceof ConditionAlwaysTrue) {
          invokePc(i);
          returnFromMethod();
        } else if (context.routineManager.getInstructionAt(i) instanceof JP jp && jp.getCondition() instanceof ConditionAlwaysTrue && getLabel(RoutineManager.fixedJumpTarget(jp)) != null) {
          invokePc(i);
          getLabel(RoutineManager.fixedJumpTarget(jp)).goto_();
        } else if (context.routineManager.getInstructionAt(i) instanceof Pop && context.routineManager.findRoutineAt(i) == null) {
          invokePc(i);
          mm.invoke("unwind", context.routineManager.addressAfter(i));
          returnFromMethod();
        } else
          tailJump(i, -1);
      }));
      handling.remove(key);
      handlerStarts.remove(key);
    }
    if (resumeAddress != null && landings().contains(resumeAddress))
      mm.invoke("ownAddress", addresses(List.of(resumeAddress))).ifNe(-1, resumeLabel::goto_);
    if (entriesBackFromUnwinding().isEmpty() && elsewhere().isEmpty())
      returnFromMethod();
    else {
      if (unwound == null)
        unwound = mm.label();
      unwound.goto_();
    }
  }

  /** The one place where an unwinding that reached this routine's frame finds out whether it comes back into it, as the catch around the whole method did. */
  private void emitUnwound() {
    if (unwound == null)
      return;
    unwound.here();
    if (!elsewhere().isEmpty()) {
      Variable own = mm.invoke("ownAddress", addresses(elsewhere()));
      elsewhere().forEach(at -> own.ifEq(at, () -> tailJump(at, -1)));
    }
    if (!entriesBackFromUnwinding().isEmpty())
      mm.invoke("isOwnAddress", addresses(entriesBackFromUnwinding())).ifTrue(start::goto_);
    returnFromMethod();
  }

  private List<Integer> elsewhere() {
    return landings().stream().filter(at -> getLabel(at) == null && context.routineManager.findRoutineAt(at) != null).sorted().toList();
  }

  private Set<Integer> entriesBackFromUnwinding() {
    Set<Integer> entries = new TreeSet<>(routine.getReturnPointsDropped().values());
    landings().stream().filter(at -> getLabel(at) != null).forEach(entries::add);
    return entries;
  }

  /** Where code that left this routine's frame comes back to it: the targets of jumps back from routines it calls, and the returns of its own calls that a RET leaving other frames goes back to. */
  private Set<Integer> landings() {
    if (landings == null) {
      landings = new HashSet<>(context.ancestorJumps().landings().getOrDefault(routine, Set.of()));
      landings.addAll(context.routineManager.returnsOfCallsInTakenByNonLocalReturns(routine, stackAnalyzer()));
    }
    return landings;
  }

  /** Where the code goes on after the instruction at this address when it falls through. */
  private int continuation(int address) {
    Instruction instruction = context.routineManager.getInstructionAt(address);
    int next = address + instruction.getLength() & 0xffff;
    return instruction instanceof Call ? stackAnalyzer().callContinuations.getOrDefault(address, next) : next;
  }

  /** A jump inside this routine: into a region entered in more than one place, through its header with nextAddress set. */
  public void goTo(int from, int target) {
    int header = structuring.headerFor(from, target);
    if (header == -1)
      getLabel(target).goto_();
    else {
      mm.invoke("setNextAddress", target);
      getLabel(header).goto_();
    }
  }

  private Variable addresses(Collection<Integer> values) {
    Variable array = mm.new_(int[].class, values.size());
    int slot = 0;
    for (int value : values)
      array.aset(slot++, value);
    return array;
  }

  public void throwAfterVirtualPop(int address) {
    throwAtVirtualPop(routine.getVirtualPop().get(address));
  }

  private void throwAtVirtualPop(int pop) {
    invokePc(pop);
    Instruction instruction = context.routineManager.getInstructionAt(pop);
    if (instruction instanceof Ld stackReset) {
      context.pc.write(pop);
      stackReset.accept(new InstructionsBytecodeGenerator(mm, this, pop));
    }
    if (instruction instanceof Pop popped)
      mm.invoke("unwind", context.routineManager.addressAfter(pop), ((Register) popped.getTarget()).getName());
    else
      mm.invoke("unwind", context.routineManager.addressAfter(pop));
    stopUnwindingHere();
  }

  public void unwind(Object nextAddress) {
    mm.invoke("unwind", nextAddress);
    stopUnwindingHere();
  }

  protected void returnFromMethod() {
    if (isJumpMember(routine))
      mm.return_(-1);
    else
      mm.return_();
  }

  private MultiValuedMap<Integer, Integer> catchPoints() {
    return context.routineManager.catchPointsOfCallsIn(routine);
  }

  private StackAnalyzer stackAnalyzer() {
    return context.symbolicExecutionAdapter.getStackAnalyzer();
  }

  public Integer plantedContinuation(int push) {
    return context.routineManager.plantedContinuation(push, stackAnalyzer());
  }

  public boolean pushesReturnAddress(int callSite) {
    return context.routineManager.pushesReturnAddress(callSite, routine, stackAnalyzer());
  }

  public void resumeAtPushed(int site, Variable value) {
    stackAnalyzer().dataOnTopAt.get(site).stream().flatMap(push -> stackAnalyzer().pushedValues.get(push).stream()).distinct().forEach(pushed -> value.ifEq(pushed, () -> {
      if (getLabel(pushed) != null)
        mm.goto_(getLabel(pushed));
      else {
        jumpInto(pushed);
        returnFromMethod();
      }
    }));
    mm.invoke("jump", value);
    afterInvoke();
    returnFromMethod();
  }

  public void leaveWithOwnData(int site) {
    for (int push : context.routineManager.ownDataLeftForOthers(site, routine, stackAnalyzer())) {
      Integer continuation = plantedContinuation(push);
      if (continuation != null)
        invokeTransformedMethod(continuation);
      else {
        Variable value = mm.invoke("pop");
        List<Integer> consumers = stackAnalyzer().dataConsumedBy.entries().stream().filter(e -> e.getValue() == push && !stackAnalyzer().shiftedReturns.containsKey(e.getKey())).map(Map.Entry::getKey).toList();
        if (!consumers.isEmpty())
          dispatch(value, consumers.stream().flatMap(ret -> stackAnalyzer().dynamicInvocation.get(ret).stream()));
      }
    }
  }

  public static class RoutineRegisterAccumulator<S> implements RoutineVisitor<List<S>> {
    protected final List<S> routineParameters = new ArrayList<>();

    public List<S> getResult() {
      return routineParameters;
    }
  }
}
