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
class Z80SnapshotTstatesAsLibspectrumTest {
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
