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

import com.fpetrola.z80.instructions.base.Instruction;

public class FlipFLopConditionFlag {
  public final FlipFlopPredicate isConditionMet;

  public ConditionExecutionListener getExecutionsListener() {
    return isConditionMet.executionsListener;
  }


  public FlipFLopConditionFlag(ConditionExecutionListener executionListener, boolean alwaysTrue) {
    isConditionMet = new FlipFlopPredicate(executionListener, alwaysTrue);
  }

  public class FlipFlopPredicate implements ConditionPredicate<Boolean> {
    public boolean state = false;
    public ConditionExecutionListener executionsListener;
    public boolean alwaysTrue;

    public FlipFlopPredicate(ConditionExecutionListener executionsListener, boolean alwaysTrue) {
      this.executionsListener = executionsListener;
      this.alwaysTrue = alwaysTrue;
    }

    @Override
    public boolean test(Boolean aBoolean, Instruction<Boolean> instruction) {
      boolean result = state;
      state = !state;
//    return Math.random() * 100 > 50;
      result = alwaysTrue || result;
      result = executionsListener.executingCondition(instruction, alwaysTrue, result);
      return result;
    }
  }
}
