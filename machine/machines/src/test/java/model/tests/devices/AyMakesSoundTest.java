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

package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for three chained AY bugs: synthesis disabled at its call site, a channel
 * step writing its result to a dead local, and a tone toggle implemented as negation (which
 * leaves zero at zero). All three silenced a 128K regardless of what was written to it.
 */
class AyMakesSoundTest {

  /** Records only the largest sample magnitude seen; never reports itself as open. */
  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void play(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  /** Builds and boots the named machine model with sound routed to the given fake device. */
  private Speccy machine(String model, Loudest listener) {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).toInstance(listener));
    speccy.init();
    speccy.picture.active = false;
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getClass().getSimpleName().equals(model))
        .findFirst().ifPresent(type -> {
          speccy.machine.selectDefault();
          speccy.machine.select(type);
        });
    speccy.sound.output.enabled = true;
    return speccy;
  }

  private int peakOf(String model, boolean playANote) {
    Loudest listener = new Loudest();
    Speccy speccy = machine(model, listener);

    if (playANote) {
      write(speccy, 0, 0x50);   // R0: tone A period, low byte
      write(speccy, 1, 0x01);   // R1: tone A period, high byte - clearly audible
      write(speccy, 8, 0x0F);   // R8: channel A volume, maximum
      write(speccy, 7, 0x3E);   // R7: mixer, tone A enabled, all else muted
    }

    speccy.sound.frame();
    return listener.peak;
  }

  private void write(Speccy speccy, int register, int value) {
    speccy.ports.write(0xFFFD, (byte) register);
    speccy.ports.write(0xBFFD, (byte) value);
  }

  @Test
  void aOneTwentyEightPlayingANoteIsHeard() {
    assertTrue(peakOf("Spec128", true) > 0,
        "the chip was set going and not one sample came out of it");
  }

  /** A 48K has no AY peripheral, so writes to its ports reach nothing and produce no sound. */
  @Test
  void aFortyEightHasNoChipToWriteTo() {
    assertEquals(0, peakOf("Spec48", true),
        "a 48K produced sound chip output, which a 48K cannot make");
    assertTrue(peakOf("Spec128", true) > 0,
        "and the same writes on a 128K have to be heard, or this proves nothing");
  }

  /**
   * Loading a snapshot can change the active machine's frame length (+2A is 1020 T-states longer
   * than 48K) after the sound buffer was sized for the old one; each frame then leaks a few
   * samples until, thousands of frames later, the buffer overflows far from the real cause.
   */
  @Test
  void theOutputFollowsAMachineWhoseFrameChangedLength() {
    Speccy speccy = machine("Spec128", new Loudest());
    // Real time is the only speed with frames long enough for two frame lengths to differ.
    speccy.speed.emulation = 100;
    // 1020 T-states is the 48K/+2A frame-length gap; subtracted generically so the test is
    // about mismatched lengths, not about these specific models.
    int machinesFrame = speccy.machine.current.getTimings().tstatesPerFrame();
    speccy.sound.sizeFor(machinesFrame - 1020);
    int sizedForTheOtherFrame = speccy.sound.frameSize();

    speccy.sound.frame();

    assertNotEquals(sizedForTheOtherFrame, speccy.sound.frameSize(),
        "the output kept a size worked out for a frame 1020 T-states shorter than the machine's");
    // A second's worth of frames is enough to reach where the old leak used to overflow.
    for (int frame = 0; frame < 8000; frame++) {
      speccy.sound.frame();
    }
  }

  /** Master volume scales the final mix linearly: half volume should halve the observed peak. */
  @Test
  void theMasterVolumeScalesWhatComesOut() {
    int full = peakOf("Spec128", true);
    Loudest listener = new Loudest();
    Speccy speccy = machine("Spec128", listener);
    speccy.sound.setVolume(50);
    write(speccy, 0, 0x50);
    write(speccy, 1, 0x01);
    write(speccy, 8, 0x0F);
    write(speccy, 7, 0x3E);
    speccy.sound.frame();

    assertTrue(full > 0, "it was not making a noise to begin with");
    assertTrue(Math.abs(listener.peak - full / 2) <= 1, "at half volume the peak was " + listener.peak + " of " + full);
  }

  @Test
  void andSaysNothingWhenNothingIsPlaying() {
    assertEquals(0, peakOf("Spec128", false),
        "silence should be silent, or the previous test proves nothing");
  }
}
