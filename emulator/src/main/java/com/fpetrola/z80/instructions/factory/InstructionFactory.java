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

package com.fpetrola.z80.instructions.factory;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.*;

public interface InstructionFactory {
  FetchNextOpcodeInstructionFactory getFetchNextOpcodeInstructionFactory();

  DJNZ DJNZ(BNotZeroCondition bnz, ImmutableOpcodeReference target);

  JP JP(ImmutableOpcodeReference target, Condition condition);

  Call Call(Condition condition, ImmutableOpcodeReference positionOpcodeReference);

  JR JR(Condition condition, ImmutableOpcodeReference target);

  Adc Adc(OpcodeReference target, ImmutableOpcodeReference source);

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

  Cpir Cpir();

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

  Ld Ld(OpcodeReference target, ImmutableOpcodeReference source);

  LdAR LdAR(OpcodeReference target, ImmutableOpcodeReference source);

  LdAI LdAI();

  Ldd Ldd();

  Ldi Ldi();

  LdOperation LdOperation(OpcodeReference target, Instruction instruction);

  Neg Neg(OpcodeReference target);

  Nop Nop();

  Out Out(ImmutableOpcodeReference target, ImmutableOpcodeReference source);

  Outd Outd();

  Pop Pop(OpcodeReference target);

  Push Push(OpcodeReference target);

  Ret Ret(Condition condition);

  RetN RetN(Condition condition);

  RL RL(OpcodeReference target);

  RLA RLA();

  RLC RLC(OpcodeReference target);

  RLCA RLCA();

  RLD RLD();

  RR RR(OpcodeReference target);

  RRA RRA();

  RRC RRC(OpcodeReference target);

  RRCA RRCA();

  RRD RRD();

  RST RST(int p);

  SCF SCF();

  SLA SLA(OpcodeReference target);

  SLL SLL(OpcodeReference target);

  SRA SRA(OpcodeReference target);

  SRL SRL(OpcodeReference target);
}
