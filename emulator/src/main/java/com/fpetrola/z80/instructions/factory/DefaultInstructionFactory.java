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
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.google.inject.Inject;

import static com.fpetrola.z80.registers.RegisterName.*;

@SuppressWarnings("ALL")
public class DefaultInstructionFactory<T extends WordNumber> implements InstructionFactory<T> {
  private RegisterPair<T> bc;
  private Register<T> de;
  private RegisterPair<T> hl;
  protected Memory<T> memory;
  private Register<T> c;
  private Register<T> _bc;
  private Register<T> _de;
  private Register<T> _hl;
  protected Register<T> sp;
  private Register<T> r;
  private Register<T> i;
  private Register<T> a;
  private Register<T> b;
  protected State<T> state;
  protected Register<T> pc;
  protected Register<T> flag;
  private IO<T> io;
  private Register<T> memptr;

  @Inject
  public DefaultInstructionFactory(State state) {
    setState(state);
  }

  private void setState(State<T> state) {
    this.state = state;
    io = state.getIo();
    pc = state.getPc();
    sp = state.getRegisterSP();
    flag = state.getRegister(F);
    a = state.getRegister(A);
    b = state.getRegister(B);
    c = state.getRegister(B);
    bc = (RegisterPair<T>) state.getRegister(BC);
    de = state.getRegister(DE);
    hl = (RegisterPair<T>) state.getRegister(HL);
    _bc = state.getRegister(BCx);
    _de = state.getRegister(DEx);
    _hl = state.getRegister(HLx);
    r = state.getRegister(R);
    i = state.getRegister(I);
    memptr = state.getMemptr();
    memory = state.getMemory();
  }

  @Override
  public DJNZ<T> DJNZ(BNotZeroCondition bnz, ImmutableOpcodeReference<T> target) {
    return new DJNZ<T>(target, bnz, pc);
  }

  @Override
  public JP JP(ImmutableOpcodeReference target, Condition condition) {
    return new JP(target, condition, pc);
  }

  @Override
  public Call Call(Condition condition, ImmutableOpcodeReference positionOpcodeReference) {
    return new Call<T>(positionOpcodeReference, condition, pc, sp, state.getMemory());
  }

  @Override
  public JR JR(Condition condition, ImmutableOpcodeReference target) {
    return new JR<T>(target, condition, pc);
  }

  @Override
  public Adc<T> Adc(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return new Adc<T>(target, source, flag);
  }

  @Override
  public Cpd Cpd() {
    return new Cpd<T>(a, flag, bc, hl, memory, io);
  }

  @Override
  public CCF CCF() {
    return new CCF<T>(flag, a);
  }

  @Override
  public Cpi Cpi() {
    return new Cpi<T>(a, flag, bc, hl, memory, io);
  }

  @Override
  public Adc16 Adc16(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Adc16<T>(target, source, flag);
  }

  @Override
  public Add Add(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Add<T>(target, source, flag);
  }

  @Override
  public Add16 Add16(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Add16<T>(target, source, flag);
  }

  @Override
  public And And(ImmutableOpcodeReference source) {
    return new And<T>(a, source, flag);
  }

  @Override
  public Or Or(ImmutableOpcodeReference source) {
    return new Or<T>(a, source, flag);
  }

  @Override
  public Sbc Sbc(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Sbc<T>(target, source, flag);
  }

  @Override
  public Sbc16 Sbc16(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Sbc16<T>(target, source, flag);
  }

  @Override
  public Sub Sub(ImmutableOpcodeReference source) {
    return new Sub<T>(a, source, flag);
  }

  @Override
  public Cp Cp(ImmutableOpcodeReference source) {
    return new Cp<T>(a, source, flag);
  }

  @Override
  public Xor Xor(ImmutableOpcodeReference source) {
    return new Xor<T>(a, source, flag);
  }

  @Override
  public BIT BIT(OpcodeReference target, int n) {
    return new BIT<T>(target, n, flag, memptr);
  }

  @Override
  public RES RES(OpcodeReference target, int n) {
    return new RES<T>(target, n, flag);
  }

  @Override
  public SET SET(OpcodeReference target, int n) {
    return new SET<T>(target, n, flag);
  }

  @Override
  public Cpir<T> Cpir() {
    return new Cpir<T>(flag, bc, pc, Cpi());
  }

  @Override
  public Cpdr Cpdr() {
    return new Cpdr<T>(pc, bc, flag, Cpd());
  }

  @Override
  public Indr Indr() {
    return new Indr<T>(pc, bc, Ind());
  }

  @Override
  public Inir Inir() {
    return new Inir<T>(pc, bc, Ini());
  }

  @Override
  public Lddr Lddr() {
    return new Lddr<T>(pc, bc, Ldd());
  }

  @Override
  public Outdr Outdr() {
    return new Outdr<T>(pc, bc, Outd());
  }

  @Override
  public Ldir Ldir() {
    return new Ldir<T>(pc, bc, Ldi());
  }

  @Override
  public Outir Outir() {
    return new Outir<T>(pc, bc, Outi());
  }

