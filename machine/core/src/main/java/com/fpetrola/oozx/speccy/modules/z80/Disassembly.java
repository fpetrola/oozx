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


package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.z80.base.ToStringInstructionVisitor;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.ReadOnlyIOImplementation;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;

import java.util.ArrayList;
import java.util.List;

/**
 * Reading a machine's memory as instructions, without the machine noticing.
 * <p>
 * A decoding-only processor state of its own, because the one that is running has no surviving
 * {@link Instruction} objects to look at, and decoding through it would bill T-states for every
 * byte somebody reads over its shoulder. Nothing here ever writes, and nothing here costs the
 * machine a cycle.
 */
public final class Disassembly {

  /** An instruction where it lies: the address, the bytes, and what they say. */
  public record Line(int address, String bytes, String instruction) {
  }

  private final State reading;
  private final DefaultInstructionFetcher decoder;
  private final Speccy machine;

  public Disassembly(Speccy machine) {
    this.machine = machine;
    reading = new State(new ReadOnlyIOImplementation(null), new Memory() {
      public int read(int address, int fetching) {
        return machine.memory.peek(address & 0xffff) & 0xff;
      }

      public void write(int address, int value) {
      }

      public void reset() {
      }
    });
    decoder = new DefaultInstructionFetcher(reading, false, false);
  }

  public int byteAt(int address) {
    return machine.memory.peek(address & 0xffff) & 0xff;
  }

  /** The instruction at an address, decoded. */
  public Instruction at(int address) {
    reading.getPc().write(address & 0xffff);
    return decoder.fetchNextInstruction();
  }

  public String instructionAt(int address) {
    return new ToStringInstructionVisitor().createToString(at(address));
  }

  /** How many bytes the instruction there takes, never less than one however it decodes. */
  public int lengthAt(int address) {
    return Math.max(1, at(address).getLength());
  }

  public String bytesAt(int address, int length) {
    StringBuilder bytes = new StringBuilder();
    for (int i = 0; i < length; i++) {
      bytes.append(i == 0 ? "" : " ").append("%02X".formatted(byteAt(address + i)));
    }
    return bytes.toString();
  }

  public Line lineAt(int address) {
    int at = address & 0xffff;
    Instruction instruction = at(at);
    return new Line(at, bytesAt(at, Math.max(1, instruction.getLength())),
        new ToStringInstructionVisitor().createToString(instruction));
  }

  /** A run of consecutive instructions from an address. */
  public List<Line> from(int address, int lines) {
    List<Line> listing = new ArrayList<>();
    for (int at = address & 0xffff; listing.size() < lines; ) {
      Line line = lineAt(at);
      listing.add(line);
      at = at + Math.max(1, lengthAt(at)) & 0xffff;
    }
    return listing;
  }
}
