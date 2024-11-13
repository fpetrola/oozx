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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.State.InterruptionMode;
import com.fpetrola.z80.instructions.types.AbstractInstruction;

public class IM extends AbstractInstruction {
  private final int mode;
  private final State state;

  public IM(State state, int mode) {
    this.state = state;
    this.mode = mode;
  }

  public void execute() {
    state.setIntMode(InterruptionMode.values()[mode]);
  }

  public String toString() {
    return "IM" + mode;
  }

  public int getMode() {
    return mode;
  }

  public void accept(InstructionVisitor<?> visitor) {
    visitor.visitingIm(this);
  }
}
