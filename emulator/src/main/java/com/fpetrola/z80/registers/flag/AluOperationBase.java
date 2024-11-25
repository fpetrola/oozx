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

public class AluOperationBase {
  protected final int FLAG_C = 0x0001;
  protected final int FLAG_N = 0x0002;
  protected final int FLAG_P = 0x0004;
  protected final int FLAG_V = 0x0004;
  protected final int FLAG_3 = 0x0008;
  protected final int FLAG_H = 0x0010;
  protected final int FLAG_5 = 0x0020;
  protected final int FLAG_Z = 0x0040;
  protected final int FLAG_S = 0x0080;

  protected int F;
  protected int Q;

  public AluOperationBase() {
  }

  protected int halfCarryAddTable(int i) {
    return new int[]{0, FLAG_H, FLAG_H, FLAG_H, 0, 0, 0, FLAG_H}[i];
  }

  protected int halfCarrySubTable(int i) {
    return new int[]{0, 0, FLAG_H, 0, FLAG_H, 0, FLAG_H, FLAG_H}[i];
  }

  protected int overflowAddTable(int i) {
    return new int[]{0, 0, 0, FLAG_V, FLAG_V, 0, 0, 0}[i];
  }

  protected int overflowSubTable(int i) {
    return new int[]{0, FLAG_V, 0, 0, 0, 0, FLAG_V, 0}[i];
  }

  protected int sz53Table(int i) {
    return i & (FLAG_3 | FLAG_5 | FLAG_S) | (i == 0 ? FLAG_Z : 0);
  }

  protected int sz53pTable(int i) {
    return sz53Table(i) | parityTable(i) | (i == 0 ? FLAG_Z : 0);
  }

  protected int parityTable(int i) {
    int j, k;
    j = i;
    int parity = 0;
    for (k = 0; k < 8; k++) {
      parity ^= j & 1;
      j >>= 1;
    }
    return (parity != 0 ? 0 : FLAG_P);
  }
}
