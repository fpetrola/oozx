/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.instructions.factory;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.*;

public interface InstructionFactoryDelegator<T extends WordNumber> extends InstructionFactory<T> {
  default DJNZ<T> DJNZ(BNotZeroCondition bnz, ImmutableOpcodeReference<T> target) {
    return getDelegate().DJNZ(bnz, target);
  }

  default JP JP(ImmutableOpcodeReference target, Condition condition) {
    return getDelegate().JP(target, condition);
  }

  default Call Call(Condition condition, ImmutableOpcodeReference positionOpcodeReference) {
    return getDelegate().Call(condition, positionOpcodeReference);
  }

  default JR JR(Condition condition, ImmutableOpcodeReference target) {
    return getDelegate().JR(condition, target);
  }

  default Adc<T> Adc(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return getDelegate().Adc(target, source);
  }

  default Cpd Cpd() {
    return getDelegate().Cpd();
  }

  default CCF CCF() {
    return getDelegate().CCF();
  }

  default Cpi Cpi() {
    return getDelegate().Cpi();
  }

  default Adc16 Adc16(OpcodeReference target, ImmutableOpcodeReference source) {
    return getDelegate().Adc16(target, source);
  }

  default Add Add(OpcodeReference target, ImmutableOpcodeReference source) {
    return getDelegate().Add(target, source);
  }

  default Add16 Add16(OpcodeReference target, ImmutableOpcodeReference source) {
    return getDelegate().Add16(target, source);
  }

  default And And(ImmutableOpcodeReference source) {
    return getDelegate().And(source);
  }

  default Or Or(ImmutableOpcodeReference source) {
    return getDelegate().Or(source);
  }

  default Sbc Sbc(OpcodeReference target, ImmutableOpcodeReference source) {
    return getDelegate().Sbc(target, source);
  }

  default Sbc16 Sbc16(OpcodeReference target, ImmutableOpcodeReference source) {
    return getDelegate().Sbc16(target, source);
  }

  default Sub Sub(ImmutableOpcodeReference source) {
    return getDelegate().Sub(source);
  }

  default Cp Cp(ImmutableOpcodeReference source) {
    return getDelegate().Cp(source);
  }

  default Xor Xor(ImmutableOpcodeReference source) {
    return getDelegate().Xor(source);
  }

  default BIT BIT(OpcodeReference target, int n) {
    return getDelegate().BIT(target, n);
  }

  default RES RES(OpcodeReference target, int n) {
    return getDelegate().RES(target, n);
  }

  default SET SET(OpcodeReference target, int n) {
    return getDelegate().SET(target, n);
  }

  default Cpir<T> Cpir() {
    return getDelegate().Cpir();
  }

  default Cpdr Cpdr() {
    return getDelegate().Cpdr();
  }

  default Indr Indr() {
    return getDelegate().Indr();
  }

  default Inir Inir() {
    return getDelegate().Inir();
  }

  default Lddr Lddr() {
    return getDelegate().Lddr();
  }

  default Outdr Outdr() {
    return getDelegate().Outdr();
  }

  default Ldir Ldir() {
    return getDelegate().Ldir();
  }

  default Outir Outir() {
    return getDelegate().Outir();
  }

  default Ind Ind() {
    return getDelegate().Ind();
  }

  default Ini Ini() {
    return getDelegate().Ini();
  }

  default Outi Outi() {
    return getDelegate().Outi();
  }

  default CPL CPL() {
    return getDelegate().CPL();
  }

  default DAA DAA() {
    return getDelegate().DAA();
  }

  default Dec Dec(OpcodeReference target) {
    return getDelegate().Dec(target);
  }

  default Dec16 Dec16(OpcodeReference target) {
    return getDelegate().Dec16(target);
  }

  default DI DI() {
    return getDelegate().DI();
  }

  default EI EI() {
    return getDelegate().EI();
  }

  default Ex Ex(OpcodeReference target, OpcodeReference source) {
    return getDelegate().Ex(target, source);
  }

  default Exx Exx() {
    return getDelegate().Exx();
  }

  default Halt Halt() {
    return getDelegate().Halt();
  }

  default IM IM(int mode) {
    return getDelegate().IM(mode);
  }

  default In In(OpcodeReference target, ImmutableOpcodeReference source) {
    return getDelegate().In(target, source);
  }

  default Inc Inc(OpcodeReference target) {
    return getDelegate().Inc(target);
  }

  default Inc16 Inc16(OpcodeReference target) {
    return getDelegate().Inc16(target);
  }

  default Ld<T> Ld(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return getDelegate().Ld(target, source);
  }

  default LdAR<T> LdAR(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return getDelegate().LdAR(target, source);
  }

  default Ldd Ldd() {
    return getDelegate().Ldd();
  }

  default Ldi Ldi() {
    return getDelegate().Ldi();
  }

  default LdOperation<T> LdOperation(OpcodeReference target, Instruction<T> instruction) {
    return getDelegate().LdOperation(target, instruction);
  }

  default Neg Neg(OpcodeReference target) {
    return getDelegate().Neg(target);
  }

  default Nop Nop() {
    return getDelegate().Nop();
  }

  default Out Out(ImmutableOpcodeReference target, ImmutableOpcodeReference source) {
    return getDelegate().Out(target, source);
  }

  default Outd Outd() {
    return getDelegate().Outd();
  }

  default Pop Pop(OpcodeReference target) {
    return getDelegate().Pop(target);
  }

  default Push Push(OpcodeReference target) {
    return getDelegate().Push(target);
  }

  default Ret Ret(Condition condition) {
    return getDelegate().Ret(condition);
  }

  default RetN RetN(Condition condition) {
    return getDelegate().RetN(condition);
  }

  default RL<T> RL(OpcodeReference target) {
    return getDelegate().RL(target);
  }

  default RLA RLA() {
    return getDelegate().RLA();
  }

  default RLC<T> RLC(OpcodeReference target) {
    return getDelegate().RLC(target);
  }

  default RLCA RLCA() {
    return getDelegate().RLCA();
  }

  default RLD RLD() {
    return getDelegate().RLD();
  }

  default RR RR(OpcodeReference target) {
    return getDelegate().RR(target);
  }

  default RRA RRA() {
    return getDelegate().RRA();
  }

  default RRC RRC(OpcodeReference target) {
    return getDelegate().RRC(target);
  }

  default RRCA RRCA() {
    return getDelegate().RRCA();
  }

  default RRD RRD() {
    return getDelegate().RRD();
  }

  default RST RST(int p) {
    return getDelegate().RST(p);
  }

  default SCF SCF() {
    return getDelegate().SCF();
  }

  default SLA SLA(OpcodeReference<T> target) {
    return getDelegate().SLA(target);
  }

  default SLL SLL(OpcodeReference target) {
    return getDelegate().SLL(target);
  }

  default SRA SRA(OpcodeReference target) {
    return getDelegate().SRA(target);
  }

  default SRL SRL(OpcodeReference target) {
    return getDelegate().SRL(target);
  }

  InstructionFactory<T> getDelegate();
}
