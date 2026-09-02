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
package com.fpetrola.z80.generate;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.UnrolledRegisterBankFactory;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

public class SpecializerTest {
  static final Path SOURCES = Path.of("src/main/java");

  static State state() {
    return new State(new IO() {
      public int in(int port) {
        return 0xFF;
      }

      public void out(int port, int value) {
      }
    }, new UnrolledRegisterBankFactory().createBank(), new MockedMemory(true));
  }

  static Instruction[] tables(State state) {
    DefaultInstructionFactory factory = new DefaultInstructionFactory(state);
    return new TableBasedOpCodeDecoder(state, OpcodeConditions.createOpcodeConditions(state.getFlag(), state.getRegister(RegisterName.B)), factory.getFetchNextOpcodeInstructionFactory(), factory, new MemoryForOpcodes(state.getMemory(), state)).getOpcodeLookupTable();
  }

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
    State state = state();
    Instruction[] table = tables(state);
    Specializer specializer = new Specializer(new SourceIndex(SOURCES));
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
    State state = state();
    Instruction[] table = tables(state);
    Specializer specializer = new Specializer(new SourceIndex(SOURCES));
    for (int[] path : new int[][]{{0x86}, {0x20}, {0x3E}, {0xDD, 0x7E}, {0xCD}, {0xED, 0xB0}, {0xE3}, {0xDD, 0xCB, 0x5E}, {0xDB}, {0xED, 0x5A}}) {
      Instruction instruction = at(table, path);
      System.out.println("==== " + java.util.Arrays.toString(path) + " " + instruction);
      System.out.println(specialized(specializer, instruction));
    }
    System.out.println("slots: " + specializer.slots.values());
  }
}
