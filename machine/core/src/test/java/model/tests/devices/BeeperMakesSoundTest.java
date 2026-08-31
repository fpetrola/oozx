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
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The speaker every Spectrum has, now that it is a thing of its own.
 * <p>
 * It moved out of Sound behind AudioSource, and it is the source that has to keep working on a
 * machine with no sound chip at all - which is most of them, and every one of these tests.
 */
class BeeperMakesSoundTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void sound_lowlevel_frame(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private int peakOf(String model, boolean flapTheSpeaker) {
    OOSpectrumConnector.noTest = true;
    Loudest listener = new Loudest();
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).toInstance(listener));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getClass().getSimpleName().equals(model))
        .findFirst().ifPresent(type -> {
          speccy.machine.selectDefault();
          speccy.machine.select(type);
        });
    speccy.settings.current.sound = true;

    if (flapTheSpeaker) {
      // A square wave by hand: the speaker bit up and down across the frame, which is all a
      // program does to make a note on a machine that has nothing else.
      for (int edge = 0; edge < 40; edge++) {
        speccy.sound.beeper(edge * 800L, (edge & 1) == 0 ? 2 : 0, 0);
      }
    }

    speccy.sound.frame();
    return listener.peak;
  }

  @Test
  void aSpeakerFlappedIsHeard() {
    assertTrue(peakOf("Spec48", true) > 0,
        "the speaker was driven and not one sample came out of it");
  }

  /** The one every machine has, so a 128K is not a different answer. */
  @Test
  void andOnAOneTwentyEightToo() {
    assertTrue(peakOf("Spec128", true) > 0);
  }

  @Test
  void andSaysNothingWhenItIsNotTouched() {
    assertEquals(0, peakOf("Spec48", false),
        "silence should be silent, or the other two prove nothing");
  }
}
