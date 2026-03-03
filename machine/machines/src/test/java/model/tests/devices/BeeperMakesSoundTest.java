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
import com.fpetrola.oozx.speccy.modules.timer.Speed;
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
    int left;
    int right;
    int[] samples = new int[0];

    public void play(int[] data, int length) {
      samples = data.clone();
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
        if ((i & 1) == 0) {
          left = Math.max(left, Math.abs(data[i]));
        } else {
          right = Math.max(right, Math.abs(data[i]));
        }
      }
    }
  }

  private Loudest listened;

  private int peakOf(String model, boolean flapTheSpeaker) {
    Loudest listener = new Loudest();
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).toInstance(listener));
    // A frame of audio is sized for the speed, and what ships is twenty thousand per cent, where
    // a frame is three samples and nothing can be heard in it, let alone measured.
    speccy.speed.emulation = Speed.REAL_TIME;
    speccy.init();
    speccy.picture.active = false;
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getClass().getSimpleName().equals(model))
        .findFirst().ifPresent(type -> {
          speccy.machine.selectDefault();
          speccy.machine.select(type);
        });
    speccy.sound.output.enabled = true;

    if (flapTheSpeaker) {
      // A square wave by hand, written to the port a program writes to: bit 4 of 0xFE is the
      // speaker. It reaches the beeper through the ULA, which is what owns one, and there is no
      // other way in - which is the point of it living there.
      for (int edge = 0; edge < 40; edge++) {
        speccy.zxClock.setTStates(edge * 800);
        speccy.ports.write(0x00FE, (byte) ((edge & 1) == 0 ? 0x10 : 0x00));
      }
    }

    speccy.sound.frame();
    listened = listener;
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

  /**
   * Both ears, written by the source rather than copied by the mixer afterwards.
   * <p>
   * The mix used to be made in mono and duplicated across at the end, which no source could
   * escape - and placing an AY's channels left and right is exactly a source needing to. It is
   * interleaved now, and a source that sounds the same in both says so by writing both.
   */
  @Test
  void reachesBothEars() {
    peakOf("Spec48", true);
    assertTrue(listened.left > 0, "nothing in the left channel");
    assertEquals(listened.left, listened.right, "one speaker should reach both ears equally");
  }

  @Test
  void andSaysNothingWhenItIsNotTouched() {
    assertEquals(0, peakOf("Spec48", false),
        "silence should be silent, or the other two prove nothing");
  }

  /**
   * How far the speaker swings while a square wave is driven between two values of the port,
   * measured in the middle of the frame: the first write steps the speaker out of silence, and
   * that step is still dying away at the start.
   */
  private int swingOf(int low, int high) {
    Loudest listener = new Loudest();
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).toInstance(listener));
    speccy.speed.emulation = Speed.REAL_TIME;
    speccy.init();
    speccy.picture.active = false;
    speccy.sound.output.enabled = true;
    speccy.zxClock.setTStates(0);
    speccy.ports.write(0x00FE, (byte) low);
    for (int edge = 0; edge < 40; edge++) {
      speccy.zxClock.setTStates(8000 + edge * 800);
      speccy.ports.write(0x00FE, (byte) ((edge & 1) == 0 ? high : low));
    }
    speccy.sound.frame();
    int frame = speccy.machine.current.getTimings().tstatesPerFrame();
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (int i = (int) (20000L * speccy.sound.frameSize() / frame); i < (int) (38000L * speccy.sound.frameSize() / frame); i++) {
      min = Math.min(min, listener.samples[i * 2]);
      max = Math.max(max, listener.samples[i * 2]);
    }
    return max - min;
  }

  /**
   * Bit 4 of the port is the speaker and bit 3 is what goes out to the tape, and both drive the
   * one speaker a Spectrum has: what the tape bit adds is a twenty-fifth of what the speaker bit
   * does, read off the speaker rather than out of the table.
   * <p>
   * Both waves start from silence, so that the step out of it is the same in each and what is
   * left is the difference between them.
   */
  @Test
  void theTapeBitAddsATwentyFifthOfWhatTheSpeakerBitDoes() {
    int speaker = swingOf(0x08, 0x18);
    int speakerAndTape = swingOf(0x08, 0x10);
    assertTrue(speakerAndTape > speaker, "the tape bit on top of the speaker should be louder, not quieter");
    assertEquals(1 / 25.0, (speakerAndTape - speaker) / (double) speaker, 0.001);
  }

  /** With no tape playing there is nothing on the MIC line to hear, so that bit alone moves nothing. */
  @Test
  void theTapeBitAloneMovesNothingWhileNoTapeIsPlaying() {
    assertEquals(0, swingOf(0x08, 0x00));
  }
}
