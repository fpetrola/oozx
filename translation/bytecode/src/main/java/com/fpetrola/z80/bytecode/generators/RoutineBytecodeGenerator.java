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
import com.fpetrola.z80.minizx.StackException;
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
import java.util.function.Supplier;

public class RoutineBytecodeGenerator {
  public final BytecodeGenerationContext context;
  public Map<String, Variable> registers = new HashMap<>();
  public Field memory;
  public MethodMaker mm;
  private final Map<java.lang.Integer, Label> labels = new HashMap<>();
  protected final Set<java.lang.Integer> positionedLabels = new HashSet<>();
  public final Routine routine;
  public Register lastMemPc = new Plain16BitRegister("lastMemPc");

  public Map<String, Variable> variables = new HashMap<>();
  public Map<String, Register> registerByVariable = new HashMap<>();
  public Map<Register, Variable> variablesByRegister = new HashMap<>();
  protected final Map<java.lang.Integer, Label> insertLabels = new HashMap<>();
  private final Map<java.lang.Integer, Label> labelsAfterLeavingCalls = new HashMap<>();
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

              int nextAddress = instruction instanceof Call ? stackAnalyzer().callContinuations.getOrDefault(address, address + instruction.getLength() & 0xffff) : address + instruction.getLength() & 0xffff;
              if (!routine.contains(nextAddress) && catchPoints().containsKey(address))
                labelsAfterLeavingCalls.put(address, mm.label().here());
              if (fallsThrough(address, instruction) && routine.contains(nextAddress) && getLabel(nextAddress) == null) {
                mm.invoke("untranslated", nextAddress);
                returnFromMethod();
              }
              if (fallsThrough(address, instruction) && routine.contains(nextAddress) && getLabel(nextAddress) != null && (nextAddress <= address || java.util.stream.IntStream.range(address + 1, nextAddress).anyMatch(a -> routine.contains(a) && context.routineManager.getInstructionAt(a) != null && context.routineManager.getInstructionAt(a) != instruction)))
                mm.goto_(getLabel(nextAddress));
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

        List<RoutineManager.CodeVariant> variants = context.routineManager.codeVariantsAt(address);
        if (!variants.isEmpty()) {
          Variable hash = mm.invoke("codeHash", variants.get(0).variableStart(), variants.get(0).variableBytes().length);
          variants.forEach(v -> hash.ifEq(v.hash(), () -> tailJump(v.relocated(address), -1)));
          mm.invoke("unknownCodeVariant", address, variants.get(0).variableStart(), variants.get(0).variableBytes().length);
        }
        List<int[]> versions = codeVersions().instructionVersions(address);
        if (!versions.isEmpty()) {
          invokePc(address);
          Variable hash = mm.invoke("codeHash", address, instruction.getLength());
          Label done = mm.label();
          versions.forEach(bytes -> hash.ifEq(Arrays.hashCode(bytes), () -> {
            Instruction version = codeVersions().decode(address, bytes);
            version.accept(new InstructionsBytecodeGenerator(mm, RoutineBytecodeGenerator.this, address));
            if (RoutineManager.fallsThrough(version))
              done.goto_();
          }));
          executeMutantCode(address, instruction);
          done.here();
        } else if (mutantCodeInInstruction(instruction, address)) {
          invokePc(address);
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

    Label label1 = mm.label();
    label1.here();
    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.labelGenerator().run());

    new ArrayList<>(labels.keySet()).stream().filter(address -> routine.contains(address) && context.routineManager.isEnteredFromOutside(routine, address)).forEach(address -> mm.invoke("isNextPC", address).ifTrue(getLabel(address)::goto_));

    Label label = getLabel(routine.getEntryPoint());
    if (label != null)
      label.goto_();
    generators.forEach(g -> g.instructionGenerator().run());

    positionedLabels.forEach(l -> labels.get(l).here());
    returnFromMethod();

    MultiValuedMap<Integer, Integer> catchPoints = catchPoints();
    List<Integer> returnPoints = catchPoints.values().stream().toList();
    List<Integer> returnPointsDropped = routine.getReturnPointsDropped().values().stream().toList();

