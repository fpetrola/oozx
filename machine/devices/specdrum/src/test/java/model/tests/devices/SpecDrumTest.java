/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.devices.specdrum.SpecDrumPeripheral;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecDrumTest {

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
    speccy.machine.select(speccy.machine.model(Spec48.class));
    return speccy;
  }

  @Test
  void aByteWrittenToItsPortIsHeard() {
    Speccy speccy = speccy();
    SpecDrumPeripheral box = (SpecDrumPeripheral) speccy.peripheralRegistry.find(SpecDrumPeripheral.class);
    box.plugIn(true);
    speccy.peripheralRegistry.update();
    assertTrue(speccy.peripheralRegistry.isActive(SpecDrumPeripheral.class));

    // A step up in the middle of the frame, which is what a program playing a sample does a
    // thousand times a second; the frame ends with it still up, so the mix cannot cancel it out.
    speccy.zxClock.addTStates(30000);
    speccy.ports.write(0xdf, (byte) 0xff);
    assertEquals(127 * 128, box.dac().level());
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