  @Override
  public Ind Ind() {
    return new Ind<T>(bc, hl, flag, memory, io);
  }

  @Override
  public Ini Ini() {
    return new Ini<T>(bc, hl, flag, memory, io);
  }

  @Override
  public Outi Outi() {
    return new Outi<T>(bc, hl, flag, memory, io);
  }

  @Override
  public CPL CPL() {
    return new CPL<T>(a, flag);
  }

  @Override
  public DAA DAA() {
    return new DAA<T>(a, flag);
  }

  @Override
  public Dec Dec(OpcodeReference target) {
    return new Dec<T>(target, flag);
  }

  @Override
  public Dec16 Dec16(OpcodeReference target) {
    return new Dec16<T>(target);
  }

  @Override
  public DI DI() {
    return new DI<T>(state);
  }

  @Override
  public EI EI() {
    return new EI<T>(state);
  }

  @Override
  public Ex Ex(OpcodeReference target, OpcodeReference source) {
    return new Ex<T>(target, source);
  }

  @Override
  public Exx Exx() {
    return new Exx<T>(bc, de, hl, _bc, _de, _hl);
  }

  @Override
  public Halt Halt() {
    return new Halt<T>(state);
  }

  @Override
  public IM IM(int mode) {
    return new IM<T>(state, mode);
  }

  @Override
  public In In(OpcodeReference target, ImmutableOpcodeReference source) {
    return new In<T>(target, source, a, bc, flag, io);
  }

  @Override
  public Inc Inc(OpcodeReference target) {
    return new Inc<T>(target, flag);
  }

  @Override
  public Inc16 Inc16(OpcodeReference target) {
    return new Inc16<T>(target);
  }

  @Override
  public Ld<T> Ld(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return new Ld<T>(target, source, flag);
  }

  @Override
  public LdAR<T> LdAR(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return new LdAR<T>(target, source, flag, state);
  }

  @Override
  public LdAI<T> LdAI() {
    return new LdAI<T>(a, i, flag, state);
  }

  @Override
  public Ldd Ldd() {
    return new Ldd<T>(de, bc, hl, flag, memory, io, a);
  }

  @Override
  public Ldi Ldi() {
    return new Ldi<T>(de, bc, hl, flag, memory, io, a);
  }

  @Override
  public LdOperation<T> LdOperation(OpcodeReference target, Instruction<T> instruction) {
    return new LdOperation<T>(target, instruction);
  }

  @Override
  public Neg Neg(OpcodeReference target) {
    return new Neg<T>(target, flag);
  }

  @Override
  public Nop Nop() {
    return new Nop<T>();
  }

  @Override
  public Out Out(ImmutableOpcodeReference target, ImmutableOpcodeReference source) {
    return new Out<T>(source, new Out.OutPortOpcodeReference(io, target, a), flag);
  }

  @Override
  public Outd Outd() {
    return new Outd<T>(bc, hl, flag, memory, io);
  }

  @Override
  public Pop Pop(OpcodeReference target) {
    return new Pop<T>(target, sp, memory, flag);
  }

  @Override
  public Push Push(OpcodeReference target) {
    return new Push<T>(target, sp, memory);
  }

  @Override
  public Ret Ret(Condition condition) {
    return new Ret<T>(condition, sp, memory, pc);
  }

  @Override
  public RetN RetN(Condition condition) {
    return new RetN(condition, sp, memory, state, pc);
  }

  @Override
  public RL<T> RL(OpcodeReference target) {
    return new RL<T>(target, flag);
  }

  @Override
  public RLA RLA() {
    return new RLA<T>(a, flag);
  }

  @Override
  public RLC<T> RLC(OpcodeReference target) {
    return new RLC<T>(target, flag);
  }

  @Override
  public RLCA RLCA() {
    return new RLCA<T>(a, flag);
  }

  @Override
  public RLD RLD() {
    return new RLD<T>(a, hl, flag, r, memory);
  }

  @Override
  public RR RR(OpcodeReference target) {
    return new RR<T>(target, flag);
  }

  @Override
  public RRA RRA() {
    return new RRA<T>(a, flag);
  }

  @Override
  public RRC RRC(OpcodeReference target) {
    return new RRC<T>(target, flag);
  }

  @Override
  public RRCA RRCA() {
    return new RRCA<T>(a, flag);
  }

  @Override
  public RRD RRD() {
    return new RRD<T>(a, hl, r, flag, memory);
  }

  @Override
  public RST RST(int p) {
    return new RST<T>(p, pc, sp, memory);
  }

  @Override
  public SCF SCF() {
    return new SCF<T>(flag, a);
  }

  @Override
  public SLA SLA(OpcodeReference<T> target) {
    return new SLA<T>(target, flag);
  }

  @Override
  public SLL SLL(OpcodeReference target) {
    return new SLL<T>(target, flag);
  }

  @Override
  public SRA SRA(OpcodeReference target) {
    return new SRA<T>(target, flag);
  }

  @Override
  public SRL SRL(OpcodeReference target) {
    return new SRL<T>(target, flag);
  }
}
