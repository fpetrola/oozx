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

import com.fpetrola.z80.bytecode.generators.helpers.BytecodeGenerationContext;
import com.fpetrola.z80.bytecode.generators.helpers.Single8BitRegisterVariable;
import com.fpetrola.z80.bytecode.generators.helpers.SmartComposed16BitRegisterVariable;
import com.fpetrola.z80.bytecode.generators.helpers.VariableDelegator;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.*;
import java.util.function.Supplier;

public class VirtualRoutineBytecodeGenerator extends RoutineBytecodeGenerator {
  public Map<VirtualRegister<?>, VirtualRegister<?>> commonRegisters = new HashMap<>();

  public VirtualRoutineBytecodeGenerator(BytecodeGenerationContext bytecodeGenerationContext, Routine routine) {
    super(bytecodeGenerationContext, routine);
  }

  public static String getRegisterName(VirtualRegister register) {
    Helper.breakInStackOverflow();

    return VirtualComposed16BitRegister.fixIndexNames(register.getName().replace(",", ""));
  }

  public static VirtualRegister getTop(VirtualRegister<?> register) {
    VirtualRegister o = (VirtualRegister) register.getVersionHandler().versions.get(0);
    return o;
  }

  protected void addVariables() {
    // Arrays.stream(RegisterName.values()).filter(r -> r.name().length() == 2).forEach(n -> addLocalVariable(n.name()));

    addReg16("AF");
    addReg16("BC");
    addReg16("DE");
    addReg16("HL");
    addReg16("IX");
    addReg16("IY");

    add8BitBoth((SmartComposed16BitRegisterVariable) variables.get("AF"));
    add8BitBoth((SmartComposed16BitRegisterVariable) variables.get("BC"));
    add8BitBoth((SmartComposed16BitRegisterVariable) variables.get("DE"));
    add8BitBoth((SmartComposed16BitRegisterVariable) variables.get("HL"));

    addLowHigh((SmartComposed16BitRegisterVariable) variables.get("IX"), "IXH", "IXL");
    addLowHigh((SmartComposed16BitRegisterVariable) variables.get("IY"), "IYH", "IYL");
  }

  public <T extends WordNumber> boolean variableExists(VirtualRegister register) {
    register = VirtualRoutineBytecodeGenerator.getTop(register);
    Variable variable = variables.get(VirtualRoutineBytecodeGenerator.getRegisterName(register));
    return variable != null;
  }

  public <T extends WordNumber> void createVariable(VirtualRegister register1) {
    VirtualRegister register = VirtualRoutineBytecodeGenerator.getTop(register1);
    String name = VirtualRoutineBytecodeGenerator.getRegisterName(register);
    registerByVariable.put(name, register);
  }

  public <T extends WordNumber> Variable getVariable2(Register register1, Supplier<Object> value) {
    Register register = RoutineBytecodeGenerator.getTop2(register1);

    String name = RoutineBytecodeGenerator.getRegisterName2(register);
    Variable variable = variables.get(name);
    if (variable != null) {
      Variable set = doSetValue(value, variable);
      variables.put(name, set);
      variablesByRegister.put(register, set);
      return variable;
    } else {
//      System.out.println("creating var: " + name + "= " + value);
      registerByVariable.put(name, register);

      Variable set = setVariable(name, value);

      variables.put(name, set);
      variablesByRegister.put(register, set);

//      getField("PC").sub(var);
      return set;
    }
  }

  public <T extends WordNumber> Variable getVariable(VirtualRegister register1, Supplier<Object> value) {
    VirtualRegister register = VirtualRoutineBytecodeGenerator.getTop(register1);

    String name = VirtualRoutineBytecodeGenerator.getRegisterName(register);
    Variable variable = variables.get(name);
    if (variable != null) {
      Variable set = doSetValue(value, variable);
      variables.put(name, set);
      variablesByRegister.put(register, set);
      return variable;
    } else {
//      System.out.println("creating var: " + name + "= " + value);
      registerByVariable.put(name, register);

      Variable set = setVariable(name, value);

      variables.put(name, set);
      variablesByRegister.put(register, set);

//      getField("PC").sub(var);
      return set;
    }
  }

  public Label getBranchLabel(Integer minLine) {
    Optional<Map.Entry<Integer, Label>> first = insertLabels.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).filter(e -> e.getKey() >= minLine).findFirst();
    return first.get().getValue();
  }

  public boolean isLabelPositioned(int labelName) {
    return positionedLabels.contains(labelName);
  }

  protected MethodMaker createMethod(int address, String methodName) {
    MethodMaker methodMaker;
    Routine routineAt = bytecodeGenerationContext.routineManager.findRoutineAt(address);
    List<String> parametersList = routineAt.accept(new RoutineRegisterAccumulator<>() {
      public void visitParameter(String register) {
        routineParameters.add(register);
      }
    });
    Object[] objects = parametersList.stream().map(s -> int.class).toArray();

    Object[] values = routineAt.accept(new RoutineRegisterAccumulator<String>() {
      public void visitReturnValue(String register) {
        routineParameters.add(register);
      }
    }).toArray();

    methodMaker = bytecodeGenerationContext.cm.addMethod(values.length == 0 ? void.class : int[].class, methodName, objects).public_();
    return methodMaker;
  }

  @Override
  public Variable invokeTransformedMethod(int jumpLabel) {
    Routine routineAt = bytecodeGenerationContext.routineManager.findRoutineAt(jumpLabel);
    Object[] array = routineAt.accept(new RoutineRegisterAccumulator<Variable>() {
      public void visitParameter(String register) {
        routineParameters.add(getVar(register));
      }
    }).toArray();
    Variable invoke = mm.invoke(RoutineBytecodeGenerator.createLabelName(jumpLabel), array);
    assignReturnValues(routineAt, invoke);
    return invoke;
  }

  @Override
  protected List<String> getListOfAllRegistersNamesForParameters() {
    return Arrays.asList("AF", "BC", "DE", "HL", "IX", "IY", "A", "F", "B", "C", "D", "E", "H", "L", "IXL", "IXH", "IYL", "IYH");
  }

  @Override
  protected void returnFromMethod() {
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
}
