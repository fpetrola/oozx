/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
import com.fpetrola.z80.cpu.RegistersGetter;
import com.fpetrola.z80.registers.RegisterName;
import snapshots.Z80State;

import z80core.IntMode;

public class RegistersBase extends DefaultRegistersSetter implements RegistersGetter {

  public RegistersBase() {
    super(null);
  }

  public RegistersBase(State state) {
    super(state);
  }

  public void xor(int oper8) {

  }

  public void cp(int oper8) {
  }

  public final int getRegPC() {
    return getState().getRegister(RegisterName.PC).read();
  }

  public final int getRegA() {
    return getRegister(RegisterName.A).read();
  }

  public final int getRegB() {
    return getRegister(RegisterName.B).read();
  }

  public final int getRegC() {
    return getRegister(RegisterName.C).read();
  }

  public final int getRegD() {
    return getRegister(RegisterName.D).read();
  }

  public final int getRegE() {
    return getRegister(RegisterName.E).read();
  }

  public final int getRegH() {
    return getRegister(RegisterName.H).read();
  }

  public final int getRegL() {
    return getRegister(RegisterName.L).read();
  }

  @Override
  public int getRegF() {
    return getRegister(RegisterName.F).read();
  }

  public final int getRegAx() {
    return getRegister(RegisterName.Ax).read();
  }

  public final int getRegFx() {
    return getRegister(RegisterName.Fx).read();
  }

  public final int getRegBx() {
    return getRegister(RegisterName.Bx).read();
  }

  public final int getRegCx() {
    return getRegister(RegisterName.Cx).read();
  }

  public final int getRegDx() {
    return getRegister(RegisterName.Dx).read();
  }

  public final int getRegEx() {
    return getRegister(RegisterName.Ex).read();
  }

  public final int getRegHx() {
    return getRegister(RegisterName.Hx).read();
  }

  public final int getRegLx() {
    return getRegister(RegisterName.Lx).read();
  }

  public final int getRegAF() {
    return getRegister(RegisterName.AF).read();
  }

  public final int getRegAFx() {
    return getRegister(RegisterName.AFx).read();
  }

  public final void setRegAFx(int word) {
    getRegister(RegisterName.AFx).write(mask16(word));
  }

  public final int getRegBC() {
    return getRegister(RegisterName.BC).read();
  }

  public final int getFlags() {
    return getFlag().read();
  }

  public final int getRegHLx() {
    return getRegister(RegisterName.HLx).read();
  }

  public final int getRegSP() {
    return getState().getRegister(RegisterName.SP).read();
  }

  public final int getRegIX() {
    return getRegister(RegisterName.IX).read();
  }

  public final int getRegIY() {
    return getRegister(RegisterName.IY).read();
  }

  public final int getRegI() {
    return getState().getRegister(RegisterName.I).read();
  }

  public final int getRegR() {
    return getState().getRegister(RegisterName.R).read();
  }

  public final int getPairIR() {
    return getState().getRegister(RegisterName.IR).read();
  }

  public final int getMemPtr() {
    return getState().getRegister(RegisterName.MEMPTR).read();
  }

  public final boolean isCarryFlag() {
    return (getFlags() & 0x01) != 0;
  }

  public final int getRegDE() {
    return getRegister(RegisterName.DE).read();
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
    return IntMode.values()[getModeINT()];
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
    state.setIM(IntMode.values()[getModeINT()]);
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
    return state.getRegister(RegisterName.MEMPTR).read();
  }

  public int getDE() {
    return getRegister(RegisterName.DE).read();
  }

  public boolean isActiveINT() {
    return state.isIntLine();
  }

  public boolean isActiveNMI() {
    return state.isActiveNMI();
  }

  @Override
  public int getModeINT() {
    return state.getInterruptionMode().ordinal();
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

  // Implementación de RegistersGetter
  @Override
  public int getRegHL() {
    return getRegister(RegisterName.HL).read();
  }

  @Override
  public int getRegBCx() {
    return getRegister(RegisterName.BCx).read();
  }

  @Override
  public int getRegDEx() {
    return getRegister(RegisterName.DEx).read();
  }

  @Override
  public boolean isZeroFlag() {
    return (getFlags() & 0x40) != 0;
  }

  @Override
  public boolean isSignFlag() {
    return (getFlags() & 0x80) != 0;
  }

  @Override
  public boolean isParityFlag() {
    return (getFlags() & 0x04) != 0;
  }

  @Override
  public boolean isHalfCarryFlag() {
    return (getFlags() & 0x10) != 0;
  }

  @Override
  public boolean isAddSubFlag() {
    return (getFlags() & 0x02) != 0;
  }

  @Override
  public boolean getLastFlagQ() {
    return state.isFlagQ();
  }

  @Override
  public boolean getFlagQ() {
    return state.isFlagQ();
  }

  @Override
  public boolean getActiveNMI() {
    return isActiveNMI();
  }

  @Override
  public boolean getActiveINT() {
    return isActiveINT();
  }

  @Override
  public boolean getIFF1() {
    return isFfIFF1();
  }

  @Override
  public boolean getIFF2() {
    return isFfIFF2();
  }

}