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

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The committed core is what the model produces today; if not, the model changed and nobody regenerated. */
public class GeneratedZ80IsCurrentTest {
  @Test
  public void committedCoreIsWhatTheModelGenerates() throws Exception {
    String committed = Files.readString(GenerateZ80.TARGET);
    assertEquals(GenerateZ80.generate(), committed, "GeneratedZ80.java is stale: run mvn test -pl emulator -Dtest=GenerateZ80");
  }
}
