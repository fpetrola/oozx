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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Composed16BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.transformations.VirtualRegister;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;

import static com.fpetrola.z80.registers.RegisterName.*;

public abstract class DefaultRegistersSetter<T extends WordNumber> implements RegistersSetter<T> {
  protected State<T> state;

  public DefaultRegistersSetter(State<T> state) {
    this.state = state;
  }

  @Override
  public final void setRegPC(int address) {
    getState().getRegister(RegisterName.PC).write(mask16(address));
  }

  @Override
  public void setFlags(int regF) {
    getFlag().write(mask8(regF));
  }

  @Override
  public final void setRegDE(int word) {
    getVirtualRegister(RegisterName.DE).write(mask16(word));
  }

  @Override
  public final void setRegA(int value) {
    getVirtualRegister(RegisterName.A).write(mask8(value));
  }

  @Override
  public final void setRegB(int value) {
    getVirtualRegister(RegisterName.B).write(mask8(value));
  }

  @Override
  public final void setRegC(int value) {
    getVirtualRegister(RegisterName.C).write(mask8(value));
  }

  @Override
  public final void setRegD(int value) {
    getVirtualRegister(RegisterName.D).write(mask8(value));
  }

  @Override
  public final void setRegE(int value) {
    getVirtualRegister(RegisterName.E).write(mask8(value));
  }

  @Override
  public final void setRegH(int value) {
    getVirtualRegister(RegisterName.H).write(mask8(value));
  }

  @Override
  public final void setRegL(int value) {
    getVirtualRegister(RegisterName.L).write(mask8(value));
  }

  @Override
  public final void setRegAx(int value) {
    getVirtualRegister(RegisterName.Ax).write(mask8(value));
  }

  @Override
  public final void setRegFx(int value) {
    getVirtualRegister(RegisterName.Fx).write(mask8(value));
  }

  @Override
  public final void setRegBx(int value) {
    getVirtualRegister(RegisterName.Bx).write(mask8(value));
  }

  @Override
  public final void setRegCx(int value) {
    getVirtualRegister(RegisterName.Cx).write(mask8(value));
  }

  @Override
  public final void setRegDx(int value) {
    getVirtualRegister(RegisterName.Dx).write(mask8(value));
  }

  @Override
  public final void setRegEx(int value) {
    getVirtualRegister(RegisterName.Ex).write(mask8(value));
  }

  @Override
  public final void setRegHx(int value) {
    getVirtualRegister(RegisterName.Hx).write(mask8(value));
  }

  @Override
  public final void setRegLx(int value) {
    getVirtualRegister(RegisterName.Lx).write(mask8(value));
  }

  @Override
  public final void setRegAF(int word) {
    getVirtualRegister(RegisterName.AF).write(mask16(word));
  }

  @Override
  public final void setRegBC(int word) {
    getVirtualRegister(RegisterName.BC).write(mask8(word));
  }

  @Override
  public final void setRegHLx(int word) {
    getVirtualRegister(RegisterName.HLx).write(mask16(word));
  }

  @Override
  public final void setRegSP(int word) {
    getState().getRegister(RegisterName.SP).write(mask16(word));
  }

  @Override
  public final void setRegIX(int word) {
    getVirtualRegister(RegisterName.IX).write(mask16(word));
  }

  @Override
  public final void setRegIY(int word) {
    getVirtualRegister(RegisterName.IY).write(mask16(word));
  }

  @Override
  public final void setRegI(int value) {
    getState().getRegister(RegisterName.I).write(mask8(value));
  }

  @Override
  public final void setRegR(int value) {
    getState().getRegister(RegisterName.R).write(mask8(value));
  }

  @Override
  public final void setMemPtr(int word) {
    getState().getRegister(RegisterName.MEMPTR).write(mask16(word));
  }

  @Override
  public final void setCarryFlag(boolean carryState) {
    Register<T> f = getFlag();
    if (carryState)
      f.write(f.read().or(0x01));
    else
      f.write(f.read().and(0xFE));
  }

  @Override
  public final void setIFF1(boolean state) {
    setFfIFF1(state);
  }

  @Override
  public final void setIFF2(boolean state) {
    setFfIFF2(state);
  }

  @Override
  public final void setNMI(boolean nmi) {
    setActiveNMI(nmi);
  }

  @Override
  public void setINTLine(boolean intLine) {
    setActiveINT(intLine);
  }

  @Override
  public final void setIM(int mode) {
    setModeINT(mode);
  }

  @Override
  public void setHalted(boolean state) {
    this.state.setHalted(state);
  }

  @Override
  public final void setPendingEI(boolean state) {
    this.state.setPendingEI(state);
  }

  @Override
  public void setFlagQ(boolean flagQ) {
    this.state.setFlagQ(flagQ);
    ;
  }

  @Override
  public void setLastFlagQ(boolean lastFlagQ) {
    this.state.setFlagQ(lastFlagQ);
  }

  @Override
  public void setMemptr(int memptr) {
    state.getRegister(RegisterName.MEMPTR).write(mask16(memptr));
  }

  @Override
  public void setDE(int DE) {
    getVirtualRegister(RegisterName.DE).write(mask16(DE));
  }

  @Override
  public void setActiveINT(boolean activeINT) {
    this.state.setINTLine(activeINT);
  }

  @Override
  public void setActiveNMI(boolean activeNMI) {
    this.state.setActiveNMI(activeNMI);
  }

  public void setModeINT(int modeINT) {
    state.setIntMode(State.InterruptionMode.values()[modeINT]);
  }

  @Override
  public void setFfIFF1(boolean ffIFF1) {
    this.state.setIff1(ffIFF1);
    ;
  }

  @Override
  public void setFfIFF2(boolean ffIFF2) {
    this.state.setIff2(ffIFF2);
    ;
  }

  @Override
  public void setPinReset(boolean pinReset) {
    this.state.setPinReset(pinReset);
  }

  protected Register<T> getVirtualRegister(RegisterName registerName) {
    Register<T> register = getState().getRegister(registerName);
    Register<T> result;

    if (registerName != IY && registerName != IX && register instanceof RegisterPair<T>) {
      RegisterPair<WordNumber> wordNumberRegisterPair = (RegisterPair<WordNumber>) register;
      Register<T> high = (Register<T>) wordNumberRegisterPair.getHigh();
      Register<T> h = (Register) getVirtualRegisterFactory().lastVirtualRegisters.get(high);
      h = h != null ? h : high;
      Register<WordNumber> low = wordNumberRegisterPair.getLow();
      Register<WordNumber> l = (VirtualRegister) getVirtualRegisterFactory().lastVirtualRegisters.get(low);
      l = l != null ? l : low;
      result = new Composed16BitRegister<>(registerName, h, l);
    } else {
      VirtualRegister<T> l = (VirtualRegister) getVirtualRegisterFactory().lastVirtualRegisters.get(register);
      if (l != null) {
        result = l;
      } else {
        result = register;
      }
    }
    return result;
  }

  public State<T> getState() {
    return state;
  }

  public abstract VirtualRegisterFactory getVirtualRegisterFactory();

  private T mask8(int value) {
    return WordNumber.createValue(value & 0xff);
  }

  protected T mask16(int word) {
    return WordNumber.createValue(word & 0xffff);
  }

  protected Register<T> getFlag() {
    return getVirtualRegister(F);
    //return getState().getFlag();
  }
}
