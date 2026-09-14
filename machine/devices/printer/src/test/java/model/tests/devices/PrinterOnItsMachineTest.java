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
      model.harness.MachineTest.select(speccy, machine);
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
