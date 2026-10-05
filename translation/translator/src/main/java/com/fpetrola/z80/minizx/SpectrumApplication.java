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

package com.fpetrola.z80.minizx;

import java.util.function.IntConsumer;
import java.util.Map;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.minizx.sync.SyncChecker;
import java.lang.reflect.Method;
import java.util.*;

public abstract class SpectrumApplication {
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
  static public IO io;

  public boolean isOwnAddress(StackException stackException, int... integers) {
    nextAddress = stackException.getNextPC();
    return Arrays.stream(integers).anyMatch(a -> a == nextAddress);
  }

  public int executeMutantCode(int address) {
    int opcode = mem[address], n = mem[address + 1 & 0xffff], nn = n | mem[address + 2 & 0xffff] << 8;
    if ((opcode & 0xc7) == 0x06) {
      write8((opcode >> 3) & 7, n);
      return address + 2;
    } else if ((opcode & 0xcf) == 0x01)
      new IntConsumer[]{this::BC, this::DE, this::HL, this::SP}[opcode >> 4].accept(nn);
    else if (opcode == 0xcd)
      invokeMethod(nn);
    else if ((opcode & 0xc0) == 0x40 && opcode != 0x76) {
      write8(opcode >> 3 & 7, read8(opcode & 7));
      return address + 1;
    } else if ((opcode & 0xe7) == 0x07) {
      A(alu(new String[]{"rlca", "rrca", "rla", "rra"}[opcode >> 3], A));
      return address + 1;
    } else if (opcode == 0x12) {
      mem[DE()] = A;
      return address + 1;
    } else
      throw new IllegalStateException("self-modified opcode %02X at %04X".formatted(opcode, address));
    return address + 3;
  }


  private int read8(int register) {
    return switch (register) {
      case 0 -> B();
      case 1 -> C();
      case 2 -> D();
      case 3 -> E();
      case 4 -> H();
      case 5 -> L();
      case 6 -> mem[HL()];
      default -> A();
    };
  }

  private void write8(int register, int value) {
    switch (register) {
      case 0 -> B(value);
      case 1 -> C(value);
      case 2 -> D(value);
      case 3 -> E(value);
      case 4 -> H(value);
      case 5 -> L(value);
      case 6 -> mem[HL()] = value;
      default -> A(value);
    }
  }

  public int codeHash(int start, int length) {
    return Arrays.hashCode(Arrays.copyOfRange(mem, start, start + length));
  }

  public void unknownCodeVariant(int address) {
    throw new IllegalStateException("code at %04X was rewritten into a shape that was not translated".formatted(address));
  }

  public void untranslated(int address) {
    throw new IllegalStateException("no translated routine at %04X".formatted(address));
  }

  public void jump(int address) {
    String name = "$" + Integer.toHexString(address).toUpperCase();
    if (StackWalker.getInstance().walk(frames -> frames.anyMatch(frame -> frame.getMethodName().equals(name))))
      throw new StackException(address);
    invokeMethod(address);
  }

