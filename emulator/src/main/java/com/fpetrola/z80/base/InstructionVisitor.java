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
import com.fpetrola.z80.opcodes.decoder.table.NullOpcodeReference;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

public interface InstructionVisitor<R> {
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

  default boolean visitingAdd16(Add16 tAdd16) {

    return false;
  }

  default void visitingAnd(And tAnd) {

  }

  default boolean visitingDec(Dec dec) {

    return false;
  }

  default void visitingDec16(Dec16 tDec16) {

  }

  default boolean visitingInc(Inc tInc) {

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

  default boolean visitingBit(BIT bit){
    return false;
  }

  default boolean visitingDjnz(DJNZ djnz) {

    return false;
  }

  default void visitingLd(Ld ld) {

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

  default boolean visitingBitOperation(BitOperation tBitOperation) {
    return false;
  }

  default void visitingPop(Pop pop) {

  }

  default boolean visitingJP(JP jp) {
    return false;
  }

  default void visitingFlag(Register flag, DefaultTargetFlagInstruction targetSourceInstruction) {

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

  default void visitConstantOpcodeReference(ConstantOpcodeReference constantOpcodeReference) {

  }

  default void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference memoryAccessOpcodeReference) {

  }

  default void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {

  }

  default void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {

  }

  default void visitEx(Ex ex) {
  }

  default void visitIn(In tIn) {

  }

  default void visitOut(Out tOut) {

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

  default boolean visitRepeatingInstruction(RepeatingInstruction tRepeatingInstruction) {

    return false;
  }

  default boolean visitLdir(Ldir ldir) {

    return false;
  }

  default boolean visitLddr(Lddr lddr) {
    return false;
  }

  default void visitBlockInstruction(BlockInstruction blockInstruction) {

  }

  default boolean visitCpir(Cpir cpir) {

    return false;
  }

  default boolean visitLdi(Ldi tLdi) {
    return false;
  }

  default void visitBNotZeroCondition(BNotZeroCondition bNotZeroCondition) {
  }

  default boolean visitingSbc16(Sbc16 sbc16) {

    return false;
  }

  default void visitingSbc(Sbc sbc) {

  }

  default void visitingAdc(Adc tAdc) {

  }

  default boolean visitingAdc16(Adc16 tAdc16) {
    return false;
  }

  default boolean visitCpdr(Cpdr tCpdr) {
    return false;
  }

  default boolean visitingRlca(RLCA rlca) {
    return false;
  }

  default boolean visitingRrca(RRCA rrca) {
    return false;
  }

  default boolean visitingRlc(RLC rlc) {
    return false;
  }

  default boolean visitingRrc(RRC rrc) {
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

  default boolean visitingSra(SRA tsra) {
    return false;
  }

  default void visitingHalt(Halt halt) {

  }

  default boolean visitCpi(Cpi cpi) {
    return false;
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

  default boolean visitOuti(Outi outi) {
    return false;
  }

  default boolean visitOutd(Outd outi) {
    return false;
  }

  default boolean visitIni(Ini tIni) {
    return false;
  }

  default boolean visitInd(Ind tInd) {
    return false;
  }

  default boolean visitCpd(Cpd cpd) {
    return false;
  }

  default boolean visitRLD(RLD rld) {
    return false;
  }

  default boolean visitRRD(RRD rrd) {
    return false;
  }

  default boolean visitLdd(Ldd ldd) {
    return false;
  }

  default boolean visitMemory16BitReference(Memory16BitReference memory16BitReference) {
    return false;
  }

  default boolean visitMemory8BitReference(Memory8BitReference memory8BitReference) {
    return false;
  }

  default boolean visiting16BitsOperation(Binary16BitsOperation binary16BitsOperation) {
    return false;
  }

  default boolean visitingRra(RRA rra) {
    return false;
  }

  default boolean visitLdAR(LdAR tLdAR) {
    return false;
  }

  default void visitNullOpcodeReference(NullOpcodeReference tNullOpcodeReference) {

  }

  default boolean visitInir(Inir inir) {
    return false;
  }

  default boolean visitIndr(Indr indr) {
    return false;
  }

  default boolean visitOutir(Outir outir) {
    return false;
  }

  default boolean visitOutdr(Outdr outdr) {
    return false;
  }
}
