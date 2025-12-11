package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;

public class Main1 {
  public static void main(String[] args) {
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
    Add.Add8TableAluOperation add8TableAluOperation = new Add.Add8TableAluOperation();
    Adc.Adc8TableAluOperation adc8TableAluOperation = new Adc.Adc8TableAluOperation();
    Sub.Sub8TableAluOperation sub8TableAluOperation = new Sub.Sub8TableAluOperation();
    Sbc.Sbc8TableAluOperation sbc8TableAluOperation = new Sbc.Sbc8TableAluOperation();
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
  }

  private static Ld getLd1B() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("A");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));
    return ld;
  }

  private static Ld getLd1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("A");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));
    return ld;
  }
}
