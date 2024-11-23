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

package com.fpetrola.z80.base;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

public interface InstructionVisitor<T extends WordNumber, R> {
  default R getResult() {
    return null;
  }

  default void setResult(R result) {
  }


  default void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
  }

  default void visitingTargetInstruction(TargetInstruction targetInstruction) {
  }

  default void visitingInstruction(AbstractInstruction tAbstractInstruction) {

  }

  default boolean visitingAdd(Add add) {

    return false;
  }

  default void visitingAdd16(Add16 tAdd16) {

  }

  default void visitingAnd(And tAnd) {

  }

  default boolean visitingDec(Dec<T> dec) {

    return false;
  }

  default void visitingDec16(Dec16 tDec16) {

  }

  default boolean visitingInc(Inc<T> tInc) {

    return false;
  }

  default void visitingOr(Or tOr) {

  }

  default void visitingSub(Sub tSub) {

  }

  default void visitingXor(Xor tXor) {

  }

  default void visitingCp(Cp tCp) {

  }

  default boolean visitingRet(Ret ret) {

    return false;
  }

  default boolean visitingCall(Call tCall) {

    return false;
  }

  default void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
  }

  default void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {

  }

  default void visitingInc16(Inc16 tInc16) {

  }

  default boolean visitingSet(SET set) {

    return false;
  }

  default boolean visitingRes(RES res) {

    return false;
  }

  default boolean visitingBit(BIT<T> bit){
    return false;
  }

  default boolean visitingDjnz(DJNZ<T> djnz) {

    return false;
  }

  default void visitingLd(Ld<T> ld) {

  }

  default boolean visitingRla(RLA rla) {
    return false;
  }

  default boolean visitingRl(RL rl) {

    return false;
  }

  default void visitingRst(RST rst) {

  }

  default void visitingIm(IM im) {

  }

  default void visitingJR(JR jr) {

  }

  default void visitingConditionAlwaysTrue(ConditionAlwaysTrue conditionAlwaysTrue) {

  }

  default void visitingConditionFlag(ConditionFlag conditionFlag) {

  }

  default boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {

    return false;
  }

  default void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction parameterizedBinaryAluInstruction) {

  }

  default void visitingBitOperation(BitOperation tBitOperation) {

  }

  default void visitingPop(Pop pop) {

  }

  default void visitingJP(JP tjp) {

  }

  default void visitingFlag(Register<T> flag, DefaultTargetFlagInstruction targetSourceInstruction) {

  }

  default void visitImmutableOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {

  }

  default void visitMutableOpcodeReference(MutableOpcodeReference mutableOpcodeReference) {

  }

  default void visitOpcodeReference(OpcodeReference opcodeReference) {

  }

  default boolean visitRegister(Register register) {

    return false;
  }

  default void visitConstantOpcodeReference(ConstantOpcodeReference<T> constantOpcodeReference) {

  }

  default void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference<T> memoryAccessOpcodeReference) {

  }

  default void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {

  }

  default void visitIndirectMemory8BitReference(IndirectMemory8BitReference<T> indirectMemory8BitReference) {

  }

  default void visitEx(Ex<T> ex) {
  }

  default void visitIn(In<T> tIn) {

  }

  default void visitOut(Out<T> tOut) {

  }

  default void visitExx(Exx exx) {

  }

  default void visitNop(Nop nop) {
  }

  default void visitDI(DI tdi) {
  }

  default void visitPush(Push push) {
  }

  default void visitEI(EI ei) {
  }

  default void visitingCcf(CCF ccf) {

  }

  default void visitingScf(SCF scf) {

  }

  default void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {

  }

  default boolean visitRepeatingInstruction(RepeatingInstruction<T> tRepeatingInstruction) {

    return false;
  }

  default void visitLdir(Ldir<T> ldir) {

  }

  default boolean visitLddr(Lddr<T> lddr) {
    return false;
  }

  default void visitBlockInstruction(BlockInstruction blockInstruction) {

  }

  default boolean visitCpir(Cpir<T> cpir) {

    return false;
  }

  default void visitLdi(Ldi<T> tLdi) {

  }

  default void visitBNotZeroCondition(BNotZeroCondition bNotZeroCondition) {
  }

  default void visitingSbc16(Sbc16<T> sbc16) {

  }

  default void visitingSbc(Sbc<T> sbc) {

  }

  default void visitingAdc(Adc tAdc) {

  }

  default boolean visitingAdc16(Adc16<T> tAdc16) {
    return false;
  }

  default void visitCpdr(Cpdr tCpdr) {
  }

  default boolean visitingRlca(RLCA rlca) {
    return false;
  }

  default boolean visitingRrca(RRCA rrca) {
    return false;
  }

  default boolean visitingRlc(RLC<T> rlc) {
    return false;
  }

  default boolean visitingRrc(RRC<T> rrc) {
    return false;
  }

  default void visitingNeg(Neg tNeg) {
  }

  default boolean visitingRr(RR trr) {
    return false;
  }

  default boolean visitingCpl(CPL cpl) {
    return false;
  }

  default boolean visitingSra(SRA<T> tsra) {
    return false;
  }

  default void visitingHalt(Halt halt) {

  }

  default void visitCpi(Cpi<T> cpi) {

  }

  default boolean visitingSll(SLL sll) {
    return false;
  }

  default boolean visitingSla(SLA sla) {
    return false;
  }

  default boolean visitingSrl(SRL srl) {
    return false;
  }

  default boolean visitingDaa(DAA daa) {
    return false;
  }

  default void visitingTargetSourceInstruction(TargetSourceInstruction targetSourceInstruction) {
  }

  default boolean visitLdOperation(LdOperation ldOperation) {
    return false;
  }

  default boolean visitOuti(Outi<T> outi) {
    return false;
  }

  default boolean visitOutd(Outd<T> outi) {
    return false;
  }

  default boolean visitIni(Ini<T> tIni) {
    return false;
  }

  default boolean visitInd(Ind<T> tInd) {
    return false;
  }

  default boolean visitCpd(Cpd<T> cpd) {
    return false;
  }

  default boolean visitRLD(RLD<T> rld) {
    return false;
  }

  default boolean visitRRD(RRD<T> rrd) {
    return false;
  }

  default boolean visitLdd(Ldd ldd) {
    return false;
  }

  default void visitMemory16BitReference(Memory16BitReference<T> memory16BitReference) {
  }

  default void visitMemory8BitReference(Memory8BitReference<T> memory8BitReference) {

  }
}
