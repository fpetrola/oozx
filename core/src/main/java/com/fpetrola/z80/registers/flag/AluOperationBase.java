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
  public static final int FLAG_5 = 0x20;
  public static final int FLAG_3 = 0x08;
  protected final static int byteSize = 8;
  // for setting
  protected final static int FLAG_S = 0x0080;
  protected final static int FLAG_Z = 0x0040;
  protected final static int FLAG_H = 0x0010;
  protected final static int FLAG_PV = 0x0004;
  protected final static int FLAG_N = 0x0002;
  protected final static int FLAG_C = 0x0001;
  /* LSB, MSB masking values */
  protected final static int lsb = 0x00FF;
  protected final static int lsw = 0x0000FFFF;
  protected final static int setBit0 = 0x0001; // or
  // mask
  // value
  protected final static int setBit1 = 0x0002;
  protected final static int setBit2 = 0x0004;
  protected final static int setBit3 = 0x0008;
  protected final static int setBit4 = 0x0010;
  protected final static int setBit5 = 0x0020;
  protected final static int setBit6 = 0x0040;
  protected final static int setBit7 = 0x0080;
  protected static final boolean[] parity = new boolean[256];
  // for resetting
  private final static int flag_S_N = 0x007F;
  private final static int flag_Z_N = 0x00BF;
  private final static int flag_5_N = 0x00DF;
  private final static int flag_H_N = 0x00EF;
  private final static int flag_3_N = 0x00F7;
  private final static int flag_PV_N = 0x00FB;
  private final static int flag_N_N = 0x00FD;
  private final static int flag_C_N = 0x00FE;
  private final static int msb = 0xFF00;
  private final static int resetBit0 = setBit0 ^ 0x00FF; // and
  // mask
  // value
  private final static int resetBit1 = setBit1 ^ 0x00FF;
  private final static int resetBit2 = setBit2 ^ 0x00FF;
  private final static int resetBit3 = setBit3 ^ 0x00FF;
  private final static int resetBit4 = setBit4 ^ 0x00FF;
  private final static int resetBit5 = setBit5 ^ 0x00FF;
  private final static int resetBit6 = setBit6 ^ 0x00FF;
  private final static int resetBit7 = setBit7 ^ 0x00FF;

  protected int data;

  public AluOperationBase(String name) {
  }

  static {
    parity[0] = true; // even parity seed value
    int position = 1; // table position
    for (int bit = 0; bit < byteSize; bit++) {
      for (int fill = 0; fill < position; fill++) {
        parity[position + fill] = !parity[fill];
      }
      position = position * 2;
    }
  }

  /* half carry flag control */
  public final void setHalfCarryFlagAdd(int left, int right, int carry) {
    left = left & 0x000f;
    right = right & 0x000f;
    setH((right + left + carry) > 0x0f);
  }

  /* half carry flag control */
  protected final void setHalfCarryFlagAdd(int left, int right) {
    left = left & 0x000F;
    right = right & 0x000F;
    setH((right + left) > 0x0F);
  }

  /* half carry flag control */
  protected final void setHalfCarryFlagSub(int left, int right) {
    left = left & 0x000F;
    right = right & 0x000F;
    setH(left < right);
  }

  /* half carry flag control */
  protected final void setHalfCarryFlagSub(int left, int right, int carry) {
    left = left & 0x000F;
    right = right & 0x000F;
    setH(left < (right + carry));
  }

  /* half carry flag control */
  /*
   * private final void setHalfCarryFlagSub16(int left, int right, int carry) {
   * left = left & 0x0FFF; right = right & 0x0FFF; setH ( left < (right+carry) );
   * }
   */
  /* 2's compliment overflow flag control */
  public void setOverflowFlagAdd(int left, int right, int carry) {
    if (left > 127)
      left = left - 256;
    if (right > 127)
      right = right - 256;
    left = left + right + carry;
    setPV((left < -128) || (left > 127));
  }

  /* 2's compliment overflow flag control */
  private void setOverflowFlagAdd(int left, int right) {
    if (left > 127)
      left = left - 256;
    if (right > 127)
      right = right - 256;
    left = left + right;
    setPV((left < -128) || (left > 127));
  }

  /* 2's compliment overflow flag control */
  protected void setOverflowFlagAdd16(int left, int right, int carry) {
    if (left > 32767)
      left = left - 65536;
    if (right > 32767)
      right = right - 65536;
    left = left + right + carry;
    setPV((left < -32768) || (left > 32767));
  }

  /* 2's compliment overflow flag control */
  protected void setOverflowFlagSub(int left, int right, int carry) {
    if (left > 127)
      left = left - 256;
    if (right > 127)
      right = right - 256;
    left = left - right - carry;
    setPV((left < -128) || (left > 127));
  }

  /* 2's compliment overflow flag control */
  protected void setOverflowFlagSub(int left, int right) {
    if (left > 127)
      left = left - 256;
    if (right > 127)
      right = right - 256;
    left = left - right;
    setPV((left < -128) || (left > 127));
  }

  /* 2's compliment overflow flag control */
  protected void setOverflowFlagSub16(int left, int right, int carry) {
    if (left > 32767)
      left = left - 65536;
    if (right > 32767)
      right = right - 65536;
    left = left - right - carry;
    setPV((left < -32768) || (left > 32767));
  }

  /*
   * test & set flag states
   */
  private final boolean getS() {
    return ((data & FLAG_S) != 0);
  }

  public boolean getZ() {
    return ((data & FLAG_Z) != 0);
  }

  protected final boolean getH() {
    return ((data & FLAG_H) != 0);
  }

  private final boolean getPV() {
    return ((data & FLAG_PV) != 0);
  }

  protected final boolean getN() {
    return ((data & FLAG_N) != 0);
  }

  public final boolean getC() {
    return ((data & FLAG_C) != 0);
  }

  protected final void setS() {
    data = data | FLAG_S;
  }

  protected final void setZ() {
    data = data | FLAG_Z;
  }

  protected final void setH() {
    data = data | FLAG_H;
  }

  protected final void setPV() {
    data = data | FLAG_PV;
  }

  protected final void setN() {
    data = data | FLAG_N;
  }

  protected final void setC() {
    data = data | FLAG_C;
  }

  protected final void setS(boolean b) {
    if (b)
      setS();
    else
      resetS();
  }

  protected final void setZ(boolean b) {
    if (b)
      setZ();
    else
      resetZ();
  }

  protected final void setH(boolean b) {
    if (b)
      setH();
    else
      resetH();
  }

  public final void setPV(boolean b) {
    if (b)
      data = data | FLAG_PV;
    else
      data = data & flag_PV_N;
  }

  protected final void setUnusedFlags(int value) {
    value = value & 0x28;
    data = data & 0xD7;
    data = data | value;
  }

  // private final void setN(boolean b) { if (b) setN(); else resetN(); }
  protected final void setC(boolean b) {
    if (b)
      setC();
    else
      resetC();
  }

  protected final void resetS() {
    data = data & flag_S_N;
  }

  protected final void resetZ() {
    data = data & flag_Z_N;
  }

  public final void resetH() {
    data = data & flag_H_N;
  }

  protected final void resetPV() {
    data = data & flag_PV_N;
  }

  public final void resetN() {
    data = data & flag_N_N;
  }

  protected final void resetC() {
    data = data & flag_C_N;
  }


  protected final void set3(boolean b) {
    if (b)
      set3();
    else
      reset3();
  }

  protected final void reset3() {
    data = data & flag_3_N;
  }

  protected final void set3() {
    data = data | FLAG_3;
  }

  protected final void set5(boolean b) {
    if (b)
      set5();
    else
      reset5();
  }

  protected final void set5() {
    data = data | FLAG_5;
  }

  protected final void reset5() {
    data = data & flag_5_N;
  }


  public final static int[] TABLE_SZ = new int[]{
      0x40, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
      0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80
  };
  public final static int[] TABLE_XY = new int[]{
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28, 0x28,
  };

  public final static short[] PARITY_TABLE = {
      4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4,
      0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0,
      0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0,
      4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4
  };
}
