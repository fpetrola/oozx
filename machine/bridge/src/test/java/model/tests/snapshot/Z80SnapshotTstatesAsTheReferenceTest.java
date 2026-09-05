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
package model.tests.snapshot;

import com.fpetrola.emulation.helpers.snapshots.SnapshotZ80;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.oozx.speccy.bridge.LibSpectrum;
import com.fpetrola.oozx.speccy.bridge.Z80Loader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The T-states a .z80 carries, read by this emulator's loader, against what libspectrum reads
 * from the same bytes: the counter of a v3 header, the value a v2 header without one gets, and
 * a save that reloads to the same count. Fuse's own library is the oracle, which is what this
 * bridge is for; the test steps aside where it is not installed.
 */
class Z80SnapshotTstatesAsTheReferenceTest {
  private static final Path V3 = Path.of("src/test/resources/snapshots/manicminer.z80");

  @BeforeAll
  static void needsLibspectrum() {
    boolean loads;
    try {
      loads = LibSpectrum.INSTANCE != null;
    } catch (Throwable notInstalled) {
      loads = false;
    }
    assumeTrue(loads, "libspectrum is not installed here");
  }

  @Test
  void aV3HeaderCounterReadsAsLibspectrumReadsIt() throws Exception {
    assertEquals(libspectrum(V3), loader(V3));
  }

  @Test
  void aV2HeaderWithoutACounterGetsWhatLibspectrumGivesIt() throws Exception {
    byte[] v3 = Files.readAllBytes(V3);
    int extended = (v3[30] & 0xff) | ((v3[31] & 0xff) << 8);
    byte[] v2 = new byte[32 + 23 + v3.length - 32 - extended];
    System.arraycopy(v3, 0, v2, 0, 30);
    v2[30] = 23;
    System.arraycopy(v3, 32, v2, 32, 23);
    System.arraycopy(v3, 32 + extended, v2, 32 + 23, v3.length - 32 - extended);
    Path file = Files.createTempFile("v2", ".z80");
    Files.write(file, v2);
    assertEquals(libspectrum(file), loader(file));
  }

  @Test
  void aSaveReloadsToTheSameCount() throws Exception {
    SpectrumState state = new SnapshotZ80().load(V3.toFile());
    for (int tstates : new int[]{0, 4, 17472, 34943, 69887}) {
      state.setTstates(tstates);
      SpectrumState back = new SnapshotZ80().loadFromBytes(new SnapshotZ80().saveToBytes(state));
      assertEquals(tstates, back.getTstates(), "saved at " + tstates);
    }
  }

  private static int loader(Path file) throws Exception {
    return new SnapshotZ80().load(file.toFile()).getTstates();
  }

  private static int libspectrum(Path file) {
    return Z80Loader.getTstates(LibSpectrum.INSTANCE, file.toString());
  }
}
