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

package com.fpetrola.z80.registers;

/**
 * The registers as plain int fields, one per register, so a generated core can read and write
 * them without going through an object.
 * <p>
 * Every field holds a value of its register's width at every point: whatever writes one masks it,
 * including a carry halfway through incrementing a pair. Code that reads a register can count on
 * that, and the generator does - it derives each width from the writes it can see here.
 */
public class UnrolledRegisterBank extends RegisterBank {
  public UnrolledRegisterBank() {

    registerAf = new AFRegister(new ARegister(), new FRegister());
    registerBc = new BCRegister(new BRegister(), new CRegister());
    registerDe = new DERegister(new DRegister(), new ERegister());
    registerHl = new HLRegister(new HRegister(), new LRegister());
    register_af = new AFxRegister(new AxRegister(), new FxRegister());
    register_bc = new BCxRegister(new BxRegister(), new CxRegister());
    register_de = new DExRegister(new DxRegister(), new ExRegister());
    register_hl = new HLxRegister(new HxRegister(), new LxRegister());
    registerIx = new IXRegister(new IXHRegister(), new IXLRegister());
    registerIy = new IYRegister(new IYHRegister(), new IYLRegister());
    registerIr = new IRRegister(new IRegister(), new RRegister());
    registerPc = new PCRegister();
    registerSp = new SPRegister();
    registerMemptr = new MEMPTRRegister();
    registerVirtual = new VirtualRegister();

  }

  protected int A;
  protected int F;
  protected int B;
  protected int C;
  protected int D;
  protected int E;
  protected int H;
  protected int L;
  protected int I;
  protected int R;
  protected int _AF;
  protected int _BC;
  protected int _DE;
  protected int _HL;
  protected int IX;
  protected int IY;
  protected int PC;
  protected int SP;
  protected int MEMPTR;
  protected int VIRTUAL;
  protected int regRBit7;

  /** A pair over two registers of the bank, which it hands out as its high and low halves. */
  abstract static class Pair extends NamedRegister implements RegisterPair {
    private final Register high;
    private final Register low;

