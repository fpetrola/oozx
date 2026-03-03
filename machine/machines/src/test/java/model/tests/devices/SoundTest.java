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
import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.sound.AudioOutput;
import com.fpetrola.oozx.speccy.modules.sound.AudioSource;
import com.fpetrola.oozx.speccy.modules.sound.Colouring;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mixer-level sound facts from the prototypes/tdd derivation not already covered by the
 * beeper, chip or speed tests individually.
 */
class SoundTest {

  /** Fake output device recording every call made to it, for assertions. */
  static class Listening extends SilentSoundDevice {
    int[] heard = new int[0];
    int frames;
    int opens;
    boolean open;
    boolean dropsWhenAhead;

    public int open(String device, int[] freq, int[] stereo) {
      open = true;
      opens++;
      return 0;
    }

    public boolean isOpen() {
      return open;
    }

    public void close() {
      open = false;
    }

    public void dropWhenAhead(boolean drop) {
      dropsWhenAhead = drop;
    }

    public void play(int[] samples, int count) {
      heard = Arrays.copyOf(samples, count);
      frames++;
    }
  }

  /** Minimal AudioSource: a single level change at a given T-state, at a fixed volume. */
  static class Level implements AudioSource {
    private final int volume;
    private final boolean flat;
    BlipSynth synth;
    private int[] scratch;

    /** Takes its output target in the constructor, matching how real sources are wired. */
    Level(AudioOutput output, int volume, boolean flat) {
      this.volume = volume;
      this.flat = flat;
      takeOutputFrom(output);
    }

    public void takeOutputFrom(AudioOutput output) {
      synth = flat ? output.newFlatSynth(volume) : output.newSynth(volume);
      scratch = new int[output.frameSize() * 2];
    }

    public void endFrame(int frameTstates) {
      synth.endFrame(frameTstates);
    }

    public int mixInto(int[] samples, int frames) {
      int count = synth.readSamples(scratch, frames, true);
      for (int i = 0; i < count; i++) {
        samples[i * 2] += scratch[i * 2];
        samples[i * 2 + 1] += scratch[i * 2];
      }
      return count;
    }
  }

