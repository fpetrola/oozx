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
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.google.inject.Inject;

import static com.fpetrola.z80.registers.RegisterName.*;

@SuppressWarnings("ALL")
public class DefaultInstructionFactory implements InstructionFactory {
  private RegisterPair bc;
  private Register de;
  private RegisterPair hl;
  protected Memory memory;
  private Register c;
  private Register _bc;
  private Register _de;
  private Register _hl;
  protected Register sp;
  private Register r;
  private Register i;
  private Register a;
  private Register b;
  protected State state;
  protected Register pc;
  protected Register flag;
  private IO io;
  private Register memptr;

  @Inject
  public DefaultInstructionFactory(State state) {
    setState(state);
  }

  private void setState(State state) {
    this.state = state;
    io = state.getIo();
    pc = state.getPc();
    sp = state.getRegisterSP();
    flag = state.getRegister(F);
    a = state.getRegister(A);
    b = state.getRegister(B);
    c = state.getRegister(B);
    bc = (RegisterPair) state.getRegister(BC);
    de = state.getRegister(DE);
    hl = (RegisterPair) state.getRegister(HL);
    _bc = state.getRegister(BCx);
    _de = state.getRegister(DEx);
    _hl = state.getRegister(HLx);
    r = state.getRegister(R);
    i = state.getRegister(I);
    memptr = state.getMemptr();
    memory = state.getMemory();
  }

  @Override
  public FetchNextOpcodeInstructionFactory getFetchNextOpcodeInstructionFactory() {
    return new FetchNextOpcodeInstructionFactory(state);
  }

  @Override
  public DJNZ DJNZ(BNotZeroCondition bnz, ImmutableOpcodeReference target) {
    return new DJNZ(target, bnz, pc);
  }

  @Override
  public JP JP(ImmutableOpcodeReference target, Condition condition) {
    return new JP(target, condition, pc);
  }

  @Override
  public Call Call(Condition condition, ImmutableOpcodeReference positionOpcodeReference) {
    return new Call(positionOpcodeReference, condition, pc, sp, state.getMemory());
  }

  @Override
  public JR JR(Condition condition, ImmutableOpcodeReference target) {
    return new JR(target, condition, pc);
  }

  @Override
  public Adc Adc(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Adc(target, source, flag);
  }

  @Override
  public Cpd Cpd() {
    return new Cpd(a, flag, bc, hl, memory, io);
  }

  @Override
  public CCF CCF() {
    return new CCF(flag, a);
  }

  @Override
  public Cpi Cpi() {
    return new Cpi(a, flag, bc, hl, memory, io);
  }

  @Override
  public Adc16 Adc16(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Adc16(target, source, flag);
  }

  @Override
  public Add Add(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Add(target, source, flag);
  }

  @Override
  public Add16 Add16(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Add16(target, source, flag);
  }

  @Override
  public And And(ImmutableOpcodeReference source) {
    return new And(a, source, flag);
  }

  @Override
  public Or Or(ImmutableOpcodeReference source) {
    return new Or(a, source, flag);
  }

  @Override
  public Sbc Sbc(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Sbc(target, source, flag);
  }

  @Override
  public Sbc16 Sbc16(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Sbc16(target, source, flag);
  }

  @Override
  public Sub Sub(ImmutableOpcodeReference source) {
    return new Sub(a, source, flag);
  }

  @Override
  public Cp Cp(ImmutableOpcodeReference source) {
    return new Cp(a, source, flag);
  }

  @Override
  public Xor Xor(ImmutableOpcodeReference source) {
    return new Xor(a, source, flag);
  }

  @Override
  public BIT BIT(OpcodeReference target, int n) {
    return new BIT(target, n, flag, memptr);
  }

  @Override
  public RES RES(OpcodeReference target, int n) {
    return new RES(target, n, flag);
  }

  @Override
  public SET SET(OpcodeReference target, int n) {
    return new SET(target, n, flag);
  }

  @Override
  public Cpir Cpir() {
    return new Cpir(flag, bc, pc, Cpi());
  }

  @Override
  public Cpdr Cpdr() {
    return new Cpdr(pc, bc, flag, Cpd());
  }

  @Override
  public Indr Indr() {
    return new Indr(pc, bc, Ind());
  }

  @Override
  public Inir Inir() {
    return new Inir(pc, bc, Ini());
  }

  @Override
  public Lddr Lddr() {
    return new Lddr(pc, bc, Ldd());
  }

  @Override
  public Outdr Outdr() {
    return new Outdr(pc, bc, Outd());
  }

  @Override
  public Ldir Ldir() {
    return new Ldir(pc, bc, Ldi());
  }

  @Override
  public Outir Outir() {
    return new Outir(pc, bc, Outi());
  }

