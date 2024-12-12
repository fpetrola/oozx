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
import com.fpetrola.z80.bytecode.generators.helpers.SmartComposed16BitRegisterVariable;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.*;
import java.util.function.Supplier;

public class VirtualRoutineBytecodeGenerator extends RoutineBytecodeGenerator {
  public VirtualRoutineBytecodeGenerator(BytecodeGenerationContext bytecodeGenerationContext, Routine routine) {
    super(bytecodeGenerationContext, routine);
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
    register = getTop(register);
    Variable variable = variables.get(getRegisterName(register));
    return variable != null;
  }

  public <T extends WordNumber> void createVariable(VirtualRegister register1) {
    VirtualRegister register = getTop(register1);
    String name = getRegisterName(register);
    registerByVariable.put(name, register);
  }

  public <T extends WordNumber> Variable getVariable2(Register register1, Supplier<Object> value) {
    Register register = getTop2(register1);

    String name = getRegisterName2(register);
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
    VirtualRegister register = getTop(register1);

    String name = getRegisterName(register);
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
    Variable invoke = mm.invoke(createLabelName(jumpLabel), array);
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
}
