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
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterFullDecodePeripheral;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The printer as the emulator sees it: a box somebody asked for, on the machines that can have one.
 * <p>
 * A 128K is offered none - its COPY drives a serial printer through the AY - so a 128 that prints
 * nothing on this port is right and not a hole. Nothing outside the printer's own package was
 * edited to get it here: it is found on the classpath like every other device.
 */
class PrinterOnItsMachineTest {

  private Speccy speccy(boolean wanted) {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.settings.current.printer = wanted;
    return speccy;
  }

  @Test
  void itIsThereOnlyWhenAskedFor() {
    Speccy speccy = speccy(false);
    speccy.machine.select(speccy.spec48);
    assertFalse(speccy.peripherals.isActive(ZxPrinterPeripheral.class), "nobody asked for a printer");

    speccy = speccy(true);
    speccy.machine.select(speccy.spec48);
    assertTrue(speccy.peripherals.isActive(ZxPrinterPeripheral.class), "a 48K takes a ZX Printer");
  }

  @Test
  void aOneTwentyEightHasNoneOfThisKind() {
    Speccy speccy = speccy(true);

    speccy.machine.select(speccy.spec128);
    assertFalse(speccy.peripherals.isActive(ZxPrinterPeripheral.class), "a 128K prints down the serial port");

    speccy.machine.select(speccy.specPlus3);
    assertFalse(speccy.peripherals.isActive(ZxPrinterPeripheral.class), "and so does a +3");
  }

  /** The full-decode one is a Timex's, and there is no Timex here, so it waits. */
  @Test
  void theFullyDecodedOneIsOffEverywhere() {
    Speccy speccy = speccy(true);
    for (com.fpetrola.oozx.Spectrum machine : speccy.machine.getMachineTypes()) {
      speccy.machine.select(machine);
      assertFalse(speccy.peripherals.isActive(ZxPrinterFullDecodePeripheral.class),
          "a machine here answered the Timex decoding: " + machine.getName());
    }
  }

  /** And through the bus, the port of a 48K's printer really is the printer's. */
  @Test
  void whatIsWrittenToThePortReachesThePaper() {
    Speccy speccy = speccy(true);
    speccy.machine.select(speccy.spec48);
    Printout paper = ((ZxPrinterPeripheral) speccy.peripherals.find(ZxPrinterPeripheral.class)).paper();

    // The motor starts, the belt is given a line's worth of time, and the write that follows is
    // what burns what the stylus was doing all along.
    speccy.peripherals.writePort(0x00fb, (byte) 0x80);
    speccy.zxClock.addTStates(320 * 220);
    speccy.peripherals.writePort(0x00fb, (byte) 0x80);

    assertEquals(1, paper.height(), "nothing came out of the printer");
  }
}
