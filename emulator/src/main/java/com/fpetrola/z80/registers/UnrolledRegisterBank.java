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

public class UnrolledRegisterBank extends RegisterBank {
  private int A;
  private int F;
  private int B;
  private int C;
  private int D;
  private int E;
  private int H;
  private int L;
  private int I;
  private int R;
  private int _AF;
  private int _BC;
  private int _DE;
  private int _HL;
  private int IX;
  private int IY;
  private int PC;
  private int SP;
  private int MEMPTR;
  private int VIRTUAL;
  private int regRBit7;

  public final class ARegister implements Register {
    public ARegister() {

    }

    public int read() {
      return A;
    }

    public void write(int value) {
      A = value;
    }

    public void increment() {
      A++;
    }

    public void decrement() {
      A = (A - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "A";
    }

    public String toString() {
      return "A";
    }
  }

  final public class FRegister implements Register {


    public FRegister() {

    }

    public int read() {
      return F;
    }

    public void write(int value) {
      F = value;
    }

    public void increment() {
      F++;
    }

    public void decrement() {
      F = (F - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "F";
    }

    public String toString() {
      return "F";
    }
  }

  final public class BRegister implements Register {


    public BRegister() {

    }

    public int read() {
      return B;
    }

    public void write(int value) {
      B = value;
    }

    public void increment() {
      B++;
    }

    public void decrement() {
      B = (B - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "B";
    }

    public String toString() {
      return "B";
    }
  }

  final public class CRegister implements Register {


    public CRegister() {

    }

    public int read() {
      return C;
    }

    public void write(int value) {
      C = value;
    }

    public void increment() {
      C++;
    }

    public void decrement() {
      C = (C - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "C";
    }

    public String toString() {
      return "C";
    }
  }

  final public class DRegister implements Register {


    public DRegister() {

    }

    public int read() {
      return D;
    }

    public void write(int value) {
      D = value;
    }

    public void increment() {
      D++;
    }

    public void decrement() {
      D = (D - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "D";
    }

    public String toString() {
      return "D";
    }
  }

  final public class ERegister implements Register {


    public ERegister() {

    }

    public int read() {
      return E;
    }

    public void write(int value) {
      E = value;
    }

    public void increment() {
      E++;
    }

    public void decrement() {
      E = (E - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "E";
    }

    public String toString() {
      return "E";
    }
  }

  final public class HRegister implements Register {


    public HRegister() {

    }

    public int read() {
      return H;
    }

    public void write(int value) {
      H = value;
    }

    public void increment() {
      H++;
    }

    public void decrement() {
      H = (H - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "H";
    }

    public String toString() {
      return "H";
    }
  }

  final public class LRegister implements Register {


    public LRegister() {

    }

    public int read() {
      return L;
    }

    public void write(int value) {
      L = value;
    }

    public void increment() {
      L++;
    }

    public void decrement() {
      L = (L - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "L";
    }

    public String toString() {
      return "L";
    }
  }

  final public class IRegister implements Register {


    public IRegister() {

    }

    public int read() {
      return I;
    }

    public void write(int value) {
      I = value;
    }

    public void increment() {
      I++;
    }

    public void decrement() {
      I = (I - 1) & 0xFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "I";
    }

    public String toString() {
      return "I";
    }
  }

  public class RRegister implements Register {

    public void write(int value) {
      regRBit7 = (value > 0x7f) ? 0x80 : 0;
      R = (value & 0x7f) | regRBit7;
    }

    public void increment() { //TODO: revisar regRBit7
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

    public int getLength() {
      return 0;
    }
  }

  final public class AxRegister implements Register {


    public AxRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "A'";
    }

    public String toString() {
      return "A'";
    }
  }

  final public class FxRegister implements Register {


    public FxRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "F'";
    }

    public String toString() {
      return "F'";
    }
  }

  final public class BxRegister implements Register {


    public BxRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "B'";
    }

    public String toString() {
      return "B'";
    }
  }

  final public class CxRegister implements Register {


    public CxRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "C'";
    }

    public String toString() {
      return "C'";
    }
  }

  final public class DxRegister implements Register {


