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
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

public class Single8BitRegisterVariable implements VariableDelegator {
  private final MethodMaker methodMaker;
  private final Variable variable;
  private final SmartComposed16BitRegisterVariable composedRegisterVariable;
  private final String nibble;
  private final RoutineBytecodeGenerator routineByteCodeGenerator;
  private Register<?> register;

  public Single8BitRegisterVariable(MethodMaker methodMaker, Variable variable, SmartComposed16BitRegisterVariable composedRegister, String nibble, RoutineBytecodeGenerator routineByteCodeGenerator) {
    this.methodMaker = methodMaker;
    this.variable = variable;
    this.composedRegisterVariable = composedRegister;
    this.nibble = nibble;
    this.routineByteCodeGenerator = routineByteCodeGenerator;
  }

  @Override
  public void setRegister(Register<?> register) {
    String name = variable.name();
    if (!register.getName().startsWith(name)) {
      throw new RuntimeException("no!");
    }
    this.register = register;
  }

  public Variable getDelegate() {
    return variable;
    //  return methodMaker.invoke(nibble, getRealVariable(composedRegisterName));
  }

  public Variable set(Object value) {
    Variable result = variable.set(RoutineBytecodeGenerator.getRealVariable(value));
    boolean noOptimization = !routineByteCodeGenerator.context.optimize16Convertion;
    if (noOptimization || ((VirtualRegister)routineByteCodeGenerator.currentRegister).getDependants().stream().anyMatch(t -> false)) {
      Variable invoke;
      if (nibble.equals("l")) {
        invoke = routineByteCodeGenerator.mm.invoke("reg16high", composedRegisterVariable.get(), result);
      } else {
        invoke = routineByteCodeGenerator.mm.invoke("reg16low", composedRegisterVariable.get(), result);
      }
      composedRegisterVariable.directSet(invoke);
    }
    return result;
  }

  public void directSet(Variable value) {
    variable.set(value);
  }

  public Class<?> classType() {
    return int.class;
  }
}
