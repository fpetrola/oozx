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

import com.fpetrola.z80.bytecode.generators.helpers.Composed16BitRegisterVariable;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.minizx.sync.SyncChecker;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.*;

public abstract class SpectrumApplication<T> {
  public SyncChecker syncChecker = new SyncChecker() {
    public int getByteFromEmu(Integer index) {
      return mem[index];
    }
  };

  public static final int INITIAL_SP_VALUE = 1234;
  public Deque<Integer> methodStack = new ArrayDeque<>();
  protected int A;
  protected int F;
  protected int B;
  protected int C;
  protected int D;
  protected int E;
  protected int H;
  protected int L;

  public int IXL;
  public int IXH;
  public int IYL;
  public int IYH;

  private int lastStackDepth;
  private Map<String, Boolean> lastUpdateFrom8 = new HashMap<>();

  public void setNextAddress(int nextAddress) {
    this.nextAddress = nextAddress;
  }

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

  public void executeMutantCode(int address) {
    if (mem[address] == 0x77) {
      wMem(HL(), A, address);
    } else if (mem[address] == 0x7E) {
      A = mem(HL(), address);
    } else if (mem[address] == 0x12) {
      wMem(DE(), A, address);
    } else if (mem[address] == 0x16) {
      D(mem[address + 1]);
    }

//    System.out.println("mutant at: " + address);
  }

  public int cp(int value1, int value2) {
    return value1 - value2;
  }

  public int inc(int value1) {
    return (value1 + 1) & 0xff;
  }

  public int inc16(int value1) {
    return (value1 + 1) & 0xffff;
  }

  public int dec(int value1) {
    return (value1 - 1) & 0xff;
  }

  public int dec16(int value1) {
    return (value1 - 1) & 0xffff;
  }

  public int add(int value1, int value2) {
    return (value1 + value2) & 0xff;
  }

  public int add16(int value1, int value2) {
    return (value1 + value2) & 0xffff;
  }

  public int flagZ(int value1) {
    return value1 << 1;
  }

