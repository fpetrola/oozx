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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.Arrays;
import java.util.Stack;

public abstract class SpectrumApplication<T> {
  public int A;
  public int F;
  public int B;
  public int C;
  public int D;
  public int E;
  public int H;
  public int L;
  public int IXH;
  public int IXL;
  public int IYH;
  public int IYL;

  public int nextAddress;
  public int initial;

  public int[] mem = new int[0x10000];
  static public IO<WordNumber> io;
  private final Stack<Integer> stack = new Stack<>();

  public int exAF(int AF) {
    int temp1 = AFx;
    AFx(AF);
    AF(temp1);
    return temp1;
  }

  public void exHLDE() {
    int temp1 = HL();
    HL(DE());
    DE(temp1);
  }

  public int exx() {
    int temp1 = BCx;
    BCx(BC);
    BC(temp1);

    int temp2 = DEx;
    DEx(DE);
    DE(temp2);

    int temp3 = HLx;
    HLx(HL);
    HL(temp2);
    return temp1;
  }

  public void push(int value) {
    stack.push(value);
    if (stack.size() > 100)
      System.out.println("mmmmmm push");
  }

  public int pop() {
    return stack.pop();
  }

  public int carry(int f) {
    return f & 1;
  }

  public boolean isNextPC(int nextPC) {
    boolean matches = nextAddress == nextPC;
    if (matches)
      nextAddress = 0;
    return matches;
  }

  public SpectrumApplication() {
    Arrays.fill(getMem(), 0);
  }

  public int in(int port) {
    return io.in(WordNumber.createValue(port)).intValue();
  }

  public int l(int value) {
    return value & 0xff;
  }

  public int h(int value) {
    return value >> 8 & 0xff;
  }

  public int reg16low(int reg16, int low) {
    return reg16 & 255 | low << 8;
  }

  public int reg16high(int reg16, int high) {
    return reg16 & 0xFF00 | high;
  }

  public int mem(int address, int pc) {
    return getMem()[address] & 0xff;
  }

  public int mem(int address, int pc, int AF, int BC, int DE, int HL, int IX, int IY, int A, int F, int B, int C, int D, int E, int H, int L, int IXL, int IXH, int IYL, int IYH) {
    updateRegisters(AF, BC, DE, HL, IX, IY, A, F, B, C, D, E, H, L, IXL, IXH, IYL, IYH);
    return mem(address, pc);
  }

  private void updateRegisters(int AF, int BC, int DE, int HL, int IX, int IY, int A, int F, int B, int C, int D, int E, int H, int L, int IXL, int IXH, int IYL, int IYH) {
    this.AF = AF;
    this.BC = BC;
    this.DE = DE;
    this.HL = HL;
    this.IX = IX;
    this.IY = IY;
    this.A = A;
    this.F = F;
    this.B = B;
    this.C = C;
    this.D = D;
    this.H = H;
    this.L = L;
    this.IXL = IXL;
    this.IXH = IXH;
    this.IYL = IYL;
    this.IYH = IYH;
  }

  public void wMem(int address, int value, int pc) {
    wMem(address, value);
  }

  public void wMem(int address, int value, int pc, int AF, int BC, int DE, int HL, int IX, int IY, int A, int F, int B, int C, int D, int E, int H, int L, int IXL, int IXH, int IYL, int IYH) {
    updateRegisters(AF, BC, DE, HL, IX, IY, A, F, B, C, D, E, H, L, IXL, IXH, IYL, IYH);
    wMem(address, value, pc);
  }

  public void wMem16(int address, int value, int pc) {
    getMem()[address] = value & 0xFF;
    getMem()[address + 1] = value >> 8;
  }

  public void wMem16(int address, int value, int pc, int AF, int BC, int DE, int HL, int IX, int IY, int A, int F, int B, int C, int D, int E, int H, int L, int IXL, int IXH, int IYL, int IYH) {
    updateRegisters(AF, BC, DE, HL, IX, IY, A, F, B, C, D, E, H, L, IXL, IXH, IYL, IYH);
    wMem16(address, value, pc);
  }

  public int mem16(int address, int pc) {
    return mem(address + 1) * 256 + mem(address);
  }

  public int mem16(int address, int pc, int AF, int BC, int DE, int HL, int IX, int IY, int A, int F, int B, int C, int D, int E, int H, int L, int IXL, int IXH, int IYL, int IYH) {
    updateRegisters(AF, BC, DE, HL, IX, IY, A, F, B, C, D, E, H, L, IXL, IXH, IYL, IYH);
    return mem16(address, pc);
  }

  public int mem(int address) {
    return getMem()[address] & 0xff;
  }

  public void wMem(int address, int value) {
    long start = System.nanoTime();
    while (start + 4000 >= System.nanoTime()) ;
    getMem()[address] = value & 0xff;
  }

  public void wMem16(int address, int value) {
    value = value & 0xffff;
    getMem()[address + 1] = value >> 8;
    getMem()[address] = value & 0xFF;
  }

