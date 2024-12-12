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

import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.bytecode.generators.helpers.*;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineVisitor;
import com.fpetrola.z80.transformations.InstructionActionExecutor;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.*;

import java.util.*;
import java.util.function.Supplier;

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
  public Map<VirtualRegister<?>, VirtualRegister<?>> commonRegisters = new HashMap<>();
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

  public static <T extends WordNumber> Register<T> getTop2(Register<T> register) {
    return register;
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

    final boolean[] ready = new boolean[]{false};
    List<Routine> routines = routine.getRoutineManager().getRoutines();

    routine.accept(new RoutineVisitor<Integer>() {
      public void visitInstruction(int address, Instruction instruction) {
        {
          boolean contains = routine.contains(address);
          if (contains) {
            if (!ready[0]) {
              bytecodeGenerationContext.pc.write(WordNumber.createValue(address));
              int firstAddress = address;

              Runnable scopeAdjuster = () -> {
                bytecodeGenerationContext.pc.write(WordNumber.createValue(address));
                new InstructionActionExecutor<>(r -> r.adjustRegisterScope()).executeAction(instruction);
              };

              Runnable labelGenerator = () -> {
                bytecodeGenerationContext.pc.write(WordNumber.createValue(address));
                JumpLabelVisitor jumpLabelVisitor1 = new JumpLabelVisitor();
                instruction.accept(jumpLabelVisitor1);
                int jumpLabel = jumpLabelVisitor1.getJumpLabel();

                //  if (jumpLabel > 0)
                addLabel(address);
              };
              Runnable instructionGenerator = () -> {
                bytecodeGenerationContext.pc.write(WordNumber.createValue(address));

                if (!ready[0]) {
                  if (address == 0xDA8D)
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
                }

              };

              generators.add(new InstructionGenerator(scopeAdjuster, labelGenerator, instructionGenerator));
            }
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

//                    if (instruction instanceof Ret && routine.virtualPop.contains(address)) {
//                      mm.invoke("incPops");
//                    }

        if (mutantCodeInInstruction(instruction, address)) {
          mm.invoke("executeMutantCode", address);
        } else {
          InstructionsBytecodeGenerator instructionsBytecodeGenerator = new InstructionsBytecodeGenerator(mm, label, RoutineBytecodeGenerator.this, address, pendingFlag);
          instruction.accept(instructionsBytecodeGenerator);
          pendingFlag = instructionsBytecodeGenerator.pendingFlag;

          if (!instructionsBytecodeGenerator.incPopsAdded && routine.getVirtualPop().containsKey(address)) {
            getField("nextAddress").set(routine.getVirtualPop().get(address) + 1);
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
        //ready[0] = true;
      }
    });

    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.scopeAdjuster().run());
    generators.forEach(g -> g.labelGenerator().run());
    Label label = getLabel(routine.getEntryPoint());
    if (label != null)
      label.goto_();
    generators.forEach(g -> g.instructionGenerator().run());

    positionedLabels.forEach(l -> labels.get(l).here());
  }

  private boolean mutantCodeInInstruction(Instruction instruction, int address) {
    Set<Integer> mutantAddress = (Set<Integer>) bytecodeGenerationContext.symbolicExecutionAdapter.getMutantAddress();
    return mutantAddress.stream().anyMatch(a1 -> a1 >= address && a1 < address + instruction.getLength());
  }

  protected void addReg16(String name) {
    Variable variable = addLocalVariable(name);
    SmartComposed16BitRegisterVariable smartComposed16BitRegisterVariable = new SmartComposed16BitRegisterVariable(mm, name, variable, this);
    variables.put(name, smartComposed16BitRegisterVariable);
  }

  protected void add8BitBoth(SmartComposed16BitRegisterVariable af) {
    addLowHigh(af, af.name().charAt(0) + "", af.name().charAt(1) + "");
  }

  protected void addLowHigh(SmartComposed16BitRegisterVariable reg16, String high, String low) {
    Single8BitRegisterVariable variableHigh = new Single8BitRegisterVariable(mm, addLocalVariable(high), reg16, "h", this);
    variables.put(high, variableHigh);
    reg16.setHigh(variableHigh);
    Single8BitRegisterVariable variableLow = new Single8BitRegisterVariable(mm, addLocalVariable(low), reg16, "l", this);
    variables.put(low, variableLow);
    reg16.setLow(variableLow);
  }

  protected void addField(String name) {
    // cm.addField(int.class, name).private_().static_();
    Variable field = mm.field(name);
    registers.put(name, field);

    if (name.length() == 2 || name.equals("R")) field = new Composed16BitRegisterVariable(mm, name);

    variables.put(name, field);
  }

  private Variable addLocalVariable(String name) {
    // cm.addField(int.class, name).private_().static_();
//    Variable variable = mm.var(int.class);
    List<String> parametersList = routine.accept(new RoutineRegisterAccumulator<>() {
      public void visitParameter(String register) {
        routineParameters.add(register);
      }
    });
    int index = parametersList.indexOf(name);
    Variable variable;
    if (index != -1) {
      variable = mm.param(index);
    } else {
      variable = mm.var(int.class).set(0);
    }
    return variable.name(name);
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

  public <T extends WordNumber> boolean variableExists2(Register register) {
    register = getTop2(register);
    Variable variable = variables.get(getRegisterName2(register));
    return variable != null;
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

  public <T extends WordNumber> Variable getExistingVariable2(Register<T> register) {
    Register topRegister = getTop2(register);
    String registerName = getRegisterName2(topRegister);

    Variable variable1 = variables.get(registerName);
    if (variable1 instanceof VariableDelegator variable) {
      variable.setRegister(register);
    }
    currentRegister = register;
    return variable1;
  }

  public <T extends WordNumber> Variable getExistingVariable(VirtualRegister<?> register) {
    VirtualRegister topRegister = getTop(register);
    String registerName = getRegisterName(topRegister);

    Variable variable1 = variables.get(registerName);
    if (variable1 instanceof VariableDelegator variable) {
      variable.setRegister(register);
    }
    currentRegister = register;
    return variable1;
  }

  public static String getRegisterName(VirtualRegister register) {
    Helper.breakInStackOverflow();

    return VirtualComposed16BitRegister.fixIndexNames(register.getName().replace(",", ""));
  }

  public static String getRegisterName2(Register register) {
    Helper.breakInStackOverflow();

    return VirtualComposed16BitRegister.fixIndexNames(register.getName().replace(",", ""));
  }

  public static VirtualRegister getTop(VirtualRegister<?> register) {
    VirtualRegister o = (VirtualRegister) register.getVersionHandler().versions.get(0);
    return o;
  }

  public Variable getVariableFromMemory(Object variable, String bits) {
    Object variable1 = getRealVariable(variable);
    if (bytecodeGenerationContext.syncEnabled) {
      List<Object> params = new ArrayList<>();
      params.add(variable1);
      params.add(lastMemPc.read().intValue());
      params.addAll(getListOfAllRegistersForParameters());
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
      params.addAll(getListOfAllRegistersForParameters());

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

  Variable getExistingVariable(String hl) {
    return getRealVariable(variables.get(hl));
  }

  public Variable invokeTransformedMethod(int jumpLabel) {
    Variable invoke;
    if (bytecodeGenerationContext.useFields) {
      invoke = mm.invoke(createLabelName(jumpLabel));
    } else {
      Routine routineAt = bytecodeGenerationContext.routineManager.findRoutineAt(jumpLabel);
      Object[] array = routineAt.accept(new RoutineRegisterAccumulator<Variable>() {
        public void visitParameter(String register) {
          routineParameters.add(getVar(register));
        }
      }).toArray();
      invoke = mm.invoke(createLabelName(jumpLabel), array);
      assignReturnValues(routineAt, invoke);
    }
    return invoke;
  }

  private Object[] getAllregistersAsParameters() {
    return getListOfAllRegistersForParameters().toArray();
  }

  private List<Variable> getListOfAllRegistersForParameters() {
    return getListOfAllRegistersNamesForParameters().stream().map(this::getVar).toList();
  }

  private void assignReturnValues(Routine routine, Variable result) {
    List<Variable> resultValues = routine.accept(new RoutineRegisterAccumulator<>() {
      public void visitReturnValue(String register) {
        routineParameters.add(getVar(register));
      }
    });
    int index = 0;
    for (Variable variable : resultValues) {
      variable.set(result.aget(index++));
    }
  }

  private List<String> getListOfAllRegistersNamesForParameters() {
    if (bytecodeGenerationContext.useFields)
      return new ArrayList<>();
    else
      return Arrays.asList("AF", "BC", "DE", "HL", "IX", "IY", "A", "F", "B", "C", "D", "E", "H", "L", "IXL", "IXH", "IYL", "IYH");
  }

  public Variable getVar(String name) {
    return getExistingVariable(name);
  }

  void returnFromMethod() {
    if (bytecodeGenerationContext.useFields)
      mm.return_();
    else {
      Object[] values = routine.accept(new RoutineRegisterAccumulator<Variable>() {
        public void visitReturnValue(String register) {
          routineParameters.add(getVar(register));
        }
      }).toArray();

      if (values.length == 0)
        mm.return_();
      else {
        Variable variable1 = mm.new_(int[].class, values.length);
        for (int i = 0; i < values.length; i++) {
          variable1.aset(i, values[i]);
        }
        mm.return_(variable1);
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
