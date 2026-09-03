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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the sound chip makes a sound.
 * <p>
 * Every register write was arriving and being queued, and the synthesis that turns a queue of
 * writes into samples was commented out at its one call site - so a 128K machine took the music
 * and produced silence. Behind that were two more: the routine that advances a channel wrote its
 * answer into its own parameter, which in C is a pointer and in Java is a local, and the square
 * wave was flipped by negating it, which leaves nought as nought.
 * <p>
 * So this asks the only question that matters about all three: set a channel going, and does
 * anything come out.
 */
class AyMakesSoundTest {

  /** Keeps the loudest sample it is handed, and opens nothing. */
  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void sound_lowlevel_frame(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  /** A machine of the named model, its sound on and heard by the given listener. */
  private Speccy machine(String model, Loudest listener) {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).toInstance(listener));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getClass().getSimpleName().equals(model))
        .findFirst().ifPresent(type -> {
          speccy.machine.selectDefault();
          speccy.machine.select(type);
        });
    speccy.settings.current.sound = true;
    return speccy;
  }

  private int peakOf(String model, boolean playANote) {
    Loudest listener = new Loudest();
    Speccy speccy = machine(model, listener);

    if (playANote) {
      write(speccy, 0, 0x50);   // channel A period, low byte
      write(speccy, 1, 0x01);   // and high, so it is well inside hearing
      write(speccy, 8, 0x0F);   // channel A at full volume
      write(speccy, 7, 0x3E);   // mixer: tone A on, everything else off
    }

    speccy.sound.frame();
    return listener.peak;
  }

  private void write(Speccy speccy, int register, int value) {
    speccy.peripherals.writePort(0xFFFD, (byte) register);
    speccy.peripherals.writePort(0xBFFD, (byte) value);
  }

  @Test
  void aOneTwentyEightPlayingANoteIsHeard() {
    assertTrue(peakOf("Spec128", true) > 0,
        "the chip was set going and not one sample came out of it");
  }

  /**
   * A 48K does not run a sound chip it has not got.
   * <p>
   * The chip lives in the peripheral now, so there is no longer a way to write to one that is not
   * there - which is the answer rather than an obstacle to asking the question. The same writes
   * that make a 128K sing reach nothing on a 48K, and a 48K synthesises nothing.
   */
  @Test
  void aFortyEightHasNoChipToWriteTo() {
    assertEquals(0, peakOf("Spec48", true),
        "a 48K produced sound chip output, which a 48K cannot make");
    assertTrue(peakOf("Spec128", true) > 0,
        "and the same writes on a 128K have to be heard, or this proves nothing");
  }

  /**
   * The output is built for the machine as it is, not as it was when the sound started.
   * <p>
   * A source is handed a frame's worth of T-states and asked for a frame's worth of samples
   * back, and the size of that answer was worked out for whatever the machine was when the
   * output was last built. Let the two be different frames - a +2A's is 1020 T-states longer
   * than a 48K's, and loading a recording's snapshot makes a machine one of those after init
   * sized the sound for the other - and every frame leaves six samples behind. A second of them
   * fills the buffer and comes out as an ArrayIndexOutOfBounds from the middle of the mix, seven
   * thousand frames after the cause, which is what made it hard to place.
   */
  @Test
  void theOutputFollowsAMachineWhoseFrameChangedLength() {
    Speccy speccy = machine("Spec128", new Loudest());
    // At real time, which is the speed a replay runs at and the only speed where a frame of
    // audio is big enough for two frame lengths to ask for different sizes at all.
    speccy.settings.current.emulationSpeed = 100;
    // On this machine, and sized for a frame 1020 T-states shorter than its own - the distance
    // between a 48K's frame and a +2A's, which is the pair this happens with. Taken off
    // whatever this machine's frame is, so the test is about two lengths disagreeing rather
    // than about which models they came from.
    speccy.sound.machineChanged(speccy.machine.current);
    int machinesFrame = speccy.machine.current.getTimings().tstatesPerFrame;
    speccy.sound.initSound(3500000, machinesFrame - 1020, true, false);
    int sizedForTheOtherFrame = speccy.sound.frameSize();

    speccy.sound.frame();

    assertNotEquals(sizedForTheOtherFrame, speccy.sound.frameSize(),
        "the output kept a size worked out for a frame 1020 T-states shorter than the machine's");
    // And it goes on playing: a second of audio is where the leak used to overflow.
    for (int frame = 0; frame < 8000; frame++) {
      speccy.sound.frame();
    }
  }

  @Test
  void andSaysNothingWhenNothingIsPlaying() {
    assertEquals(0, peakOf("Spec128", false),
        "silence should be silent, or the previous test proves nothing");
  }
}
