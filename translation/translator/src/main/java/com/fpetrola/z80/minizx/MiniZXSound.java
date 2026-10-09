package com.fpetrola.z80.minizx;

import com.fpetrola.oozx.speccy.devices.ula.Beeper;
import com.fpetrola.oozx.speccy.modules.sound.AudioOutput;
import com.fpetrola.oozx.speccy.modules.sound.Colouring;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipBuffer;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;

import java.util.Arrays;

/** The emulator's beeper fed by the translated game's OUTs, timed by its T-states, and emptied into a sound card at every frame. */
public class MiniZXSound implements AudioOutput {
  private static final int SAMPLE_RATE = 44100, CLOCK = 3_500_000, FRAME_TSTATES = 69888;
  private static final Colouring SMALL_SPEAKER = new Colouring(200, -37.0);
  private final SoundCard card;
  private final Beeper beeper;
  private final int frameSize;
  private final int[] mix;
  private long frameStart;

  public MiniZXSound(SoundCard card) {
    this.card = card;
    frameSize = BlipBuffer.samplesInAFrame(SAMPLE_RATE, CLOCK, FRAME_TSTATES);
    mix = new int[frameSize * 2];
    card.open(null, new int[]{SAMPLE_RATE}, new int[]{1});
    Sound.Output output = new Sound.Output();
    output.enabled = true;
    beeper = new Beeper(this, () -> false, output);
  }

  public BlipSynth newSynth(int volumePercent) {
    return new BlipSynth(BlipBuffer.BLIP_HIGH_QUALITY, SAMPLE_RATE, holdsAFrame(), CLOCK, SMALL_SPEAKER, volumePercent / 100.0);
  }

  public BlipSynth newFlatSynth(int volumePercent) {
    return new BlipSynth(BlipBuffer.BLIP_HIGH_QUALITY, SAMPLE_RATE, holdsAFrame(), CLOCK, SMALL_SPEAKER.flat(), volumePercent / 100.0);
  }

  private int holdsAFrame() {
    return Math.max(1000, (int) Math.ceil(frameSize * 1000.0 / SAMPLE_RATE) + 100);
  }

  public int frameSize() {
    return frameSize;
  }

  public void out(long tstates, int port, int value) {
    if ((port & 1) == 0)
      beeper.write(tstates - frameStart, ((value & 0x10) != 0 ? 2 : 0) + ((value & 0x08) == 0 ? 1 : 0), false);
  }

  public void frame(long tstates) {
    beeper.endFrame((int) (tstates - frameStart));
    frameStart = tstates;
    Arrays.fill(mix, 0);
    card.play(mix, beeper.mixInto(mix, frameSize) * 2);
  }

  public void close() {
    beeper.close();
    card.close();
  }
}
