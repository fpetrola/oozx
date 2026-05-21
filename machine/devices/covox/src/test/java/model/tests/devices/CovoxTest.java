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
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.devices.covox.CovoxPeripheral;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CovoxTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void play(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).toInstance(new Loudest()));
    speccy.init();
    speccy.picture.active = false;
    speccy.sound.output.enabled = true;
    speccy.machine.select(speccy.machine.model(Pentagon.class));
    return speccy;
  }

  @Test
  void aByteWrittenToItsPortIsHeard() {
    Speccy speccy = speccy();
    CovoxPeripheral box = (CovoxPeripheral) speccy.peripheralRegistry.find(CovoxPeripheral.class);
    box.plugIn(true);
    speccy.peripheralRegistry.update();
    assertTrue(speccy.peripheralRegistry.isActive(CovoxPeripheral.class));

    // A step up in the middle of the frame, which is what a program playing a sample does a
    // thousand times a second; the frame ends with it still up, so the mix cannot cancel it out.
    speccy.zxClock.addTStates(30000);
    speccy.ports.write(0xfb, (byte) 0xff);
    assertEquals(255 * 128, box.dac().level());
    speccy.sound.frame();
    assertTrue(((Loudest) speccy.sound.card()).peak > 0, "the box made no sound");

    box.plugIn(false);
    speccy.peripheralRegistry.update();
    Loudest heard = (Loudest) speccy.sound.card();
    heard.peak = 0;
    speccy.sound.frame();
    assertEquals(0, heard.peak, "unplugged, it is still in the mix");
  }
}
