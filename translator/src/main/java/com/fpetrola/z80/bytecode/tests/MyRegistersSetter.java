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

package com.fpetrola.z80.bytecode.tests;

import com.fpetrola.z80.cpu.RegistersSetter;
import com.fpetrola.z80.minizx.SpectrumApplication;

public class MyRegistersSetter implements RegistersSetter {
  private final SpectrumApplication spectrumApplication;

  public MyRegistersSetter(SpectrumApplication spectrumApplication) {
    this.spectrumApplication = spectrumApplication;
  }

  @Override
  public void setRegPC(int address) {

  }

  @Override
  public void setFlags(int regF) {
    spectrumApplication.F(regF);
  }

  @Override
  public void setRegDE(int word) {
    spectrumApplication.DE(word);
  }

  @Override
  public void setRegA(int value) {
    spectrumApplication.A(value);
  }

  @Override
  public void setRegB(int value) {
    spectrumApplication.B(value);
  }

  @Override
  public void setRegC(int value) {
    spectrumApplication.C(value);
  }

  @Override
  public void setRegD(int value) {
    spectrumApplication.D(value);

  }

  @Override
  public void setRegE(int value) {
    spectrumApplication.E(value);

  }

  @Override
  public void setRegH(int value) {
    spectrumApplication.H(value);

  }

  @Override
  public void setRegL(int value) {
    spectrumApplication.L(value);

  }

  @Override
  public void setRegAFx(int value) {
    spectrumApplication.AFx(value);
  }

  @Override
  public void setRegBCx(int value) {
    spectrumApplication.BCx(value);
  }

  @Override
  public void setRegDEx(int value) {
    spectrumApplication.DEx(value);
  }

  @Override
  public void setRegAF(int word) {
    spectrumApplication.AF(word);
  }

  @Override
  public void setRegBC(int word) {
    spectrumApplication.BC(word);

  }

  @Override
  public void setRegHLx(int word) {
    spectrumApplication.HLx(word);

  }

  @Override
  public void setRegSP(int word) {
    spectrumApplication.SP(word);

  }

  @Override
  public void setRegIX(int word) {
    spectrumApplication.IX(word);

  }

  @Override
  public void setRegIY(int word) {
    spectrumApplication.IY(word);

  }

  @Override
  public void setRegI(int value) {
    spectrumApplication.I(value);

  }

  @Override
  public void setRegR(int value) {
    spectrumApplication.setR(value);

  }

  @Override
  public void setMemPtr(int word) {
    spectrumApplication.MEMPTR = word;

  }

  @Override
  public void setCarryFlag(boolean carryState) {
    spectrumApplication.F((spectrumApplication.F() & 0xFE) | (carryState ? 1 : 0));

  }

  @Override
  public void setIFF1(boolean state) {
  }

  @Override
  public void setIFF2(boolean state) {

  }

  @Override
  public void setNMI(boolean nmi) {

  }

  @Override
  public void setINTLine(boolean intLine) {

  }

  @Override
  public void setIM(int mode) {

  }

  @Override
  public void setHalted(boolean state) {

  }

  @Override
  public void setPendingEI(boolean state) {

  }

  @Override
  public void setFlagQ(boolean flagQ) {

  }

  @Override
  public void setLastFlagQ(boolean lastFlagQ) {

  }

  @Override
  public void setMemptr(int memptr) {

  }

  @Override
  public void setDE(int DE) {
    spectrumApplication.DE = DE;

  }

  @Override
  public void setActiveINT(boolean activeINT) {

  }

  @Override
  public void setActiveNMI(boolean activeNMI) {

  }

  @Override
  public void setModeINT(int modeINT) {

  }

  @Override
  public void setFfIFF1(boolean ffIFF1) {

  }

  @Override
  public void setFfIFF2(boolean ffIFF2) {

  }

  @Override
  public void setPinReset(boolean pinReset) {

  }
}
