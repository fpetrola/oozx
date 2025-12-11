package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;

public class Z80UnRolled {
  // ============ ALU Operations ============
  // All ALU operations initialized from their respective instruction classes
  public Add.AddTableAluOperation addTableAluOperation;
  public Adc.AdcTableAluOperation adcTableAluOperation;
  public Sub.SubTableAluOperation subTableAluOperation;
  public Sbc.SbcTableAluOperation sbcTableAluOperation;
  public And.AndTableAluOperation andTableAluOperation;
  public Xor.XorTableAluOperation xorTableAluOperation;
  public Or.OrTableAluOperation orTableAluOperation;
  public Cp.CpTableAluOperation cpTableAluOperation;

  // ============ 8-bit Registers (Primary Set) ============
  private int A;      // Accumulator
  private int B;      // Register B
  private int C;      // Register C
  private int D;      // Register D
  private int E;      // Register E
  private int H;      // Register H
  private int L;      // Register L
  private int F;      // Flags

  // ============ 8-bit Registers (Shadow/Alternate Set) ============
  private int Ax;     // Alternate A
  private int Bx;     // Alternate B
  private int Cx;     // Alternate C
  private int Dx;     // Alternate D
  private int Ex;     // Alternate E
  private int Hx;     // Alternate H
  private int Lx;     // Alternate L
  private int Fx;     // Alternate Flags

  // ============ 16-bit Registers (Primary Pairs) ============
  private int AF;     // A:F pair (31:24 | 23:16)
  private int BC;     // B:C pair
  private int DE;     // D:E pair
  private int HL;     // H:L pair

  // ============ 16-bit Registers (Shadow Pairs) ============
  private int AFx;    // Alternate A:F pair
  private int BCx;    // Alternate B:C pair
  private int DEx;    // Alternate D:E pair
  private int HLx;    // Alternate H:L pair

  // ============ Index Registers ============
  private int IX;     // Index Register X
  private int IY;     // Index Register Y
  private int IXH;    // IX High byte
  private int IXL;    // IX Low byte
  private int IYH;    // IY High byte
  private int IYL;    // IY Low byte

  // ============ Control Registers ============
  private int PC;     // Program Counter
  private int SP;     // Stack Pointer
  private int I;      // Interrupt Vector Register
  private int R;      // Refresh Counter
  private int MEMPTR; // Memory Pointer (temporary)

  // ============ Memory and Status ============
  private Memory memory;
  private Register flag;

  // ============ Control Flags ============
  private boolean IME;       // Interrupt Master Enable
  private int interruptMode; // 0, 1, or 2
  private boolean halt;      // HALT instruction flag
}
