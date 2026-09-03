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
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.UnrolledRegisterBankFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the generated core from the model: {@code mvn test -pl emulator -Dtest=GenerateZ80}. */
public class GenerateZ80 {
  public static final Path TARGET = Path.of("src/main/java/com/fpetrola/z80/cpu/GeneratedZ80.java");
  /** The model the generator reads: the instances come from it and so does the source it inlines. */
  static final Path SOURCES = Path.of("src/main/java");

  /** A machine that only has to be built, not run: the generator reads its instruction graph. */
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

  public static String generate() {
    State state = state();
    return new CoreGenerator(state, tables(state), new SourceIndex(SOURCES)).generate();
  }

  @Test
  public void write() throws Exception {
    Files.writeString(TARGET, generate());
    System.out.println("written " + TARGET.toAbsolutePath());
  }
}
