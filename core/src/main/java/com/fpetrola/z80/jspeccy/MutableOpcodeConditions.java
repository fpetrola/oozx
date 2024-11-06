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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.RegisterName;

public class MutableOpcodeConditions extends OpcodeConditions {
  private ConditionExecutionListener executionsListener;

  public MutableOpcodeConditions(State state2, ConditionExecutionListener executionsListener) {
    super(state2.getFlag(), state2.getRegister(RegisterName.B));
    this.executionsListener = executionsListener;
  }

  public ConditionAlwaysTrue t() {
    ConditionAlwaysTrue f = super.t();
    FlipFLopConditionFlag flipFLopConditionFlag = new FlipFLopConditionFlag(executionsListener, true);
    f.isConditionMet = flipFLopConditionFlag.isConditionMet;
    return f;
  }

  public ConditionFlag f(int flag) {
    ConditionFlag f = super.f(flag);
    FlipFLopConditionFlag flipFLopConditionFlag = new FlipFLopConditionFlag(executionsListener, false);
    f.isConditionMet = flipFLopConditionFlag.isConditionMet;
    return f;
  }

  public ConditionFlag nf(int flag) {
    ConditionFlag f = super.nf(flag);
    FlipFLopConditionFlag flipFLopConditionFlag = new FlipFLopConditionFlag(executionsListener, false);
    f.isConditionMet = flipFLopConditionFlag.isConditionMet;
    return f;
  }

  @Override
  public BNotZeroCondition bnz() {
    BNotZeroCondition f = super.bnz();
    FlipFLopConditionFlag flipFLopConditionFlag = new FlipFLopConditionFlag(executionsListener, false);
    f.isConditionMet = flipFLopConditionFlag.isConditionMet;
    return f;
  }
}
