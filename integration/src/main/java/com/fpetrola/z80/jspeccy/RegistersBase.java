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

import com.fpetrola.z80.bytecode.DefaultRegistersSetter;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import snapshots.Z80State;

import z80core.IntMode;

public class RegistersBase<T extends WordNumber> extends DefaultRegistersSetter<T> {

  public RegistersBase() {
    super(null);
  }

  public RegistersBase(State<T> state) {
    super(state);
  }

  public void xor(int oper8) {

  }

  public void cp(int oper8) {
  }

  public final int getRegPC() {
    return getState().getRegister(RegisterName.PC).read().intValue();
  }

  public final int getRegA() {
    return getRegister(RegisterName.A).read().intValue();
  }

  public final int getRegB() {
    return getRegister(RegisterName.B).read().intValue();
  }

  public final int getRegC() {
    return getRegister(RegisterName.C).read().intValue();
  }

  public final int getRegD() {
    return getRegister(RegisterName.D).read().intValue();
  }

  public final int getRegE() {
    return getRegister(RegisterName.E).read().intValue();
  }

  public final int getRegH() {
    return getRegister(RegisterName.H).read().intValue();
  }

  public final int getRegL() {
    return getRegister(RegisterName.L).read().intValue();
  }

  public final int getRegAx() {
    return getRegister(RegisterName.Ax).read().intValue();
  }

  public final int getRegFx() {
    return getRegister(RegisterName.Fx).read().intValue();
  }

  public final int getRegBx() {
    return getRegister(RegisterName.Bx).read().intValue();
  }

  public final int getRegCx() {
    return getRegister(RegisterName.Cx).read().intValue();
  }

  public final int getRegDx() {
    return getRegister(RegisterName.Dx).read().intValue();
  }

  public final int getRegEx() {
    return getRegister(RegisterName.Ex).read().intValue();
  }

  public final int getRegHx() {
    return getRegister(RegisterName.Hx).read().intValue();
  }

  public final int getRegLx() {
    return getRegister(RegisterName.Lx).read().intValue();
  }

  public final int getRegAF() {
    return getRegister(RegisterName.AF).read().intValue();
  }

  public final int getRegAFx() {
    return getRegister(RegisterName.AFx).read().intValue();
  }

  public final void setRegAFx(int word) {
    getRegister(RegisterName.AFx).write(mask16(word));
  }

  public final int getRegBC() {
    return getRegister(RegisterName.BC).read().intValue();
  }

  public final int getFlags() {
    return getFlag().read().intValue();
  }

  public final int getRegHLx() {
    return getRegister(RegisterName.HLx).read().intValue();
  }

  public final int getRegSP() {
    return getState().getRegister(RegisterName.SP).read().intValue();
  }

  public final int getRegIX() {
    return getRegister(RegisterName.IX).read().intValue();
  }

  public final int getRegIY() {
    return getRegister(RegisterName.IY).read().intValue();
  }

  public final int getRegI() {
    return getState().getRegister(RegisterName.I).read().intValue();
  }

  public final int getRegR() {
    return getState().getRegister(RegisterName.R).read().intValue();
  }

  public final int getPairIR() {
    return getState().getRegister(RegisterName.IR).read().intValue();
  }

  public final int getMemPtr() {
    return getState().getRegister(RegisterName.MEMPTR).read().intValue();
  }

  public final boolean isCarryFlag() {
    return (getFlags() & 0x01) != 0;
  }

  public final int getRegDE() {
    return getRegister(RegisterName.DE).read().intValue();
  }

