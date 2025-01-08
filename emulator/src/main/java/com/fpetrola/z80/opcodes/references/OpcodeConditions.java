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

import com.fpetrola.z80.registers.Register;

public class OpcodeConditions {
  protected Register register;
  protected Register b;

  OpcodeConditions(Register flag, Register b) {
    register = flag;
    this.b = b;
  }

  public static OpcodeConditions createOpcodeConditions(Register flag, Register b) {
    return new OpcodeConditions(flag, b);
  }

  public ConditionAlwaysTrue t() {
    return new ConditionAlwaysTrue();
  }

  public ConditionFlag f(int flag) {
    return new ConditionFlag(register, flag, false);
  }

  public ConditionFlag nf(int flag) {
    return new ConditionFlag(register, flag, true);
  }

  public BNotZeroCondition bnz() {
    return new BNotZeroCondition(b);
  }
}
