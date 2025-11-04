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

import com.fpetrola.z80.bytecode.DefaultZ80InstructionDriver;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.transformations.InstructionFetcherForTest;
import com.fpetrola.z80.transformations.RoutineFinderInstructionSpy;

import static com.fpetrola.z80.registers.Flags.CARRY_FLAG;
import static com.fpetrola.z80.registers.Flags.ZERO_FLAG;

public class CPUExecutionContext extends DefaultZ80InstructionDriver implements Z80ContextDriver {
  OpcodeTargets ot;
  OpcodeConditions opc;
  Register flag;
  protected InstructionSpy spy;

  public CPUExecutionContext(InstructionSpy spy, OOZ80 z80) {
    super(z80);
    this.spy = spy;
  }

  public CPUExecutionContext(InstructionSpy spy, OOZ80 z80, OpcodeConditions opcodeConditions) {
    this(spy, z80);
    spy.reset(getState());
    this.z80.reset();
    this.z80.getInstructionFetcher().reset();
    spy.doContinue();
    ot = new OpcodeTargets(getState(), new MemoryForOpcodes(getState().getMemory(), getState()));
    flag = getState().getFlag();
    opc = opcodeConditions;
  }

  @Override
  public void reset() {
    getInstructionFetcherForTest().reset();
    z80.reset();
  }

  public InstructionFetcherForTest getInstructionFetcherForTest() {
    return (InstructionFetcherForTest) z80.getInstructionFetcher();
  }

  public int add(Instruction instruction) {
    return getInstructionFetcherForTest().add(instruction);
  }

  public Instruction getInstructionAt(int i) {
    return getInstructionFetcherForTest().getInstructionAt(i);
  }

  public Instruction getTransformedInstructionAt(int i) {
    return getInstructionFetcherForTest().getTransformedInstructionAt(i);
  }

  @Override
  public RoutineFinderInstructionSpy getRegisterTransformerInstructionSpy() {
    return (RoutineFinderInstructionSpy) spy;
  }

  @Override
  public Register r(RegisterName registerName) {
    return getState().r(registerName);
  }

  @Override
  public RegisterPair rp(RegisterName registerName) {
    return (RegisterPair) getState().r(registerName);
  }

  @Override
  public Register f() {
    return flag;
  }

  @Override
  public Register pc() {
    return getState().getPc();
  }

  @Override
  public OpcodeReference iRR(Register memoryReader) {
    return ot.iRR(memoryReader);
  }

  @Override
  public OpcodeReference iRRn(Register register, int plus) {
    return new CachedMemoryPlusRegister8BitReference((Integer) new Integer(plus), register, mem(), pc(), 0);
  }

  @Override
  public ImmutableOpcodeReference c(int value) {
    return ot.c(value);
  }

  @Override
  public OpcodeReference iiRR(Register memoryWriter) {
    return ot.iiRR(memoryWriter);
  }

  @Override
  public OpcodeReference iinn(int delta) {
    return ot.iinn(delta);
  }

  @Override
  public Condition nz() {
    return opc.nf(ZERO_FLAG);
  }

  @Override
  public BNotZeroCondition bnz() {
    return opc.bnz();
  }

  @Override
  public Condition z() {
    return opc.f(ZERO_FLAG);
  }

  @Override
  public Condition nc() {
    return opc.nf(CARRY_FLAG);
  }

  @Override
  public Condition c() {
    return opc.f(CARRY_FLAG);
  }

  @Override
  public Condition t() {
    return opc.t();
  }

  @Override
  public ImmutableOpcodeReference nn(int delta) {
    return ot.nn(delta);
  }
}