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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fpetrola.emulation.helpers.snapshots;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A snapshot format that was not written here reads its own files like any other. */
class AFormatThatArrivedTest {

  /** A format nobody has ever heard of, which is the point. */
  private static class FromAJar implements SnapshotFile {
    public boolean reads(File file) {
      return SnapshotFile.named(file, ".nobody");
    }

    public SpectrumState load(File file) {
      return new SpectrumState();
    }

    public boolean save(File file, SpectrumState state) {
      return true;
    }
  }

  @AfterEach
  void nothingArrivedAfterAll() {
    SnapshotFactory.alsoRead(List.of());
  }

  @Test
  void whatThisBuildWasBornKnowingIsStillRead() {
    assertTrue(SnapshotFactory.getSnapshot(new File("game.z80")) instanceof SnapshotZ80);
    assertTrue(SnapshotFactory.getSnapshot(new File("GAME.SNA")) instanceof SnapshotSNA);
    assertNull(SnapshotFactory.getSnapshot(new File("game.nobody")), "nobody reads that yet");
  }

  @Test
  void oneThatArrivedReadsItsOwn() {
    FromAJar arrived = new FromAJar();
    SnapshotFactory.alsoRead(List.of(arrived));

    assertSame(arrived, SnapshotFactory.getSnapshot(new File("game.nobody")));
    assertTrue(SnapshotFactory.getSnapshot(new File("game.z80")) instanceof SnapshotZ80,
        "one that arrived took over a format this build already read");
  }

  /** What is found includes what ships, and one thing found twice is one format. */
  @Test
  void aFormatWrittenHereAndAlsoFoundIsOneFormat() {
    int written = SnapshotFactory.formats().size();
    SnapshotFactory.alsoRead(List.of(new SnapshotZ80(), new FromAJar()));

    assertEquals(written + 1, SnapshotFactory.formats().size(),
        "the Z80 was counted twice: " + SnapshotFactory.formats());
  }
}
