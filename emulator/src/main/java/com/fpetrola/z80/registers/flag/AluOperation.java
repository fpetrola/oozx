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

package com.fpetrola.z80.registers.flag;

import com.fpetrola.z80.registers.Register;

public class AluOperation extends AluOperationBase {
  public AluOperation() {
    super();
    F = 0;
    if (execute(0, 0, 0) != -1) {
      ToPrimitiveIntTriFunction triFunction = this::execute;
      init(triFunction);
    } else if (execute(0, 0) != -1) {
      ToPrimitiveIntBiFunction biFunction = this::execute;
      init(biFunction);
    }
  }

  public int execute(int A, int value, int carry) {
    return -1;
  }

  public int execute(int value, int carry) {
    return -1;
  }

  protected void init(ToPrimitiveIntBiFunction biFunction) {
  }

  public void init(ToPrimitiveIntTriFunction triFunction) {
  }

  public int executeWithCarry(int regA, Register flag) {
    F = flag.read();
    int result = execute(regA, flag.read() & 0x01);
    flag.write(F);
    return result;
  }

  public int executeWithCarry(int value, int regA, Register flag) {
    F = flag.read();
    return executeWithCarry2(value, regA, flag.read() & 0x01, flag);
  }

  public int executeWithCarry2(int value, int regA, int carry, Register flag) {
    F = flag.read();
    int result = execute(regA, value, carry & 1);
    flag.write(F);
    return result;
  }

  public int executeWithoutCarry(int value, int regA, Register flag) {
    F = flag.read();
    int result = execute(regA, value, 0);
    flag.write(F);
    return result;
  }
}
