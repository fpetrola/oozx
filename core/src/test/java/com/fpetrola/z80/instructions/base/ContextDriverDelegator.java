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

package com.fpetrola.z80.instructions.base;

import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.transformations.RegisterTransformerInstructionSpy;

import java.util.function.Supplier;

public class ContextDriverDelegator<T extends WordNumber> implements Z80ContextDriver<T> {
  protected Z80ContextDriver<T> currentContext;

  public ContextDriverDelegator(Z80ContextDriver<T> currentContext) {
    this.currentContext = currentContext;
  }

  public int add(Instruction<T> instruction) {
    return currentContext.add(instruction);
  }

  @Override
  public State<T> getState() {
    return currentContext.getState();
  }


  public void step() {
    currentContext.step();
  }


  public Register<T> r(RegisterName registerName) {
    return currentContext.r(registerName);
  }

  public RegisterPair<T> rp(RegisterName registerName) {
    return currentContext.rp(registerName);
  }

  public MockedMemory<T> mem() {
    return currentContext.mem();
  }

  @Override
  public MockedMemory<T> initMem(Supplier<T[]> supplier) {
    return currentContext.initMem(supplier);
  }


  public Register<T> f() {
    return currentContext.f();
  }

  @Override
  public Register<T> pc() {
    return currentContext.pc();
  }


  public OpcodeReference iRR(Register<T> memoryReader) {
    return currentContext.iRR(memoryReader);
  }

  @Override
  public OpcodeReference iRRn(Register<T> register, int plus) {
    return currentContext.iRRn(register, plus);
  }

  public ImmutableOpcodeReference<T> c(int value) {
    return currentContext.c(value);
  }

  public Condition nz() {
    return currentContext.nz();
  }

  public BNotZeroCondition bnz() {
    return currentContext.bnz();
  }

  public Condition z() {
    return currentContext.z();
  }

  @Override
  public Condition nc() {
    return currentContext.nc();
  }

  @Override
  public Condition c() {
    return currentContext.c();
  }

  public Condition t() {
    return currentContext.t();
  }


  public OpcodeReference iiRR(Register<T> memoryWriter) {
    return currentContext.iiRR(memoryWriter);
  }

  @Override
  public OpcodeReference iinn(int delta) {
    return currentContext.iinn(delta);
  }

  @Override
  public ImmutableOpcodeReference nn(int delta) {
    return currentContext.nn(delta);
  }


  public Instruction getInstructionAt(int i) {
    return currentContext.getInstructionAt(i);
  }

  @Override
  public Instruction getTransformedInstructionAt(int i) {
    return currentContext.getTransformedInstructionAt(i);
  }

  @Override
  public RegisterTransformerInstructionSpy getRegisterTransformerInstructionSpy() {
    return currentContext.getRegisterTransformerInstructionSpy();
  }
}
