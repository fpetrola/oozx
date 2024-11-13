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

package com.fpetrola.z80.cpu;

/** The read side of RegistersSetter. */
public interface RegistersGetter {
  int getRegA();
  int getRegB();
  int getRegC();
  int getRegD();
  int getRegE();
  int getRegH();
  int getRegL();
  int getRegF();

  int getRegAx();
  int getRegBx();
  int getRegCx();
  int getRegDx();
  int getRegEx();
  int getRegHx();
  int getRegLx();
  int getRegFx();

  int getRegPC();
  int getRegSP();
  int getRegIX();
  int getRegIY();
  int getRegI();
  int getRegR();
  int getMemPtr();

  int getRegAF();
  int getRegBC();
  int getRegDE();
  int getRegHL();
  int getRegAFx();
  int getRegBCx();
  int getRegDEx();
  int getRegHLx();

  boolean getIFF1();
  boolean getIFF2();
  boolean isHalted();
  boolean isPendingEI();
  boolean getActiveNMI();
  boolean getActiveINT();
  int getModeINT();
  
  boolean getFlagQ();
  boolean getLastFlagQ();
  
  boolean isCarryFlag();
  boolean isZeroFlag();
  boolean isSignFlag();
  boolean isParityFlag();
  boolean isHalfCarryFlag();
  boolean isAddSubFlag();
}
