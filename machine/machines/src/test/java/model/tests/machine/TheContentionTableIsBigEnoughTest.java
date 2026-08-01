package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.ula.ContentionTable;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contention tables are indexed by the clock, which runs past the end of a frame while a
 * recording plays, so they are longer than one frame - four of the longest a model has. Nothing
 * else says so: if a model with a longer frame is added, or the margin is cut, this is what tells.
 */
class TheContentionTableIsBigEnoughTest {
  @Test
  void everyModelsFrameFitsFourTimesOver() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();

    for (Spectrum model : speccy.machine.getMachineTypes()) {
      int frame = model.getTimings().tstatesPerFrame();
      assertTrue(frame <= ContentionTable.LONGEST_FRAME,
          model.getName() + " has a frame of " + frame + ", longer than the " + ContentionTable.LONGEST_FRAME + " the tables are sized for");
    }
  }
}
