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
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class LdOperation<T extends WordNumber> extends AbstractInstruction<T> {
  protected Instruction<T> instruction;
  protected OpcodeReference<T> target;

  public LdOperation(OpcodeReference target, Instruction<T> instruction) {
    this.target = target;
    this.instruction = instruction;
  }

  public int execute() {
    instruction.execute();
    if (instruction instanceof TargetInstruction<T> targetInstruction) {
      T read = targetInstruction.getTarget().read();
      target.write(read);
    }
    return cyclesCost;
  }

  public String toString() {
    return "LD " + target + "," + instruction;
  }
}