  public int mem16(int i) {
    return (mem(i + 1) * 256 + mem(i)) & 0xffff;
  }

  public int[] result(int... results) {
    return results;
  }

  public int[] ldir(int HL, int DE, int BC) {
    while (BC != 0) {
      wMem(DE, mem(HL));
      BC--;
      HL++;
      DE++;
    }
    return new int[]{HL, DE, BC};
  }

  public void ldir() {
    while (BC() != 0) {
      wMem(DE(), mem(HL()));
      BC(BC() - 1);
      HL(HL() + 1);
      DE(DE() + 1);
    }
  }

  public void lddr() {

  }

  public int[] cpir(int HL, int BC, int A) {
    int result = -1;
    while (BC != 0 && result != A) {
      result = mem(HL);
      BC--;
      HL++;
    }
    return new int[]{HL, BC};
  }

  public void cpir() {
    int result = -1;
    while (BC() != 0 && result != A) {
      result = mem(HL());
      BC(BC() - 1);
      HL(HL() + 1);
    }
  }

  public void cpdr() {

  }

  public void AF(int value) {
    AF = value & 0xffff;
    A = AF >> 8;
    F = AF & 0xFF;
  }

  public void BC(int value) {
    BC = value & 0xffff;
    B = BC >> 8;
    C = BC & 0xFF;
  }

  public void DE(int value) {
    DE = value & 0xffff;
    D = DE >> 8;
    E = DE & 0xFF;
  }

  public void HL(int value) {
    HL = value & 0xffff;
    H = HL >> 8;
    L = HL & 0xFF;
  }

  public void IX(int value) {
    IX = value & 0xffff;
    IXH = IX >> 8;
    IXL = IX & 0xFF;
  }

  public void IY(int value) {
    IY = value & 0xffff;
    IYH = IY >> 8;
    IYL = IY & 0xFF;
  }

  public int pair(int a, int f) {
    return ((a & 0xFF) << 8) | (f & 0xFF);
  }

  public int[] rlc(int a, int F) {
    F = (a & 128) >> 7;
    int i = ((a << 1) & 0xfe) | (a & 0xFF) >> 7;
    return new int[]{i & 0xff, F};
  }

  public int[] rl(int a, int F) {
    int lastCarry = carry(F) & 0x01;
    F = (a & 128) >> 7;
    int i = ((a << 1) & 0xfe) | lastCarry;
    return new int[]{i & 0xff, F};
  }

  public int rrc(int a) {
    F = a & 1;
    return ((a & 0xff) >> 1) | ((a & 0x01) << 7) & 0xff;
  }

  public int rlc(int a) {
    F = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | (a & 0xFF) >> 7;
  }

  public int rl(int a) {
    int lastCarry = carry(F) & 0x01;
    F = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | lastCarry;
  }

  public int sl(int a) {
    int lastCarry = 0;
    F = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | lastCarry;
  }

  public void update16Registers() {
    BC(pair(B, C));
    DE(pair(D, E));
    HL(pair(H, L));
    AF(pair(A, F));
    IX(pair(IXH, IXL));
    IY(pair(IYH, IYL));
  }


  public int AF;
  public int BC;
  public int DE;
  public int HL;
  public int Ax;
  public int Fx;
  public int Bx;
  public int Cx;
  public int Dx;
  public int Ex;
  public int Hx;
  public int Lx;
  public int AFx;
  public int BCx;
  public int DEx;
  public int HLx;
  public int IX;
  public int IY;
  public int PC;
  public int SP;
  public int I;
  public int R;
  public int IR;
  public int VIRTUAL;
  public int MEMPTR;

  public void AFx(int value) {
    AFx = value & 0xffff;
    Ax = AFx >> 8;
    Fx = AFx & 0xFF;
  }

  public void BCx(int value) {
    BCx = value & 0xffff;
    Bx = BCx >> 8;
    Cx = BCx & 0xFF;
  }

  public void DEx(int value) {
    DEx = value & 0xffff;
    Dx = DEx >> 8;
    Ex = DEx & 0xFF;
  }

  public void HLx(int value) {
    HLx = value & 0xffff;
    Hx = HLx >> 8;
    Lx = HLx & 0xFF;
  }

  public int AFx() {
    return ((Ax & 0xFF) << 8) | (Fx & 0xFF);
  }

  public int AF() {
    return ((A & 0xFF) << 8) | (F & 0xFF);
  }

  public int BC() {
    return ((B & 0xFF) << 8) | (C & 0xFF);
  }

  public int DE() {
    return ((D & 0xFF) << 8) | (E & 0xFF);
  }

  public int HL() {
    return ((H & 0xFF) << 8) | (L & 0xFF);
  }

  public int IX() {
    return ((IXH & 0xFF) << 8) | (IXL & 0xFF);
  }

  public int IY() {
    return ((IYH & 0xFF) << 8) | (IYL & 0xFF);
  }

  public int[] getMem() {
    return mem;
  }
}
