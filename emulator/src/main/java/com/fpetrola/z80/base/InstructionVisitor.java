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

public interface InstructionVisitor<T extends WordNumber> {
  default void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
  }

  default void visitingTargetInstruction(TargetInstruction targetInstruction) {
  }

  default void visitingInstruction(AbstractInstruction tAbstractInstruction) {

  }

  default void visitingAdd(Add add) {

  }

  default void visitingAdd16(Add16 tAdd16) {

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

  default void visitingDjnz(DJNZ<T> djnz) {

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

  default void visitRepeatingInstruction(RepeatingInstruction tRepeatingInstruction) {

  }

  default void visitLdir(Ldir ldir) {

  }

  default void visitLddr(Lddr lddr) {

  }

  default void visitBlockInstruction(BlockInstruction blockInstruction) {

  }

  default void visitCpir(Cpir cpir) {

  }

  default void visitLdi(Ldi tLdi) {

  }

  default void visitBNotZeroCondition(BNotZeroCondition bNotZeroCondition) {
  }

  default void visitingSbc16(Sbc16 sbc16) {

  }

  default void visitingSbc(Sbc<T> sbc) {

  }

  default void visitingAdc(Adc tAdc) {

  }

  default void visitingAdc16(Adc16 tAdc16) {

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

  default void visitCpi(Cpi cpi) {

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
}