    Pair(String name, Register high, Register low) {
      super(name);
      this.high = high;
      this.low = low;
    }

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }
  }

  public final class ARegister extends NamedRegister {
    public ARegister() {
      super("A");
    }

    public int read() {
      return A;
    }

    public void write(int value) {
      A = value & 0xFF;
    }

    public void increment() {
      A = (A + 1) & 0xFF;
    }

    public void decrement() {
      A = (A - 1) & 0xFF;
    }
  }

  final class FRegister extends NamedRegister {

    public FRegister() {
      super("F");
    }

    public int read() {
      return F;
    }

    public void write(int value) {
      F = value & 0xFF;
    }

    public void increment() {
      F = (F + 1) & 0xFF;
    }

    public void decrement() {
      F = (F - 1) & 0xFF;
    }
  }

  final class BRegister extends NamedRegister {

    public BRegister() {
      super("B");
    }

    public int read() {
      return B;
    }

    public void write(int value) {
      B = value & 0xFF;
    }

    public void increment() {
      B = (B + 1) & 0xFF;
    }

    public void decrement() {
      B = (B - 1) & 0xFF;
    }
  }

  final class CRegister extends NamedRegister {

    public CRegister() {
      super("C");
    }

    public int read() {
      return C;
    }

    public void write(int value) {
      C = value & 0xFF;
    }

    public void increment() {
      C = (C + 1) & 0xFF;
    }

    public void decrement() {
      C = (C - 1) & 0xFF;
    }
  }

  final class DRegister extends NamedRegister {

    public DRegister() {
      super("D");
    }

    public int read() {
      return D;
    }

    public void write(int value) {
      D = value & 0xFF;
    }

    public void increment() {
      D = (D + 1) & 0xFF;
    }

    public void decrement() {
      D = (D - 1) & 0xFF;
    }
  }

  final class ERegister extends NamedRegister {

    public ERegister() {
      super("E");
    }

    public int read() {
      return E;
    }

    public void write(int value) {
      E = value & 0xFF;
    }

    public void increment() {
      E = (E + 1) & 0xFF;
    }

    public void decrement() {
      E = (E - 1) & 0xFF;
    }
  }

  final class HRegister extends NamedRegister {

    public HRegister() {
      super("H");
    }

    public int read() {
      return H;
    }

    public void write(int value) {
      H = value & 0xFF;
    }

    public void increment() {
      H = (H + 1) & 0xFF;
    }

    public void decrement() {
      H = (H - 1) & 0xFF;
    }
  }

  final class LRegister extends NamedRegister {

    public LRegister() {
      super("L");
    }

    public int read() {
      return L;
    }

    public void write(int value) {
      L = value & 0xFF;
    }

    public void increment() {
      L = (L + 1) & 0xFF;
    }

    public void decrement() {
      L = (L - 1) & 0xFF;
    }
  }

  final class IRegister extends NamedRegister {

    public IRegister() {
      super("I");
    }

    public int read() {
      return I;
    }

    public void write(int value) {
      I = value & 0xFF;
    }

    public void increment() {
      I = (I + 1) & 0xFF;
    }

    public void decrement() {
      I = (I - 1) & 0xFF;
    }
  }

  public class RRegister implements Register {

    public void write(int value) {
      regRBit7 = (value > 0x7f) ? 0x80 : 0;
      R = (value & 0x7f) | regRBit7;
    }

    public void increment() { // TODO: verify regRBit7 handling here
      R = (R + 1 & 0x7f) | regRBit7;
    }

    public void decrement() {
      R = (R - 1) & 0xFF;
    }

    public String getName() {
      return "R";
    }

    public int read() {
      return R;
    }

  }

  final class AxRegister extends NamedRegister {

    public AxRegister() {
      super("A'");
    }

    public int read() {
      return _AF >> 8;
    }

    public void write(int value) {
      _AF = (_AF & 0x00FF) | ((value & 0xFF) << 8);
    }

    public void increment() {
      _AF = (_AF + 0x100) & 0xFFFF;
    }

    public void decrement() {
      _AF = (_AF - 0x100) & 0xFFFF;
    }
  }

  final class FxRegister extends NamedRegister {

    public FxRegister() {
      super("F'");
    }

    public int read() {
      return _AF & 0xFF;
    }

    public void write(int value) {
      _AF = (_AF & 0xFF00) | (value & 0xFF);
    }

    public void increment() {
      _AF = (_AF + 1) & 0xFFFF;
    }

    public void decrement() {
      _AF = (_AF - 1) & 0xFFFF;
    }
  }

  final class BxRegister extends NamedRegister {

    public BxRegister() {
      super("B'");
    }

    public int read() {
      return _BC >> 8;
    }

    public void write(int value) {
      _BC = (_BC & 0x00FF) | ((value & 0xFF) << 8);
    }

    public void increment() {
      _BC = (_BC + 0x100) & 0xFFFF;
    }

    public void decrement() {
      _BC = (_BC - 0x100) & 0xFFFF;
    }
  }

  final class CxRegister extends NamedRegister {

    public CxRegister() {
      super("C'");
    }

    public int read() {
      return _BC & 0xFF;
    }

    public void write(int value) {
      _BC = (_BC & 0xFF00) | (value & 0xFF);
    }

    public void increment() {
      _BC = (_BC + 1) & 0xFFFF;
    }

    public void decrement() {
      _BC = (_BC - 1) & 0xFFFF;
    }
  }

  final class DxRegister extends NamedRegister {

    public DxRegister() {
      super("D'");
    }

    public int read() {
      return _DE >> 8;
    }

    public void write(int value) {
      _DE = (_DE & 0x00FF) | ((value & 0xFF) << 8);
    }

    public void increment() {
      _DE = (_DE + 0x100) & 0xFFFF;
    }

    public void decrement() {
      _DE = (_DE - 0x100) & 0xFFFF;
    }
  }

  final class ExRegister extends NamedRegister {

    public ExRegister() {
      super("E'");
    }

    public int read() {
      return _DE & 0xFF;
    }

    public void write(int value) {
      _DE = (_DE & 0xFF00) | (value & 0xFF);
    }

    public void increment() {
      _DE = (_DE + 1) & 0xFFFF;
    }

    public void decrement() {
      _DE = (_DE - 1) & 0xFFFF;
    }
  }

  final class HxRegister extends NamedRegister {

    public HxRegister() {
      super("H'");
    }

    public int read() {
      return _HL >> 8;
    }

    public void write(int value) {
      _HL = (_HL & 0x00FF) | ((value & 0xFF) << 8);
    }

    public void increment() {
      _HL = (_HL + 0x100) & 0xFFFF;
    }

    public void decrement() {
      _HL = (_HL - 0x100) & 0xFFFF;
    }
  }

  final class LxRegister extends NamedRegister {

    public LxRegister() {
      super("L'");
    }

    public int read() {
      return _HL & 0xFF;
    }

    public void write(int value) {
      _HL = (_HL & 0xFF00) | (value & 0xFF);
    }

    public void increment() {
      _HL = (_HL + 1) & 0xFFFF;
    }

    public void decrement() {
      _HL = (_HL - 1) & 0xFFFF;
    }
  }

  final class IXHRegister extends NamedRegister {

    public IXHRegister() {
      super("IXH");
    }

    public int read() {
      return IX >> 8;
    }

    public void write(int value) {
      IX = (IX & 0x00FF) | ((value & 0xFF) << 8);
    }

    public void increment() {
      IX = (IX + 0x100) & 0xFFFF;
    }

    public void decrement() {
      IX = (IX - 0x100) & 0xFFFF;
    }
  }

  final class IXLRegister extends NamedRegister {

    public IXLRegister() {
      super("IXL");
    }

    public int read() {
      return IX & 0xFF;
    }

    public void write(int value) {
      IX = (IX & 0xFF00) | (value & 0xFF);
    }

    public void increment() {
      IX = (IX + 1) & 0xFFFF;
    }

    public void decrement() {
      IX = (IX - 1) & 0xFFFF;
    }
  }

  final class IYHRegister extends NamedRegister {

    public IYHRegister() {
      super("IYH");
    }

    public int read() {
      return IY >> 8;
    }

    public void write(int value) {
      IY = (IY & 0x00FF) | ((value & 0xFF) << 8);
    }

    public void increment() {
      IY = (IY + 0x100) & 0xFFFF;
    }

    public void decrement() {
      IY = (IY - 0x100) & 0xFFFF;
    }
  }

  final class IYLRegister extends NamedRegister {

    public IYLRegister() {
      super("IYL");
    }

    public int read() {
      return IY & 0xFF;
    }

    public void write(int value) {
      IY = (IY & 0xFF00) | (value & 0xFF);
    }

    public void increment() {
      IY = (IY + 1) & 0xFFFF;
    }

    public void decrement() {
      IY = (IY - 1) & 0xFFFF;
    }
  }

  final class AFRegister extends Pair {

    public AFRegister(Register high, Register low) {
      super("AF", high, low);
    }

    public int read() {
      return (A << 8) | F;
    }

    public void write(int value) {
      A = (value >>> 8) & 0xFF;
      F = value & 0xFF;
    }

    public void increment() {
      F = (F + 1) & 0xFF;
      if (F == 0)
        A = (A + 1) & 0xFF;
    }

    public void decrement() {
      F = (F - 1) & 0xFF;
      if (F == 0xFF)
        A = (A - 1) & 0xFF;
    }
  }

  final class BCRegister extends Pair {

    public BCRegister(Register high, Register low) {
      super("BC", high, low);
    }

    public int read() {
      return (B << 8) | C;
    }

    public void write(int value) {
      B = (value >>> 8) & 0xFF;
      C = value & 0xFF;
    }

    public void increment() {
      C = (C + 1) & 0xFF;
      if (C == 0)
        B = (B + 1) & 0xFF;
    }

    public void decrement() {
      C = (C - 1) & 0xFF;
      if (C == 0xFF)
        B = (B - 1) & 0xFF;
    }
  }

  final class DERegister extends Pair {

    public DERegister(Register high, Register low) {
      super("DE", high, low);
    }

    public int read() {
      return (D << 8) | E;
    }

    public void write(int value) {
      D = (value >>> 8) & 0xFF;
      E = value & 0xFF;
    }

    public void increment() {
      E = (E + 1) & 0xFF;
      if (E == 0)
        D = (D + 1) & 0xFF;
    }

    public void decrement() {
      E = (E - 1) & 0xFF;
      if (E == 0xFF)
        D = (D - 1) & 0xFF;
    }
  }

  final class HLRegister extends Pair {

    public HLRegister(Register high, Register low) {
      super("HL", high, low);
    }

    public int read() {
      return (H << 8) | L;
    }

    public void write(int value) {
      H = (value >>> 8) & 0xFF;
      L = value & 0xFF;
    }

    public void increment() {
      L = (L + 1) & 0xFF;
      if (L == 0)
        H = (H + 1) & 0xFF;
    }

    public void decrement() {
      L = (L - 1) & 0xFF;
      if (L == 0xFF)
        H = (H - 1) & 0xFF;
    }
  }

  final class IRRegister extends Pair {

    public IRRegister(Register high, Register low) {
      super("IR", high, low);
    }

    public int read() {
      return (I << 8) | getR();
    }

    private int getR() {
      return getLow().read();
    }

    public void write(int value) {
      I = (value >>> 8) & 0xFF;
      getLow().write(value);
    }

    /** R counts on its own seven bits and never carries into I, so the pair moves when R does. */
    public void increment() {
      getLow().increment();
    }

    public void decrement() {
      getLow().decrement();
    }
  }

  final class AFxRegister extends Pair {

    public AFxRegister(Register high, Register low) {
      super("AF'", high, low);
    }

    public int read() {
      return _AF;
    }

    public void write(int value) {
      _AF = value & 0xFFFF;
    }

    public void increment() {
      _AF = (_AF + 1) & 0xFFFF;
    }

    public void decrement() {
      _AF = (_AF - 1) & 0xFFFF;
    }
  }

  final class BCxRegister extends Pair {

    public BCxRegister(Register high, Register low) {
      super("BC'", high, low);
    }

    public int read() {
      return _BC;
    }

    public void write(int value) {
      _BC = value & 0xFFFF;
    }

    public void increment() {
      _BC = (_BC + 1) & 0xFFFF;
    }

    public void decrement() {
      _BC = (_BC - 1) & 0xFFFF;
    }
  }

  final class DExRegister extends Pair {

    public DExRegister(Register high, Register low) {
      super("DE'", high, low);
    }

    public int read() {
      return _DE;
    }

    public void write(int value) {
      _DE = value & 0xFFFF;
    }

    public void increment() {
      _DE = (_DE + 1) & 0xFFFF;
    }

    public void decrement() {
      _DE = (_DE - 1) & 0xFFFF;
    }
  }

  final class HLxRegister extends Pair {

    public HLxRegister(Register high, Register low) {
      super("HL'", high, low);
    }

    public int read() {
      return _HL;
    }

    public void write(int value) {
      _HL = value & 0xFFFF;
    }

    public void increment() {
      _HL = (_HL + 1) & 0xFFFF;
    }

    public void decrement() {
      _HL = (_HL - 1) & 0xFFFF;
    }
  }

  final class IXRegister extends Pair {

    public IXRegister(Register high, Register low) {
      super("IX", high, low);
    }

    public int read() {
      return IX;
    }

    public void write(int value) {
      IX = value & 0xFFFF;
    }

    public void increment() {
      IX = (IX + 1) & 0xFFFF;
    }

    public void decrement() {
      IX = (IX - 1) & 0xFFFF;
    }
  }

  final class IYRegister extends Pair {

    public IYRegister(Register high, Register low) {
      super("IY", high, low);
    }

    public int read() {
      return IY;
    }

    public void write(int value) {
      IY = value & 0xFFFF;
    }

    public void increment() {
      IY = (IY + 1) & 0xFFFF;
    }

    public void decrement() {
      IY = (IY - 1) & 0xFFFF;
    }
  }

  final class PCRegister extends NamedRegister {

    public PCRegister() {
      super("PC");
    }

    public int read() {
      return PC;
    }

    public void write(int value) {
      PC = value & 0xFFFF;
    }

    public void increment() {
      PC = (PC + 1) & 0xFFFF;
    }

    public void decrement() {
      PC = (PC - 1) & 0xFFFF;
    }
  }

  final class SPRegister extends NamedRegister {

    public SPRegister() {
      super("SP");
    }

    public int read() {
      return SP;
    }

    public void write(int value) {
      SP = value & 0xFFFF;
    }

    public void increment() {
      SP = (SP + 1) & 0xFFFF;
    }

    public void decrement() {
      SP = (SP - 1) & 0xFFFF;
    }
  }

  final class MEMPTRRegister extends NamedRegister {

    public MEMPTRRegister() {
      super("MEMPTR");
    }

    public int read() {
      return MEMPTR;
    }

    public void write(int value) {
      MEMPTR = value & 0xFFFF;
    }

    public void increment() {
      MEMPTR = (MEMPTR + 1) & 0xFFFF;
    }

    public void decrement() {
      MEMPTR = (MEMPTR - 1) & 0xFFFF;
    }
  }

  final class VirtualRegister extends NamedRegister {

    public VirtualRegister() {
      super("VIRTUAL");
    }

    public int read() {
      return VIRTUAL;
    }

    public void write(int value) {
      VIRTUAL = value & 0xFFFF;
    }

    public void increment() {
      VIRTUAL = (VIRTUAL + 1) & 0xFFFF;
    }

    public void decrement() {
      VIRTUAL = (VIRTUAL - 1) & 0xFFFF;
    }
  }
}

