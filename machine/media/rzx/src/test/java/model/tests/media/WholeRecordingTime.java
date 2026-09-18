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

import com.fpetrola.oozx.rzx.RzxSession;
import com.fpetrola.z80.minizx.RzxPlayback;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.TimeUnit;

/**
 * A whole recording, timed. The work is fixed by construction - the same 85368 frames and the same
 * 355229553 instructions every time - so the seconds are the emulator and nothing else.
 * <p>
 * The other measurement here, InstructionsPerSecond, runs a loop of its own and is steady to half a
 * percent; it is steady because it touches none of what a game touches - no contention, no ports,
 * no interrupt, no screen - and it therefore cannot see what this one sees. Use that one to ask
 * whether a change cost anything, and this one to ask how long something actually takes.
 */
@EnabledIfSystemProperty(named = "oozx.measure", matches = "true")
@Timeout(value = 20, unit = TimeUnit.MINUTES)
class WholeRecordingTime extends MachineTest {
  @Test
  void theWholeThing() throws Exception {
    com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "processor", "Generated");
    RzxSession session = RzxSession.open(model.harness.TestFiles.testFile("/rzx/jsw-full.rzx"));
    session.getSpeccy().picture.active = false;
    session.getSpeccy().sound.output.enabled = false;
    RzxPlayback playback = session.getPlayback();
    long start = System.nanoTime();
    int frames = 0;
    while (session.playFrame()) frames++;
    double seconds = (System.nanoTime() - start) / 1e9;
    System.out.printf("TOTAL %d frames, %d instrucciones, %.2f s, %.1f MIPS%n",
        frames, playback.getInstructions(), seconds, playback.getInstructions() / seconds / 1e6);
    session.release();
    com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "processor", null);
  }
}
