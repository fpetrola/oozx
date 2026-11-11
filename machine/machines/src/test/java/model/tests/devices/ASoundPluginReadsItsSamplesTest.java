package model.tests.devices;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.blip.BlipSynth;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

/**
 * The AY plugin reads its synth with the three-argument readSamples it was built against, asking for stereo. That
 * read stays, and gives what the two-argument one gives.
 */
class ASoundPluginReadsItsSamplesTest {
  @Test
  @SuppressWarnings("deprecation")
  void theReadThePublishedPluginsMakeGivesTheSameSamples() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    BlipSynth asThePluginReads = played(speccy.sound.newSynth(100));
    BlipSynth asTheMachineReads = played(speccy.sound.newSynth(100));
    int[] plugin = new int[4096];
    int[] machine = new int[4096];

    int count = asThePluginReads.readSamples(plugin, 1024, true);

    assertEquals(count, asTheMachineReads.readSamples(machine, 1024));
    assertArrayEquals(machine, plugin);
  }

  private static BlipSynth played(BlipSynth synth) {
    for (int t = 0; t < 69888; t += 2000) synth.update(t, (t / 2000) % 2 == 0 ? 8000 : -8000);
    synth.endFrame(69888);
    return synth;
  }
}
