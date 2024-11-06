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
import com.fpetrola.z80.registers.Register;

import java.util.function.Predicate;

public class BNotZeroCondition<T extends WordNumber> extends ConditionBase {
  public void setB(Register<T> b) {
    this.b = b;
  }

  private Register<T> b;

  public BNotZeroCondition(Register<T> b, ConditionPredicate<Boolean> predicate) {
    super(predicate);
    this.b = b;
  }

  public BNotZeroCondition(Register<T> b) {
    this(b, (b1, i) -> b1);
  }

  public boolean conditionMet(Instruction instruction) {
    return filterCondition(b.read().isNotZero(), instruction);
  }

  public Register<T> getB() {
    return b;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitBNotZeroCondition(this);
  }
}
