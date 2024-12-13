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

package com.fpetrola.z80.bytecode.generators.helpers;

import com.fpetrola.z80.bytecode.generators.RoutineBytecodeGenerator;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.transformations.IVirtual8BitsRegister;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

public class SmartComposed16BitRegisterVariable implements VariableDelegator {
  private final MethodMaker methodMaker;
  private final String name;
  private final Variable variable;
  private final RoutineBytecodeGenerator routineByteCodeGenerator;
  private Register<?> register;
  private Single8BitRegisterVariable variableLow;
  private Single8BitRegisterVariable variableHigh;

  public SmartComposed16BitRegisterVariable(MethodMaker methodMaker, String name, Variable variable, RoutineBytecodeGenerator routineByteCodeGenerator) {
    this.methodMaker = methodMaker;
    this.name = name;
    this.variable = variable;
    this.routineByteCodeGenerator = routineByteCodeGenerator;
  }

  @Override
  public void setRegister(Register<?> register) {
    if (!register.getName().startsWith(name)) {
      throw new RuntimeException("no!");
    }
    this.register = register;
  }


  public Variable set(Object value) {
    Variable result = variable.set(RoutineBytecodeGenerator.getRealVariable(value));
    VirtualComposed16BitRegister<?> currentRegister = (VirtualComposed16BitRegister<?>) register;

    IVirtual8BitsRegister<?> low = currentRegister.getLow();
    boolean noOptimization = !routineByteCodeGenerator.context.optimize16Convertion;

    if (noOptimization || !low.getDependants().stream().anyMatch(VirtualRegister::isComposed2))
      variableLow.directSet(variable.and(0xFF));
    else
      System.out.println("low");

    IVirtual8BitsRegister<?> high = currentRegister.getHigh();
    if (noOptimization || !high.getDependants().stream().anyMatch(VirtualRegister::isComposed2)) {
      variableHigh.directSet(variable.shr(8));
    } else
      System.out.println("high");
    return result;
  }

  public Variable getDelegate() {
//    VirtualRegister<?> register1 = byteCodeGenerator.currentRegister;
//    List<? extends VirtualRegister<?>> previousVersions = register1.getPreviousVersions();
//    if (previousVersions.stream().allMatch(VirtualRegister::isMixRegister)) {
//      Variable high = byteCodeGenerator.variables.get(name.charAt(0) + "");
//      Variable low = byteCodeGenerator.variables.get(name.charAt(1) + "");
//      if (high == null || low == null)
//        return variable;
//      else
//        return high.shl(8).or(ByteCodeGenerator.getRealVariable(low));
//    }
    return variable;
  }

  public String name() {
    return name;
  }

//  public Variable set(Object value) {
//    return methodMaker.invoke(name, value);
//  }

  public Class<?> classType() {
    return int.class;
  }

  public void setLow(Single8BitRegisterVariable variableLow) {
    this.variableLow = variableLow;
  }

  public void setHigh(Single8BitRegisterVariable variableHigh) {
    this.variableHigh = variableHigh;
  }

  public void directSet(Variable value) {
    variable.set(value);
  }
}
