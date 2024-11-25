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

public class CpiOperation extends TableAluOperation {
  protected void calculate(int A, int value, int BC) {
    int bytetemp = A - value;
    int lookup = ((A & 0x08) >> 3) |
        ((value & 0x08) >> 2) |
        ((bytetemp & 0x08) >> 1);
    F = (F & FLAG_C) | (BC != 0 ? (FLAG_V | FLAG_N) : FLAG_N) |
        halfCarrySubTable(lookup) | (bytetemp != 0 ? 0 : FLAG_Z) |
        (bytetemp & FLAG_S);
    if ((F & FLAG_H) != 0) bytetemp--;
    F |= (bytetemp & FLAG_3) | ((bytetemp & 0x02) != 0 ? FLAG_5 : 0);
    Q = F;
  }
}
