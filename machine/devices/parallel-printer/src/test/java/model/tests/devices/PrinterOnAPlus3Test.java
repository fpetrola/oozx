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
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrinterOnAPlus3Test {

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.machine.select(speccy.specPlus3);
    return speccy;
  }

  private ParallelPrinterPeripheral printer(Speccy speccy) {
    return (ParallelPrinterPeripheral) speccy.peripherals.find(ParallelPrinterPeripheral.class);
  }

  @Test
  void thePlus3PrintsThroughItsOwnPortAndStrobe() {
    Speccy speccy = speccy();
    ParallelPrinterPeripheral printer = printer(speccy);
    printer.plugIn(true);
    speccy.peripherals.update();
    assertTrue(speccy.peripherals.isActive(ParallelPrinterPeripheral.class));
    assertEquals(0xfe, speccy.peripherals.readPort(0x0ffd) & 0xff, "a printer that is there reads BUSY low");

    // What the +3 ROM does for LPRINT: the byte on the data port, then the strobe down and up
    // on bit 4 of 0x1ffd, with the paging bits it already had.
    for (char c : "OK".toCharArray()) {
      speccy.peripherals.writePort(0x0ffd, (byte) c);
      speccy.peripherals.writePort(0x1ffd, (byte) 0x10);
      speccy.zxClock.addTStates(30);
      speccy.peripherals.writePort(0x1ffd, (byte) 0x00);
      speccy.zxClock.addTStates(300);
    }
    assertEquals("OK", printer.printer().text());
  }

  @Test
  void withoutAPrinterThePortReadsNothing() {
    Speccy speccy = speccy();
    assertFalse(speccy.peripherals.isActive(ParallelPrinterPeripheral.class));
    assertEquals(0xff, speccy.peripherals.readPort(0x0ffd) & 0xff);
  }
}
