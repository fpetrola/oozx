/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fpetrola.oozx.generated;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OopCore;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.registers.UnrolledRegisterBank;

/**
 * The core of a machine built to be read by the generator, not to run: the bank the generated core
 * will be, and the contention counted inside, so that the memory the Z80 builds is the one the
 * generated core inlines.
 */
public class ModelCore extends OopCore {
  public RegisterBank bank(Memory memory, IO io) {
    return new UnrolledRegisterBank();
  }

  public boolean countsItsOwnContention() {
    return true;
  }
}
