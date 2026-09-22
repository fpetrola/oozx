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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Chloe280Se;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.ula.ContentionTable;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Chloe is the machine that can be told to run eight times as fast, which is the only way a
 * frame comes out longer than the contention tables are sized for. It moved here with the machine
 * itself: what a model does is tried where that model is.
 */
class ARunawayChloeIsNotContendedTest {
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
    Spectrum chloe = speccy.machine.model(Chloe280Se.class);
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
