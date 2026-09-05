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

package model.tests.formats;

import com.fpetrola.emulation.helpers.snapshots.SnapshotFactory;
import com.fpetrola.emulation.helpers.snapshots.SnapshotZ80;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What Java made of a snapshot, said in bytes libspectrum wrote.
 * <p>
 * Two readers hold a snapshot in their own shapes and there is no comparing a C struct with a
 * Java object. So both are written back out by the same writer, libspectrum's: the reference
 * writes what it read, and it writes what Java read, and the two byte arrays have to be equal.
 * Every field is in them, including the ones nobody thought to check, and the encoding is the
 * same one on both sides because the same code produced it.
 */
class SnapshotsAsLibspectrumHasThemTest {
  @BeforeAll
  static void needsLibspectrum() {
    assumeTrue(LibspectrumOracle.present(), "libspectrum is not installed here");
  }

  @Test
  void everySnapshotJavaReadsHoldsWhatTheReferenceSaysItHolds() throws Exception {
    List<String> refused = new ArrayList<>(), differ = new ArrayList<>(), taken = new ArrayList<>();
    int agreed = 0;
    for (Path file : snapshots()) {
      byte[] original = Files.readAllBytes(file);
      int type = type(file);
      byte[] reference = LibspectrumOracle.rewritten(original, type);
      if (reference == null) {
        // A file the reference will not have. Java should not have it either: a snapshot it
        // takes is one it will put into a machine, and these are files that cannot be put into
        // one - a .sna whose stack pointer is not in RAM has no PC to pop off it.
        try {
          new SnapshotFactory().getSnapshot(file.toFile()).load(file.toFile());
          taken.add(file.getFileName().toString());
        } catch (Throwable refusedToo) {
          // as the reference does
        }
        continue;
      }

      byte[] asJavaReadIt;
      try {
        SpectrumState state = new SnapshotFactory().getSnapshot(file.toFile()).load(file.toFile());
        asJavaReadIt = LibspectrumOracle.rewritten(new SnapshotZ80().saveToBytes(state), LibspectrumOracle.Z80);
      } catch (Throwable thrown) {
        refused.add(file.getFileName() + ": " + thrown);
        continue;
      }
      if (asJavaReadIt == null) {
        differ.add(file.getFileName() + ": libspectrum will not read back what java wrote");
      } else if (!java.util.Arrays.equals(reference, asJavaReadIt)) {
        differ.add(file.getFileName() + ": " + firstDifference(reference, asJavaReadIt));
      } else {
        agreed++;
      }
    }
    System.out.println(agreed + " snapshots read the same as the reference reads them");
    assertEquals(List.of(), taken, "snapshots the reference refuses and java takes");
    assertEquals(List.of(), refused, "snapshots java cannot read at all");
    assertEquals(List.of(), differ, "snapshots java reads differently from the reference");
  }

  /** Where two writings of the same snapshot part company, which is the field that was read wrong. */
  private static String firstDifference(byte[] reference, byte[] java) {
    if (reference.length != java.length) {
      return "libspectrum wrote " + reference.length + " bytes, java's state wrote " + java.length;
    }
    for (int at = 0; at < reference.length; at++) {
      if (reference[at] != java[at]) {
        return String.format("first difference at byte %d: %02X against %02X", at, reference[at], java[at]);
      }
    }
    return "same";
  }

  private static int type(Path file) {
    String name = file.getFileName().toString();
    return name.endsWith(".z80") ? LibspectrumOracle.Z80
        : name.endsWith(".szx") ? LibspectrumOracle.SZX
        : name.endsWith(".sna") ? LibspectrumOracle.SNA
        : LibspectrumOracle.SP;
  }

  private static List<Path> snapshots() throws Exception {
    List<Path> all = new ArrayList<>();
    for (String extension : new String[]{"z80", "szx", "sna", "sp"}) {
      all.addAll(LibspectrumOracle.corpus(extension));
    }
    // A real game as well as the reference's own corners: the corners are all but empty, and a
    // snapshot of a machine that has been running is the one with something in every field.
    Path game = Path.of("src/test/resources/snapshots/manicminer.z80");
    if (Files.isReadable(game)) {
      all.add(game);
    }
    return all;
  }
}
