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
  public final static int FLAG_C = 0x0001;
  protected final static int FLAG_N = 0x0002;
  protected final static int FLAG_P = 0x0004;
  protected final static int FLAG_V = 0x0004;
  protected final static int FLAG_3 = 0x0008;
  protected final static int FLAG_H = 0x0010;
  protected final static int FLAG_5 = 0x0020;
  protected final static int FLAG_Z = 0x0040;
  protected final static int FLAG_S = 0x0080;

  protected static int[] halfCarryAddTable = {0, FLAG_H, FLAG_H, FLAG_H, 0, 0, 0, FLAG_H};
  protected static int[] halfCarrySubTable = {0, 0, FLAG_H, 0, FLAG_H, 0, FLAG_H, FLAG_H};
  protected static int[] overflowAddTable = {0, 0, 0, FLAG_V, FLAG_V, 0, 0, 0};
  protected static int[] overflowSubTable = {0, FLAG_V, 0, 0, 0, 0, FLAG_V, 0};

  protected static int[] sz53Table = new int[0x100];
  protected static int[] parityTable = new int[0x100];
  protected static int[] sz53pTable = new int[0x100];

  public int F;
  protected int Q;

  public AluOperationBase() {
  }

  static {
    int i, j, k;
    int parity;

    for (i = 0; i < 0x100; i++) {
      sz53Table[i] = i & (FLAG_3 | FLAG_5 | FLAG_S);
      j = i;
      parity = 0;
      for (k = 0; k < 8; k++) {
        parity ^= j & 1;
        j >>= 1;
      }
      parityTable[i] = (parity != 0 ? 0 : FLAG_P);
      sz53pTable[i] = sz53Table[i] | parityTable[i];
    }

    sz53Table[0] |= FLAG_Z;
    sz53pTable[0] |= FLAG_Z;
  }
}
