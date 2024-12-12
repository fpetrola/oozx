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

package com.fpetrola.z80.instructions.cache;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.BNotZeroCondition;
import com.fpetrola.z80.opcodes.references.Condition;
import com.fpetrola.z80.opcodes.references.ConditionAlwaysTrue;
import com.fpetrola.z80.opcodes.references.ConditionFlag;

public class ConditionCloner implements InstructionVisitor {
  private final InstructionCloner instructionCloner;
  public Condition result;

  public ConditionCloner(InstructionCloner instructionCloner) {
    this.instructionCloner = instructionCloner;
  }

  public void visitingConditionFlag(ConditionFlag conditionFlag) {
    result = new ConditionFlag<>(instructionCloner.clone(conditionFlag.getRegister()), conditionFlag.getFlag(), conditionFlag.isNegate(), conditionFlag.isConditionMet);
  }

  public void visitBNotZeroCondition(BNotZeroCondition bNotZeroCondition) {
    result = new BNotZeroCondition<>(instructionCloner.clone(bNotZeroCondition.getB()), InstructionCloner.clone(bNotZeroCondition.isConditionMet));
  }


  @Override
  public void visitingConditionAlwaysTrue(ConditionAlwaysTrue conditionAlwaysTrue) {
    result = new ConditionAlwaysTrue();
  }

}