  @Override
  public Ind Ind() {
    return new Ind(bc, hl, flag, memory, io);
  }

  @Override
  public Ini Ini() {
    return new Ini(bc, hl, flag, memory, io);
  }

  @Override
  public Outi Outi() {
    return new Outi(bc, hl, flag, memory, io);
  }

  @Override
  public CPL CPL() {
    return new CPL(a, flag);
  }

  @Override
  public DAA DAA() {
    return new DAA(a, flag);
  }

  @Override
  public Dec Dec(OpcodeReference target) {
    return new Dec(target, flag);
  }

  @Override
  public Dec16 Dec16(OpcodeReference target) {
    return new Dec16(target);
  }

  @Override
  public DI DI() {
    return new DI(state);
  }

  @Override
  public EI EI() {
    return new EI(state);
  }

  @Override
  public Ex Ex(OpcodeReference target, OpcodeReference source) {
    return new Ex(target, source, flag);
  }

  @Override
  public Exx Exx() {
    return new Exx(bc, de, hl, _bc, _de, _hl);
  }

  @Override
  public Halt Halt() {
    return new Halt(state);
  }

  @Override
  public IM IM(int mode) {
    return new IM(state, mode);
  }

  @Override
  public In In(OpcodeReference target, ImmutableOpcodeReference source) {
    return new In(target, source, a, bc, flag, io);
  }

  @Override
  public Inc Inc(OpcodeReference target) {
    return new Inc(target, flag);
  }

  @Override
  public Inc16 Inc16(OpcodeReference target) {
    return new Inc16(target);
  }

  @Override
  public Ld Ld(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Ld(target, source, flag);
  }

  @Override
  public LdAR LdAR(OpcodeReference target, ImmutableOpcodeReference source) {
    return new LdAR(target, source, flag, state);
  }

  @Override
  public LdAI LdAI() {
    return new LdAI(a, i, flag, state);
  }

  @Override
  public Ldd Ldd() {
    return new Ldd(de, bc, hl, flag, memory, io, a);
  }

  @Override
  public Ldi Ldi() {
    return new Ldi(de, bc, hl, flag, memory, io, a);
  }

  @Override
  public LdOperation LdOperation(OpcodeReference target, Instruction instruction) {
    return new LdOperation(target, instruction);
  }

  @Override
  public Neg Neg(OpcodeReference target) {
    return new Neg(target, flag);
  }

  @Override
  public Nop Nop() {
    return new Nop();
  }

  @Override
  public Out Out(ImmutableOpcodeReference target, ImmutableOpcodeReference source) {
    return new Out(source, new Out.OutPortOpcodeReference(io, target, a), flag);
  }

  @Override
  public Outd Outd() {
    return new Outd(bc, hl, flag, memory, io);
  }

  @Override
  public Pop Pop(OpcodeReference target) {
    return new Pop(target, sp, memory, flag);
  }

  @Override
  public Push Push(OpcodeReference target) {
    return new Push(target, sp, memory);
  }

  @Override
  public Ret Ret(Condition condition) {
    return new Ret(condition, sp, memory, pc);
  }

  @Override
  public RetN RetN(Condition condition) {
    return new RetN(condition, sp, memory, state, pc);
  }

  @Override
  public RL RL(OpcodeReference target) {
    return new RL(target, flag);
  }

  @Override
  public RLA RLA() {
    return new RLA(a, flag);
  }

  @Override
  public RLC RLC(OpcodeReference target) {
    return new RLC(target, flag);
  }

  @Override
  public RLCA RLCA() {
    return new RLCA(a, flag);
  }

  @Override
  public RLD RLD() {
    return new RLD(a, hl, flag, r, memory);
  }

  @Override
  public RR RR(OpcodeReference target) {
    return new RR(target, flag);
  }

  @Override
  public RRA RRA() {
    return new RRA(a, flag);
  }

  @Override
  public RRC RRC(OpcodeReference target) {
    return new RRC(target, flag);
  }

  @Override
  public RRCA RRCA() {
    return new RRCA(a, flag);
  }

  @Override
  public RRD RRD() {
    return new RRD(a, hl, r, flag, memory);
  }

  @Override
  public RST RST(int p) {
    return new RST((p & 0xFFFF), pc, sp, memory);
  }

  @Override
  public SCF SCF() {
    return new SCF(flag, a);
  }

  @Override
  public SLA SLA(OpcodeReference target) {
    return new SLA(target, flag);
  }

  @Override
  public SLL SLL(OpcodeReference target) {
    return new SLL(target, flag);
  }

  @Override
  public SRA SRA(OpcodeReference target) {
    return new SRA(target, flag);
  }

  @Override
  public SRL SRL(OpcodeReference target) {
    return new SRL(target, flag);
  }
}
