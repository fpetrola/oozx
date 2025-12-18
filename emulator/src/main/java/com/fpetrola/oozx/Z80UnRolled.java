package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.UnrolledRegisterBank;

public class Z80UnRolled extends UnrolledRegisterBank {
  public RLA.RLAAluOperation rLAAluOperation = new RLA.RLAAluOperation();
  public RRA.RRAAluOperation rRAAluOperation = new RRA.RRAAluOperation();
  public RLCA.RlcaTableAluOperation rlcaTableAluOperation = new RLCA.RlcaTableAluOperation();
  public RRCA.RRCAAluOperation rRCAAluOperation = new RRCA.RRCAAluOperation();
  public RLC.RlcTable1AluOperation rlcTable1AluOperation = new RLC.RlcTable1AluOperation();
  public RRC.RRCAluOperation rRCAluOperation = new RRC.RRCAluOperation();
  public RL.RlTableAluOperation rlTableAluOperation = new RL.RlTableAluOperation();
  public RR.RrTableAluOperation rrTableAluOperation = new RR.RrTableAluOperation();
  public SLA.SlaTableAluOperation slaTableAluOperation = new SLA.SlaTableAluOperation();
  public SRA.SRAAluOperation sRAAluOperation = new SRA.SRAAluOperation();
  public SRL.SrlTableAluOperation srlTableAluOperation = new SRL.SrlTableAluOperation();
  public SLL.SLLAluOperation sLLAluOperation = new SLL.SLLAluOperation();
  public RLD.RldTableAluOperation rldTableAluOperation = new RLD.RldTableAluOperation();
  public RRD.RrdTableAluOperation rrdTableAluOperation = new RRD.RrdTableAluOperation();
  public Add.AddTableAluOperation addTableAluOperation = new Add.AddTableAluOperation();
  public Adc.AdcTableAluOperation adcTableAluOperation = new Adc.AdcTableAluOperation();
  public Sub.SubTableAluOperation subTableAluOperation = new Sub.SubTableAluOperation();
  public Sbc.SbcTableAluOperation sbcTableAluOperation = new Sbc.SbcTableAluOperation();
  public And.AndTableAluOperation andTableAluOperation = new And.AndTableAluOperation();
  public Xor.XorTableAluOperation xorTableAluOperation = new Xor.XorTableAluOperation();
  public Or.OrTableAluOperation orTableAluOperation = new Or.OrTableAluOperation();
  public Cp.CpTableAluOperation cpTableAluOperation = new Cp.CpTableAluOperation();
  public Inc.Inc8TableAluOperation inc8TableAluOperation = new Inc.Inc8TableAluOperation();
  public Dec.Dec8TableAluOperation dec8TableAluOperation = new Dec.Dec8TableAluOperation();
  public DAA.DaaTableAluOperation daaTableAluOperation = new DAA.DaaTableAluOperation();
  public CPL.CplTableAluOperation cplTableAluOperation = new CPL.CplTableAluOperation();
  public SCF.ScfTableAluOperation scfTableAluOperation = new SCF.ScfTableAluOperation();
  public CCF.CcfTableAluOperation ccfTableAluOperation = new CCF.CcfTableAluOperation();
  public BIT.BitAluOperation bitAluOperation = new BIT.BitAluOperation();
  public Ini.IniTableAluOperation iniTableAluOperation = new Ini.IniTableAluOperation();
  public Ldi.LdiTableAluOperation ldiTableAluOperation = new Ldi.LdiTableAluOperation();
  public Cpi.CpiTableAluOperation cpiTableAluOperation = new Cpi.CpiTableAluOperation();
  public LdAI.LdaiTableAluOperation ldaiTableAluOperation = new LdAI.LdaiTableAluOperation();
  public LdAR.LdarTableAluOperation ldarTableAluOperation = new LdAR.LdarTableAluOperation();
  public Add16.Add16TableAluOperation add16TableAluOperation = new Add16.Add16TableAluOperation();
  public Adc16.Adc16TableAluOperation adc16TableAluOperation = new Adc16.Adc16TableAluOperation();
  public Sbc16.Sbc16TableAluOperation sbc16TableAluOperation = new Sbc16.Sbc16TableAluOperation();
  public Cpd.CpdTableAluOperation cpdTableAluOperation = new Cpd.CpdTableAluOperation();
  public In.InAluOperation inAluOperation = new In.InAluOperation();
  public Outi.OutiTableAluOperation outiTableAluOperation = new Outi.OutiTableAluOperation();
  public Neg.NegTableAluOperation negTableAluOperation = new Neg.NegTableAluOperation();

  protected Memory memory;

  public void setMemory(Memory memory) {
    this.memory = memory;
  }

  public int execute(int opcode) {
    return -1;
  }

  public int read16(int address) {
    int var1 = address + 1 & '\uffff';
    int var2 = memory.read(var1, 0) & 0xff;
    int var3 = address + 2 & '\uffff';
    int var4 = (memory.read(var3, 0) & 0xff) << 8;
    return var2 | var4;
  }
}
