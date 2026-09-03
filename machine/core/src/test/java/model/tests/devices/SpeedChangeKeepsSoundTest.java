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
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.sound.blip.BlipBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That turning turbo on and off again leaves a machine still making a noise.
 * <p>
 * Changing speed rebuilds the output, because a synth is built for a speed and one built for the
 * old one plays at the wrong rate. Rebuilding it used to make the sources again as well; now they
 * are made by whatever owns them - the ULA takes a speaker, a peripheral takes a chip - and none
 * of that happens on a change of speed. So the rebuild emptied the mixer and the machine went
 * quiet, which is what the rocket button did.
 */
class SpeedChangeKeepsSoundTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void sound_lowlevel_frame(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private int aNoteOn(Speccy speccy, Loudest listener) {
    listener.peak = 0;
    for (int edge = 0; edge < 40; edge++) {
      speccy.zxClock.setTStates(edge * 800);
      speccy.peripherals.writePort(0x00FE, (byte) ((edge & 1) == 0 ? 0x10 : 0x00));
    }
    speccy.sound.frame();
    return listener.peak;
  }

  @Test
  void theSpeakerSurvivesTurboAndComingBack() {
    Emulation.noTest = true;
    Loudest listener = new Loudest();
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).toInstance(listener));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.settings.current.sound = true;

    assertTrue(aNoteOn(speccy, listener) > 0, "it was not making a noise to begin with");

    speccy.z80.changeSpeed(15000);
    assertTrue(aNoteOn(speccy, listener) > 0, "turbo silenced the speaker");

    speccy.z80.changeSpeed(100);
    assertTrue(aNoteOn(speccy, listener) > 0, "coming back from turbo silenced the speaker");
  }

  /**
   * And a source never keeps any of a frame back, at any speed.
   * <p>
   * A frame is put into a source's buffer as T-states and taken out as samples, and the two sums
   * have to agree to the sample: the buffer converts with a factor rounded to 1/65536 of a
   * sample, so at turbo, where a frame is 2.7525 samples, it rounds up to 3 and anyone emptying
   * the 2 that the same sum in floating point truncates to leaves a quarter of a sample behind
   * every frame. A hundred and eighty thousand frames later - fifteen seconds of a replay at the
   * speed one runs at now - the buffer is full and the mix throws
   * ArrayIndexOutOfBoundsException, with nothing in the stack to say which frame filled it.
   */
  @Test
  void aFrameNeverPutsInMoreThanOneTakesOut() {
    BlipBuffer buffer = new BlipBuffer();
    buffer.setSampleRate(44100, 1000);
    // A +2A's frame, the longer of the two, and the speeds the machine is actually run at:
    // real time, the rocket button, and the ones either side of them.
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
