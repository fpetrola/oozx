/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

import com.fpetrola.z80.bytecode.generators.helpers.*;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.minizx.NotSolvedStackException;
import com.fpetrola.z80.minizx.StackException;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineVisitor;
import org.cojen.maker.Field;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Comparator.comparingInt;

public class RoutineBytecodeGenerator {
  public final BytecodeGenerationContext bytecodeGenerationContext;
  public Map<String, Variable> registers = new HashMap<>();
  public Field memory;
  public MethodMaker mm;
  private final Map<Integer, Label> labels = new HashMap<>();
  protected final Set<Integer> positionedLabels = new HashSet<>();
  public final Routine routine;
  public Register<WordNumber> lastMemPc = new Plain16BitRegister<WordNumber>("lastMemPc");

  public Map<String, Variable> variables = new HashMap<>();
  public Map<String, Register> registerByVariable = new HashMap<>();
  public Map<Register, Variable> variablesByRegister = new HashMap<>();
  protected final Map<Integer, Label> insertLabels = new HashMap<>();
  public DefaultTargetFlagInstruction lastTargetFlagInstruction;
  private PendingFlagUpdate pendingFlag;
  public Instruction currentInstruction;
  public Register<?> currentRegister;

  public static <S> S getRealVariable(S variable) {
    Object variable1 = variable;
    if (variable1 instanceof VariableDelegator)
      variable1 = ((Variable) variable1).get();
    return (S) variable1;
  }

  public RoutineBytecodeGenerator(BytecodeGenerationContext bytecodeGenerationContext, Routine routine) {
    this.bytecodeGenerationContext = bytecodeGenerationContext;
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

    routine.accept(new RoutineVisitor<Integer>() {
      public void visitInstruction(int address, Instruction instruction) {
        {
          boolean contains = routine.contains(address);
          if (contains) {
            bytecodeGenerationContext.pc.write(WordNumber.createValue(address));
            int firstAddress = address;

            Runnable scopeAdjuster = () -> {
              bytecodeGenerationContext.pc.write(WordNumber.createValue(address));
//                new InstructionActionExecutor<>(r -> r.adjustRegisterScope()).executeAction(instruction);
            };

            Runnable labelGenerator = () -> {
              bytecodeGenerationContext.pc.write(WordNumber.createValue(address));
              JumpLabelVisitor jumpLabelVisitor1 = new JumpLabelVisitor();
              instruction.accept(jumpLabelVisitor1);
              addLabel(address);
            };
            Runnable instructionGenerator = () -> {
              bytecodeGenerationContext.pc.write(WordNumber.createValue(address));

              if (address == 37527)
                System.out.print("");

              currentInstruction = instruction;
              List<Routine> list = new ArrayList<>(routine.getInnerRoutines().stream().filter(routine1 -> routine1.contains(address)).toList());
              if (!list.isEmpty())
                invokeInnerIfAvailable(address, list);
              else
                generateInstruction(address, instruction, firstAddress);

              int nextAddress = address + instruction.getLength();
              List<Routine> list2 = routines.stream().filter(routine1 -> routine1.virtual && routine1 != routine && routine1.getEntryPoint() == nextAddress).toList();
              if (!list2.isEmpty())
                invokeInnerIfAvailable(nextAddress, list2);

            };

            generators.add(new InstructionGenerator(scopeAdjuster, labelGenerator, instructionGenerator));
          }
        }
      }

      private void generateInstruction(int address, Instruction instruction, int firstAddress) {
        lastMemPc.write(WordNumber.createValue(address));

        if (!(instruction instanceof ConditionalInstruction<?, ?>) && pendingFlag != null) {
          if (!pendingFlag.processed)
            pendingFlag.update(false);
        }

        int label = -1;
        if (getLabel(address) != null) {
          label = firstAddress;
          hereLabel(label);
        }

        if (mutantCodeInInstruction(instruction, address)) {
          mm.invoke("executeMutantCode", address);
        } else {
          InstructionsBytecodeGenerator instructionsBytecodeGenerator = new InstructionsBytecodeGenerator(mm, label, RoutineBytecodeGenerator.this, address, pendingFlag);
          instruction.accept(instructionsBytecodeGenerator);
          pendingFlag = instructionsBytecodeGenerator.pendingFlag;

          if (!instructionsBytecodeGenerator.incPopsAdded && routine.getVirtualPop().containsKey(address)) {
            int nextAddress = routine.getVirtualPop().get(address) + 1;
            throwStackException(nextAddress, StackException.class);
//            getField("nextAddress").set(nextAddress);
            returnFromMethod();
          }
        }
      }

      private void invokeInnerIfAvailable(int address, List<Routine> list) {
        Routine first = list.getFirst();
        if (first.getStartAddress() == address) {
          invokeTransformedMethod(first.getStartAddress());
          Routine routineAt = bytecodeGenerationContext.routineManager.findRoutineAt(first.getStartAddress());
          if (routineAt.isCallable())
            returnFromMethod();
          else
            System.out.println("dsgdg");
        } else {
          System.out.print("");
        }
      }
    });

    Label label1 = mm.label();
    label1.here();
    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.labelGenerator().run());

    Label label = getLabel(routine.getEntryPoint());
    if (label != null)
      label.goto_();
    generators.forEach(g -> g.instructionGenerator().run());