  public final void setZ80State(Z80State state) {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    setRegA(state.getRegA());
    setFlags(state.getRegF());
    setRegB(state.getRegB());
    setRegC(state.getRegC());
    setRegD(state.getRegD());
    setRegE(state.getRegE());
    setRegH(state.getRegH());
    setRegL(state.getRegL());
    setRegAx(state.getRegAx());
    setRegFx(state.getRegFx());
    setRegBx(state.getRegBx());
    setRegCx(state.getRegCx());
    setRegDx(state.getRegDx());
    setRegEx(state.getRegEx());
    setRegHx(state.getRegHx());
    setRegLx(state.getRegLx());
    setRegIX(state.getRegIX());
    setRegIY(state.getRegIY());
    setRegSP(state.getRegSP());
    setRegPC(state.getRegPC());
    setRegI(state.getRegI());
    setRegR(state.getRegR());
    setMemptr(state.getMemPtr());
    setHalted(state.isHalted());
    setFfIFF1(state.isIFF1());
    setFfIFF2(state.isIFF2());
    setModeINT(state.getIM().ordinal());
    setActiveINT(state.isINTLine());
    setPendingEI(state.isPendingEI());
    setActiveNMI(state.isNMI());
    setFlagQ(false);
    setLastFlagQ(state.isFlagQ());

//    getState().updateFromEmulator();
  }

  public final boolean isIFF1() {
    return isFfIFF1();
  }

  public final boolean isIFF2() {
    return isFfIFF2();
  }

  public final boolean isNMI() {
    return isActiveNMI();
  }

  // La línea de NMI se activa por impulso, no por nivel
  public final void triggerNMI() {
    setActiveNMI(true);
  }

  // La línea INT se activa por nivel
  public final boolean isINTLine() {
    return isActiveINT();
  }

  // Acceso al modo de interrupción
  public final IntMode getIM() {
    return getModeINT();
  }

  public final boolean isHalted() {
    return state.isHalted();
  }

  public void setPinReset() {
    setPinReset(true);
  }

  public final boolean isPendingEI() {
    return state.isPendingEI();
  }

  public final Z80State getZ80State() {
    Z80State state = new Z80State();
    state.setRegA(getRegA());
    state.setRegF(getFlags());
    state.setRegB(getRegB());
    state.setRegC(getRegC());
    state.setRegD(getRegD());
    state.setRegE(getRegE());
    state.setRegH(getRegH());
    state.setRegL(getRegL());
    state.setRegAx(getRegAx());
    state.setRegFx(getRegFx());
    state.setRegBx(getRegBx());
    state.setRegCx(getRegCx());
    state.setRegDx(getRegDx());
    state.setRegEx(getRegEx());
    state.setRegHx(getRegHx());
    state.setRegLx(getRegLx());
    state.setRegIX(getRegIX());
    state.setRegIY(getRegIY());
    state.setRegSP(getRegSP());
    state.setRegPC(getRegPC());
    state.setRegI(getRegI());
    state.setRegR(getRegR());
    state.setMemPtr(getMemptr());
    state.setHalted(isHalted());
    state.setIFF1(isFfIFF1());
    state.setIFF2(isFfIFF2());
    state.setIM(getModeINT());
    state.setINTLine(isActiveINT());
    state.setPendingEI(isPendingEI());
    state.setNMI(isActiveNMI());
    state.setFlagQ(isLastFlagQ());
    return state;
  }

  public boolean isFlagQ() {
    return state.isFlagQ();
  }

  public boolean isLastFlagQ() {
    return state.isFlagQ();
  }

  public int getMemptr() {
    return state.getRegister(RegisterName.MEMPTR).read().intValue();
  }

  public int getDE() {
    return getRegister(RegisterName.DE).read().intValue();
  }

  public boolean isActiveINT() {
    return state.isIntLine();
  }

  public boolean isActiveNMI() {
    return state.isActiveNMI();
  }

  public IntMode getModeINT() {
    return IntMode.values()[state.getInterruptionMode().ordinal()];
  }

  public boolean isFfIFF1() {
    return state.isIff1();
  }

  public boolean isFfIFF2() {
    return state.isIff2();
  }

  public boolean isPinReset() {
    return state.isPinReset();
  }

  public void setState(State state) {
    this.state = state;
  }

}