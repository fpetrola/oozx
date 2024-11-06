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

package com.fpetrola.z80.instructions;

import com.fpetrola.z80.mmu.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class RRD<T extends WordNumber> extends RLD<T> {
  public RRD(Register<T> a, Register<T> hl, Register<T> r, Register<T> flag, Memory<T> memory) {
    super(a, hl, flag, r, memory);
  }

  protected void executeAlu(T value) {
    RLD.rldTableAluOperation.executeWithCarry(value, flag);
  }

  protected int getTemp1(int nibble2, int nibble3, int nibble4) {
    return (nibble2 << 4) | nibble3;
  }

  protected int getRegA1(int nibble1, int nibble4, int nibble3) {
    return (nibble1 << 4) | nibble4;
  }
}