  protected void invokeMethod(int address) {
    try {
      Method method;
      try {
        method = getClass().getMethod("$" + Integer.toHexString(address).toUpperCase());
      } catch (NoSuchMethodException decimalNamed) {
        method = getClass().getMethod("$" + address);
      }
      method.invoke(this);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtime)
        throw runtime;
      throw new RuntimeException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("no translated routine at %04X".formatted(address), e);
    }
  }



  private final Register aluTarget = new Plain8BitRegister("target");
  private final Register aluSource = new Plain8BitRegister("source");
  private final Register aluFlag = new Plain8BitRegister("F");
  private final Register wideTarget = new Plain16BitRegister("wideTarget");
  private final Register wideSource = new Plain16BitRegister("wideSource");
  private final Map<String, Instruction> alu = Map.ofEntries(
      Map.entry("add", new Add(aluTarget, aluSource, aluFlag)), Map.entry("adc", new Adc(aluTarget, aluSource, aluFlag)),
      Map.entry("sub", new Sub(aluTarget, aluSource, aluFlag)), Map.entry("sbc", new Sbc(aluTarget, aluSource, aluFlag)),
      Map.entry("and", new And(aluTarget, aluSource, aluFlag)), Map.entry("or", new Or(aluTarget, aluSource, aluFlag)),
      Map.entry("xor", new Xor(aluTarget, aluSource, aluFlag)), Map.entry("cp", new Cp(aluTarget, aluSource, aluFlag)),
      Map.entry("inc", new Inc(aluTarget, aluFlag)), Map.entry("dec", new Dec(aluTarget, aluFlag)),
      Map.entry("neg", new Neg(aluTarget, aluFlag)), Map.entry("cpl", new CPL(aluTarget, aluFlag)), Map.entry("daa", new DAA(aluTarget, aluFlag)),
      Map.entry("scf", new SCF(aluFlag, aluTarget)), Map.entry("ccf", new CCF(aluFlag, aluTarget)),
      Map.entry("rlca", new RLCA(aluTarget, aluFlag)), Map.entry("rrca", new RRCA(aluTarget, aluFlag)),
      Map.entry("rla", new RLA(aluTarget, aluFlag)), Map.entry("rra", new RRA(aluTarget, aluFlag)),
      Map.entry("rlc", new RLC(aluTarget, aluFlag)), Map.entry("rrc", new RRC(aluTarget, aluFlag)),
      Map.entry("rl", new RL(aluTarget, aluFlag)), Map.entry("rr", new RR(aluTarget, aluFlag)),
      Map.entry("sla", new SLA(aluTarget, aluFlag)), Map.entry("sra", new SRA(aluTarget, aluFlag)),
      Map.entry("srl", new SRL(aluTarget, aluFlag)), Map.entry("sll", new SLL(aluTarget, aluFlag)),
      Map.entry("add16", new Add16(wideTarget, wideSource, aluFlag)), Map.entry("adc16", new Adc16(wideTarget, wideSource, aluFlag)),
      Map.entry("sbc16", new Sbc16(wideTarget, wideSource, aluFlag)));

  public int alu(String operation, int target, int source) {
    Instruction instruction = alu.get(operation);
    boolean wide = operation.endsWith("16");
    Register result = wide ? wideTarget : aluTarget;
    result.write(target);
    (wide ? wideSource : aluSource).write(source);
    aluFlag.write(F);
    instruction.execute();
    F(aluFlag.read());
    return result.read();
  }

  public int alu(String operation, int target) {
    return alu(operation, target, 0);
  }

  public void bit(int n, int value) {
    BIT bit = new BIT(aluTarget, n, aluFlag, new Plain16BitRegister("memptr"));
    aluTarget.write(value);
    aluFlag.write(F);
    bit.execute();
    F(aluFlag.read());
  }

  public boolean flag(int mask, boolean negate) {
    return ((F & mask) == mask) != negate;
  }

  public int inc16(int value1) {
    return (value1 + 1) & 0xffff;
  }


  public int dec16(int value1) {
    return (value1 - 1) & 0xffff;
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
    SP = SP - 2 & 0xffff;
    mem[SP] = value & 0xff;
    mem[SP + 1 & 0xffff] = value >> 8 & 0xff;
  }

  public int pop() {
    int value = mem[SP] | mem[SP + 1 & 0xffff] << 8;
    SP = SP + 2 & 0xffff;
    return value;
  }




  public boolean isNextPC(int nextPC) {
    boolean matches = nextAddress == nextPC;
    if (matches)
      nextAddress = 0;
    return matches;
  }

  public SpectrumApplication() {
    Arrays.fill(mem, 0);
    io = new DefaultMiniZXIO();
  }

  public int in(int port, int pc) {
    return io.in(port);
  }

  public int inC(int port, int pc) {
    int value = in(port, pc);
    aluFlag.write(F);
    new In.InAluOperation().execute2ValuesAndCarry(value, F, aluFlag);
    F(aluFlag.read());
    return value;
  }

  public int mem(int address, int pc) {
    return mem[address];
  }

  public void wMem(int address, int value, int pc) {
    mem[address] = value;
  }

  public void wMem16(int address, int value, int pc) {
    mem[address] = value & 0xFF;
    mem[address + 1] = value >>> 8;
  }

  public int mem16(int address, int pc) {
    return (mem[address + 1] << 8) + mem[address];
  }

  public int mem(int address) {
    return mem[address];
  }

  public void wMem(int address, int value) {
    mem[address] = value;
  }

  public static void waitNanos(int i) {
    long start = System.nanoTime();
    while (start + i >= System.nanoTime()) ;
  }

  public void pc(int address, int rdelta) {
    PC = address;
  }

  public void halt(int address) {
  }

  public void ldir(int address) {
    ldi();
    while (BC() != 0) {
      pc(address, 2);
      ldi();
    }
  }

  public void ldi() {
    blockStep(1, true);
  }

  public void lddr(int address) {
    ldd();
    while (BC() != 0) {
      pc(address, 2);
      ldd();
    }
  }

  public void ldd() {
    blockStep(-1, true);
  }

  public void cpir(int address) {
    cpi();
    while (BC() != 0 && (F & 0x40) == 0) {
      pc(address, 2);
      cpi();
    }
  }

  public void cpi() {
    blockStep(1, false);
  }

  public void cpdr(int address) {
    cpd();
    while (BC() != 0 && (F & 0x40) == 0) {
      pc(address, 2);
      cpd();
    }
  }

  public void cpd() {
    blockStep(-1, false);
  }

  private void blockStep(int direction, boolean copy) {
    int hl = HL();
    if (copy) {
      int de = DE();
      mem[de] = mem[hl];
      DE(de + direction & 0xffff);
    }
    HL(hl + direction & 0xffff);
    BC(BC() - 1 & 0xffff);
    int carry = F & 0x01;
    if (!copy)
      alu("cp", A, mem[hl]);
    F(F & (copy ? ~0x16 : ~0x05) | (copy ? 0 : carry) | (BC() != 0 ? 0x04 : 0));
  }


  public void loadState(State state) {
    System.arraycopy(state.getMemory().getData(), 0, mem, 0, mem.length);
    AF(state.getRegister(RegisterName.AF).read());
    BC(state.getRegister(RegisterName.BC).read());
    DE(state.getRegister(RegisterName.DE).read());
    HL(state.getRegister(RegisterName.HL).read());
    AFx(state.getRegister(RegisterName.AFx).read());
    BCx(state.getRegister(RegisterName.BCx).read());
    DEx(state.getRegister(RegisterName.DEx).read());
    HLx(state.getRegister(RegisterName.HLx).read());
    IX(state.getRegister(RegisterName.IX).read());
    IY(state.getRegister(RegisterName.IY).read());
    SP(state.getRegisterSP().read());
    R(state.getRegisterR().read());
    I = state.getRegI().read();
    iff = state.isIff1();
    interruptMode = state.getInterruptionMode().ordinal();
  }

  public void im(int mode) {
    interruptMode = mode;
  }

  public void ei() {
    iff = true;
    interruptsDelayed = true;
  }

  public void di() {
    iff = false;
  }

  public boolean isIff() {
    return iff;
  }



  public void AF(int value) {
    AF = value & 0xffff;
    A = AF >> 8;
    F = AF & 0xFF;
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
  protected boolean iff;
  protected boolean interruptsDelayed;
  protected int interruptMode = 1;

  public int R() {
    return R;
  }

  public int ldAR() {
    F(F & 0x01 | R & 0xa8 | (R == 0 ? 0x40 : 0) | (iff ? 0x04 : 0));
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
    return io.in(port);
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

    protected void pc(char c) {
      pc(c, 1);
    }
}
