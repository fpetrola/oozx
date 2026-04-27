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
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterFullDecodePeripheral;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterPeripheral;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Printer availability as an opt-in device across machines. A 128K offers no ZX Printer port
 * (its COPY targets a serial printer via the AY instead), so silence there is correct. Wired up
 * purely via classpath discovery, with no edits outside the printer's own package.
 */
class PrinterOnItsMachineTest {

  private Speccy speccy(boolean wanted) {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    ((ZxPrinterPeripheral) speccy.peripheralRegistry.find(ZxPrinterPeripheral.class)).plugIn(wanted);
    ((ZxPrinterFullDecodePeripheral) speccy.peripheralRegistry.find(ZxPrinterFullDecodePeripheral.class)).plugIn(wanted);
    return speccy;
  }

  @Test
  void itIsThereOnlyWhenAskedFor() {
    Speccy speccy = speccy(false);
    speccy.machine.select(speccy.machine.model(Spec48.class));
    assertFalse(speccy.peripheralRegistry.isActive(ZxPrinterPeripheral.class), "nobody asked for a printer");

    speccy = speccy(true);
    speccy.machine.select(speccy.machine.model(Spec48.class));
    assertTrue(speccy.peripheralRegistry.isActive(ZxPrinterPeripheral.class), "a 48K takes a ZX Printer");
  }

  @Test
  void aOneTwentyEightHasNoneOfThisKind() {
    Speccy speccy = speccy(true);

    speccy.machine.select(speccy.machine.model(Spec128.class));
    assertFalse(speccy.peripheralRegistry.isActive(ZxPrinterPeripheral.class), "a 128K prints down the serial port");

    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    assertFalse(speccy.peripheralRegistry.isActive(ZxPrinterPeripheral.class), "and so does a +3");
  }

  /** The full-address-decode variant is Timex-specific; inactive since no Timex model exists here. */
  @Test
  void theFullyDecodedOneIsOffEverywhere() {
    Speccy speccy = speccy(true);
    for (com.fpetrola.oozx.speccy.machine.Spectrum machine : speccy.machine.getMachineTypes()) {
      speccy.machine.select(machine);
      assertFalse(speccy.peripheralRegistry.isActive(ZxPrinterFullDecodePeripheral.class),
          "a machine here answered the Timex decoding: " + machine.getName());
    }
  }

  /** Confirms the port write actually reaches the printer through the real port bus. */
  @Test
  void whatIsWrittenToThePortReachesThePaper() {
    Speccy speccy = speccy(true);
    speccy.machine.select(speccy.machine.model(Spec48.class));
    Printout paper = ((ZxPrinterPeripheral) speccy.peripheralRegistry.find(ZxPrinterPeripheral.class)).paper();

    // Starts the motor, lets a full line's worth of belt travel pass, then writes again to
    // burn whatever the stylus state was throughout.
    speccy.ports.write(0x00fb, (byte) 0x80);
    speccy.zxClock.addTStates(320 * 220);
    speccy.ports.write(0x00fb, (byte) 0x80);

    assertEquals(1, paper.height(), "nothing came out of the printer");
  }
}
