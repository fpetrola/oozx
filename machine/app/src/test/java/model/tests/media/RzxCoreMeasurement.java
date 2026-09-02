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
import com.fpetrola.z80.minizx.RzxPlayback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;

/** Numbers for a recording on whichever core -Doozx.cpu picks: mvn test -pl machine/app -Doozx.measure=true -Dtest=RzxCoreMeasurement. */
@EnabledIfSystemProperty(named = "oozx.measure", matches = "true")
class RzxCoreMeasurement {
  @Test
  void framesAndTStatesPerRecordedFrame() throws Exception {
    Emulation.noTest = true;
    RzxSession session = RzxSession.open(model.harness.TestFiles.testFile("/rzx/jsw-full.rzx"));
    RzxPlayback playback = session.getPlayback();
    var clock = session.getSpeccy().z80.ooz80.getState().clock;
    long start = System.nanoTime();
    int machineFrames = 0;
    long tstates = 0;
    for (int frame = 0; frame < 6000; frame++) {
      int before = clock.getTStates();
      playback.playFrame();
      int elapsed = playback.takeElapsedMachineFrames();
      machineFrames += elapsed;
      tstates += (clock.getTStates() - before) + (long) elapsed * 69888;
    }
    double seconds = (System.nanoTime() - start) / 1e9;
    System.out.printf("core=%s recordedFrames=6000 machineFrames=%d tstatesPerRecordedFrame=%.1f instructions=%d seconds=%.2f fps=%.0f speed=%.0f%%%n",
        System.getProperty("oozx.cpu", "oop"), machineFrames, tstates / 6000.0, playback.getInstructions(), seconds, 6000 / seconds, 6000 / seconds / 50 * 100);
    session.getSpeccy().uiDisplay.active = false;
    session.getSpeccy().settings.current.sound = false;
    for (int round = 0; round < 2; round++) {
      start = System.nanoTime();
      for (int frame = 0; frame < 3000; frame++)
        session.playFrame();
      seconds = (System.nanoTime() - start) / 1e9;
      System.out.printf("session core=%s frames=3000 (with presentFrame, no sound) seconds=%.2f fps=%.0f speed=%.0f%%%n", System.getProperty("oozx.cpu", "oop"), seconds, 3000 / seconds, 3000 / seconds / 50 * 100);
    }
    session.release();
    var speccy = session.getSpeccy();
    for (int round = 0; round < 2; round++) {
      long previous = speccy.zxClock.getTStates();
      int seen = 0;
      start = System.nanoTime();
      while (seen < 3000) {
        speccy.z80.doOpcodes();
        speccy.eventManager.eventDoEvents();
        long now = speccy.zxClock.getTStates();
        if (now < previous) seen++;
        previous = now;
      }
      seconds = (System.nanoTime() - start) / 1e9;
      System.out.printf("own loop on JSW core=%s frames=3000 seconds=%.2f fps=%.0f speed=%.0f%%%n", System.getProperty("oozx.cpu", "oop"), seconds, 3000 / seconds, 3000 / seconds / 50 * 100);
    }
  }
}