    if (!returnPoints.isEmpty() || !returnPointsDropped.isEmpty()) {
//      returnPoints = returnPoints.stream().filter(i -> routine.contains(i)).toList();

      Set<Integer> keys = new HashSet<>(catchPoints.keys());
      keys.forEach(entry -> {
        Integer key = entry;
        Label tryStart = getLabel(key);
        int continuation = stackAnalyzer().callContinuations.getOrDefault(key, context.routineManager.addressAfter(key));
        Label tryEnd = routine.contains(continuation) ? getLabel(continuation) : labelsAfterLeavingCalls.get(key);
        var e = mm.catch_(tryStart, tryEnd, StackException.class);
        Variable nextAddress = e.invoke("getNextPC");

        Collection<Integer> integers = catchPoints.get(key);
        integers.forEach(i -> {
          nextAddress.ifEq(i, () -> {
            loadPoppedReturnAddress(i, key);
            Label label3 = getLabel(i);
            if (label3 != null)
              label3.goto_();
            else if (context.routineManager.getInstructionAt(i) instanceof Ret ret && ret.getCondition() instanceof ConditionAlwaysTrue) {
              invokePc(i);
              returnFromMethod();
            } else if (context.routineManager.getInstructionAt(i) instanceof JP jp && jp.getCondition() instanceof ConditionAlwaysTrue && getLabel(RoutineManager.fixedJumpTarget(jp)) != null) {
              invokePc(i);
              getLabel(RoutineManager.fixedJumpTarget(jp)).goto_();
            } else if (context.routineManager.getInstructionAt(i) instanceof Pop && context.routineManager.findRoutineAt(i) == null) {
              invokePc(i);
              throwStackException(context.routineManager.addressAfter(i), StackException.class);
            } else
              tailJump(i, -1);
          });
        });
        e.throw_();
      });

      List<Integer> droppedPoints = new ArrayList<>(new HashSet<>(returnPointsDropped));
      if (!droppedPoints.isEmpty()) {
        mm.catch_(label1, StackException.class, (Variable exception) -> {
          Variable points = mm.new_(int[].class, droppedPoints.size());
          for (int i = 0; i < droppedPoints.size(); i++)
            points.aset(i, droppedPoints.get(i));
          mm.invoke("isOwnAddress", exception, points).ifTrue(label1::goto_);
          exception.throw_();
        });
        label1.insert(() -> droppedPoints.forEach(point -> {
          Label target = getLabel(point);
          if (target != null)
            mm.invoke("isNextPC", point).ifTrue(target::goto_);
        }));
      }

//      mm.catch_(label10, StackException.class, (Variable exception) -> {
//        ArrayList<Integer> points = new ArrayList<>(returnPoints);
//        points.addAll(returnPointsDropped);
//        int size = points.size();
//        Variable value = mm.new_(int[].class, size);
//
//        for (int i = 0; i < size; i++) {
//          value.aset(i, points.get(i));
//        }
//        mm.invoke("isOwnAddress", exception, value).ifTrue(() -> {
////          label1.goto_();
//          mm.invoke("DE");
//        });
//        exception.throw_();
//      });
    }

  }

  private boolean mutantCodeInInstruction(Instruction instruction, int address) {
    Set<java.lang.Integer> mutantAddress = (Set<java.lang.Integer>) context.symbolicExecutionAdapter.getMutantAddress();
    Set<java.lang.Integer> operands = CodeVersions.operandOffsets(instruction);
    return java.util.stream.IntStream.range(0, instruction.getLength()).anyMatch(i -> mutantAddress.contains(address + i) && !operands.contains(i));
  }

  private void executeMutantCode(int address, Instruction instruction) {
    Variable executedUpTo = mm.invoke("executeMutantCode", address);
    executedUpTo.ifNe(address + instruction.getLength() & 0xffff, () -> {
      for (int inside = address + 1; inside < address + instruction.getLength(); inside++)
        if (getLabel(inside) != null) {
          Label overlapping = getLabel(inside);
          executedUpTo.ifEq(inside, overlapping::goto_);
        }
      executedUpTo.ifNe(-1, () -> mm.invoke("jump", executedUpTo));
      returnFromMethod();
    });
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

  public void jumpInto(int address) {
    Routine owner = context.routineManager.findRoutineAt(address);
    if (owner != null && context.routineManager.isEnteredFromOutside(owner, address)) {
      mm.invoke("setNextAddress", address);
      invokeTransformedMethod(owner.getEntryPoint());
    } else if (owner != null)
      invokeTransformedMethod(address);
    else
      mm.invoke("untranslated", address);
  }

  public void tailJump(int address, int site) {
    Routine owner = context.routineManager.findRoutineAt(address);
    if (isJumpMember(routine) && isJumpMember(owner) && (site == -1 || ownDataLeftForOthers(site).isEmpty())) {
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
      invoke = isJumpMember(target) && target.getEntryPoint() == jumpLabel ? mm.invoke("runJumps", jumpLabel) : mm.invoke(labelName);
    } catch (Exception e) {
      System.out.println("not found: " + labelName + " from " + routine + " at " + Helper.formatAddress(context.pc.read()) + " callers " + context.routineManager.callers.get(jumpLabel) + " owner " + context.routineManager.findRoutineAt(jumpLabel));
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
    if (!context.direct)
      mm.invoke("pc", context.routineManager.originalAddress(address), rDelta);
  }

  public boolean virtualPopOnBranch(int address) {
    Integer pop = routine.getVirtualPop().get(address);
    return pop != null && pop != context.routineManager.addressAfter(address);
  }

  private void loadPoppedReturnAddress(int returnPoint, int callSite) {
    for (int length = 1; length <= 2; length++)
      if (context.routineManager.getInstructionAt(returnPoint - length) instanceof Pop pop && pop.getLength() == length)
        getExistingVariable((Register) pop.getTarget()).set(pushesReturnAddress(callSite) ? mm.invoke("pop") : context.routineManager.addressAfter(callSite));
  }

  public void throwAfterVirtualPop(int address) {
    throwAtVirtualPop(routine.getVirtualPop().get(address));
  }

  private void throwAtVirtualPop(int pop) {
    invokePc(pop);
    if (context.routineManager.getInstructionAt(pop) instanceof Ld stackReset) {
      context.pc.write(pop);
      stackReset.accept(new InstructionsBytecodeGenerator(mm, this, pop));
    }
    throwStackException(context.routineManager.addressAfter(pop), StackException.class);
  }

  public void throwStackException(Object nextAddress, Class<? extends Exception> type) {
    Variable variable = mm.new_(type, nextAddress);
    variable.throw_();
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
    Collection<Integer> values = stackAnalyzer().pushedValues.get(push);
    if (values.size() != 1 || !stackAnalyzer().dataConsumedBy.containsValue(push) || !(context.routineManager.getInstructionAt(push) instanceof Push pushInstruction))
      return null;
    int value = values.iterator().next();
    Routine target = context.routineManager.findRoutineAt(value);
    boolean loadedRightBefore = context.routineManager.getInstructionAt(context.routineManager.addressBefore(push)) instanceof Ld ld && ld.getSource() instanceof Memory16BitReference
        && ld.getTarget() instanceof Register loaded && pushInstruction.getTarget() instanceof Register pushed && loaded.getName().equals(pushed.getName());
    return loadedRightBefore && target != null && target.getEntryPoint() == value ? value : null;
  }

  public List<Integer> ownPushesConsumedAt(int ret) {
    return stackAnalyzer().dataConsumedBy.get(ret).stream().filter(routine::contains).toList();
  }

  public boolean pushesReturnAddress(int callSite) {
    return stackAnalyzer().poppedCallSites.containsKey(callSite) && !routine.getReturnPoints().containsKey(callSite) || context.routineManager.pushedReturnSites.contains(callSite);
  }

  private List<Integer> ownDataLeftForOthers(int site) {
    return stackAnalyzer().dataOnTopAt.get(site).stream().filter(push -> routine.contains(push) && consumedOutside(push)).toList();
  }

  public void leaveWithOwnData(int site) {
    ownDataLeftForOthers(site).forEach(push -> {
      Integer continuation = plantedContinuation(push);
      if (continuation != null)
        invokeTransformedMethod(continuation);
      else {
        Variable value = mm.invoke("pop");
        if (!stackAnalyzer().dataConsumedBy.entries().stream().allMatch(e -> e.getValue() != push || stackAnalyzer().shiftedReturns.containsKey(e.getKey())))
          mm.invoke("jump", value);
      }
    });
  }

  private boolean consumedOutside(int push) {
    return stackAnalyzer().dataConsumedBy.entries().stream().anyMatch(e -> e.getValue() == push && !routine.contains(e.getKey()));
  }

  public static class RoutineRegisterAccumulator<S> implements RoutineVisitor<List<S>> {
    protected final List<S> routineParameters = new ArrayList<>();

    public List<S> getResult() {
      return routineParameters;
    }
  }
}
