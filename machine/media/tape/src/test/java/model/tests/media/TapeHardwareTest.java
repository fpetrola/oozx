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

package model.tests.media;

import com.fpetrola.oozx.speccy.modules.tape.TapeHardware;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reading the machine off the tape instead of guessing it from the file's name.
 * <p>
 * A TZX declares the machines it runs on and marks the ones whose features it uses, and that
 * block was being counted and thrown away. The consequence was silence: a game that offers AY
 * music on a 128K and none on a 48K would start on the 48K unless somebody had named the file
 * for it, so the music was there and nobody heard it.
 */
class TapeHardwareTest {

  private static final int COMPUTERS = 0x00;
  private static final int RUNS = 0x00;
  private static final int USES_ITS_FEATURES = 0x01;
  private static final int RUNS_WITHOUT_USING = 0x02;
  private static final int DOES_NOT_RUN = 0x03;

  private static final int ID_48K = 0x01;
  private static final int ID_128K = 0x03;
  private static final int ID_PLUS2 = 0x04;
  private static final int ID_PLUS3 = 0x05;

  /** A TZX carrying nothing but a hardware block saying these things. */
  private static File tzxDeclaring(int... machineAndSupport) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(new byte[]{'Z', 'X', 'T', 'a', 'p', 'e', '!', 0x1A, 1, 20});
    out.write(0x33);
    out.write(machineAndSupport.length / 2);
    for (int i = 0; i < machineAndSupport.length; i += 2) {
      out.write(COMPUTERS);
      out.write(machineAndSupport[i]);
      out.write(machineAndSupport[i + 1]);
    }
    File file = new File(Files.createTempDirectory("tzx").toFile(), "game.tzx");
    file.deleteOnExit();
    try (FileOutputStream stream = new FileOutputStream(file)) {
      stream.write(out.toByteArray());
    }
    return file;
  }

  @Test
  void aTapeThatUsesTheOneTwentyEightGetsOne() throws Exception {
    assertEquals(Optional.of("Spectrum 128K"),
        TapeHardware.bestMachineFor(tzxDeclaring(ID_48K, RUNS_WITHOUT_USING, ID_128K, USES_ITS_FEATURES)),
        "it runs on both and only one of them has the music");
  }

  @Test
  void aTapeThatOnlyKnowsAFortyEightStaysOnOne() throws Exception {
    assertEquals(Optional.of("Spectrum 48K"), TapeHardware.bestMachineFor(tzxDeclaring(ID_48K, USES_ITS_FEATURES)));
  }

  /** Several named and none singled out: the biggest of them, which is what a person would pick. */
  @Test
  void severalNamedMeansTheBiggest() throws Exception {
    assertEquals(Optional.of("Spectrum Plus 3"),
        TapeHardware.bestMachineFor(tzxDeclaring(ID_48K, RUNS, ID_128K, RUNS, ID_PLUS3, RUNS)));
  }

  /**
   * The one worth being careful about: a tape using the 128K's sound that also runs on a +3
   * wants the 128K. Taking the biggest without reading the rest would answer +3 and lose the
   * reason the block was read at all.
   */
  @Test
  void oneItUsesBeatsABiggerOneItMerelyRunsOn() throws Exception {
    assertEquals(Optional.of("Spectrum 128K"),
        TapeHardware.bestMachineFor(tzxDeclaring(ID_128K, USES_ITS_FEATURES, ID_PLUS3, RUNS_WITHOUT_USING)));
  }

  @Test
  void aMachineItRefusesToRunOnIsNotOffered() throws Exception {
    assertEquals(Optional.of("Spectrum 48K"),
        TapeHardware.bestMachineFor(tzxDeclaring(ID_48K, RUNS, ID_PLUS2, DOES_NOT_RUN)));
  }

  /** A TAP states nothing, so the caller is told nothing and falls back to the file's name. */
  @Test
  void aTapSaysNothingAtAll() throws Exception {
    File tap = new File(Files.createTempDirectory("taps").toFile(), "game.tap");
    tap.deleteOnExit();
    try (FileOutputStream stream = new FileOutputStream(tap)) {
      stream.write(new byte[]{19, 0, 0, 3, 't', 'e', 's', 't'});
    }
    assertEquals(Optional.empty(), TapeHardware.bestMachineFor(tap));
  }
}
