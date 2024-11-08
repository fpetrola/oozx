/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
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
