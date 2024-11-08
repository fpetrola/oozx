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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.ExecutionPoint;

import java.util.function.Supplier;

public interface ComplexInstructionSpy<T> extends InstructionSpy<T> {

  default boolean isReadAccessCapture() {
    return false;
  }

  default long getExecutionNumber() {
    return 0;
  }

  default boolean[] getBitsWritten() {
    return new boolean[0];
  }


  default Instruction getFetchedAt(int address) {
    return null;
  }


  default boolean wasFetched(int address) {
    return false;
  }


  default boolean isIndirectReference() {
    return false;
  }

  default void setSpritesArray(boolean[] bitsWritten) {

  }


  default void undo() {

  }


  default boolean isStructureCapture() {
    return false;
  }


  default void enableStructureCapture() {
  }


  default void switchToIndirectReference() {
  }


  default void switchToDirectReference() {
  }


  default <T> T executeInPause(Supplier<T> object) {
    return object.get();
  }


  default void setSecondZ80(Z80Cpu z802) {

  }


  default ExecutionPoint getLastExecutionPoint() {
    return null;
  }


  default void export() {

  }


  default void enableReadAccessCapture() {

  }


  default void setGameName(String gameName) {

  }
}