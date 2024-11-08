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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.State.InterruptionMode;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class IM<T extends WordNumber> extends AbstractInstruction<T> {
  int mode;
  private State<T> state;

  public IM(State state, int mode) {
    this.state = state;
    this.mode = mode;
  }

  public int execute() {
    state.setIntMode(InterruptionMode.values()[mode]);
    return 4;
  }

  public String toString() {
    return "IM" + mode;
  }

  public int getMode() {
    return mode;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitingIm(this);
  }
}
