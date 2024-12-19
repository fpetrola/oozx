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

package com.fpetrola.z80.se;

import com.fpetrola.z80.opcodes.references.IntegerWordNumber;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class DirectAccessWordNumber extends IntegerWordNumber {
  public final int pc;
  public final int address;

  public DirectAccessWordNumber(int i, int pc, int address) {
    super(i);
    this.pc = pc;
    this.address = address;
  }

  @Override
  public <T extends WordNumber> T left(int i) {
    return super.left(i);
  }

  public IntegerWordNumber createInstance(int value) {
    return new DirectAccessWordNumber(value & 0xFFFF, pc, address);
  }
}
