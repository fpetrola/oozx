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

import model.tags.Slow;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The committed core is what this machine produces today. It is not only the instructions: this
 * core carries the machine's memory and its contention inside, so a change to ContendedMemory,
 * to Memory or to RecordingPhaseProcessor changes it too, and nothing else would notice.
 */
@Slow
public class GeneratedSpectrumZ80IsCurrentTest {
  static final Path SOURCE = Path.of("src/main/java/com/fpetrola/oozx/generated/GeneratedSpectrumZ80.java");

  @Test
  public void committedCoreIsWhatTheMachineGenerates() throws Exception {
    String committed = Files.readString(SOURCE);
    assertEquals(GeneratedCores.written(), committed,
        "GeneratedSpectrumZ80.java is stale: build machine/generated, which writes it");
  }
}