  private final Listening card = new Listening();
  private final Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
      binder -> binder.bind(SoundCard.class).toInstance(card));

  /** Configures a 48K running at real-time speed, needed for frame sizes big enough to assert on. */
  private Speccy spectrum48() {
    speccy.speed.emulation = Speed.REAL_TIME;
    speccy.init();
    speccy.picture.active = false;
    speccy.sound.output.enabled = true;
    return speccy;
  }

  private void on(Class<? extends Spectrum> model) {
    speccy.machine.select(speccy.machine.model(model));
  }

  private static int peak(int[] samples, int fromSlot, int toSlot) {
    int peak = 0;
    for (int i = fromSlot; i < toSlot; i++) peak = Math.max(peak, Math.abs(samples[i]));
    return peak;
  }

  /** 69888 T-states at 3.5MHz maps to 880-and-a-fraction 44.1kHz samples, rounded up to 881. */
  @Test
  void aFrameOfOutputIsTheFramesTStatesAtTheCardsRate() {
    spectrum48();
    assertEquals(881, speccy.sound.frameSize());
  }

  /** With no source active, one silent stereo frame still reaches the card each frame. */
  @Test
  void nothingPlayingIsAFrameOfSilenceHandedToTheCard() {
    spectrum48();
    speccy.sound.frame();

    assertEquals(1, card.frames);
    assertTrue(card.heard.length >= (speccy.sound.frameSize() - 1) * 2, "left and right, interleaved: " + card.heard.length);
    assertTrue(Arrays.stream(card.heard).allMatch(sample -> sample == 0));
  }

  /** T-state 35000 falls in sample 441, where the level change should first appear. */
  @Test
  void aLevelIsHeardFromTheSampleItsTStateFallsIn() {
    spectrum48();
    Level level = speccy.sound.add(new Level(speccy.sound, 100, true));

    level.synth.update(35000, 10000);
    speccy.sound.frame();

    assertTrue(peak(card.heard, 0, 430 * 2) < 200, "silent up to it, but for the synth's ringing");
    assertTrue(peak(card.heard, 445 * 2, 460 * 2) > 5000, "and there from then on");
  }

  /** Tripling the speed should shrink the frame's sample count to a third. */
  @Test
  void atAnotherSpeedTheFrameIsThatManyFewerSamples() {
    spectrum48();
    int atRealTime = speccy.sound.frameSize();

    speccy.timer.changeSpeed(300);

    assertTrue(Math.abs(speccy.sound.frameSize() * 3 - atRealTime) <= 3, "a third of " + atRealTime + ", rounded up");
  }

  /** Switching machines drops the previous machine's sources and hears its speaker once, not twice. */
  @Test
  void aNewMachineBringsItsOwnSources() {
    spectrum48();
    Level left = speccy.sound.add(new Level(speccy.sound, 100, true));
    on(Spec128.class);
    on(Spec48.class);
    speccy.sound.output.enabled = true;

    left.synth.update(0, 10000);
    for (int edge = 0; edge < 40; edge++) {
      speccy.zxClock.setTStates(edge * 800);
      speccy.ports.write(0x00FE, (byte) ((edge & 1) == 0 ? 0x10 : 0x00));
    }
    speccy.sound.frame();
    int once = peak(card.heard, 0, card.heard.length);

    assertTrue(once > 0, "the speaker is heard on the new machine");
    assertTrue(once < 2 * 12800, "once, and without the source the old machine had");
  }

  /** Pause releases the card; unpause reopens it and sound resumes normally. */
  @Test
  void pausedAndUnpausedTheMachineIsStillHeard() {
    spectrum48();
    speccy.sound.pause();
    assertFalse(card.open, "let go of");

    speccy.sound.unpause();
    for (int edge = 0; edge < 40; edge++) {
      speccy.zxClock.setTStates(edge * 800);
      speccy.ports.write(0x00FE, (byte) ((edge & 1) == 0 ? 0x10 : 0x00));
    }
    speccy.sound.frame();

    assertTrue(card.open);
    assertTrue(peak(card.heard, 0, card.heard.length) > 0, "the speaker was not heard after unpausing");
  }

  /** A removed source contributes nothing further to the mix. */
  @Test
  void aSourceUnpluggedIsNotHeard() {
    spectrum48();
    Level level = speccy.sound.add(new Level(speccy.sound, 100, true));

    speccy.sound.remove(level);
    level.synth.update(0, 10000);
    speccy.sound.frame();

    assertEquals(0, peak(card.heard, 0, card.heard.length));
  }

  /** Per-source volume scales linearly: quarter volume should peak at about a quarter. */
  @Test
  void eachSourceHasItsOwnLoudness() {
    spectrum48();
    Level loud = speccy.sound.add(new Level(speccy.sound, 100, true));
    loud.synth.update(35000, 10000);
    speccy.sound.frame();
    int full = peak(card.heard, 0, card.heard.length);
    speccy.sound.remove(loud);

    Level quiet = speccy.sound.add(new Level(speccy.sound, 25, true));
    quiet.synth.update(35000, 10000);
    speccy.sound.frame();
    int quarter = peak(card.heard, 0, card.heard.length);

    assertTrue(full > 0);
    assertTrue(Math.abs(quarter - full / 4) <= full / 50, "at a quarter the peak was " + quarter + " of " + full);
  }

  /** A write timestamped slightly past frame-end (~400 T-states, worst-case one instruction)
   * is not dropped; it appears in the following frame instead. */
  @Test
  void aWritePastTheEndOfTheFrameIsInTheNext() {
    spectrum48();
    Level level = speccy.sound.add(new Level(speccy.sound, 100, true));

    level.synth.update(69888 + 400, 10000);
    speccy.sound.frame();
    assertEquals(0, peak(card.heard, 0, card.heard.length), "not in this frame");

    speccy.sound.frame();
    assertTrue(peak(card.heard, 0, card.heard.length) > 5000, "in the next");
  }

  /** Card blocks the machine below real-time speed but is told to drop samples above it. */
  @Test
  void belowRealTimeTheCardHoldsTheMachineAndAboveItDropsWhatItHasNoRoomFor() {
    spectrum48();
    assertFalse(card.dropsWhenAhead, "at real time it waits");

    speccy.timer.changeSpeed(50);
    assertFalse(card.dropsWhenAhead, "and below it");

    speccy.timer.changeSpeed(300);
    assertTrue(card.dropsWhenAhead, "above it, it drops");
  }

  /** Reopening the card is tied to machine changes, not to changes of emulation speed. */
  @Test
  void aChangeOfSpeedDoesNotReopenTheCardAndANewMachineDoes() {
    spectrum48();
    int opens = card.opens;

    speccy.timer.changeSpeed(300);
    speccy.timer.changeSpeed(100);
    assertEquals(opens, card.opens, "the same line, whatever the speed");

    on(Spec128.class);
    assertEquals(opens + 1, card.opens);
  }

  /** Muted output still drains sources each frame, so nothing backs up and bursts once unmuted. */
  @Test
  void withTheOutputOffTheCardHearsNothingAndTheSourcesAreStillEmptied() {
    spectrum48();
    Level level = speccy.sound.add(new Level(speccy.sound, 100, true));
    speccy.sound.output.enabled = false;

    level.synth.update(35000, 10000);
    speccy.sound.frame();
    assertEquals(0, card.frames, "nothing reached the card");

    speccy.sound.output.enabled = true;
    speccy.sound.frame();
    assertTrue(peak(card.heard, 0, card.heard.length) < 200, "no step from the frame that was not heard");
  }

  /** Speaker choice changes the tone filter applied; a flat (DAC) synth bypasses it entirely. */
  @Test
  void theSpeakerColoursWhatGoesThroughItAndADacHasNone() {
    spectrum48();

    assertEquals(new Colouring(200, -37.0), speccy.sound.newSynth(100).colouring(), "the speaker in the case");
    speccy.sound.speaker(Sound.Speakers.LARGE_TV);
    assertEquals(new Colouring(1000, -67.0), speccy.sound.newSynth(100).colouring());
    assertEquals(new Colouring(1000, 0.0), speccy.sound.newFlatSynth(100).colouring(), "a DAC keeps only the bass cut");
  }

  /** A 128K's frame is also a fiftieth of a second, just at its own clock rate: 882 samples. */
  @Test
  void theFrameIsSizedForTheMachinesOwnClock() {
    spectrum48();
    on(Spec128.class);
    assertEquals(882, speccy.sound.frameSize());
  }
}
