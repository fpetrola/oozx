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

package com.fpetrola.z80.registers;

import com.fpetrola.z80.opcodes.references.WordNumber;

public class Flags {

  public final static int CARRY_FLAG = 0x01;
  public final static int NEGATIVE_FLAG = 0x02;
  public final static int PARITY_FLAG = 0x04;
  public final static int X_FLAG = 0x0800;
  public final static int HALF_CARRY_FLAG = 0x10;
  public final static int Y_FLAG = 0x200;
  public final static int ZERO_FLAG = 0x40;
  public final static int SIGNIFICANT_FLAG = 0x80;

  /**
   * Set/Unset a flag (or a set of flags) in the flag register
   *
   * @param r    reference to register flag
   * @param flag flags to set
   * @param set  if true will set, unset otherwise
   */
  public final static <T extends WordNumber> void setFlag(Register<T> r, int flag, boolean set) {
    final int currentFlags = r.read().intValue();

    if (set) {
      r.write(WordNumber.createValue(currentFlags | flag));
    } else {
      r.write(WordNumber.createValue(currentFlags & ~(flag)));
    }
  }

  /**
   * Get if a flag or set of flags are set in the flag register
   *
   * @param r    reference to the flag register
   * @param flag flags to check
   * @return true if all flags passed in flag param are set
   */
  public final static <T extends WordNumber> boolean getFlag(Register<T> r, int flag) {

    final int currentFlags = r.read().intValue();

    return ((currentFlags & flag) == flag);

  }

  /**
   * Copy all flags from a value. This will apply the flag as mask on the
   * value and set on the flag register. After the operation, for each flag
   * passed on the flag param the bit will match the value and for the other
   * values will remain untouched on the flag register.
   *
   * @param r     reference to the flag register
   * @param flag  flags to copy from value
   * @param value a value that will be used as reference for the flags
   */
  public final static <T extends WordNumber> void copyFrom(Register<T> r, int flag, int value) {
    final int currentFlag = r.read().intValue() & ~(flag);
    r.write(WordNumber.createValue(currentFlag | (value & flag)));
  }

  public final static String toString(int flag) {

    StringBuilder sb = new StringBuilder();

    if ((flag & CARRY_FLAG) == CARRY_FLAG)
      sb.append('C');

    if ((flag & NEGATIVE_FLAG) == NEGATIVE_FLAG)
      sb.append('N');

    if ((flag & PARITY_FLAG) == PARITY_FLAG)
      sb.append('P');

    if ((flag & X_FLAG) == X_FLAG)
      sb.append('X');

    if ((flag & HALF_CARRY_FLAG) == HALF_CARRY_FLAG)
      sb.append('H');

    if ((flag & Y_FLAG) == Y_FLAG)
      sb.append('Y');

    if ((flag & ZERO_FLAG) == ZERO_FLAG)
      sb.append('Z');

    if ((flag & SIGNIFICANT_FLAG) == SIGNIFICANT_FLAG)
      sb.append('S');

    return sb.toString();

  }

}
