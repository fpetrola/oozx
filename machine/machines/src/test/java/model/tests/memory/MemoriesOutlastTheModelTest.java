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
 * The memories are the hardware's, not the model's. Eight machines are alive at once over one set
 * of chips and take turns being the current one, so switching model does not hand out new memories
 * and does not lose what is in them - which is the reason SpectrumMemory is its own object rather than
 * something a Spectrum owns. Nothing else in the suite would notice if that changed.
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
  void aByteWrittenUnderOneModelIsStillThereUnderTheNext() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.machine.model(Spec128.class));
    Ram top = (Ram) speccy.memory.writing(0xc000).memory();
    speccy.memory.poke(0xc000, (byte) 0x55);

    speccy.machine.select(speccy.machine.model(Spec48.class));

    assertSame(top, speccy.memory.writing(0xc000).memory(),
        "the model changed, and the chip at the top did not");
    assertEquals(0x55, speccy.memory.peek(0xc000) & 0xff,
        "so what was written under the 128 is still there under the 48");
  }
}
