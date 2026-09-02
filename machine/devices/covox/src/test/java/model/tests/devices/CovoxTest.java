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
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.devices.covox.CovoxPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CovoxTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void sound_lowlevel_frame(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).toInstance(new Loudest()));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.settings.current.sound = true;
    speccy.machine.select(speccy.pentagon);
    return speccy;
  }

  @Test
  void aByteWrittenToItsPortIsHeard() {
    Speccy speccy = speccy();
    CovoxPeripheral box = (CovoxPeripheral) speccy.peripherals.find(CovoxPeripheral.class);
    box.plugIn(true);
    speccy.peripherals.update();
    assertTrue(speccy.peripherals.isActive(CovoxPeripheral.class));

    // A step up in the middle of the frame, which is what a program playing a sample does a
    // thousand times a second; the frame ends with it still up, so the mix cannot cancel it out.
    speccy.zxClock.addTStates(30000);
    speccy.peripherals.writePort(0xfb, (byte) 0xff);
    assertEquals(255 * 128, box.dac().level());
    speccy.sound.frame();
    assertTrue(((Loudest) speccy.sound.getJavaSoundDevice()).peak > 0, "the box made no sound");

    box.plugIn(false);
    speccy.peripherals.update();
    Loudest heard = (Loudest) speccy.sound.getJavaSoundDevice();
    heard.peak = 0;
    speccy.sound.frame();
    assertEquals(0, heard.peak, "unplugged, it is still in the mix");
  }
}
