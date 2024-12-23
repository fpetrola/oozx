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

package com.fpetrola.z80.instructions.cache;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class InstructionCloner<T extends WordNumber, R> implements InstructionVisitor<T, Object> {
  InstructionFactory instructionFactory;
  protected AbstractInstruction cloned;
  Object result;

  public Object getResult() {
    return result;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  public InstructionCloner(InstructionFactory instructionFactory) {
    this.instructionFactory = instructionFactory;
  }

  public Instruction<T> clone(Instruction<T> instruction) {
    cloned = null;
    instruction.accept(this);
    int length = instruction.getLength();
    int length1 = cloned.getLength();
    if (length != length1)
      cloned.incrementLengthBy(length - length1);

    if (cloned == null) {
      throw new RuntimeException("clone not supported for: " + instruction.getClass());
    }
    return cloned;
  }



  @Override
  public boolean visitCpdr(Cpdr tCpdr) {
    setCloned(instructionFactory.Cpdr(), tCpdr);
    return false;
  }

  @Override
  public boolean visitingCpl(CPL cpl) {
    setCloned(instructionFactory.CPL(), cpl);
    return false;
  }

  @Override
  public boolean visitOutd(Outd<T> outi) {
    setCloned(instructionFactory.Outd(), outi);
    return false;
  }

  @Override
  public boolean visitOuti(Outi<T> outi) {
    setCloned(instructionFactory.Outi(), outi);
    return false;
  }

  @Override
  public void visitCpi(Cpi<T> cpi) {
    setCloned(instructionFactory.Cpi(), cpi);
  }

  @Override
  public boolean visitCpd(Cpd<T> cpd) {
    setCloned(instructionFactory.Cpd(), cpd);
    return false;
  }

  @Override
  public boolean visitIni(Ini<T> tIni) {
    setCloned(instructionFactory.Ini(), tIni);
    return true;
  }

  @Override
  public boolean visitInd(Ind<T> tInd) {
    setCloned(instructionFactory.Ind(), tInd);
    return true;
  }

  @Override
  public boolean visitIndr(Indr indr) {
    setCloned(instructionFactory.Indr(), indr);
    return false;
  }

  @Override
  public boolean visitInir(Inir inir) {
    setCloned(instructionFactory.Inir(), inir);
    return false;
  }

  @Override
  public boolean visitOutdr(Outdr outdr) {
    setCloned(instructionFactory.Outdr(), outdr);
    return false;
  }

  @Override
  public boolean visitOutir(Outir outir) {
    setCloned(instructionFactory.Outir(), outir);
    return false;
  }

  @Override
  public void visitingHalt(Halt halt) {
    setCloned(instructionFactory.Halt(), halt);
  }

  @Override
  public boolean visitRRD(RRD<T> rrd) {
    setCloned(instructionFactory.RRD(), rrd);
    return false;
  }

  @Override
  public boolean visitRLD(RLD<T> rld) {
    setCloned(instructionFactory.RLD(), rld);
    return false;
  }

  @Override
  public void visitPush(Push push) {
    setCloned(instructionFactory.Push(clone(push.getTarget())), push);
  }

  @Override
  public void visitingCcf(CCF ccf) {
    setCloned(instructionFactory.CCF(), ccf);
  }

  @Override
  public void visitingScf(SCF scf) {
    setCloned(instructionFactory.SCF(), scf);
  }

//  @Override
//  public void visitingBitOperation(BitOperation bitOperation) {
//    setCloned(instructionFactory.BIT(clone(bitOperation.getTarget()), bitOperation.getN()), bitOperation);
//  }

  @Override
  public boolean visitingCall(Call tCall) {
    setCloned(instructionFactory.Call(clone(tCall.getCondition()), clone(tCall.getPositionOpcodeReference())), tCall);
    return false;
  }

  private Condition cloneCondition(Condition condition1) {
    Condition condition = condition1;
    ConditionTransformer conditionTransformer = new ConditionTransformer();
    condition.accept(conditionTransformer);
    Condition result1 = conditionTransformer.getResult();
    return result1;
  }

  private class ConditionTransformer implements InstructionVisitor {
    public Condition getResult() {
      return result;
    }

    public Condition result;

    public ConditionTransformer() {
    }

    public void visitingConditionFlag(ConditionFlag conditionFlag) {
      result = new ConditionFlag<>(conditionFlag.getRegister(), conditionFlag.getFlag(), conditionFlag.isNegate(), InstructionCloner.clone(conditionFlag.isConditionMet));
    }

    public void visitingConditionAlwaysTrue(ConditionAlwaysTrue conditionAlwaysTrue) {
      ConditionAlwaysTrue result1 = new ConditionAlwaysTrue();
      result1.isConditionMet = InstructionCloner.clone(conditionAlwaysTrue.isConditionMet);
      result = result1;
    }

    public void visitBNotZeroCondition(BNotZeroCondition bNotZeroCondition) {
      result = new BNotZeroCondition(bNotZeroCondition.getB(), InstructionCloner.clone(bNotZeroCondition.isConditionMet));
    }
  }

  @Override
  public boolean visitCpir(Cpir cpir) {
    setCloned(instructionFactory.Cpir(), cpir);
    return false;
  }

  @Override
  public void visitLdi(Ldi tLdi) {
    setCloned(instructionFactory.Ldi(), tLdi);
  }

  @Override
  public void visitLdir(Ldir ldir) {
    setCloned(instructionFactory.Ldir(), ldir);
  }

  @Override
  public boolean visitLddr(Lddr lddr) {
    setCloned(instructionFactory.Lddr(), lddr);
    return false;
  }

  @Override
  public void visitEx(Ex ex) {
    setCloned(instructionFactory.Ex(clone(ex.getTarget()), clone(ex.getSource())), ex);
  }

  @Override
  public boolean visitingAdd16(Add16 add16) {
    setCloned(instructionFactory.Add16(clone(add16.getTarget()), clone(add16.getSource())), add16);
    return false;
  }

  @Override
  public void visitingAdc(Adc sbc16) {
    setCloned(instructionFactory.Adc(clone(sbc16.getTarget()), clone(sbc16.getSource())), sbc16);
  }

  @Override
  public boolean visitingAdc16(Adc16 sbc16) {
    setCloned(instructionFactory.Adc16(clone(sbc16.getTarget()), clone(sbc16.getSource())), sbc16);
    return false;
  }

  @Override
  public void visitingSbc(Sbc<T> sbc16) {
    setCloned(instructionFactory.Sbc(clone(sbc16.getTarget()), clone(sbc16.getSource())), sbc16);
  }

  @Override
  public void visitIn(In sbc16) {
    setCloned(instructionFactory.In(clone(sbc16.getTarget()), clone(sbc16.getSource())), sbc16);
  }

  @Override
  public boolean visitingSbc16(Sbc16 sbc16) {
    setCloned(instructionFactory.Sbc16(clone(sbc16.getTarget()), clone(sbc16.getSource())), sbc16);
    return false;
  }

  @Override
  public void visitingPop(Pop tjp) {
    setCloned(instructionFactory.Pop(clone(tjp.getTarget())), tjp);
  }

  @Override
  public void visitExx(Exx exx) {
    setCloned(instructionFactory.Exx(), exx);
  }

  @Override
  public void visitingSub(Sub tjp) {
    setCloned(instructionFactory.Sub(clone(tjp.getSource())), tjp);
  }

  @Override
  public boolean visitingAdd(Add tjp) {
    setCloned(instructionFactory.Add(clone(tjp.getTarget()), clone(tjp.getSource())), tjp);
    return false;
  }

  @Override
  public void visitingCp(Cp tCp) {
    setCloned(instructionFactory.Cp(clone(tCp.getSource())), tCp);
  }

  @Override
  public void visitingDec16(Dec16 tjp) {
    setCloned(instructionFactory.Dec16(clone(tjp.getTarget())), tjp);
  }

  @Override
  public void visitEI(EI ei) {
    setCloned(instructionFactory.EI(), ei);
  }

  @Override
  public void visitDI(DI tdi) {
    setCloned(instructionFactory.DI(), tdi);
  }

  @Override
  public boolean visitingJP(JP jp) {
    setCloned(instructionFactory.JP(clone(jp.getPositionOpcodeReference()), clone(jp.getCondition())), jp);
    return false;
  }

  @Override
  public void visitOut(Out tOut) {
    setCloned(instructionFactory.Out(tOut.getTarget(), clone(tOut.getSource())), tOut);
  }

  public void setCloned(AbstractInstruction cloned, AbstractInstruction instruction) {
    this.cloned = cloned;
    this.cloned.setLength(instruction.getLength());
  }

  public <R extends PublicCloneable> R clone(R cloneable) {
    try {
      return (R) cloneable.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public Register<T> clone(Register<T> register) {
    OpcodeReferenceCloner<T> opcodeReferenceCloner = new OpcodeReferenceCloner<>(instructionFactory);
    register.accept(opcodeReferenceCloner);
    return (Register<T>) opcodeReferenceCloner.getResult();
  }

  public OpcodeReference<T> clone(OpcodeReference<T> opcodeReference) {
    OpcodeReferenceCloner<T> opcodeReferenceCloner = new OpcodeReferenceCloner<>(instructionFactory);
    opcodeReference.accept(opcodeReferenceCloner);
    return (OpcodeReference<T>) opcodeReferenceCloner.getResult();
  }

  public <R1 extends ImmutableOpcodeReference<T>> R1 clone(R1 immutableOpcodeReference) {
    OpcodeReferenceCloner<T> opcodeReferenceCloner = new OpcodeReferenceCloner<>(instructionFactory);
    immutableOpcodeReference.accept(opcodeReferenceCloner);
    return (R1) opcodeReferenceCloner.getResult();
  }

  @Override
  public void visitNop(Nop nop) {
    setCloned(instructionFactory.Nop(), nop);
  }

  public void visitingInc16(Inc16 inc16) {
    setCloned(instructionFactory.Inc16(clone(inc16.getTarget())), inc16);
  }

  public boolean visitingSet(SET set) {
    setCloned(instructionFactory.SET(clone(set.getTarget()), set.getN()), set);
    return false;
  }

  public boolean visitingRes(RES res) {
    setCloned(instructionFactory.RES(clone(res.getTarget()), res.getN()), res);
    return false;
  }

  public boolean visitingBit(BIT bit) {
    setCloned(instructionFactory.BIT(clone(bit.getTarget()), bit.getN()), bit);
    return false;
  }

  public boolean visitingDjnz(DJNZ<T> djnz) {
    setCloned(instructionFactory.DJNZ(clone(djnz.getCondition()), djnz.getPositionOpcodeReference()), djnz);
    return false;
  }

  public void visitingLd(Ld ld) {
    setCloned(instructionFactory.Ld(clone(ld.getTarget()), clone(ld.getSource())), ld);
  }

  public boolean visitingInc(Inc inc) {
    setCloned(instructionFactory.Inc(clone(inc.getTarget())), inc);
    return false;
  }

  public boolean visitingRla(RLA rla) {
    setCloned(instructionFactory.RLA(), rla);
    return false;
  }

  public boolean visitingRl(RL rl) {
    setCloned(instructionFactory.RL(rl.getTarget()), rl);
    return false;
  }

  public boolean visitingRet(Ret ret) {
    setCloned(instructionFactory.Ret(ret.getCondition()), ret);
    return false;
  }

  public void visitingAnd(And and) {
    setCloned(instructionFactory.And(and.getSource()), and);
  }

  public void visitingOr(Or or) {
    setCloned(instructionFactory.Or(or.getSource()), or);
  }

  public void visitingXor(Xor xor) {
    setCloned(instructionFactory.Xor(xor.getSource()), xor);
  }

  public void visitingRst(RST rst) {
    setCloned(instructionFactory.RST(rst.getP()), rst);
  }

  public void visitingIm(IM im) {
    setCloned(instructionFactory.IM(im.getMode()), im);
  }

  public boolean visitingDec(Dec dec) {
    setCloned(instructionFactory.Dec(clone(dec.getTarget())), dec);
    return false;
  }

  public void visitingJR(JR jr) {
    setCloned(instructionFactory.JR(clone(jr.getCondition()), clone(jr.getPositionOpcodeReference())), jr);
  }

  public <S extends Condition> S clone(S condition) {
    Condition condition1 = cloneCondition(condition);
    return (S) condition1;
//    ConditionCloner visitor = new ConditionCloner(this);
//    condition.accept(visitor);
//    return (S) visitor.result;
  }

  @Override
  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {
    Constructor<?>[] constructors = parameterizedUnaryAluInstruction.getClass().getConstructors();
    try {
      cloned = (AbstractInstruction) constructors[0].newInstance(clone(parameterizedUnaryAluInstruction.getTarget()), parameterizedUnaryAluInstruction.getFlag());
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  public static ConditionPredicate<Boolean> clone(ConditionPredicate isConditionMet) {
    if (isConditionMet instanceof FlipFLopConditionFlag.FlipFlopPredicate flipFlopPredicate) {
      return new FlipFLopConditionFlag(flipFlopPredicate.executionsListener, flipFlopPredicate.alwaysTrue).isConditionMet;
    } else
      return isConditionMet;
  }
}
