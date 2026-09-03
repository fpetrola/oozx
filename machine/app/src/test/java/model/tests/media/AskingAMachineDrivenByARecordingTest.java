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

import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.rzx.RzxSession;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asking a machine for something while a recording is driving it.
 * <p>
 * Anything asked of a running machine from outside - change the speed, change the model, plug a
 * device in - is queued and done between instructions, because doing it from another thread in
 * the middle of a frame is how this emulator has hung before. The queue was emptied at the end
 * of the ordinary run loop and nowhere else, and a recording does not use that loop: it drives
 * its machine frame by frame. So while one played, everything asked for was queued and none of
 * it ever happened, silently.
 */
class AskingAMachineDrivenByARecordingTest {

  private static File recording() {
    try {
      return model.harness.TestFiles.testFile("/rzx/jsw-full.rzx");
    } catch (Exception e) {
      throw new IllegalStateException("the jsw-full.rzx test resource is missing", e);
    }
  }

  @Test
  void what_was_asked_for_happens_while_the_recording_plays() {
    Emulation.noTest = true;
    RzxSession session = RzxSession.open(recording());

    boolean[] done = {false};
    session.getSpeccy().z80.later(() -> done[0] = true);

    assertTrue(session.playFrame(), "the recording should have a frame to play");

    assertTrue(done[0], "what was asked for while a recording plays must actually happen");
  }

  /**
   * A recording picked off the disk has to be recognised as one, because that is what decides
   * which window it opens in. Handed to a machine instead it is read as a snapshot, which is
   * what Open File did with one until now.
   */
  @Test
  void a_recording_is_recognised_as_one_and_a_snapshot_is_not() {
    assertTrue(RzxSession.isRecording(recording().getAbsolutePath()),
        "the very file this can open must be recognised as a recording");
    assertFalse(RzxSession.isRecording("manicminer.z80"), "a snapshot is a machine's to load");
    assertFalse(RzxSession.isRecording(null));
  }

  @Test
  void changing_the_speed_while_a_recording_plays_changes_the_speed() {
    Emulation.noTest = true;
    RzxSession session = RzxSession.open(recording());
    int before = session.getSpeccy().settings.current.emulationSpeed;

    session.getSpeccy().z80.mockCore.setGeneralOption("turbo", true);
    session.playFrame();

    assertTrue(session.getSpeccy().settings.current.emulationSpeed > before,
        "turbo asked for while a recording plays left the speed at " + before);
    // And it is the speed the player paces itself by, so the recording really does run faster.
    assertEquals(session.getSpeccy().settings.current.emulationSpeed,
        (int) session.getSpeccy().z80.mockCore.getEmulationSpeed(),
        "the speed the player reads must be the speed that was set");
  }
}
