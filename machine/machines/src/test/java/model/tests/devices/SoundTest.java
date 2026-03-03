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
 * How a Spectrum's noises reach the card: the facts the derivation in prototypes/tdd earned that
 * the beeper's, the chip's and the speed's tests did not already state, against the real mixer.
 */
class SoundTest {

  /** A card that keeps what it was handed and how it was treated, so a test can listen to it. */
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

  /** A source that is a level set at a T-state, as loud as it says, through the speaker or not. */
  static class Level implements AudioSource {
    private final int volume;
    private final boolean flat;
    BlipSynth synth;
    private int[] scratch;

    /** Handed its output on the way in, as the beeper and the DAC are: the mixer's add hands out nothing. */
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

  /** A 48K at real time, which is the only speed where a frame of audio is big enough to be measured. */
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

  /**
   * A frame of output is as many samples as the frame's T-states take at the card's rate: a
   * 48K's 69888 at 3.5MHz is 880 and a fraction of them at 44.1kHz, rounded up so that whatever
   * a frame puts in a frame takes out.
   */
  @Test
  void aFrameOfOutputIsTheFramesTStatesAtTheCardsRate() {
    spectrum48();
    assertEquals(881, speccy.sound.frameSize());
  }

  /** Nothing playing is a frame of silence, in both ears, handed to the card once a frame. */
  @Test
  void nothingPlayingIsAFrameOfSilenceHandedToTheCard() {
    spectrum48();
    speccy.sound.frame();

    assertEquals(1, card.frames);
    assertTrue(card.heard.length >= (speccy.sound.frameSize() - 1) * 2, "left and right, interleaved: " + card.heard.length);
    assertTrue(Arrays.stream(card.heard).allMatch(sample -> sample == 0));
  }

  /** A level set at a T-state is heard from the sample that T-state falls in: 35000 T-states is sample 441. */
  @Test
  void aLevelIsHeardFromTheSampleItsTStateFallsIn() {
    spectrum48();
    Level level = speccy.sound.add(new Level(speccy.sound, 100, true));

    level.synth.update(35000, 10000);
    speccy.sound.frame();

    assertTrue(peak(card.heard, 0, 430 * 2) < 200, "silent up to it, but for the synth's ringing");
    assertTrue(peak(card.heard, 445 * 2, 460 * 2) > 5000, "and there from then on");
  }

  /** At three times real time the same frame is a third as many samples, so a note comes out three times as high. */
  @Test
  void atAnotherSpeedTheFrameIsThatManyFewerSamples() {
    spectrum48();
    int atRealTime = speccy.sound.frameSize();

    speccy.timer.changeSpeed(300);

    assertTrue(Math.abs(speccy.sound.frameSize() * 3 - atRealTime) <= 3, "a third of " + atRealTime + ", rounded up");
  }

  /** A new machine brings its own sources: the speaker is heard once on it, not twice, and what the old one had is gone. */
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

  /** Paused, the card is let go of; asked for again, the machine is heard as before. */
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

  /** A source unplugged is not heard: the mix is of what is on the machine now. */
  @Test
  void aSourceUnpluggedIsNotHeard() {
    spectrum48();
    Level level = speccy.sound.add(new Level(speccy.sound, 100, true));

    speccy.sound.remove(level);
    level.synth.update(0, 10000);
    speccy.sound.frame();

    assertEquals(0, peak(card.heard, 0, card.heard.length));
  }

  /** Each source has a loudness of its own under the master's: one at a quarter peaks at a quarter of one at full. */
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

  /**
   * A write can land past the end of the frame: a source is handed the clock as it stands, and
   * the clock passes the frame's end before whoever drives it takes a frame off. It is not lost
   * and not in this frame: it is in the next. The synth keeps a margin of a few samples past the
   * end for it - some four hundred T-states, where the clock stands at most an instruction past.
   */
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

  /** Below real time the card, waiting for room, holds the machine; above it, it is told to drop what it has no room for. */
  @Test
  void belowRealTimeTheCardHoldsTheMachineAndAboveItDropsWhatItHasNoRoomFor() {
    spectrum48();
    assertFalse(card.dropsWhenAhead, "at real time it waits");

    speccy.timer.changeSpeed(50);
    assertFalse(card.dropsWhenAhead, "and below it");

    speccy.timer.changeSpeed(300);
    assertTrue(card.dropsWhenAhead, "above it, it drops");
  }

  /** The card is opened for a machine, not for a speed: a speed slid along its slider does not reopen it, a new machine does. */
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

  /** With the output off the card hears nothing, but the sources are still emptied every frame, or what they made comes out later all at once. */
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

  /**
   * A speaker colours what goes through it, and which speaker decides how: the small one in the
   * case, or a television's. A DAC has no speaker in front of it and goes to the mix as it is.
   */
  @Test
  void theSpeakerColoursWhatGoesThroughItAndADacHasNone() {
    spectrum48();

    assertEquals(new Colouring(200, -37.0), speccy.sound.newSynth(100).colouring(), "the speaker in the case");
    speccy.sound.speaker(Sound.Speakers.LARGE_TV);
    assertEquals(new Colouring(1000, -67.0), speccy.sound.newSynth(100).colouring());
    assertEquals(new Colouring(1000, 0.0), speccy.sound.newFlatSynth(100).colouring(), "a DAC keeps only the bass cut");
  }

  /** The frame is sized for the machine's own clock: a 128's is a fiftieth of a second like the 48K's, 882 samples. */
  @Test
  void theFrameIsSizedForTheMachinesOwnClock() {
    spectrum48();
    on(Spec128.class);
    assertEquals(882, speccy.sound.frameSize());
  }
}
