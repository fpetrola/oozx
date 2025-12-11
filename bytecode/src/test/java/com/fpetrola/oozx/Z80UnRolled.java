package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;

public class Z80UnRolled {
  // ============ ALU Operations ============
  RLA.RLAAluOperation rLAAluOperation = new RLA.RLAAluOperation();
  RRA.RRAAluOperation rRAAluOperation = new RRA.RRAAluOperation();
  RLCA.RlcaTableAluOperation rlcaTableAluOperation = new RLCA.RlcaTableAluOperation();
  RRCA.RRCAAluOperation rRCAAluOperation = new RRCA.RRCAAluOperation();
  RLC.RlcTable1AluOperation rlcTable1AluOperation = new RLC.RlcTable1AluOperation();
  RRC.RRCAluOperation rRCAluOperation = new RRC.RRCAluOperation();
  RL.RlTableAluOperation rlTableAluOperation = new RL.RlTableAluOperation();
  RR.RrTableAluOperation rrTableAluOperation = new RR.RrTableAluOperation();
  SLA.SlaTableAluOperation slaTableAluOperation = new SLA.SlaTableAluOperation();
  SRA.SRAAluOperation sRAAluOperation = new SRA.SRAAluOperation();
  SRL.SrlTableAluOperation srlTableAluOperation = new SRL.SrlTableAluOperation();
  SLL.SLLAluOperation sLLAluOperation = new SLL.SLLAluOperation();
  RLD.RldTableAluOperation rldTableAluOperation = new RLD.RldTableAluOperation();
  RRD.RrdTableAluOperation rrdTableAluOperation = new RRD.RrdTableAluOperation();
  Add.AddTableAluOperation addTableAluOperation = new Add.AddTableAluOperation();
  Adc.AdcTableAluOperation adcTableAluOperation = new Adc.AdcTableAluOperation();
  Sub.SubTableAluOperation subTableAluOperation = new Sub.SubTableAluOperation();
  Sbc.SbcTableAluOperation sbcTableAluOperation = new Sbc.SbcTableAluOperation();
  And.AndTableAluOperation andTableAluOperation = new And.AndTableAluOperation();
  Xor.XorTableAluOperation xorTableAluOperation = new Xor.XorTableAluOperation();
  Or.OrTableAluOperation orTableAluOperation = new Or.OrTableAluOperation();
  Cp.CpTableAluOperation cpTableAluOperation = new Cp.CpTableAluOperation();
  Inc.Inc8TableAluOperation inc8TableAluOperation = new Inc.Inc8TableAluOperation();
  Dec.Dec8TableAluOperation dec8TableAluOperation = new Dec.Dec8TableAluOperation();
  DAA.DaaTableAluOperation daaTableAluOperation = new DAA.DaaTableAluOperation();
  CPL.CplTableAluOperation cplTableAluOperation = new CPL.CplTableAluOperation();
  SCF.ScfTableAluOperation scfTableAluOperation = new SCF.ScfTableAluOperation();
  CCF.CcfTableAluOperation ccfTableAluOperation = new CCF.CcfTableAluOperation();
  BIT.BitAluOperation bitAluOperation = new BIT.BitAluOperation();
  Ini.IniTableAluOperation iniTableAluOperation = new Ini.IniTableAluOperation();
  Ldi.LdiTableAluOperation ldiTableAluOperation = new Ldi.LdiTableAluOperation();
  Cpi.CpiTableAluOperation cpiTableAluOperation = new Cpi.CpiTableAluOperation();
  LdAI.LdaiTableAluOperation ldaiTableAluOperation = new LdAI.LdaiTableAluOperation();
  LdAR.LdarTableAluOperation ldarTableAluOperation = new LdAR.LdarTableAluOperation();
  Add16.Add16TableAluOperation add16TableAluOperation = new Add16.Add16TableAluOperation();
  Adc16.Adc16TableAluOperation adc16TableAluOperation = new Adc16.Adc16TableAluOperation();
  Sbc16.Sbc16TableAluOperation sbc16TableAluOperation = new Sbc16.Sbc16TableAluOperation();
  Cpd.CpdTableAluOperation cpdTableAluOperation = new Cpd.CpdTableAluOperation();
  In.InAluOperation inAluOperation = new In.InAluOperation();
  Outi.OutiTableAluOperation outiTableAluOperation = new Outi.OutiTableAluOperation();
  Neg.NegTableAluOperation negTableAluOperation = new Neg.NegTableAluOperation();

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
