package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.ula.ContentionTable;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  /**
   * That is every machine at rest, and at rest is the only way a machine is held up: one told to
   * run faster than it was built to is not contended at all, which is why its frame being longer
   * than any of these is not a size anybody has to keep.
   */
  @Test
  void aMachineToldToRunFasterIsNotHeldUpAtAll() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    Spectrum chloe = speccy.machine.model(com.fpetrola.oozx.speccy.machine.Chloe280Se.class);
    speccy.machine.select(chloe);
    boolean heldUpAtRest = false;
    for (int tState = 0; tState < ContentionTable.LONGEST_FRAME; tState++) {
      heldUpAtRest |= speccy.ula.contention.delay[tState] != 0;
    }
    assertTrue(heldUpAtRest, "this machine should be held up somewhere while it runs as it was built to");

    speccy.ports.write(0x8e3b, (byte) 0x06);

    assertTrue(chloe.getTimings().tstatesPerFrame() > ContentionTable.LONGEST_FRAME,
        "eight times a frame should be longer than any frame at rest");
    for (int tState = 0; tState < ContentionTable.LONGEST_FRAME; tState++) {
      assertEquals(0, speccy.ula.contention.delay[tState], "held up at " + tState + " while running faster");
    }
  }
}
