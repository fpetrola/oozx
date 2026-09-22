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
