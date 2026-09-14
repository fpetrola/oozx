package model.tests.memory;

import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The memories are the hardware's, not the model's. Every machine this build has is alive at once
 * over one set of chips and they take turns being the current one, so switching model does not hand
 * out new memories - which is the reason SpectrumMemory is its own object rather than something a
 * Spectrum owns. Nothing else in the suite would notice if that changed.
 * <p>
 * What is in them does not outlast the switch: putting a machine in is switching a machine on, and
 * that is {@code aMachinePutInDoesNotFindWhatTheLastOneLeft} in the reset facts. The chip is the
 * same chip; what it is holding is this machine's.
 */
class MemoriesOutlastTheModelTest {
  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  @Test
  void theChipAtTheTopIsTheSameChipUnderTheNextModel() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.machine.model(Spec128.class));
    Ram top = (Ram) speccy.memory.writing(0xc000).memory();

    speccy.machine.select(speccy.machine.model(Spec48.class));

    assertSame(top, speccy.memory.writing(0xc000).memory(),
        "the model changed, and the chip at the top did not");
    assertEquals(0, speccy.memory.peek(0xc000) & 0xff,
        "and the machine now in found it as a machine just switched on finds it");
  }
}
