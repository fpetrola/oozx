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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.types.Instruction;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the generated core from the model: {@code mvn test -pl emulator -Dtest=GenerateZ80}. */
public class GenerateZ80 {
  public static final Path TARGET = Path.of("src/main/java/com/fpetrola/z80/cpu/GeneratedZ80.java");

  public static String generate() {
    State state = SpecializerTest.state();
    Instruction[] table = SpecializerTest.tables(state);
    return new CoreGenerator(state, table, new SourceIndex(SpecializerTest.SOURCES)).generate();
  }

  @Test
  public void write() throws Exception {
    Files.writeString(TARGET, generate());
    System.out.println("written " + TARGET.toAbsolutePath());
  }
}
