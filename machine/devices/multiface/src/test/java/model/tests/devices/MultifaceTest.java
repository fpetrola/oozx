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

import model.harness.MachineTest;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.devices.multiface.MultifacePeripheral;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.multiface.Multiface128Peripheral;
import com.fpetrola.oozx.speccy.devices.multiface.Multiface3Peripheral;
import com.fpetrola.oozx.speccy.devices.multiface.MultifaceOnePeripheral;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultifaceTest extends MachineTest {

  /** Minimal fake ROM: on NMI, writes a marker byte, unpages itself, and returns. */
  private static File markingRom() throws IOException {
    byte[] image = new byte[MultifacePeripheral.ROM_SIZE];
    // NMI vector 0x0066: sets A, writes it to RAM, reads a port, then returns from NMI.
    byte[] routine = {0x3E, 0x5A, 0x32, 0x00, 0x20, (byte) 0xDB, 0x1F, (byte) 0xED, 0x45};
    System.arraycopy(routine, 0, image, 0x0066, routine.length);
    File file = File.createTempFile("multiface", ".rom");
    file.deleteOnExit();
    Files.write(file.toPath(), image);
    return file;
  }

  private Speccy speccy() {
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(Spec48.class));
    return speccy;
  }

  private MultifacePeripheral one(Speccy speccy) {
    return (MultifacePeripheral) speccy.peripheralRegistry.find(MultifaceOnePeripheral.class);
  }

  private MultifacePeripheral aFortyEightWithAOne(Speccy speccy, String rom) {
    speccy.roms.choose("MultifaceOnePeripheral", rom);
    MultifacePeripheral one = one(speccy);
    one.plugIn(true);
    assertTrue(speccy.peripheralRegistry.update(), "a Multiface arrives with a hard reset, and did not ask for one");
    speccy.machine.reset(true);
    assertTrue(one.isAvailable(), "the ROM was not read at the reset");
    return one;
  }

  @Test
  void theRedButtonTakesTheMachineIntoTheMultifaceAndItsRoutineComesBack() throws IOException {
    Speccy speccy = speccy();
    MultifacePeripheral one = aFortyEightWithAOne(speccy, markingRom().getPath());
    runFrames(speccy, 2);
    assertFalse(one.isPaged());

    assertTrue(one.redButton(), "the button did nothing");
    for (int frame = 0; frame < 5 && one.ram(0) != 0x5A; frame++) {
      runFrames(speccy, 1);
    }
    assertEquals(0x5A, one.ram(0), "the NMI routine in the Multiface's ROM never ran");
    assertFalse(one.isPaged(), "IN A,(0x1F) should have paged the One out again");
    assertEquals(0xF3, speccy.memory.peek(0) & 0xff, "the machine's ROM did not come back");
  }

  @Test
  void switchedToStealthTheOneIsNotThere() throws IOException {
    Speccy speccy = speccy();
    one(speccy).setStealth(true);
    MultifacePeripheral one = aFortyEightWithAOne(speccy, markingRom().getPath());
    assertFalse(one.isJ2());
    assertFalse(one.redButton(), "stealth: the button must do nothing");
    speccy.ports.read(0x9f);
    assertFalse(one.isPaged(), "stealth: reading 0x9f must not page it in");
  }

  @Test
  void thePortPagesItInAndOut() throws IOException {
    Speccy speccy = speccy();
    MultifacePeripheral one = aFortyEightWithAOne(speccy, markingRom().getPath());
    speccy.ports.read(0x9f);
    assertTrue(one.isPaged(), "IN A,(0x9F) pages the One in");
    assertEquals(0x3E, speccy.memory.peek(0x66) & 0xff, "the ROM is not where the processor reads");
    speccy.memory.poke(0x2001, (byte) 0x77);
    assertEquals(0x77, one.ram(1), "the RAM is not at 0x2000");
    assertRamPages(speccy, 5, 2, 0);
    speccy.ports.read(0x1f);
    assertFalse(one.isPaged(), "IN A,(0x1F) pages the One out");
  }

  @Test
  void eachOneWasSoldForItsMachine() {
    Speccy speccy = speccy();
    MultifacePeripheral one = one(speccy);
    MultifacePeripheral m128 = (MultifacePeripheral) speccy.peripheralRegistry.find(Multiface128Peripheral.class);
    MultifacePeripheral m3 = (MultifacePeripheral) speccy.peripheralRegistry.find(Multiface3Peripheral.class);
    assertTrue(one.fitsOn(speccy.machine.model(Spec48.class)));
    assertFalse(one.fitsOn(speccy.machine.model(Spec128.class)));
    assertTrue(m128.fitsOn(speccy.machine.model(Spec48.class)));
    assertTrue(m128.fitsOn(speccy.machine.model(Spec128.class)));
    assertFalse(m128.fitsOn(speccy.machine.model(SpecPlus3.class)));
    assertTrue(m3.fitsOn(speccy.machine.model(SpecPlus3.class)));
    assertTrue(m3.fitsOn(speccy.machine.model(SpecPlus2A.class)));
    assertFalse(m3.fitsOn(speccy.machine.model(Spec128.class)));
    assertFalse(one.fitsOn(speccy.machine.model(Pentagon.class)));
  }

  /** With the real ROM present, pressing the button should display the Multiface menu. */
  @Test
  void theRealOneDrawsItsMenu() {
    File rom = new File(System.getProperty("user.home"), "detodo/spectrum/Roms/mf1.rom");
    Assumptions.assumeTrue(rom.isFile(), "no Multiface One ROM on this machine");
    Speccy speccy = speccy();
    MultifacePeripheral one = aFortyEightWithAOne(speccy, rom.getPath());
    runFrames(speccy, 120);
    long before = screenSum(speccy);
    assertTrue(one.redButton());
    runFrames(speccy, 60);
    assertTrue(screenSum(speccy) != before, "the Multiface's menu did not appear on the screen");
  }

  private static long screenSum(Speccy speccy) {
    long sum = 0;
    for (int address = 0x4000; address < 0x5b00; address++) {
      sum = sum * 31 + (speccy.memory.peek(address) & 0xff);
    }
    return sum;
  }


  /** A hard reset must zero this peripheral's own RAM. */
  @Test
  void aHardResetLeavesNothingInItsRam() throws IOException {
    Speccy speccy = speccy();
    MultifacePeripheral one = aFortyEightWithAOne(speccy, markingRom().getPath());
    speccy.ports.read(0x9f);
    speccy.memory.poke(0x2001, (byte) 0x77);
    assertEquals(0x77, one.ram(1));

    speccy.machine.reset(true);
    assertEquals(0, one.ram(1), "a hard reset did not clear its RAM");
  }
}
