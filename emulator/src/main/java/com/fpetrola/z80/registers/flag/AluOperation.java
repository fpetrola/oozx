/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.z80.registers.flag;

import com.fpetrola.z80.registers.Register;

public abstract class AluOperation extends AluOperationBase {
  public AluOperation() {
    super();
    F = 0;
    if (calculate2Values1Boolean(0, 0, 0) != -1) {
      ToPrimitiveIntBiAndBooleanFunction triFunction = (value1, value2, carry) -> calculate2Values1Boolean(value2, value1, carry);
      init(triFunction);
    } else if (calculate1Value1Boolean(0, 0) != -1) {
      ToPrimitiveIntBiFunction biFunction = this::calculate1Value1Boolean;
      init(biFunction);
    } else if (calculate3Values(0, 0, 0) != -1) {
      ToPrimitiveIntTriFunction triFunction = this::calculate3Values;
      init(triFunction);
    }
  }

  protected int calculate2Values1Boolean(int value1, int value2, int carry) {
    return -1;
  }

  protected int calculate3Values(int value1, int value2, int value3) {
    return -1;
  }

  protected int calculate1Value1Boolean(int value, int carry) {
    return -1;
  }

  protected void init(ToPrimitiveIntBiFunction biFunction) {
  }

  public void init(ToPrimitiveIntBiAndBooleanFunction triFunction) {
  }

  public void init(ToPrimitiveIntTriFunction triFunction) {
  }

  public int execute2ValuesAndCarry(int value1, int value2, Register flag) {
    return execute2Values1Boolean(value1, value2, flag.read() & 0x01, flag);
  }

  public abstract int execute2Values1Boolean(int value, int regA, int carry, Register flag);

  public abstract int execute2Values(int value1, int value2, Register flag);
}
