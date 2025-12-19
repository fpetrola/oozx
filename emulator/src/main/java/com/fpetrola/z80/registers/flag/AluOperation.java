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

public class AluOperation extends AluOperationBase {
  private final int method;
  private ToPrimitiveIntTriFunction triFunction;

  public AluOperation() {
    triFunction = null;
    if (calculate2Values1Boolean(0, 0, 0) != -1) {
      method = 1;
      triFunction = this::calculate2Values1Boolean;
    } else if (calculate1Value(0) != -1) {
      method = 2;
      triFunction = (value1, value2, carry) -> this.calculate1Value(value1);
    } else if (calculate3Values(0, 0, 0) != -1) {
      method = 3;
      triFunction = this::calculate3Values;
    } else
      method = 0;
  }

  protected int calculate2Values1Boolean(int value1, int value2, int carry) {
    return -1;
  }

  protected int calculate3Values(int value1, int value2, int value3) {
    return -1;
  }

  protected int calculate1Value(int value) {
    return -1;
  }

  public int execute2ValuesAndCarry(int value1, int value2, int flag) {
    F = flag;
    int result = 0;

    if (method == 1) {
      result = calculate2Values1Boolean(value1, value2, flag);
    } else if (method == 2) {
      result = calculate1Value(value1);
    } else if (method == 3) {
      result = calculate3Values(value1, value2, flag);
    }
    return result & 0xFF;
  }

  public int execute2ValuesAndCarry(int value1, int value2, Register flag) {
    return execute2Values1Boolean(value1, value2, flag.read() & 0x01, flag);
  }

  public int execute2Values1Boolean(int value1, int value2, int booleanValue, Register flag) {
    return executeWrappingF(value1, value2, booleanValue, flag);
  }

  public int execute2Values(int value1, int value2, Register flag) {
    return executeWrappingF(value1, value2, 0, flag);
  }

  public void execute3Values(int value1, int value2, int value3, Register flag) {
    executeWrappingF(value1, value2, value3, flag);
  }

  private int executeWrappingF(int value1, int value2, int value3, Register flag) {
    F = flag.read();
    int data1 = triFunction.applyAsInt(value1, value2, value3) & 0xFF;
    flag.write(F & 0xFF);
    return data1;
  }
}
