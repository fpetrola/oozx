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

public interface InstructionFactory<T extends WordNumber> {
  DJNZ<T> DJNZ(BNotZeroCondition bnz, ImmutableOpcodeReference<T> target);

  JP JP(ImmutableOpcodeReference target, Condition condition);

  Call Call(Condition condition, ImmutableOpcodeReference positionOpcodeReference);

  JR JR(Condition condition, ImmutableOpcodeReference target);

  Adc<T> Adc(OpcodeReference<T> target, ImmutableOpcodeReference<T> source);

  Cpd Cpd();

  CCF CCF();

  Cpi Cpi();

  Adc16 Adc16(OpcodeReference target, ImmutableOpcodeReference source);

  Add Add(OpcodeReference target, ImmutableOpcodeReference source);

  Add16 Add16(OpcodeReference target, ImmutableOpcodeReference source);

  And And(ImmutableOpcodeReference source);

  Or Or(ImmutableOpcodeReference source);

  Sbc Sbc(OpcodeReference target, ImmutableOpcodeReference source);

  Sbc16 Sbc16(OpcodeReference target, ImmutableOpcodeReference source);

  Sub Sub(ImmutableOpcodeReference source);

  Cp Cp(ImmutableOpcodeReference source);

  Xor Xor(ImmutableOpcodeReference source);

  BIT BIT(OpcodeReference target, int n);

  RES RES(OpcodeReference target, int n);

  SET SET(OpcodeReference target, int n);

  Cpir<T> Cpir();

  Cpdr Cpdr();

  Indr Indr();

  Inir Inir();

  Lddr Lddr();

  Outdr Outdr();

  Ldir Ldir();

  Outir Outir();

  Ind Ind();

  Ini Ini();

  Outi Outi();

  CPL CPL();

  DAA DAA();

  Dec Dec(OpcodeReference target);

  Dec16 Dec16(OpcodeReference target);

  DI DI();

  EI EI();

  Ex Ex(OpcodeReference target, OpcodeReference source);

  Exx Exx();

  Halt Halt();

  IM IM(int mode);

  In In(OpcodeReference target, ImmutableOpcodeReference source);

  Inc Inc(OpcodeReference target);

  Inc16 Inc16(OpcodeReference target);

  Ld<T> Ld(OpcodeReference<T> target, ImmutableOpcodeReference<T> source);

  LdAR<T> LdAR(OpcodeReference<T> target, ImmutableOpcodeReference<T> source);

  Ldd Ldd();

  Ldi Ldi();

  LdOperation<T> LdOperation(OpcodeReference target, Instruction<T> instruction);

  Neg Neg(OpcodeReference target);

  Nop Nop();

  Out Out(ImmutableOpcodeReference target, ImmutableOpcodeReference source);

  Outd Outd();

  Pop Pop(OpcodeReference target);

  Push Push(OpcodeReference target);

  Ret Ret(Condition condition);

  RetN RetN(Condition condition);

  RL<T> RL(OpcodeReference target);

  RLA RLA();

  RLC<T> RLC(OpcodeReference target);

  RLCA RLCA();

  RLD RLD();

  RR RR(OpcodeReference target);

  RRA RRA();

  RRC RRC(OpcodeReference target);

  RRCA RRCA();

  RRD RRD();

  RST RST(int p);

  SCF SCF();

  SLA SLA(OpcodeReference<T> target);

  SLL SLL(OpcodeReference target);

  SRA SRA(OpcodeReference target);

  SRL SRL(OpcodeReference target);
}
