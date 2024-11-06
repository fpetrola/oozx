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

package com.fpetrola.z80.opcodes.references;

import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.jspeccy.ConditionPredicate;
import com.fpetrola.z80.registers.Flags;
import com.fpetrola.z80.registers.Register;

import java.util.function.Predicate;

public class ConditionFlag<T extends WordNumber> extends ConditionBase {
  private Register<T> register;
  private final int flag;
  private final boolean negate;

  public ConditionFlag(Register register, int flag, boolean negate, ConditionPredicate<Boolean> isConditionMet) {
    super(isConditionMet);
    this.register = register;
    this.flag = flag;
    this.negate = negate;
  }

  public ConditionFlag(Register register, int flag, boolean negate) {
    this(register, flag, negate, (b, i) -> b);
  }

  public boolean conditionMet(Instruction instruction) {
    return filterCondition(negate != ((register.read().intValue() & flag) == flag), instruction);
  }

  public Register<T> getRegister() {
    return register;
  }

  public void setRegister(Register<T> register) {
    this.register = register;
  }

  public int getFlag() {
    return flag;
  }

  public boolean isNegate() {
    return negate;
  }

  public String toString() {
    return ((negate) ? "N" : "") + Flags.toString(flag);
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitingConditionFlag(this);
  }
}
