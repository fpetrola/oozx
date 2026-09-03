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
package model.tests.regression;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Frames per second of the machine's own loop on whichever core -Doozx.cpu picks: mvn test -pl machine/core -Doozx.measure=true -Dtest=LoopCoreMeasurement. */
@EnabledIfSystemProperty(named = "oozx.measure", matches = "true")
class LoopCoreMeasurement {
  @Test
  void framesPerSecondOfTheOwnLoop() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(), binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    runFrames(speccy, 500);
    for (int round = 0; round < 3; round++) {
      long start = System.nanoTime();
      runFrames(speccy, 3000);
      double seconds = (System.nanoTime() - start) / 1e9;
      System.out.printf("loop core=%s frames=3000 seconds=%.2f fps=%.0f speed=%.0f%%%n", System.getProperty("oozx.cpu", "oop"), seconds, 3000 / seconds, 3000 / seconds / 50 * 100);
    }
  }

  private void runFrames(Speccy speccy, int frames) {
    long previous = speccy.zxClock.getTStates();
    int seen = 0;
    while (seen < frames) {
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
      long now = speccy.zxClock.getTStates();
      if (now < previous) seen++;
      previous = now;
    }
  }
}
