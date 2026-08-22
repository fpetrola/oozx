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

package model.tests.media;

import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.rzx.RzxSession;
import com.fpetrola.z80.minizx.RzxPlayback;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.TimeUnit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The oracle that needs no library: a recording of somebody playing a real game.
 * <p>
 * A recording carries the snapshot the machine started from and, frame by frame, every byte the
 * game read from a port. Replaying it only stays in step while the machine does exactly what the
 * machine that was recorded did - one register loaded wrong out of the snapshot, one byte of
 * memory missing, and the game takes a different path within a frame or two and asks for reads
 * that are not there. So a replay that tracks for hundreds of frames says the snapshot was
 * loaded whole and the machine runs it right, which no comparison of fields can say.
 * <p>
 * And the other direction: the state saved out of a machine that has been running is put back
 * into it, and the recording is played on from there. Anything the file lost shows up as the
 * replay parting company with it.
 */
class RzxOracleTest {
  private static final int FRAMES_BEFORE = 200, FRAMES_AFTER = 200;

  private static RzxSession playing() throws Exception {
    return playing(model.harness.TestFiles.testFile("/rzx/jsw-full.rzx"));
  }

  private static RzxSession playing(java.io.File recording) {
    RzxSession session = RzxSession.open(recording);
    session.getSpeccy().picture.active = false;
    session.getSpeccy().sound.output.enabled = false;
    session.getSpeccy().sound.setCard(new com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice());
    return session;
  }

  @Test
  void aRecordedGameFollowsItsRecording() throws Exception {
    RzxSession session = playing();
    RzxPlayback playback = session.getPlayback();
    try {
      for (int frame = 0; frame < FRAMES_BEFORE + FRAMES_AFTER; frame++) {
        assertTrue(session.playFrame(), "the recording still has frame " + frame);
        assertEquals(playback.getFrameIndex(), playback.getPlayerFrameIndex(),
            "the machine and the recording part company at frame " + frame);
      }
    } finally {
      session.release();
    }
  }

  /**
   * The state saved out of a machine that has been running goes back into it whole.
   * <p>
   * The RAM and the registers, which is what a .z80 of a 48K carries: the ROM is not in the file,
   * and this recording brought its own, so after being put back the machine has the ROM its own
   * file holds. What proves the state came back is not the bytes but what follows them - the
   * recording plays on from where it was, and anything the file lost would send the game down a
   * different path within a frame or two.
   */
  @Test
  void theStateSavedOutOfARunningGameGoesBackInWhole() throws Exception {
    RzxSession session = playing();
    RzxPlayback playback = session.getPlayback();
    try {
      session.getPlayback().playFrames(FRAMES_BEFORE);
      Speccy speccy = session.getSpeccy();

      byte[] ramBefore = ramOf(speccy);
      Map<String, Integer> registersBefore = registersOf(speccy);

      Path saved = Files.createTempFile("mid-recording", ".z80");
      Snapshots.of(speccy).save(saved.toString());
      Snapshots.of(speccy).load(saved.toString());

      assertEquals(registersBefore, registersOf(speccy), "a register did not come back");
      assertArrayEquals(ramBefore, ramOf(speccy), "a byte of RAM did not come back");

      for (int frame = 0; frame < FRAMES_AFTER; frame++) {
        assertTrue(session.playFrame(), "the recording still has frame " + frame);
        assertEquals(playback.getFrameIndex(), playback.getPlayerFrameIndex(),
            "after being saved and put back, the machine parts company at frame " + frame);
      }
    } finally {
      session.release();
    }
  }

