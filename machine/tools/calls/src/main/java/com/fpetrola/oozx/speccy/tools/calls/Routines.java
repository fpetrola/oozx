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


package com.fpetrola.oozx.speccy.tools.calls;

import com.fpetrola.oozx.speccy.modules.z80.Disassembly;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The routines a run found, and how far each one goes.
 * <p>
 * Where they start is watched, not guessed: a routine is a place something called. How far they
 * go is read, by following the instructions from the entry until one of them ends the routine.
 * The two halves are why this can say things a disassembler alone cannot - a disassembler has
 * to decide what is code before it can read it, and this was told by the machine.
 */
public final class Routines {

  /** Long enough for any real routine; a run-on means the walk met data, not a routine. */
  private static final int MOST = 2000;

  /** A routine: where it starts, how far it goes, how often it was called and how it ends. */
  public record Routine(int address, int bytes, int instructions, long times, String ends,
                        int callers) {
  }

  private Routines() {
  }

  /** What the run found, the most called first. */
  public static List<Routine> found(List<CallTree.Call> program, Disassembly reading) {
    Map<Integer, long[]> perAddress = new LinkedHashMap<>();
    addUp(program, perAddress);
    List<Routine> routines = new ArrayList<>();
    perAddress.forEach((address, counts) ->
        routines.add(walk(address, counts[0], (int) counts[1], reading)));
    routines.sort((one, other) -> Long.compare(other.times(), one.times()));
    return routines;
  }

  /** The same routine called from two places in the tree is one routine, called twice. */
  private static void addUp(List<CallTree.Call> calls, Map<Integer, long[]> perAddress) {
    for (CallTree.Call call : calls) {
      long[] counts = perAddress.computeIfAbsent(call.address(), at -> new long[2]);
      counts[0] += call.times();
      counts[1] += call.back().size();
      addUp(call.made(), perAddress);
    }
  }

  /**
   * How far a routine goes, by reading forward from its entry.
   * <p>
   * It ends at the first instruction that cannot fall through: an unconditional RET or JP. A
   * conditional one does not end it - the routine goes on for whoever did not take the branch -
   * and neither does a call, however far away it goes, because it comes back.
   */
  private static Routine walk(int entry, long times, int callers, Disassembly reading) {
    int at = entry;
    int instructions = 0;
    String ends = "runs on";
    while (instructions < MOST) {
      int opcode = reading.byteAt(at);
      String said = reading.instructionAt(at).trim();
      at = at + reading.lengthAt(at) & 0xffff;
      instructions++;
      if (endsIt(opcode)) {
        ends = said;
        break;
      }
    }
    return new Routine(entry, at - entry & 0xffff, instructions, times, ends, callers);
  }

  /** RET, JP nn, JR e and JP (HL): the opcodes after which the next byte is not this routine. */
  private static boolean endsIt(int opcode) {
    return opcode == 0xc9 || opcode == 0xc3 || opcode == 0x18 || opcode == 0xe9;
  }
}
