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

/**
 * Interface para leer el estado de los registros de la CPU Z80.
 * Este es el complemento de RegistersSetter, permitiendo leer valores en lugar de escribirlos.
 */
public interface RegistersGetter {
  // Registros de 8 bits
  int getRegA();
  int getRegB();
  int getRegC();
  int getRegD();
  int getRegE();
  int getRegH();
  int getRegL();
  int getRegF();

  // Registros alternativos de 8 bits
  int getRegAx();
  int getRegBx();
  int getRegCx();
  int getRegDx();
  int getRegEx();
  int getRegHx();
  int getRegLx();
  int getRegFx();

  // Registros de propósito especial de 16 bits
  int getRegPC();
  int getRegSP();
  int getRegIX();
  int getRegIY();
  int getRegI();
  int getRegR();
  int getMemPtr();

  // Registros de 16 bits combinados
  int getRegAF();
  int getRegBC();
  int getRegDE();
  int getRegHL();
  int getRegAFx();
  int getRegBCx();
  int getRegDEx();
  int getRegHLx();

  // Flags de interrupción
  boolean getIFF1();
  boolean getIFF2();
  boolean isHalted();
  boolean isPendingEI();
  boolean getActiveNMI();
  boolean getActiveINT();
  int getModeINT();
  
  // Flags especiales
  boolean getFlagQ();
  boolean getLastFlagQ();
  
  // Flags individuales
  boolean isCarryFlag();
  boolean isZeroFlag();
  boolean isSignFlag();
  boolean isParityFlag();
  boolean isHalfCarryFlag();
  boolean isAddSubFlag();
}