  /**
   * The same oracle over a whole archive of recordings, which is the strongest one this emulator
   * has: a recording keeps step only while the machine does what the recorded one did, so a
   * directory of them checks the processor, the memory, the ports and the timing at once, against
   * hardware nobody here wrote.
   * <p>
   * Off unless pointed at a directory, because the archive is not in the repository:
   * mvn -o -B test -pl machine/rzx -Dtest=RzxOracleTest -Dsurefire.failIfNoSpecifiedTests=false
   *     -Doozx.archive=$HOME/detodo/spectrum/rzx-archive
   * <p>
   * It leaves target/rzx-archive.txt behind, a line per recording, so that a run is diffed against
   * the one before it rather than read. Do not raise oozx.recordings much past a hundred in one
   * JVM: a session is a whole machine and the fork dies of memory around a hundred and fifty.
   * oozx.skip walks the rest in batches.
   */
  @Test
  @EnabledIfSystemProperty(named = "oozx.archive", matches = ".+")
  @Timeout(value = 60, unit = TimeUnit.MINUTES)
  void everyRecordingInTheArchiveKeepsStep() throws Exception {
    java.io.File[] archive = new java.io.File(System.getProperty("oozx.archive"))
        .listFiles((directory, name) -> name.endsWith(".rzx"));
    assertTrue(archive != null && archive.length > 0, "no recordings under oozx.archive");
    java.util.Arrays.sort(archive);

    int frames = Integer.getInteger("oozx.frames", 100);
    int skip = Integer.getInteger("oozx.skip", 0);
    int limit = Integer.getInteger("oozx.recordings", 100);

    StringBuilder report = new StringBuilder();
    java.util.List<String> partedCompany = new java.util.ArrayList<>();
    int played = 0;
    for (int i = skip; i < archive.length && played < limit; i++) {
      played++;
      report.append(oneRecording(archive[i], frames, partedCompany)).append('\n');
    }

    java.nio.file.Path artefact = java.nio.file.Path.of("target", "rzx-archive.txt");
    java.nio.file.Files.createDirectories(artefact.getParent());
    java.nio.file.Files.writeString(artefact, report);
    System.out.printf("%d recordings, %d frames each at most, written to %s%n",
        played, frames, artefact.toAbsolutePath());

    assertTrue(partedCompany.isEmpty(), "recordings the machine parted company with: " + partedCompany);
  }

  /** A line for the report, and the recording's name on the list if the machine lost step. */
  private static String oneRecording(java.io.File recording, int frames, java.util.List<String> partedCompany) {
    RzxSession session;
    try {
      session = playing(recording);
    } catch (RuntimeException | Error cannotOpen) {
      return recording.getName() + " 0 unreadable:" + cannotOpen.getClass().getSimpleName();
    }
    RzxPlayback playback = session.getPlayback();
    int frame = 0;
    try {
      while (frame < frames && session.playFrame()) {
        if (playback.getFrameIndex() != playback.getPlayerFrameIndex()) {
          partedCompany.add(recording.getName() + "@" + frame);
          return recording.getName() + " " + frame + " parted-company";
        }
        frame++;
      }
      return recording.getName() + " " + frame + (frame < frames ? " ended" : " ok");
    } catch (RuntimeException whilePlaying) {
      partedCompany.add(recording.getName() + "@" + frame + " " + whilePlaying);
      return recording.getName() + " " + frame + " threw:" + whilePlaying.getClass().getSimpleName();
    } finally {
      session.release();
    }
  }

  /** The 48K a snapshot of this machine carries: from 0x4000 up, the ROM being the machine's own. */
  private static byte[] ramOf(Speccy speccy) {
    byte[] ram = new byte[0xC000];
    for (int address = 0; address < ram.length; address++) {
      ram[address] = (byte) speccy.memory.peek(0x4000 + address);
    }
    return ram;
  }

  private static Map<String, Integer> registersOf(Speccy speccy) {
    Map<String, Integer> registers = new LinkedHashMap<>();
    for (RegisterName name : new RegisterName[]{RegisterName.AF, RegisterName.BC, RegisterName.DE,
        RegisterName.HL, RegisterName.AFx, RegisterName.BCx, RegisterName.DEx, RegisterName.HLx,
        RegisterName.IX, RegisterName.IY, RegisterName.SP, RegisterName.PC, RegisterName.I}) {
      registers.put(name.name(), speccy.cpu.getOoz80().getState().getRegister(name).read());
    }
    registers.put("iff1", speccy.cpu.getOoz80().getState().isIff1() ? 1 : 0);
    registers.put("im", speccy.cpu.getOoz80().getState().getInterruptionMode().ordinal());
    return registers;
  }
}
