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
package com.fpetrola.z80.generate;

import com.fpetrola.oozx.generated.GeneratedCores;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.github.javaparser.ast.stmt.Statement;
import model.tags.Slow;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Prints what the specializer makes of a handful of instructions. It asserts nothing: it is for reading. */
@Slow
public class SpecializerTest {

  static Instruction at(Instruction[] table, int... path) {
    Instruction i = table[path[0]];
    for (int k = 1; k < path.length; k++)
      i = ((DefaultFetchNextOpcodeInstruction) i).getTable()[path[k]];
    return i;
  }

  static String specialized(Specializer specializer, Instruction instruction) {
    specializer.newCase();
    List<Statement> statements = specializer.statementsOf(specializer.of(instruction), "execute");
    StringBuilder sb = new StringBuilder();
    for (Statement s : statements)
      sb.append(s).append('\n');
    return sb.toString();
  }

  @Test
  public void jrWithMemptr() {
    State state = GeneratedCores.model().cpu.getOoz80().getState();
    Instruction[] table = CoreGenerator.tables(state);
    Specializer specializer = new Specializer(GeneratedCores.modelIndex());
    Specializer.Obj memptr = specializer.of(new com.fpetrola.z80.cpu.MemptrUpdater(state.getMemptr(), state.getMemory()));
    for (int[] path : new int[][]{{0x20}, {0xCD}, {0xC0}}) {
      Specializer.Obj leaf = specializer.of(at(table, path));
      specializer.newCase();
      List<Statement> body = new java.util.ArrayList<>();
      specializer.call(memptr, "updateBefore", List.of(leaf), body);
      specializer.call(leaf, "execute", List.of(), body);
      specializer.call(memptr, "updateAfter", List.of(leaf), body);
      System.out.println("==== " + java.util.Arrays.toString(path));
      body.forEach(System.out::println);
    }
  }

  @Test
  public void firstShapes() {
    State state = GeneratedCores.model().cpu.getOoz80().getState();
    Instruction[] table = CoreGenerator.tables(state);
    Specializer specializer = new Specializer(GeneratedCores.modelIndex());
    for (int[] path : new int[][]{{0x86}, {0x20}, {0x3E}, {0xDD, 0x7E}, {0xCD}, {0xED, 0xB0}, {0xE3}, {0xDD, 0xCB, 0x5E}, {0xDB}, {0xED, 0x5A}}) {
      Instruction instruction = at(table, path);
      System.out.println("==== " + java.util.Arrays.toString(path) + " " + instruction);
      System.out.println(specialized(specializer, instruction));
    }
    System.out.println("slots: " + specializer.slots.values());
  }
}
