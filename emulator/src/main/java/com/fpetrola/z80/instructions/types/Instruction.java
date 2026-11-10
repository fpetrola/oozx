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

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.base.InstructionVisitor;

public interface Instruction {
  void execute();

  int getLength();

  void accept(InstructionVisitor<?> visitor);

  /** Where the machine goes once this has run, or -1 when it just goes on to the next one. */
  default int getNextPC() {
    return -1;
  }

  /** Grows by the bytes a prefix or an operand adds to the opcode. One with no bytes of its own stays empty. */
  default void incrementLengthBy(int by) {
  }

  /** How far fetching this moved R, which a cached copy has to move it again by. */
  default int getRDelta() {
    return 0;
  }

  default void setRDelta(int rdelta) {
  }
}
