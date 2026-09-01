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

/**
 * The flag tables every ALU operation reads, worked out once when the class loads.
 * <p>
 * They used to be worked out on every call: the four small ones allocated a fresh int[] of eight
 * elements per flag calculation, and parity counted bits around a loop. That is a handful of
 * instructions in the hottest path an emulator has - the flags are set by nearly every opcode -
 * and none of it depends on anything but the index.
 */
public class AluOperationBase {
  protected static final int FLAG_C = 0x0001;
  protected static final int FLAG_N = 0x0002;
  protected static final int FLAG_P = 0x0004;
  protected static final int FLAG_V = 0x0004;
  protected static final int FLAG_3 = 0x0008;
  protected static final int FLAG_H = 0x0010;
  protected static final int FLAG_5 = 0x0020;
  protected static final int FLAG_Z = 0x0040;
  protected static final int FLAG_S = 0x0080;

  /** Indexed by the three carry bits an addition or a subtraction leaves, as Fuse indexes them. */
  private static final int[] HALF_CARRY_ADD = {0, FLAG_H, FLAG_H, FLAG_H, 0, 0, 0, FLAG_H};
  private static final int[] HALF_CARRY_SUB = {0, 0, FLAG_H, 0, FLAG_H, 0, FLAG_H, FLAG_H};
  private static final int[] OVERFLOW_ADD = {0, 0, 0, FLAG_V, FLAG_V, 0, 0, 0};
  private static final int[] OVERFLOW_SUB = {0, FLAG_V, 0, 0, 0, 0, FLAG_V, 0};

  /**
   * One entry per byte: the sign and the two undocumented bits, the parity, and both together.
   * Zero is not in them - it is asked of the whole value, because a caller can hand this a 16 bit
   * result whose low byte happens to be zero and that is not a zero result.
   */
  private static final int[] SZ53 = new int[0x100];
  private static final int[] PARITY = new int[0x100];
  private static final int[] SZ53P = new int[0x100];

  static {
    for (int i = 0; i < 0x100; i++) {
      int bits = 0;
      for (int bit = i; bit != 0; bit >>= 1) {
        bits ^= bit & 1;
      }
      PARITY[i] = bits != 0 ? 0 : FLAG_P;
      SZ53[i] = i & (FLAG_3 | FLAG_5 | FLAG_S);
      SZ53P[i] = SZ53[i] | PARITY[i];
    }
  }

  public int F;
  protected int Q;

  final protected int halfCarryAddTable(int i) {
    return HALF_CARRY_ADD[i];
  }

  final protected int halfCarrySubTable(int i) {
    return HALF_CARRY_SUB[i];
  }

  final protected int overflowAddTable(int i) {
    return OVERFLOW_ADD[i];
  }

  final protected int overflowSubTable(int i) {
    return OVERFLOW_SUB[i];
  }

  final protected int sz53Table(int i) {
    return SZ53[i & 0xff] | (i == 0 ? FLAG_Z : 0);
  }

  final protected int sz53pTable(int i) {
    return SZ53P[i & 0xff] | (i == 0 ? FLAG_Z : 0);
  }

  final protected int parityTable(int i) {
    return PARITY[i & 0xff];
  }
}
