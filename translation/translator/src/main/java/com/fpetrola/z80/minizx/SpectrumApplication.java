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

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.memory.MemoryBanks;
import com.fpetrola.z80.tstates.UncontendedTiming;
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

  public static final int INITIAL_SP_VALUE = 1234, ROM_END = 0x4000;
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

  public int ownAddress(StackException stackException, int... integers) {
    if (isOwnAddress(stackException, integers))
      return nextAddress;
    throw stackException;
  }

  private OOZ80 mutantExecutor;

  public int executeMutantCode(int address) {
    if (mutantExecutor == null)
      mutantExecutor = EmulatedMiniZX.createTimedOOZ80(new DefaultMiniZXIO() {
        public int in(int port) {
          return SpectrumApplication.this.in(port);
        }

        public void out(int port, int value) {
          SpectrumApplication.this.out(port, value);
        }
      });
    ((MockedMemory) mutantExecutor.getState().getMemory()).init(() -> mem);
    State state = mutantExecutor.getState();
    storeRegisters(state);
    state.getPc().write(address);
    state.getRegisterR().write(R);
    long before = state.clock.getTStates();
    Instruction instruction = mutantExecutor.getInstructionFetcher().fetchNextInstruction();
    if (instruction instanceof Call call) {
      boolean taken = call.getCondition().conditionMet(call);
      tstates += costAt(address, call.getLength())[taken ? 1 : 0];
      fetched(1);
      if (taken)
        invokeMethod(call.calculateJumpAddress());
      return address + call.getLength();
    }
    if (instruction instanceof Ret ret) {
      boolean taken = ret.getCondition().conditionMet(ret);
      tstates += costAt(address, ret.getLength())[taken ? 1 : 0];
      fetched(1);
      return taken ? -1 : address + ret.getLength();
    }
    mutantExecutor.getInstructionExecutor().execute(instruction);
    tstates += state.clock.getTStates() - before;
    loadRegisters(state);
    fetchCounter += Math.min(state.getRegisterR().read() - R & 0x7f, 2);
    R = R & 0x80 | state.getRegisterR().read() & 0x7f;
    return state.getPc().read();
  }

  public void fetched(int count) {
    R = R & 0x80 | R + count & 0x7f;
    fetchCounter += count;
  }

  private int[] costAt(int address, int length) {
    return UncontendedTiming.costOf(java.util.Arrays.copyOfRange(mem, address, address + length));
  }

  public void storeRegisters(State state) {
    state.getRegister(RegisterName.AF).write(AF());
    state.getRegister(RegisterName.BC).write(BC());
    state.getRegister(RegisterName.DE).write(DE());
    state.getRegister(RegisterName.HL).write(HL());
    state.getRegister(RegisterName.AFx).write(AFx());
    state.getRegister(RegisterName.BCx).write(BCx());
    state.getRegister(RegisterName.DEx).write(DEx());
    state.getRegister(RegisterName.HLx).write(HLx());
    state.getRegister(RegisterName.IX).write(IX());
    state.getRegister(RegisterName.IY).write(IY());
    state.getRegisterSP().write(SP);
    state.getRegisterR().write(R);
    state.getRegI().write(I);
    state.setIff1(iff);
    state.setIff2(iff2);
    state.setIntMode(State.InterruptionMode.values()[interruptMode]);
  }

  public int codeHash(int start, int length) {
    return Arrays.hashCode(Arrays.copyOfRange(mem, start, start + length));
  }

  public void unknownCodeVariant(int address, int variableStart, int length) {
    StringBuilder bytes = new StringBuilder();
    for (int i = variableStart; i < variableStart + length; i++)
      bytes.append("%02x".formatted(mem[i]));
    throw new IllegalStateException("code at %04X was rewritten into a shape that was not translated: %04X = %s".formatted(address, variableStart, bytes));
  }

  public void untranslated(int address) {
    throw new IllegalStateException("no translated routine at %04X".formatted(address));
  }

  public void jump(int address) {
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
      if (method.invoke(this) instanceof Integer next && next != -1)
        getClass().getMethod("runJumps", int.class).invoke(this, next);
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

  public void rld() {
    int memory = mem[HL()];
    wMem(HL(), (memory << 4 | A & 0x0f) & 0xff);
    nibblesRotated(A & 0xf0 | memory >> 4);
  }

  public void rrd() {
    int memory = mem[HL()];
    wMem(HL(), (A & 0x0f) << 4 | memory >> 4);
    nibblesRotated(A & 0xf0 | memory & 0x0f);
  }

  private void nibblesRotated(int a) {
    int carry = F & 1;
    A(alu("or", a, 0));
    F(F & ~1 | carry);
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
    wMem(SP, value & 0xff);
    wMem(SP + 1 & 0xffff, value >> 8 & 0xff);
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

  private static final int BLOCK_REPEAT = 21, BLOCK_END = 16;
  public long tstates;
  public int fetchCounter;
  public MiniZXSound sound;
  public MemoryBanks banks;

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
    wMem(address, value);
  }

  public void wMem16(int address, int value, int pc) {
    wMem(address, value & 0xFF);
    wMem(address + 1 & 0xffff, value >>> 8);
  }

  public int mem16(int address, int pc) {
    return (mem[address + 1] << 8) + mem[address];
  }

  public int mem(int address) {
    return mem[address];
  }

  public void wMem(int address, int value) {
    if (address >= ROM_END)
      mem[address] = value;
  }

  public static void waitNanos(int i) {
    long start = System.nanoTime();
    while (start + i >= System.nanoTime()) ;
  }

  public void pc(int address, int rdelta) {
    PC = address;
  }

  public void pc(int address, int rdelta, int cost) {
    pc(address, rdelta);
    tstates += cost;
  }

  public void tstates(int extra) {
    tstates += extra;
  }

  public void out(int port, int value) {
    if (banks != null && MemoryBanks.pages(port))
      banks.write(value, mem);
    io.out(port, value);
    if (sound != null)
      sound.out(tstates, port, value);
  }

  public void halt(int address) {
  }

  public void ldir(int address) {
    ldi();
    while (BC() != 0) {
      pc(address, 2, BLOCK_REPEAT);
      ldi();
    }
    tstates -= BLOCK_REPEAT - BLOCK_END;
  }

  public void ldi() {
    blockStep(1, true);
  }

  public void lddr(int address) {
    ldd();
    while (BC() != 0) {
      pc(address, 2, BLOCK_REPEAT);
      ldd();
    }
    tstates -= BLOCK_REPEAT - BLOCK_END;
  }

  public void ldd() {
    blockStep(-1, true);
  }

  public void cpir(int address) {
    cpi();
    while (BC() != 0 && (F & 0x40) == 0) {
      pc(address, 2, BLOCK_REPEAT);
      cpi();
    }
    tstates -= BLOCK_REPEAT - BLOCK_END;
  }

  public void cpi() {
    blockStep(1, false);
  }

  public void cpdr(int address) {
    cpd();
    while (BC() != 0 && (F & 0x40) == 0) {
      pc(address, 2, BLOCK_REPEAT);
      cpd();
    }
    tstates -= BLOCK_REPEAT - BLOCK_END;
  }

  public void cpd() {
    blockStep(-1, false);
  }

  public void outi() {
    outStep(1);
  }

  public void outd() {
    outStep(-1);
  }

  private void outStep(int direction) {
    int value = mem[HL()];
    B(B() - 1 & 0xff);
    out(BC(), value);
    HL(HL() + direction & 0xffff);
    int sum = value + L();
    F(((value & 0x80) != 0 ? 0x02 : 0) | (sum > 0xff ? 0x11 : 0) | (Integer.bitCount(sum & 7 ^ B()) % 2 == 0 ? 0x04 : 0) | B() & 0xa8 | (B() == 0 ? 0x40 : 0));
  }

  private void blockStep(int direction, boolean copy) {
    int hl = HL();
    if (copy) {
      int de = DE();
      wMem(de, mem[hl]);
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
    if (state.getMemory() instanceof MockedMemory memory && memory.banks != null)
      banks = memory.banks.copyOf(memory.getData());
    loadRegisters(state);
  }

  private void loadRegisters(State state) {
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
    iff2 = state.isIff2();
    interruptMode = state.getInterruptionMode().ordinal();
  }

  public void im(int mode) {
    interruptMode = mode;
  }

  public void ei() {
    iff = iff2 = true;
  }

  public void di() {
    iff = iff2 = false;
  }

  public boolean isIff() {
    return iff;
  }

  public boolean acceptsInterrupt() {
    return iff;
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
  protected boolean iff, iff2;
  protected int interruptMode = 1;

  public int R() {
    return R;
  }

  public int ldAR() {
    return ldInterruptRegister(R);
  }

  public int ldAI() {
    return ldInterruptRegister(I);
  }

  private int ldInterruptRegister(int value) {
    F(F & 0x01 | value & 0xa8 | (value == 0 ? 0x40 : 0) | (iff2 ? 0x04 : 0));
    return value;
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
    return B;
  }

  public void B(int b) {
    B = b & 0xff;
    BC = B << 8 | BC & 0xff;
  }

  public void B_16(int b) {
    BC = BC & 0xff | ((b & 0xff) << 8);
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