  public int sra(int value1) {
    return (value1 >> 1) | (value1 & 0x80);
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
    int temp1 = BCx();
    BCx(BC());
    BC(temp1);

    int temp2 = DEx();
    DEx(DE());
    DE(temp2);

    int temp3 = HLx();
    HLx(HL());
    HL(temp3);
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

  public SpectrumApplication() {
    Arrays.fill(getMem(), 0);
    io = new DefaultMiniZXIO();
  }

  public int in(int port, int pc) {
    return io.in(WordNumber.createValue(port)).intValue();
  }

  public int mem(int address, int pc) {
    return getMem()[address] & 0xff;
  }

  public void wMem(int address, int value, int pc) {
    wMem(address, value);
  }

  public void wMem16(int address, int value, int pc) {
    getMem()[address] = value & 0xFF;
    getMem()[address + 1] = value >> 8;
  }

  public int mem16(int address, int pc) {
    return mem(address + 1) * 256 + mem(address);
  }

  public int mem(int address) {
    return getMem()[address] & 0xff;
  }

  public void wMem(int address, int value) {
    getMem()[address] = value & 0xff;
  }

  public static void waitNanos(int i) {
    long start = System.nanoTime();
    while (start + i >= System.nanoTime()) ;
  }

  public void pc(int address, int rdelta) {
    PC = address;
  }

  public void ldir() {
    int bc = BC();
    int de = DE();
    int hl = HL();
    while (bc-- != 0) {
      pc(-1, 16);
      wMem(de++, mem(hl++));
    }
    BC(bc);
    HL(hl);
    DE(de);
  }

  public void lddr() {
    while (BC() != 0) {
      wMem(DE(), mem(HL()));
      BC(BC() - 1);
      HL(HL() - 1);
      DE(DE() - 1);
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
    A= AF >> 8;
    F= AF & 0xFF;
  }

  public void BC(int value) {
    BC = value & 0xffff;
    lastUpdateFrom8.put("B", false);

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
  }

  public void IY(int value) {
    IY = value & 0xffff;
  }

  public int pair(int a, int f) {
    return ((a & 0xFF) << 8) | (f & 0xFF);
  }

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
    int lastCarry = carry(F) & 0x01;
    F = carry = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | lastCarry;
  }

  public int sl(int a) {
    int lastCarry = 0;
    F = carry = (a & 128) >> 7;
    return ((a << 1) & 0xfe) | lastCarry;
  }

  public int sr(int a) {
    F = carry = (a & 1) >> 7;
    return ((a & 0xff) >> 1);
  }

  public int getCarry() {
    return carry;
  }

  public void ccf() {
    carry = ~carry;
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
  public int SP = INITIAL_SP_VALUE;
  protected int I;

  public int R() {
    return R;
  }

  public void R(int r) {
    R = r;
  }

  protected int R;
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
    return AF;
  }

  public int BC() {
    boolean l = lastUpdateFrom8.get("B");
//    if (l) {
//      System.out.println("asfsaf");
//    }
    return BC;
  }

  public int DE() {
    return DE;
  }

  public int HL() {
    return HL;
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
    return IX;
  }

  public int IY() {
    return IY;
  }

  public int[] getMem() {
    return mem;
  }

  public int in(int port) {
    return io.in(WordNumber.createValue(port)).intValue();
  }

  public int A_16() {
    return AF >> 8;
  }

  public int AF_8() {
    int i = A << 8 | AF & 0xff;
//    AF= i;
    return i;
  }

  public int B_16() {
    int i = BC >> 8;
    B = i & 0xff;
    return B;
  }

  public int C_16() {
    int i = BC & 0xff;
    C = i;
    return C;
  }

  public int BC_8() {
    int i = B << 8 | BC & 0xff;
    BC = i;
    return BC;
  }

  public int D_16() {
    int i = DE >> 8;
    D = i;
    return D;
  }

  public int E_16() {
    int i = DE & 0xff;
    E = i;
    return E;
  }

  public int DE_8() {
    int i = D << 8 | DE & 0xff;
//    DE = i;
    return i;
  }

  public int H_16() {
    int i = HL >> 8;
    H = i;
    return H;
  }

  public int L_16() {
    int i = HL & 0xff;
    L = i;
    return L;
  }

  public int HL_8() {
    int i = H << 8 | HL & 0xff;
    HL = i;
    return i;
  }

  public int A() {
    return A;
  }

  public void A(int a) {
    A = a;
    AF = A << 8 | AF & 0xff;
  }

  public int F() {
    return F;
  }

  public void F(int f) {
    F = f;
    AF = AF & 0xff00 | F & 0xff;
  }

  public int B() {
    boolean l = lastUpdateFrom8.get("B");
//    if (!l) {
//      System.out.println("asfsaf");
//    }
    return B;
  }

  public void B(int b) {
    B = b & 0xff;
    lastUpdateFrom8.put("B", true);
    BC = B << 8 | BC & 0xff;
  }

  public void B_16(int b) {
    BC = BC & 0xff | ((b & 0xff) << 8);
    lastUpdateFrom8.put("B", false);
  }

  public int C() {
//    int i = BC & 0xff;
//    if (i != C)
//      System.out.println("asfsaf");
    return C;
  }

  public void C(int c) {
    C = c;
    BC = BC & 0xff00 | c & 0xff;
  }

  public void C_16(int c) {
    C = c;
    BC = BC & 0xff00 | c & 0xff;
  }

  public int D() {
    return D;
  }

  public void D(int d) {
    D = d;
    DE = D << 8 | DE & 0xff;
  }

  public int E() {
    return E;
  }

  public void E(int e) {
    E = e;
    DE = DE & 0xff00 | e & 0xff;
  }

  public int H() {
    int i = (HL & 0xff00) >> 8;
//    if (i != H)
//      System.out.println("asfsaf");
    return H;
  }

  public void H(int h) {
    H = h & 0xff;
    HL = H << 8 | HL & 0xff;
  }

  public int L() {
    int i = (HL & 0xff);
//    if (i != L)
//      System.out.println("asfsaf");
    return L;
  }

  public void L(int l) {
    L = l;
    HL = HL & 0xff00 | l & 0xff;
  }

  public int IXH() {
    return IX >> 8;
  }

  public void IXH(int IXH) {
    this.IX = IXH << 8 | (IX & 0xff);
  }

  public int IXL() {
    return IX & 0xff;
  }

  public void IXL(int IXL) {
    this.IX = (IX & 0xff00) | IXL;
  }

  public int IYH() {
    return IY >> 8;
  }

  public void IYH(int IYH) {
    this.IY = IYH << 8 | (IY & 0xff);
  }

  public int IYL() {
    return IY & 0xff;
  }

  public void IYL(int IYL) {
    this.IY = (IY & 0xff00) | IYL;
  }

  public int I() {
    return I;
  }

  public void I(int i) {
    I = i;
  }

  public int getR() {
    return R;
  }

  public void setR(int r) {
    R = r;
  }
}
