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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.registers.RegisterName;

import static com.fpetrola.z80.registers.RegisterName.*;

public class State<T extends WordNumber> {
  private RunState runState;

  public enum InterruptionMode {IM0, IM1, IM2}

  private InterruptionMode intMode;

  private RegisterBank<T> registers;
  private Memory<T> memory;
  private IO<T> io;

  private boolean halted;
  private boolean iff1;
  private boolean iff2;
  private boolean intLine;
  private boolean activeNMI;
  private boolean pendingEI;
  private boolean flagQ;
  private boolean pinReset;

  public State(IO io, RegisterBank registerBank, Memory memory) {
    this.registers = registerBank;
    this.io = io;
    this.memory = memory;
  }

  public State(IO io, Memory memory) {
    this(io, new DefaultRegisterBankFactory().createBank(), memory);
  }

  public Register<T> getFlag() {
    return getRegister(F);
  }

  public Register<T> r(RegisterName name) {
    return this.registers.get(name);
  }

  public Register<T> getRegister(RegisterName name) {
    return this.registers.get(name);
  }

  public void setHalted(boolean halted) {
    this.halted = halted;
    runState = halted ? RunState.STATE_STOPPED_NORMAL : RunState.STATE_RUNNING;
  }

  public boolean isHalted() {
    return this.halted;
  }

  public void enableInterrupt() {
    iff1 = iff2 = true;
  }

  public void resetInterrupt() {
    iff1 = iff2 = false;
  }

  public String toString() {
    return "registers=" + registers + ", halted=" + halted + ", iff1=" + isIff1() + ", iff2=" + isIff2();
  }

  public boolean isIff1() {
    return iff1;
  }

  public void setIff1(boolean iff1) {
    this.iff1 = iff1;
  }

  public boolean isIff2() {
    return iff2;
  }

  public void setIff2(boolean iff2) {
    this.iff2 = iff2;
  }

  public InterruptionMode getInterruptionMode() {
    return intMode;
  }

  public void setIntMode(InterruptionMode intMode) {
    this.intMode = intMode;
  }

  public Memory<T> getMemory() {
    return memory;
  }

  public IO<T> getIo() {
    return io;
  }

  public void setINTLine(boolean intLine) {
    this.intLine = intLine;
  }

  public boolean isIntLine() {
    return intLine;
  }

  public boolean isActiveNMI() {
    return activeNMI;
  }

  public void setActiveNMI(boolean activeNMI) {
    this.activeNMI = activeNMI;
  }

  public boolean isPendingEI() {
    return pendingEI;
  }

  public void setPendingEI(boolean pendingEI) {
    this.pendingEI = pendingEI;
  }

  public boolean isFlagQ() {
    return flagQ;
  }

  public void setFlagQ(boolean flagQ) {
    this.flagQ = flagQ;
  }

  public boolean isPinReset() {
    return pinReset;
  }

  public void setPinReset(boolean pinReset) {
    this.pinReset = pinReset;
  }


  public Register<T> getPc() {
    return this.getRegister(PC);
  }

  public Register<T> getMemptr() {
    return this.getRegister(MEMPTR);
  }

  public Register<T> getRegI() {
    return this.getRegister(I);
  }

  public Register<T> getRegisterSP() {
    return this.getRegister(SP);
  }

  public Register<T> getRegisterR() {
    return this.getRegister(R);
  }

  public void setRunState(RunState runState) {
    this.runState = runState;
  }

  public RunState getRunState() {
    return runState;
  }

  public enum RunState {
    STATE_STOPPED_NORMAL("stopped"),
    STATE_STOPPED_BREAK("breakpoint"),
    STATE_STOPPED_ADDR_FALLOUT("stopped (address fallout)"),
    STATE_STOPPED_BAD_INSTR("stopped (instruction fallout)"),
    STATE_RUNNING("running");
    private final String name;

    RunState(String name) {
      this.name = name;
    }

    public String toString() {
      return this.name;
    }
  }
}
