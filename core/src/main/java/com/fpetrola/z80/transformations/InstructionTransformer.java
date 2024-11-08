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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.visitor.*;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("ALL")
public class InstructionTransformer<T extends WordNumber> extends InstructionTransformerBase<T> {
  public final VirtualRegisterFactory virtualRegisterFactory;

  public void setCurrentInstruction(Instruction currentInstruction) {
    this.currentInstruction = currentInstruction;
  }

  private Instruction currentInstruction;

  public InstructionTransformer(InstructionFactory instructionFactory, VirtualRegisterFactory virtualRegisterFactory) {
    super(instructionFactory);
    this.virtualRegisterFactory = virtualRegisterFactory;
  }

  @Override
  public Instruction<T> clone(Instruction<T> instruction) {
    virtualRegisterFactory.initTransaction();
    Instruction<T> cloned1 = super.clone(instruction);
    virtualRegisterFactory.endTransaction();
    return cloned1;
  }

  public void visitingHalt(Halt halt) {
    setCloned(instructionFactory.Halt(), halt);
  }

  public void visitingLd(Ld ld) {
    setCloned(instructionFactory.Ld(clone(ld.getTarget()), clone(ld.getSource())), ld);
    TargetSourceInstruction cloned1 = (TargetSourceInstruction) cloned;

    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, new VirtualFetcher()));
    cloned1.setSource(createRegisterReplacement(cloned1.getSource(), null, new VirtualFetcher()));
