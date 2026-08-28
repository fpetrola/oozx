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

package model.tests;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.rzx.RzxSession;
import com.fpetrola.z80.minizx.RzxPlayback;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays a recording against the machine and checks it stays in step.
 * <p>
 * A recording desynchronises quietly: the emulator goes on running and the game goes on doing
 * something, just not what was recorded. So the thing asserted is that the frames the driver
 * counted and the frames the port handed out agree, which is what parts company the moment the
 * fetch count drifts, and that the screen is the one the recording reaches.
 */
class RzxPlaybackTest {

  private static File recording() {
    try {
      return Path.of(RzxPlaybackTest.class.getResource("/rzx/jsw-full.rzx").toURI()).toFile();
    } catch (Exception e) {
      throw new IllegalStateException("the jsw-full.rzx test resource is missing", e);
    }
  }

  @Test
  void the_recording_drives_the_machine_frame_for_frame() {
    OOSpectrumConnector.noTest = true;
    RzxSession session = RzxSession.open(recording());
    RzxPlayback playback = session.getPlayback();

    assertEquals(85369, playback.getFrameCount(), "frames in the recording");

    int asked = 6000;
    int played = playback.playFrames(asked);

    assertEquals(asked, played, "frames played");
    assertEquals(asked, playback.getFrameIndex(), "frames the driver counted");
    // The port advances on its own count of what it handed out; if the two disagree the
    // recording has drifted even though nothing has failed.
    assertEquals(asked, playback.getPlayerFrameIndex(), "frames the port handed out");
    assertTrue(playback.getInstructions() > 20_000_000,
        "a recording of this length runs tens of millions of instructions, not thousands");

    // Jet Set Willy names the room it is in on the bottom line of the screen. Reaching one at
    // all means the recorded player got past the copy protection and into the game.
    assertTrue(screenText(session.getSpeccy()).contains("The "),
        "expected to be inside a room, screen reads: " + screenText(session.getSpeccy()));
  }

  /** Reads the room name line, which Jet Set Willy prints in the standard ROM character set. */
  private static String screenText(Speccy speccy) {
    StringBuilder line = new StringBuilder();
    for (int column = 0; column < 32; column++) {
      line.append(characterAt(speccy, 16, column));
    }
    return line.toString();
  }

  private static char characterAt(Speccy speccy, int row, int column) {
    int[] pattern = new int[8];
    for (int pixelRow = 0; pixelRow < 8; pixelRow++) {
      int y = row * 8 + pixelRow;
      int address = 0x4000 + ((y >> 6) << 11) + ((y & 7) << 8) + (((y >> 3) & 7) << 5) + column;
      pattern[pixelRow] = speccy.memory.readByteInternal(address) & 0xFF;
    }
    for (int code = 32; code < 127; code++) {
      boolean same = true;
      for (int pixelRow = 0; pixelRow < 8 && same; pixelRow++) {
        int romByte = speccy.memory.readByteInternal(0x3D00 + (code - 32) * 8 + pixelRow) & 0xFF;
        same = romByte == pattern[pixelRow];
      }
      if (same) {
        return (char) code;
      }
    }
    return '?';
  }
}
