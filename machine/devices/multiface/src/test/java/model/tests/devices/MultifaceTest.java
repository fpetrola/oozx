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

  /** A ROM whose NMI routine leaves its mark in the Multiface's RAM, pages itself out and returns. */
  private static File markingRom() throws IOException {
    byte[] image = new byte[MultifacePeripheral.ROM_SIZE];
    // 0x0066: LD A,0x5A; LD (0x2000),A; IN A,(0x1F); RETN
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

  /** With the real ROM, if it is on this machine: the button brings the Multiface's menu up on the screen. */
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


  /** Its RAM is RAM: the machine going off and on again leaves nothing in it. */
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