    positionedLabels.forEach(l -> labels.get(l).here());
    Label label2 = mm.label();
    label2.here();

    List<Integer> integers = routine.getReturnPoints().values().stream().toList();
    if (!integers.isEmpty())
      mm.catch_(label1, StackException.class, (Variable exception) -> {
        Variable value = mm.new_(int[].class, integers.size());
        for (int i = 0; i < integers.size(); i++) {
          value.aset(i, integers.get(i));
        }
        mm.invoke("isOwnAddress", exception, value).ifTrue(label1::goto_);
        exception.throw_();
      });

    invokeReturnPoints();

  }

  private boolean mutantCodeInInstruction(Instruction instruction, int address) {
    Set<Integer> mutantAddress = (Set<Integer>) bytecodeGenerationContext.symbolicExecutionAdapter.getMutantAddress();
    return mutantAddress.stream().anyMatch(a1 -> a1 >= address && a1 < address + instruction.getLength());
  }

  protected void addField(String name) {
    // cm.addField(int.class, name).private_().static_();
    Variable field = mm.field(name);
    registers.put(name, field);

    if (name.length() == 2 || name.equals("R")) field = new Composed16BitRegisterVariable(mm, name);

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
    MethodMaker methodMaker = bytecodeGenerationContext.methods.get(methodName);

    if (methodMaker == null) {
      methodMaker = createMethod(address, methodName);
    }

    bytecodeGenerationContext.methods.put(methodName, methodMaker);

    return methodMaker;
  }

  protected MethodMaker createMethod(int address, String methodName) {
    return bytecodeGenerationContext.cm.addMethod(void.class, methodName).public_();
  }

  public <T extends WordNumber> Variable getField(String name) {
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

  public <T extends WordNumber> Variable getExistingVariable(Register<T> register) {
    String registerName = register.getName().replace(",", "");

    Variable variable1 = variables.get(registerName);
    if (variable1 instanceof VariableDelegator variable) {
      variable.setRegister(register);
    }
    currentRegister = register;
    return variable1;
  }

  public Variable getVariableFromMemory(Object variable, String bits) {
    Object variable1 = getRealVariable(variable);
    if (bytecodeGenerationContext.syncEnabled) {
      List<Object> params = new ArrayList<>();
      params.add(variable1);
      params.add(lastMemPc.read().intValue());
      addOtherMemSyncParameters(params);
      return mm.invoke("mem" + bits, params.toArray());
    } else {
      if (bits.equals("16"))
        return mm.invoke("mem" + bits, variable1);
      else
        return memory.aget(variable1);
    }
  }

  public void writeVariableToMemory(Object o, Object variable, String bits) {
    Object variable1 = getRealVariable(variable);
    Object o1 = getRealVariable(o);
    if (bytecodeGenerationContext.syncEnabled) {
      List<Object> params = new ArrayList<>();
      params.add(variable1);
      params.add(o1);
      params.add(lastMemPc.read().intValue());
      addOtherMemSyncParameters(params);

      mm.invoke("wMem" + bits, params.toArray());
    } else {
      if (bits.equals("16"))
        mm.invoke("wMem" + bits, variable1, o1);
      else {
//        memory.aset(variable1, o1);
        memory.aset(variable1, o1 instanceof Variable variable2 ? variable2 : (Integer) o1 & 0xff);
      }
    }
  }

  protected void addOtherMemSyncParameters(List<Object> params) {
  }

  public Variable getExistingVariable(String hl) {
    return getRealVariable(variables.get(hl));
  }

  public Variable invokeTransformedMethod(int jumpLabel) {
    return mm.invoke(createLabelName(jumpLabel));
  }

  public void throwStackException(Object nextAddress, Class<? extends Exception> type) {
    Variable variable = mm.new_(type, nextAddress);
    variable.throw_();
  }

  protected void returnFromMethod() {
    mm.return_();
  }

  void invokeReturnPoints() {
    Label label2 = labels.get(routine.getEntryPoint());
    List<Integer> i = routine.getReturnPoints().values().stream().toList();
    List<Integer> integers = new ArrayList<>(new HashSet<>(i));
//    label2.insert(() -> {
//      Variable nextAddress = getField("nextAddress").get();
//      nextAddress.ifNe(0, () -> throwStackException(nextAddress, NotSolvedStackException.class));
//    });
    if (!integers.isEmpty()) {
      List<Integer> integers1 = integers.subList(0, Math.min(1, integers.size() - 1));
      integers.forEach(ga -> insertIfNextPc(ga, label2));
    }
  }

  private void insertIfNextPc(Integer ga, Label label2) {
    label2.insert(() -> {
      Variable isNextPC = mm.invoke("isNextPC", ga);
      isNextPC.ifTrue(() -> {
        Label label1 = getLabel(ga);
        if (label1 != null) {
          label1.goto_();
        } else {
          throwStackException(ga + 1, StackException.class);
//              Variable nextAddress = routineByteCodeGenerator.getField("nextAddress");
//              nextAddress.set(ga + 1);
//              routineByteCodeGenerator.returnFromMethod();
        }
      });
    });
  }

  public static class RoutineRegisterAccumulator<S> implements RoutineVisitor<List<S>> {
    protected final List<S> routineParameters = new ArrayList<>();

    public List<S> getResult() {
      return routineParameters;
    }
  }
}
