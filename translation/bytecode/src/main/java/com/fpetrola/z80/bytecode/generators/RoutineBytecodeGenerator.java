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
import com.fpetrola.z80.routines.Space;
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
  /** The instruction whose code the method emits right after each one: a fall through to any other needs a jump. */
  private final Map<Integer, Integer> emittedAfter = new HashMap<>();
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
  /** The parts of this routine and of its parent, by entry, and where each one is called, once: every other way into the part goes there. */
  private final Map<Integer, Routine> partEntries = new TreeMap<>();
  private final Map<Routine, Label> partCalls = new HashMap<>();
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
    RoutineManager.Outlined mine = context.routineManager.outlined.get(routine);
    context.routineManager.outlined.forEach((piece, part) -> {
      if (piece != routine && (part.parent() == routine || mine != null && part.parent() == mine.parent()))
        partEntries.put(piece.getEntryPoint(), piece);
    });
  }

  public static String createLabelName(RoutineManager routineManager, int label) {
    return "$" + routineManager.spaceOf(label) + Helper.formatAddress(routineManager.originalAddress(label));
  }

  private String createLabelName(int label) {
    return createLabelName(context.routineManager, label);
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
    int[] lastVisited = {-1};

    routine.accept(new RoutineVisitor<java.lang.Integer>() {
      public void visitInstruction(int address, Instruction instruction) {
        {
          boolean contains = routine.contains(address);
          if (contains) {
            if (lastVisited[0] != -1)
              emittedAfter.put(lastVisited[0], address);
            lastVisited[0] = address;
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
              if (fallsThrough(address, instruction) && routine.contains(nextAddress) && getLabel(nextAddress) != null && (structuring.headerFor(address, nextAddress) != -1 || emittedAfter.get(address) == null || emittedAfter.get(address) != nextAddress))
                goTo(address, nextAddress);
              Routine continuationOwner = context.routineManager.findRoutineAt(nextAddress);
              RoutineManager.Outlined mine = context.routineManager.outlined.get(routine);
              if (fallsThrough(address, instruction) && !routine.contains(nextAddress) && (mine != null && mine.exit(nextAddress, address) != -1 || continuationOwner != null && (continuationOwner.getEntryPoint() == nextAddress ? !continuationOwner.isVirtual() : context.routineManager.isEnteredFromOutside(continuationOwner, nextAddress))))
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
          region.entries().forEach(entry -> dispatchRegionEntry(address, entry));

        List<RoutineManager.CodeVariant> variants = context.routineManager.codeVariantsAt(address);
        if (!variants.isEmpty()) {
          variants.stream().filter(v -> v.hasInstructionAt(address)).forEach(v -> mm.invoke("codeHash", v.variableStart(), v.variableBytes().length, addresses(Arrays.stream(v.varying()).boxed().toList())).ifEq(v.hash(), () -> tailJump(v.relocated(address), -1)));
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
          if (!context.routineManager.synthetic(address))
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

    List<Integer> entries = context.routineManager.entriesFromOutside(routine, stackAnalyzer(), context.ancestorJumps()).stream().filter(labels::containsKey).toList();
    context.dispatchedEntries.addAll(entries);
    context.routineManager.outlined.values().stream().filter(part -> part.parent() == routine).flatMap(part -> part.exits().stream().map(RoutineManager.Outlined.Continuation::to)).filter(routine::contains).forEach(this::addLabel);
    List<Integer> dispatched = Stream.concat(entries.stream(), partEntries.values().stream().filter(part -> context.routineManager.outlined.get(part).parent() == routine).flatMap(part -> entriesOf(part).stream())).toList();
    structuring = new ControlFlowStructuring(routine, context.routineManager, Stream.concat(entries.stream(), dispatched.stream().map(address -> partEntries.values().stream().filter(part -> part.contains(address)).findFirst().map(Routine::getEntryPoint).orElse(address))).distinct().toList(), stackAnalyzer(), Stream.concat(landings().stream(), routine.getReturnPointsDropped().values().stream()).toList(), this::mayUnwindAt);
    dispatched.forEach(address -> dispatchEntry(-1, address));

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
    int next = Space.next(address, instruction.getLength());
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
    MethodMaker methodMaker = context.methods.get(address);

    if (methodMaker == null) {
      methodMaker = createMethod(address, methodName);
    }

    context.methods.put(address, methodMaker);

    return methodMaker;
  }

  protected MethodMaker createMethod(int address, String methodName) {
    Routine owner = context.routineManager.findRoutineAt(address);
    RoutineManager.Outlined part = context.routineManager.outlined.get(owner);
    boolean entry = owner != null && owner.getEntryPoint() == address;
    return context.cm.addMethod(entry && (isJumpMember(owner) || part != null && part.reports()) ? int.class : void.class, methodName).public_();
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
    Routine top = owner == null ? null : context.routineManager.topParent(owner);
    if (owner != null && (top != owner || context.routineManager.isEnteredFromOutside(owner, address))) {
      mm.invoke("setNextAddress", address);
      invokeTransformedMethod(top.getEntryPoint());
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
    RoutineManager.Outlined part = context.routineManager.outlined.get(owner), mine = context.routineManager.outlined.get(routine);
    int exit = mine != null ? mine.exit(address, site) : -1;
    if (exit != -1) {
      if (mine.reports())
        mm.return_(exit);
      else
        mm.return_();
    } else if (partContaining(address) != null) {
      goTo(site, partContaining(address).getEntryPoint(), address);
    } else if (owner != null && context.routineManager.callersWhoseReturnIsTaken(routine, site != -1 ? site : context.pc.read(), stackAnalyzer()).contains(owner) && context.ancestorJumps().goesBack(routine, owner)) {
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

  /** The call of a part and what follows its outcome, emitted at the first reference; any other reference jumps there. */
  private void callPart(Routine part) {
    if (partCalls.containsKey(part)) {
      partCalls.get(part).goto_();
      return;
    }
    partCalls.put(part, mm.label().here());
    ControlFlowStructuring.Region region = structuring.regions().get(part.getEntryPoint());
    if (region != null)
      region.entries().forEach(entry -> dispatchRegionEntry(part.getEntryPoint(), entry));
    RoutineManager.Outlined outlined = context.routineManager.outlined.get(part);
    Variable outcome = mm.invoke(createLabelName(part.getEntryPoint()));
    if (context.mayUnwind(part))
      afterInvoke();
    List<RoutineManager.Outlined.Continuation> exits = outlined.exits();
    for (int i = 0; i < exits.size(); i++) {
      RoutineManager.Outlined.Continuation goesOn = exits.get(i);
      if (outlined.reports())
        outcome.ifEq(i, () -> goOn(part, goesOn));
      else
        goOn(part, goesOn);
    }
    if (outlined.reports() || exits.isEmpty())
      returnFromMethod();
  }

  private void goOn(Routine part, RoutineManager.Outlined.Continuation exit) {
    Routine sibling = partContaining(exit.to());
    if (routine.contains(exit.to()) || sibling != null)
      goTo(part.getEntryPoint(), sibling != null ? sibling.getEntryPoint() : exit.to(), exit.to());
    else
      tailJump(exit.to(), exit.from());
  }

  /** Whether an unwinding can come back from going on at this address: the routine there may unwind. */
  private boolean mayUnwindAt(int address) {
    return context.mayUnwind(context.routineManager.findRoutineAt(address));
  }

  /** The part of this routine's family the address is in, entered through the part's call with the address carried. */
  private Routine partContaining(int address) {
    return partEntries.values().stream().filter(candidate -> candidate.contains(address)).findFirst().orElse(null);
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
      if (target != null && target.getEntryPoint() != jumpLabel && !context.methods.containsKey(jumpLabel)) {
        mm.invoke("setNextAddress", jumpLabel);
        entry = target.getEntryPoint();
        labelName = createLabelName(entry);
        context.dispatchedEntries.add(jumpLabel);
      }
      Routine top = target == null ? null : context.routineManager.topParent(target);
      if (top != null && top != target && entry == target.getEntryPoint()) {
        if (entry == jumpLabel)
          mm.invoke("setNextAddress", jumpLabel);
        target = top;
        entry = top.getEntryPoint();
        labelName = createLabelName(entry);
      }
      boolean chain = isJumpMember(target) && target.getEntryPoint() == entry;
      invoke = chain ? mm.invoke("runJumps", entry) : mm.invoke(labelName);
      if (chain || context.mayUnwind(target))
        afterInvoke();
    } catch (IllegalStateException e) {
      throw new IllegalStateException("cannot invoke " + labelName + " from " + routine + " at " + Helper.formatAddress(context.pc.read()) + ", owner " + context.routineManager.findRoutineAt(jumpLabel), e);
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
      bytes[i] = context.symbolicExecutionAdapter.state.getMemory().read(Space.address(address + i), 0);
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
        if (getLabel(i) != null)
          goTo(key, i);
        else if (context.routineManager.getInstructionAt(i) instanceof Ret ret && ret.getCondition() instanceof ConditionAlwaysTrue) {
          invokePc(i);
          returnFromMethod();
        } else if (context.routineManager.getInstructionAt(i) instanceof JP jp && jp.getCondition() instanceof ConditionAlwaysTrue && getLabel(RoutineManager.fixedJumpTarget(jp)) != null) {
          invokePc(i);
          goTo(i, RoutineManager.fixedJumpTarget(jp));
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

  private Set<Integer> landings() {
    if (landings == null)
      landings = context.routineManager.landingsOf(routine, stackAnalyzer(), context.ancestorJumps());
    return landings;
  }

  private int continuation(int address) {
    return context.routineManager.continuation(address, stackAnalyzer());
  }

  /** A jump inside this routine: into a region entered in more than one place, through its header with nextAddress set. */
  public void goTo(int from, int target) {
    goTo(from, target, target);
  }

  /** A jump to {@code target} that carries {@code carry} as the address to go on at, when it is inside the part at the target. */
  private void goTo(int from, int target, int carry) {
    int header = structuring.headerFor(from, target);
    int at = header == -1 ? target : header;
    if (carry != at)
      mm.invoke("setNextAddress", carry);
    if (partEntries.containsKey(at))
      callPart(partEntries.get(at));
    else if (getLabel(at) != null)
      getLabel(at).goto_();
    else
      throw new IllegalStateException("no label for " + Helper.formatAddress(at) + " (from " + Helper.formatAddress(from) + " to " + Helper.formatAddress(target) + ", carrying " + Helper.formatAddress(carry) + ") in " + routine);
  }

  /** The addresses of a part the code outside its family comes in at: its entry (a call, a jump, a RET that goes on there) and what its own method dispatches. */
  private List<Integer> entriesOf(Routine part) {
    List<Integer> entries = new ArrayList<>(List.of(part.getEntryPoint()));
    entries.addAll(context.routineManager.entriesFromOutside(part, stackAnalyzer(), context.ancestorJumps()));
    return entries;
  }

  /** An entry of a region: a part entered there is entered at any of its own entries. */
  private void dispatchRegionEntry(int from, int entry) {
    if (partEntries.containsKey(entry))
      entriesOf(partEntries.get(entry)).forEach(address -> dispatchEntry(from, address));
    else
      dispatchEntry(from, entry);
  }

  private void dispatchEntry(int from, int address) {
    Routine part = partContaining(address);
    mm.invoke("isNextPC", address).ifTrue(() -> goTo(from, part != null ? part.getEntryPoint() : address, address));
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
    RoutineManager.Outlined mine = context.routineManager.outlined.get(routine);
    if (isJumpMember(routine))
      mm.return_(-1);
    else if (mine != null && mine.reports())
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

  /** The data this routine pushed for a RET of another routine is taken here when the site leaves and it is on top: the site may also be reached on a path that did not push it, where the top is something else, such as the return address of this frame. */
  public void leaveWithOwnData(int site) {
    List<Integer> pushes = context.routineManager.ownDataLeftForOthers(site, routine, stackAnalyzer());
    for (int push : pushes) {
      Integer continuation = plantedContinuation(push);
      if (continuation != null && !context.routineManager.pushedForReal(push, stackAnalyzer()))
        goOnAt(continuation, site, pushes.size() == 1);
      else {
        Label take = mm.label(), done = mm.label();
        Set<Integer> pushed = new TreeSet<>(stackAnalyzer().pushedValues.get(push));
        if (!pushed.isEmpty()) {
          Variable top = mm.invoke("peek");
          pushed.forEach(value -> top.ifEq(value, take));
          done.goto_();
        }
        take.here();
        Variable value = mm.invoke("pop");
        List<Integer> consumers = stackAnalyzer().dataConsumedBy.entries().stream().filter(e -> e.getValue() == push && !stackAnalyzer().shiftedReturns.containsKey(e.getKey())).map(Map.Entry::getKey).toList();
        if (!consumers.isEmpty())
          goOnAt(value, consumers.stream().flatMap(ret -> stackAnalyzer().dynamicInvocation.get(ret).stream()).toList(), site, pushes.size() == 1);
        done.here();
      }
    }
  }

  /** Goes on at a target once the data left for others was taken: within the method when the target has a label, as a turn of the loop or a jump out of the routine when nothing else of the routine's is on the stack ({@code alone}), else as a call that comes back here, which is when it returns false. */
  private boolean goOnAt(int target, int site, boolean alone) {
    if (getLabel(target) != null)
      goTo(site, target);
    else if (alone)
      tailJump(target, -1);
    else
      jumpInto(target);
    return alone || getLabel(target) != null;
  }

  /** The same at the recorded target a value names, in the emulator when it is none of them. */
  private void goOnAt(Variable value, Collection<Integer> targets, int site, boolean alone) {
    Label reached = mm.label();
    targets.stream().distinct().sorted().forEach(target -> value.ifEq(target, () -> {
      if (!goOnAt(target, site, alone))
        reached.goto_();
    }));
    mm.invoke("jump", value);
    afterInvoke();
    reached.here();
  }

  public static class RoutineRegisterAccumulator<S> implements RoutineVisitor<List<S>> {
    protected final List<S> routineParameters = new ArrayList<>();

    public List<S> getResult() {
      return routineParameters;
    }
  }
}
