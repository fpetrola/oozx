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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.z80.generate.Specializer;
import com.fpetrola.z80.memory.Memory;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;

/** What the specializer makes of the machine's memory, printed. It asserts nothing: it is for reading. */
@model.tags.Slow
public class SpecializeMemoryTest {
  @Test
  public void readAndWrite() {
    Speccy speccy = GeneratedCores.model();
    Memory memory = speccy.cpu.getOoz80().getState().getMemory();
    System.out.println("the memory is a " + memory.getClass().getName());
    Specializer specializer = new Specializer(GeneratedCores.modelIndex());
    specializer.terminals.remove(Memory.class);
    specializer.shared.put(speccy.memory, "ram");
    specializer.shared.put(speccy.ula, "ula");
    specializer.shared.put(speccy.zxClock, "clock");
    specializer.shared.put(speccy.display, "display");
    // The clock and the display stay calls: small, concrete, and off the arithmetic of an access.
    specializer.terminals.put(speccy.zxClock.getClass(), "clock");
    specializer.terminals.put(speccy.display.getClass(), "display");
    specializer.opaque.addAll(List.of("reading", "writing"));
    specializer.newCase();
    List<Statement> read = specializer.statementsOf(specializer.of(memory), "read", new NameExpr("address"), new NameExpr("fetching"));
    System.out.println("==== read");
    read.forEach(s -> System.out.println(s));
    specializer.newCase();
    System.out.println("==== write");
    specializer.statementsOf(specializer.of(memory), "write", new NameExpr("address"), new NameExpr("value")).forEach(s -> System.out.println(s));
  }
}
