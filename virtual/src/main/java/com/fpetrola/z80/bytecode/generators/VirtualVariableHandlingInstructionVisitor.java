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

import org.cojen.maker.Variable;

import java.util.function.BiConsumer;

public class VirtualVariableHandlingInstructionVisitor extends VariableHandlingInstructionVisitor{
  public VirtualVariableHandlingInstructionVisitor(BiConsumer<Object, Variable> variableAction, RoutineBytecodeGenerator routineByteCodeGenerator1) {
    super(variableAction, routineByteCodeGenerator1);
  }

//  public static Optional<Map.Entry<VirtualRegister<?>, VirtualRegister<?>>> getFromCommonRegisters(Variable variable, RoutineBytecodeGenerator routineByteCodeGenerator) {
//    return routineByteCodeGenerator.commonRegisters.entrySet().stream().filter(e -> getRegisterName(e.getKey()).equals(variable.name())).findFirst();
//  }
//
//  @Override
//  protected void createResult(Variable variable) {
//    Optional<Map.Entry<VirtualRegister<?>, VirtualRegister<?>>> fromCommonRegisters = getFromCommonRegisters(variable, routineByteCodeGenerator);
//    VirtualRegister<?> s = fromCommonRegisters.isEmpty() ? null : fromCommonRegisters.get().getValue();
//
//    if (s != null) {
//      if (!s.getName().equals(variable.name())) {
//        routineByteCodeGenerator.getExistingVariable(s).set(variable);
//      }
//    } else {
//      routineByteCodeGenerator.commonRegisters.entrySet().stream().forEach(e -> {
//        if (e.getKey() instanceof VirtualComposed16BitRegister<?> virtualComposed16BitRegister && virtualComposed16BitRegister.isMixRegister()) {
//          boolean contains = routineByteCodeGenerator.getExistingVariable(virtualComposed16BitRegister.getLow()) == variable;
//          contains |= routineByteCodeGenerator.getExistingVariable(virtualComposed16BitRegister.getHigh()) == variable;
//          if (contains) {
//            Variable commonHigh = get8BitCommon(virtualComposed16BitRegister.getHigh());
//            Variable commonLow = get8BitCommon(virtualComposed16BitRegister.getLow());
//            Variable variable1;
//
//            if (commonHigh == null) variable1 = commonLow.and(0xFF);
//            else variable1 = commonHigh.shl(8).or(commonLow.and(0xFF));
//
//            routineByteCodeGenerator.getExistingVariable(e.getValue()).set(variable1);
//          }
//        }
//      });
//    }
//  }
//  private Variable get8BitCommon(IVirtual8BitsRegister<?> virtualRegister) {
//    VirtualComposed16BitRegister<?> virtualComposed16BitRegister = virtualRegister.getVirtualComposed16BitRegister();
//    if (virtualComposed16BitRegister.isMixRegister())
//      return routineByteCodeGenerator.getExistingVariable(virtualRegister);
//    return routineByteCodeGenerator.getExistingVariable(virtualComposed16BitRegister);
//  }
}