    public DxRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "D'";
    }

    public String toString() {
      return "D'";
    }
  }

  final public class ExRegister implements Register {


    public ExRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "E'";
    }

    public String toString() {
      return "E'";
    }
  }

  final public class HxRegister implements Register {


    public HxRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "H'";
    }

    public String toString() {
      return "H'";
    }
  }

  final public class LxRegister implements Register {


    public LxRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "L'";
    }

    public String toString() {
      return "L'";
    }
  }

  final public class IXHRegister implements Register {


    public IXHRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "IXH";
    }

    public String toString() {
      return "IXH";
    }
  }

  final public class IXLRegister implements Register {


    public IXLRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "IXL";
    }

    public String toString() {
      return "IXL";
    }
  }

  final public class IYHRegister implements Register {


    public IYHRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "IYH";
    }

    public String toString() {
      return "IYH";
    }
  }

  final public class IYLRegister implements Register {


    public IYLRegister() {

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

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "IYL";
    }

    public String toString() {
      return "IYL";
    }
  }

  final public class AFRegister implements RegisterPair {

    private final ARegister high;
    private final FRegister low;

    public AFRegister(ARegister high, FRegister low) {

      this.high = high;
      this.low = low;
    }

    public int read() {
      return (A << 8) | F;
    }

    public void write(int value) {
      A = value >>> 8;
      F = value & 0xFF;
    }

    public void increment() {
      if (++F < 0x100) return;
      F = 0;
      if (++A < 0x100) return;
      A = 0;
    }

    public void decrement() {
      if (--F >= 0) return;
      F = 0xFF;
      if (--A >= 0) return;
      A = 0xFF;
    }

    public ARegister getHigh() {
      return high;
    }

    public FRegister getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "AF";
    }

    public String toString() {
      return "AF";
    }
  }

  final public class BCRegister implements RegisterPair {

    private final BRegister high;
    private final CRegister low;

    public BCRegister(BRegister high, CRegister low) {

      this.high = high;
      this.low = low;
    }

    public int read() {
      return (B << 8) | C;
    }

    public void write(int value) {
      B = value >>> 8;
      C = value & 0xFF;
    }

    public void increment() {
      if (++C < 0x100) return;
      C = 0;
      if (++B < 0x100) return;
      B = 0;
    }

    public void decrement() {
      if (--C >= 0) return;
      C = 0xFF;
      if (--B >= 0) return;
      B = 0xFF;
    }

    public BRegister getHigh() {
      return high;
    }

    public CRegister getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "BC";
    }

    public String toString() {
      return "BC";
    }
  }

  final public class DERegister implements RegisterPair {

    private final DRegister high;
    private final ERegister low;

    public DERegister(DRegister high, ERegister low) {

      this.high = high;
      this.low = low;
    }

    public int read() {
      return (D << 8) | E;
    }

    public void write(int value) {
      D = value >>> 8;
      E = value & 0xFF;
    }

    public void increment() {
      if (++E < 0x100) return;
      E = 0;
      if (++D < 0x100) return;
      D = 0;
    }

    public void decrement() {
      if (--E >= 0) return;
      E = 0xFF;
      if (--D >= 0) return;
      D = 0xFF;
    }

    public DRegister getHigh() {
      return high;
    }

    public ERegister getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "DE";
    }

    public String toString() {
      return "DE";
    }
  }

  final public class HLRegister implements RegisterPair {

    private final HRegister high;
    private final LRegister low;

    public HLRegister(HRegister high, LRegister low) {

      this.high = high;
      this.low = low;
    }

    public int read() {
      return (H << 8) | L;
    }

    public void write(int value) {
      H = value >>> 8;
      L = value & 0xFF;
    }

    public void increment() {
      if (++L < 0x100) return;
      L = 0;
      if (++H < 0x100) return;
      H = 0;
    }

    public void decrement() {
      if (--L >= 0) return;
      L = 0xFF;
      if (--H >= 0) return;
      H = 0xFF;
    }

    public HRegister getHigh() {
      return high;
    }

    public LRegister getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "HL";
    }

    public String toString() {
      return "HL";
    }
  }

  final public class IRRegister implements RegisterPair {

    private final Register high;
    private final Register low;

    public IRRegister(Register high, Register low) {

      this.high = high;
      this.low = low;
    }

    public int read() {
      return (I << 8) | getR();
    }

    private int getR() {
      return low.read();
    }

    public void write(int value) {
      I = value >>> 8;
      low.write(value);
    }

    public void increment() {
      low.increment();
      if (low.read() < 0x100) return;
      low.write(0);
      if (++I < 0x100) return;
      I = 0;
    }

    public void decrement() {
      low.increment();
      if (low.read() >= 0) return;
      low.write(0xff);
      if (--I >= 0) return;
      I = 0xFF;
    }

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "IR";
    }

    public String toString() {
      return "IR";
    }
  }

  final public class AFxRegister implements RegisterPair {

    private final Register high;
    private final Register low;

    public AFxRegister(Register high, Register low) {

      this.high = high;
      this.low = low;
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

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "AF'";
    }

    public String toString() {
      return "AF'";
    }
  }

  final public class BCxRegister implements RegisterPair {

    private final Register high;
    private final Register low;

    public BCxRegister(Register high, Register low) {

      this.high = high;
      this.low = low;
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

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "BC'";
    }

    public String toString() {
      return "BC'";
    }
  }

  final public class DExRegister implements RegisterPair {

    private final Register high;
    private final Register low;

    public DExRegister(Register high, Register low) {

      this.high = high;
      this.low = low;
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

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "DE'";
    }

    public String toString() {
      return "DE'";
    }
  }

  final public class HLxRegister implements RegisterPair {

    private final Register high;
    private final Register low;

    public HLxRegister(Register high, Register low) {

      this.high = high;
      this.low = low;
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

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "HL'";
    }

    public String toString() {
      return "HL'";
    }
  }

  final public class IXRegister implements RegisterPair {

    private final Register high;
    private final Register low;

    public IXRegister(Register high, Register low) {

      this.high = high;
      this.low = low;
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

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "IX";
    }

    public String toString() {
      return "IX";
    }
  }

  final public class IYRegister implements RegisterPair {

    private final Register high;
    private final Register low;

    public IYRegister(Register high, Register low) {

      this.high = high;
      this.low = low;
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

    public Register getHigh() {
      return high;
    }

    public Register getLow() {
      return low;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "IY";
    }

    public String toString() {
      return "IY";
    }
  }

  final public class PCRegister implements Register {


    public PCRegister() {

    }

    public int read() {
      return PC;
    }

    public void write(int value) {
      PC = value;
    }

    public void increment() {
      PC++;
    }

    public void decrement() {
      PC--;
      PC &= 0xFFFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "PC";
    }

    public String toString() {
      return "PC";
    }
  }

  final public class SPRegister implements Register {


    public SPRegister() {

    }

    public int read() {
      return SP;
    }

    public void write(int value) {
      SP = value;
    }

    public void increment() {
      SP++;
    }

    public void decrement() {
      SP--;
      SP &= 0xFFFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "SP";
    }

    public String toString() {
      return "SP";
    }
  }

  final public class MEMPTRRegister implements Register {


    public MEMPTRRegister() {

    }

    public int read() {
      return MEMPTR;
    }

    public void write(int value) {
      MEMPTR = value;
    }

    public void increment() {
      MEMPTR++;
    }

    public void decrement() {
      MEMPTR--;
      MEMPTR &= 0xFFFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "MEMPTR";
    }

    public String toString() {
      return "MEMPTR";
    }
  }

  final public class VirtualRegister implements Register {


    public VirtualRegister() {

    }

    public int read() {
      return VIRTUAL;
    }

    public void write(int value) {
      VIRTUAL = value;
    }

    public void increment() {
      VIRTUAL++;
    }

    public void decrement() {
      VIRTUAL--;
      VIRTUAL &= 0xFFFF;
    }

    public int getLength() {
      return 0;
    }

    public String getName() {
      return "VIRTUAL";
    }

    public String toString() {
      return "VIRTUAL";
    }
  }
}