//    cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), null, new VirtualFetcher()));
  }

  public void visitingInc16(Inc16 inc16) {
    setCloned(instructionFactory.Inc16(clone(inc16.getTarget())), inc16);
    Inc16 cloned1 = (Inc16) cloned;
    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, new VirtualFetcher()));
  }

  public void visitingDjnz(DJNZ<T> djnz) {
    setCloned(instructionFactory.DJNZ(clone(djnz.getCondition()), clone(djnz.getPositionOpcodeReference())), djnz);
    DJNZ djnz1 = (DJNZ) cloned;
    djnz1.accept(new ConditionTransformerVisitor(new VirtualFetcher()));
  }

  public void visitingJR(JR jr) {
    setCloned(instructionFactory.JR(clone(jr.getCondition()), clone(jr.getPositionOpcodeReference())), jr);
    JR clonedJr = (JR) cloned;
    clonedJr.accept(new ConditionTransformerVisitor(new VirtualFetcher()));
  }

  @Override
  public boolean visitingRet(Ret ret) {
    setCloned(instructionFactory.Ret(clone(ret.getCondition())), ret);
    Ret clonedRet = (Ret) cloned;
    clonedRet.accept(new ConditionTransformerVisitor(new VirtualFetcher()));
    return false;
  }

  public void visitNop(Nop nop) {
    setCloned(instructionFactory.Nop(), nop);
  }

  @Override
  public void visitDI(DI tdi) {
    setCloned(instructionFactory.DI(), tdi);
  }

  @Override
  public void visitEI(EI ei) {
    setCloned(instructionFactory.EI(), ei);
  }

  @Override
  public void visitingCcf(CCF ccf) {
    setCloned(instructionFactory.CCF(), ccf);
    CCF cloned1 = (CCF) cloned;
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), cloned1, virtualFetcher));
  }

  @Override
  public void visitingScf(SCF scf) {
    setCloned(instructionFactory.SCF(), scf);
    SCF cloned1 = (SCF) cloned;
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), cloned1, virtualFetcher));
  }

  @Override
  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {
    super.visitingParameterizedUnaryAluInstruction(parameterizedUnaryAluInstruction);
    ParameterizedUnaryAluInstruction cloned1 = (ParameterizedUnaryAluInstruction) cloned;
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, virtualFetcher));
    cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), cloned1, virtualFetcher));
    return false;
  }

  @Override
  public void visitingJP(JP jp) {
    setCloned(instructionFactory.JP(clone(jp.getPositionOpcodeReference()), clone(jp.getCondition())), jp);
    JP clonedJp = (JP) cloned;
    VirtualFetcher virtualFetcher = new VirtualFetcher();

    clonedJp.setPositionOpcodeReference(createRegisterReplacement(clonedJp.getPositionOpcodeReference(), clonedJp, virtualFetcher));
    clonedJp.accept(new ConditionTransformerVisitor(virtualFetcher));
  }

  @Override
  public void visitEx(Ex ex) {
    setCloned(instructionFactory.Ex(clone(ex.getTarget()), clone(ex.getSource())), ex);
    Ex clonedEx = (Ex) cloned;
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    clonedEx.setTarget(createRegisterReplacement(clonedEx.getTarget(), clonedEx, virtualFetcher));
    clonedEx.setSource(createRegisterReplacement(clonedEx.getSource(), clonedEx, virtualFetcher));
  }

  public boolean visitingCall(Call call) {
    setCloned(instructionFactory.Call(clone(call.getCondition()), clone(call.getPositionOpcodeReference())), call);
    Call clonedCall = (Call) cloned;
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    clonedCall.setPositionOpcodeReference(createRegisterReplacement(clonedCall.getPositionOpcodeReference(), clonedCall, virtualFetcher));
    clonedCall.accept(new ConditionTransformerVisitor(virtualFetcher));
    return false;
  }

  public void visitIn(In in) {
    setCloned(instructionFactory.In(clone(in.getTarget()), clone(in.getSource())), in);
    In cloned1 = (In) cloned;

    VirtualFetcher virtualFetcher = new VirtualFetcher();
    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, virtualFetcher));
    cloned1.setSource(createRegisterReplacement(cloned1.getSource(), null, virtualFetcher));
    cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), cloned1, virtualFetcher));

  }

  public void visitOut(Out out) {
    setCloned(instructionFactory.Out(clone(out.getTarget()), clone(out.getSource())), out);
    TargetSourceInstruction cloned1 = (TargetSourceInstruction) cloned;

    // cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, new VirtualFetcher()));
    cloned1.setSource(createRegisterReplacement(cloned1.getSource(), null, new VirtualFetcher()));
  }

  public void visitExx(Exx exx) {
    setCloned(instructionFactory.Exx(), exx);
    Exx cloned1 = (Exx) cloned;

    cloned1.setBc(createRegisterReplacement(cloned1.getBc(), cloned1, new VirtualFetcher()));
    cloned1.set_bc(createRegisterReplacement(cloned1.get_bc(), cloned1, new VirtualFetcher()));
    cloned1.setDe(createRegisterReplacement(cloned1.getDe(), cloned1, new VirtualFetcher()));
    cloned1.set_de(createRegisterReplacement(cloned1.get_de(), cloned1, new VirtualFetcher()));
    cloned1.setHl(createRegisterReplacement(cloned1.getHl(), cloned1, new VirtualFetcher()));
    cloned1.set_hl(createRegisterReplacement(cloned1.get_hl(), cloned1, new VirtualFetcher()));
  }

  @Override
  public void visitingDec16(Dec16 dec16) {
    setCloned(instructionFactory.Dec16(clone(dec16.getTarget())), dec16);
    Dec16 cloned1 = (Dec16) cloned;

    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, new VirtualFetcher()));
  }

  public void visitingBitOperation(BitOperation bitOperation) {
    Constructor<?>[] constructors = bitOperation.getClass().getConstructors();
    try {
      AbstractInstruction cloned1 = (AbstractInstruction) constructors[0].newInstance(clone(bitOperation.getTarget()), bitOperation.getN(), bitOperation.getFlag());
      setCloned(cloned1, bitOperation);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }

    BitOperation cloned1 = (BitOperation) cloned;

    VirtualFetcher virtualFetcher = new VirtualFetcher();

    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, virtualFetcher));
    if (bitOperation instanceof BIT)
      cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), cloned1, virtualFetcher));
  }

  public void visitingRst(RST rst) {
    setCloned(instructionFactory.RST(rst.getP()), rst);
  }

  public void visitPush(Push push) {
    setCloned(instructionFactory.Push(clone(push.getTarget())), push);
    Push cloned1 = (Push) cloned;

    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, new VirtualFetcher()));
  }

  @Override
  public void visitingPop(Pop pop) {
    setCloned(instructionFactory.Pop(clone(pop.getTarget())), pop);
    Pop cloned1 = (Pop) cloned;
    cloned1.setTarget(createRegisterReplacement(cloned1.getTarget(), cloned1, new VirtualFetcher()));
  }

  public void visitLdir(Ldir ldir) {
    setCloned(instructionFactory.Ldir(), ldir);
    Ldir cloned1 = (Ldir) cloned;

    Ldi instructionToRepeat = (Ldi) cloned1.getInstructionToRepeat();
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    RegisterPair bcReplacement = createRegisterReplacement(cloned1.getBc(), cloned1, virtualFetcher);
    cloned1.setBc(bcReplacement);

    instructionToRepeat.setBc(bcReplacement);
    instructionToRepeat.setHl(createRegisterReplacement(instructionToRepeat.getHl(), cloned1, virtualFetcher));
    instructionToRepeat.setDe(createRegisterReplacement(instructionToRepeat.getDe(), cloned1, virtualFetcher));
    instructionToRepeat.setFlag(createRegisterReplacement(instructionToRepeat.getFlag(), cloned1, virtualFetcher));

    cloned1.setInstructionToRepeat(instructionToRepeat);
  }

  @Override
  public void visitLddr(Lddr lddr) {
    setCloned(instructionFactory.Lddr(), lddr);
    Lddr cloned1 = (Lddr) cloned;

    Ldd instructionToRepeat = (Ldd) cloned1.getInstructionToRepeat();
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    RegisterPair bcReplacement = createRegisterReplacement(cloned1.getBc(), cloned1, virtualFetcher);
    cloned1.setBc(bcReplacement);

    instructionToRepeat.setBc(bcReplacement);
    instructionToRepeat.setHl(createRegisterReplacement(instructionToRepeat.getHl(), cloned1, virtualFetcher));
    instructionToRepeat.setDe(createRegisterReplacement(instructionToRepeat.getDe(), cloned1, virtualFetcher));
    instructionToRepeat.setFlag(createRegisterReplacement(instructionToRepeat.getFlag(), cloned1, virtualFetcher));
  }

  @Override
  public void visitLdi(Ldi tLdi) {
    setCloned(instructionFactory.Ldi(), tLdi);
    Ldi cloned1 = (Ldi) cloned;

    VirtualFetcher virtualFetcher = new VirtualFetcher();
    cloned1.setBc(createRegisterReplacement(cloned1.getBc(), cloned1, virtualFetcher));
    cloned1.setHl(createRegisterReplacement(cloned1.getHl(), cloned1, virtualFetcher));
    cloned1.setDe(createRegisterReplacement(cloned1.getDe(), cloned1, virtualFetcher));
    cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), cloned1, virtualFetcher));
  }

  @Override
  public void visitCpir(Cpir cpir) {
    setCloned(instructionFactory.Cpir(), cpir);
    Cpir cloned1 = (Cpir) cloned;

    Cpi instructionToRepeat = (Cpi) cloned1.getInstructionToRepeat();
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    RegisterPair bcReplacement = createRegisterReplacement(cloned1.getBc(), cloned1, virtualFetcher);
    cloned1.setBc(bcReplacement);

    instructionToRepeat.setBc(bcReplacement);
    instructionToRepeat.setHl(createRegisterReplacement(instructionToRepeat.getHl(), cloned1, virtualFetcher));
    instructionToRepeat.setA(createRegisterReplacement(instructionToRepeat.getA(), cloned1, virtualFetcher));
    instructionToRepeat.setFlag(createRegisterReplacement(instructionToRepeat.getFlag(), cloned1, virtualFetcher));

    cloned1.setInstructionToRepeat(instructionToRepeat);
  }


  @Override
  public void visitingIm(IM im) {
    setCloned(instructionFactory.IM(im.getMode()), im);
  }

  private <R extends PublicCloneable> R createRegisterReplacement(R cloneable, Instruction currentInstruction1, VirtualFetcher virtualFetcher) {
    if (cloneable instanceof MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
      OpcodeReference target1 = (OpcodeReference) memoryPlusRegister8BitReference.getTarget();
      ImmutableOpcodeReference result;
      if (target1 instanceof Register register) {
        result = virtualRegisterFactory.createVirtualRegister(null, register, virtualFetcher);
      } else {
        result = clone(memoryPlusRegister8BitReference.getTarget());
      }

      try {
        MemoryPlusRegister8BitReference clone = (MemoryPlusRegister8BitReference) memoryPlusRegister8BitReference.clone();
        clone.setTarget(result);
        return (R) clone;
//        return (R) new MemoryPlusRegister8BitReference(result, memoryPlusRegister8BitReference.getMemory(), memoryPlusRegister8BitReference.getPc(), memoryPlusRegister8BitReference.getValueDelta());
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    } else if (cloneable instanceof IndirectMemory8BitReference indirectMemory8BitReference) {
      OpcodeReference target1 = (OpcodeReference) indirectMemory8BitReference.target;
      ImmutableOpcodeReference result;
      if (target1 instanceof Register register) {
        result = virtualRegisterFactory.createVirtualRegister(null, register, virtualFetcher);
      } else {
        result = clone(indirectMemory8BitReference.target);
      }

      return (R) new IndirectMemory8BitReference(result, indirectMemory8BitReference.getMemory());
    } else if (cloneable instanceof IndirectMemory16BitReference indirectMemory16BitReference) {
      OpcodeReference target1 = (OpcodeReference) indirectMemory16BitReference.target;
      ImmutableOpcodeReference result;
      if (target1 instanceof Register register) {
        result = virtualRegisterFactory.createVirtualRegister(null, register, virtualFetcher);
      } else {
        result = clone(indirectMemory16BitReference.target);
      }

      return (R) new IndirectMemory16BitReference(result, indirectMemory16BitReference.getMemory());
    } else if (cloneable instanceof Register register) {
      return (R) virtualRegisterFactory.createVirtualRegister(currentInstruction1, register, virtualFetcher);
    } else
      return super.clone(cloneable);
  }


  @Override
  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction parameterizedBinaryAluInstruction) {
    super.visitingParameterizedBinaryAluInstruction(parameterizedBinaryAluInstruction);
    ParameterizedBinaryAluInstruction cloned1 = (ParameterizedBinaryAluInstruction) cloned;
    VirtualFetcher virtualFetcher = new VirtualFetcher();
    OpcodeReference targetReplacement = createRegisterReplacement(cloned1.getTarget(), cloned1, virtualFetcher);
    if (cloned1.getTarget() == cloned1.getSource()) {
      cloned1.setTarget(targetReplacement);
      cloned1.setSource(targetReplacement);
    } else {
      cloned1.setTarget(targetReplacement);
      cloned1.setSource(createRegisterReplacement(cloned1.getSource(), cloned1, virtualFetcher));
    }
    cloned1.setFlag(createRegisterReplacement(cloned1.getFlag(), cloned1, virtualFetcher));
  }

  private class ConditionTransformerVisitor implements InstructionVisitor {
    private final VirtualFetcher virtualFetcher;

    public ConditionTransformerVisitor(VirtualFetcher virtualFetcher) {
      this.virtualFetcher = virtualFetcher;
    }

    public void visitingConditionFlag(ConditionFlag conditionFlag) {
      conditionFlag.setRegister(createRegisterReplacement(conditionFlag.getRegister(), null, virtualFetcher));
    }

    public void visitBNotZeroCondition(BNotZeroCondition bNotZeroCondition) {
      bNotZeroCondition.setB(createRegisterReplacement(bNotZeroCondition.getB(), null, virtualFetcher));
    }
  }
}
