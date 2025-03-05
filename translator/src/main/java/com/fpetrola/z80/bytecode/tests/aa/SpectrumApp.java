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

package com.fpetrola.z80.bytecode.tests.aa;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.minizx.*;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Stack;

public abstract class SpectrumApp<T> {

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

  public int nextAddress = 0;
  public int initial;

  public int[] mem = new int[0x10000];
  static public IO<WordNumber> io;
  private final Stack<Integer> stack = new Stack<>();
  protected int carry;

  public boolean isOwnAddress(StackException stackException, int... integers) {
    nextAddress = stackException.getNextPC();
    return Arrays.stream(integers).anyMatch(a -> a == nextAddress);
  }

  public void pc(int address) {
  }

  public void executeMutantCode(int address) {
    if (mem[address] == 0x77) {
      wMem(HL(), A, address);
    } else if (mem[address] == 0x7E) {
      A = mem(HL(), address);
    } else if (mem[address] == 0x12) {
      wMem(DE(), A, address);
    } else if (mem[address] == 0x16) {
      D = mem[address + 1];
    } else if (mem[address] == 0x06) {
      B = mem[address + 1];
    } else if (mem[address] == 0x11) {
      int value = mem16(address + 1);
      D = value >> 8;
      E = value & 0xFF;
    } else if (mem[address] == 0x3E) {
      A = mem[address + 1];
    } else if (mem[address] == 0x01) {
      int value = mem16(address + 1);
      B = value >> 8;
      C = value & 0xFF;
    } else if (mem[address] == 0xCD) {
      invokeMethod(mem16(address + 1));
    }

//    System.out.println("mutant at: " + address);
  }

  private void invokeMethod(int address) {
    try {
      String formatted = "$%04X".formatted(address);
      Method method = getClass().getMethod(formatted);
      method.invoke(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void SP(int value) {
    SP = value;
  }

  public int SP() {
    return SP;
  }

  public int ex_iSP_REG(int reg) {
    int temp1 = pop();
    push(reg);
    return temp1;
  }

  public int exAF(int AF) {
    int temp1 = AFx();
    AFx(AF());
    A = (temp1 & 0xffff) >> 8;
    F = temp1 & 0xffff & 0xFF;
    A = AF() >> 8;
    // F = AF & 0xFF;
    return temp1;
  }

  public void exHLDE() {
    int temp1 = HL();
    int value = DE();
    H = value >> 8;
    L = value & 0xFF;
    D = temp1 >> 8;
    E = temp1 & 0xFF;
  }

  public int exx() {
    int temp1 = BCx();
    BCx(BC());
    B = temp1 >> 8;
    C = temp1 & 0xFF;

    int temp2 = DEx();
    DEx(DE());
    D = temp2 >> 8;
    E = temp2 & 0xFF;

    int temp3 = HLx();
    HLx(HL());
    H = temp3 >> 8;
    L = temp3 & 0xFF;
    return temp1;
  }

  public void push(int value) {
    stack.push(value);
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

  public SpectrumApp() {
    Arrays.fill(getMem(), 0);
  }

  public int in(int port, int pc) {
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
//    waitNanos(40);
    return getMem()[address] & 0xff;
  }

  public void wMem(int address, int value) {
//    waitNanos(40);
    getMem()[address] = value & 0xff;
  }

  public void waitNanos(int i) {
    long start = System.nanoTime();
    while (start + i >= System.nanoTime()) ;
  }

  public void waitMilis(int i) {
    long start = System.currentTimeMillis();
    while (start + i >= System.currentTimeMillis()) ;
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
//      wMem(DE(), mem(HL()));
      mem[DE()] = mem[HL()];
      int value = BC() - 1;
      B = value >> 8;
      C = value & 0xFF;
      int value2 = HL() + 1;
      H = value2 >> 8;
      L = value2 & 0xFF;
      int value1 = DE() + 1;
      D = value1 >> 8;
      E = value1 & 0xFF;
    }
  }

  public void ldi() {
    mem[DE()] = mem[HL()];
    int value = BC() - 1;
    B = value >> 8;
    C = value & 0xFF;
    int value2 = HL() + 1;
    H = value2 >> 8;
    L = value2 & 0xFF;
    int value1 = DE() + 1;
    D = value1 >> 8;
    E = value1 & 0xFF;
    if (BC() == 0)
      F = -1;
    else
      F = 1;
  }

  public void lddr() {
    while (BC() != 0) {
      wMem(DE(), mem(HL()));
      int value = BC() - 1;
      B = value >> 8;
      C = value & 0xFF;
      int value2 = HL() - 1;
      H = value2 >> 8;
      L = value2 & 0xFF;
      int value1 = DE() - 1;
      D = value1 >> 8;
      E = value1 & 0xFF;
    }
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
    do {
      result = mem(HL());
      int value = BC() - 1;
      B = value >> 8;
      C = value & 0xFF;
      int value1 = HL() + 1;
      H = value1 >> 8;
      L = value1 & 0xFF;
    } while (BC() != 0 && result != (A & 0xff));
  }

  public void cpdr() {

  }

  public int pair(int a, int f) {
    return ((a & 0xFF) << 8) | (f & 0xFF);
  }

//  public int[] rlc(int a, int F) {
//    F = (a & 128) >> 7;
//    int i = ((a << 1) & 0xfe) | (a & 0xFF) >> 7;
//    return new int[]{i & 0xff, F};
//  }
//
//  public int[] rl(int a, int F) {
//    int lastCarry = carry(F) & 0x01;
//    F = (a & 128) >> 7;
//    int i = ((a << 1) & 0xfe) | lastCarry;
//    return new int[]{i & 0xff, F};
//  }

  public int rrc(int a) {
    F = carry = a & 1;
    return ((a & 0xff) >> 1) | ((a & 0x01) << 7) & 0xff;
  }

  public int rr(int a) {
    int lastCarry = (carry(F) & 0x01) << 7;
    F = carry = a & 1;
    return ((a & 0xff) >> 1) | lastCarry;
  }

  public int rlc(int a) {
    F = carry = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | (a & 0xFF) >> 7;
  }

  public int rl(int a) {
    int lastCarry = carry & 0x01;
    F = carry = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | lastCarry;
  }

  public int sl(int a) {
    int lastCarry = 0;
    F = carry = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | lastCarry;
  }

  public int sr(int a) {
    F = carry = (a & 1);
    return ((a & 0xff) >> 1);
  }

  public int getCarry() {
    return carry;
  }

  public void ccf() {
    carry = ~carry;
  }

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
  public int PC;
  public int SP;
  public int I;

  public int R() {
    return R;
  }

  public void R(int r) {
    R = r;
  }

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

  public int AFx() {
    return ((Ax & 0xFF) << 8) | (Fx & 0xFF);
  }

  public int BCx() {
    return ((Bx & 0xFF) << 8) | (Cx & 0xFF);
  }

  public int DEx() {
    return ((Dx & 0xFF) << 8) | (Ex & 0xFF);
  }

  public int HLx() {
    return ((Hx & 0xFF) << 8) | (Lx & 0xFF);
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

  public int in(int port) {
    return io.in(WordNumber.createValue(port)).intValue();
  }
}
