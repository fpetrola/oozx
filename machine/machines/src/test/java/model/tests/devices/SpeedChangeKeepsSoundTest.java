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

package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: rebuilding the output synth on a speed change used to also drop and never
 * recreate the sound sources themselves (now owned by the ULA/peripherals, not the mixer),
 * silencing the machine whenever turbo mode was toggled.
 */
class SpeedChangeKeepsSoundTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void play(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private int aNoteOn(Speccy speccy, Loudest listener) {
    listener.peak = 0;
    for (int edge = 0; edge < 40; edge++) {
      speccy.zxClock.setTStates(edge * 800);
      speccy.ports.write(0x00FE, (byte) ((edge & 1) == 0 ? 0x10 : 0x00));
    }
    speccy.sound.frame();
    return listener.peak;
  }

  @Test
  void theSpeakerSurvivesTurboAndComingBack() {
    Loudest listener = new Loudest();
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).toInstance(listener));
    speccy.init();
    speccy.picture.active = false;
    speccy.sound.output.enabled = true;

    assertTrue(aNoteOn(speccy, listener) > 0, "it was not making a noise to begin with");

    speccy.timer.changeSpeed(15000);
    assertTrue(aNoteOn(speccy, listener) > 0, "turbo silenced the speaker");

    speccy.timer.changeSpeed(100);
    assertTrue(aNoteOn(speccy, listener) > 0, "coming back from turbo silenced the speaker");
  }

  /**
   * Regression: the T-state-to-sample conversion factor (rounded to 1/65536 of a sample)
   * disagreed with a naive floating-point truncation at high turbo speeds (e.g. 2.7525
   * samples/frame), leaking a fraction of a sample per frame until, ~180000 frames later, the
   * buffer overflowed with no indication of the original cause.
   */
  @Test
  void aFrameNeverPutsInMoreThanOneTakesOut() {
    BlipBuffer buffer = new BlipBuffer();
    buffer.setSampleRate(44100, 1000);
    // Uses the +2A's (longer) frame length, across every speed the UI actually offers.
    for (int speed : new int[]{25, 50, 100, 200, 1000, 5000, 10000, 15000, 20000, 30000}) {
      long clockRate = 3500000L * speed / 100 * 2;
      int asked = BlipBuffer.samplesInAFrame(44100, clockRate, 70908);
      buffer.clockRate(clockRate);
      buffer.clear(true);
      for (int frame = 0; frame < 500; frame++) {
        buffer.endFrame(70908);
        assertTrue(buffer.samplesAvail() <= asked,
            "at " + speed + "% a frame left " + (buffer.samplesAvail() - asked)
                + " samples behind, and they add up until the buffer is full");
        buffer.removeSamples(Math.min(buffer.samplesAvail(), asked));
      }
    }
  }
}
