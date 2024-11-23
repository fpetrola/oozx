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

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class IniAluOperation extends TableAluOperation {
  protected <T extends WordNumber> T update(T value, T b, Register<T> c, int i) {
    int initemp = value.intValue() & 0xff;
    int C = c.read().intValue() & 0xff;
    int B = b.intValue() & 0xff;
    int initemp2 = (initemp + C + i) & 0xff;
    F = ((initemp & 0x80) != 0 ? FLAG_N : 0) |
        ((initemp2 < initemp) ? FLAG_H | FLAG_C : 0) |
        (parityTable[(initemp2 & 0x07) ^ B] != 0 ? FLAG_P : 0) |
        sz53Table[B];
    Q = F;
    return WordNumber.createValue(F);
  }
}
