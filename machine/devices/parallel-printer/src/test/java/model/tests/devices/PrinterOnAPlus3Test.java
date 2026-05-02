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
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrinterOnAPlus3Test {

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    return speccy;
  }

  private ParallelPrinterPeripheral printer(Speccy speccy) {
    return (ParallelPrinterPeripheral) speccy.peripheralRegistry.find(ParallelPrinterPeripheral.class);
  }

  @Test
  void thePlus3PrintsThroughItsOwnPortAndStrobe() {
    Speccy speccy = speccy();
    ParallelPrinterPeripheral printer = printer(speccy);
    printer.plugIn(true);
    speccy.peripheralRegistry.update();
    assertTrue(speccy.peripheralRegistry.isActive(ParallelPrinterPeripheral.class));
    assertEquals(0xfe, speccy.ports.read(0x0ffd) & 0xff, "a printer that is there reads BUSY low");

    // What the +3 ROM does for LPRINT: the byte on the data port, then the strobe down and up
    // on bit 4 of 0x1ffd, with the paging bits it already had.
    for (char c : "OK".toCharArray()) {
      speccy.ports.write(0x0ffd, (byte) c);
      speccy.ports.write(0x1ffd, (byte) 0x10);
      speccy.zxClock.addTStates(30);
      speccy.ports.write(0x1ffd, (byte) 0x00);
      speccy.zxClock.addTStates(300);
    }
    assertEquals("OK", printer.printer().text());
  }

  @Test
  void withoutAPrinterThePortReadsNothing() {
    Speccy speccy = speccy();
    assertFalse(speccy.peripheralRegistry.isActive(ParallelPrinterPeripheral.class));
    assertEquals(0xff, speccy.ports.read(0x0ffd) & 0xff);
  }
}
