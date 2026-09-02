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
import com.fpetrola.oozx.speccy.devices.divide.DivIdePeripheral;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DivIdeTest {

  private static final int CONTROL = 0xe3;
  private static final int DATA = 0xa3;
  private static final int COUNT = 0xab;
  private static final int SECTOR = 0xaf;
  private static final int CYLINDER_LOW = 0xb3;
  private static final int CYLINDER_HIGH = 0xb7;
  private static final int HEAD = 0xbb;
  private static final int COMMAND = 0xbf;
  private static final int HEADER = 0x216;

  /** Eight kilobytes of NOPs with a mark at the start. */
  private static File pretendEprom() throws IOException {
    byte[] image = new byte[DivIdePeripheral.PAGE_SIZE];
    image[0] = (byte) 0xA1;
    File file = File.createTempFile("divide", ".rom");
    file.deleteOnExit();
    Files.write(file.toPath(), image);
    return file;
  }

  private static File aDisk(int sectors) throws IOException {
    File file = File.createTempFile("divide", ".hdf");
    file.deleteOnExit();
    IdeChannel.createHdf(file, sectors);
    try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
      out.seek(HEADER + 5L * IdeChannel.SECTOR);
      out.write("DIVIDE".getBytes(StandardCharsets.US_ASCII));
    }
    return file;
  }

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.machine.select(speccy.spec48);
    return speccy;
  }

  private DivIdePeripheral aFortyEightWithADivIde(Speccy speccy, String eprom) {
    speccy.settings.current.romDivide = eprom;
    DivIdePeripheral divide = (DivIdePeripheral) speccy.peripherals.find(DivIdePeripheral.class);
    divide.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    return divide;
  }

  private int in(Speccy speccy, int port) {
    return speccy.peripherals.readPort(port) & 0xff;
  }

  private void out(Speccy speccy, int port, int value) {
    speccy.peripherals.writePort(port, (byte) value);
  }

  private int peek(Speccy speccy, int address) {
    return speccy.memory.readByteInternal(address) & 0xff;
  }

  private void poke(Speccy speccy, int address, int value) {
    speccy.memory.writeByteInternal2(address, (byte) value);
  }

  /** Fuse's own vectors: CONMEM, the page bits, MAPRAM, and MAPRAM staying set. */
  @Test
  void theControlRegisterPagesTheWayFuseSaysItDoes() throws IOException {
    Speccy speccy = speccy();
    DivIdePeripheral divide = aFortyEightWithADivIde(speccy, pretendEprom().getPath());
    assertFalse(divide.isPaged());
    out(speccy, CONTROL, 0x80);
    assertTrue(divide.isPaged(), "CONMEM did not page it in");
    assertEquals(0xA1, peek(speccy, 0x0000), "the EPROM is not at the bottom");
    poke(speccy, 0x2000, 0x11);
    assertEquals(0x11, peek(speccy, 0x2000), "RAM page 0 is not at 0x2000");
    out(speccy, CONTROL, 0x83);
    assertEquals(0x00, peek(speccy, 0x2000), "page 3 should be at 0x2000 now");
    poke(speccy, 0x2000, 0x33);
    divide.setAutomap(true);
    out(speccy, CONTROL, 0x40);
    assertTrue(divide.isPaged(), "MAPRAM did not page it in");
    assertEquals(0x33, peek(speccy, 0x0000), "MAPRAM puts page 3 where the EPROM was");
    assertEquals(0x11, peek(speccy, 0x2000));
    poke(speccy, 0x0000, 0x55);
    assertEquals(0x33, peek(speccy, 0x0000), "page 3 is read-only at the bottom");
    out(speccy, CONTROL, 0x02);
    assertEquals(0x42, divide.control(), "MAPRAM cannot be cleared by a port write");
    assertEquals(0x33, peek(speccy, 0x0000));
    assertEquals(0x00, peek(speccy, 0x2000), "page 2 should be at 0x2000");
    speccy.machine.reset(true);
    assertEquals(0, divide.control(), "a hard reset clears MAPRAM");
    assertFalse(divide.isPaged());
    assertEquals(0xF3, peek(speccy, 0x0000));
  }

  @Test
  void theAutomapperOnlyWorksWithTheJumperOrMapram() throws IOException {
    Speccy speccy = speccy();
    DivIdePeripheral divide = aFortyEightWithADivIde(speccy, pretendEprom().getPath());
    speccy.settings.current.divideWp = false;
    speccy.z80.jump(0x3d00);
    speccy.z80.step();
    assertFalse(divide.isPaged(), "with the EPROM writable the automapper is ignored");
    speccy.settings.current.divideWp = true;
    divide.refresh();
    assertTrue(divide.isPaged(), "the jumper lets the automapper act");
    assertEquals(0xA1, peek(speccy, 0x0000));
    speccy.z80.jump(0x1ff8);
    speccy.z80.step();
    assertFalse(divide.isPaged(), "0x1ff8 did not page it out");
    speccy.z80.jump(0x0066);
    speccy.z80.step();
    assertTrue(divide.isPaged(), "0x0066 did not page it in");
  }

  @Test
  void aSectorComesThroughThePortsAndOneGoesBackToTheFileOnCommit() throws IOException {
    Speccy speccy = speccy();
    DivIdePeripheral divide = aFortyEightWithADivIde(speccy, null);
    File disk = aDisk(32);
    divide.insert(IdeChannel.MASTER, disk);
    assertEquals(32, divide.drive(IdeChannel.MASTER).sectors());

    out(speccy, HEAD, 0xe0);
    out(speccy, COUNT, 1);
    out(speccy, SECTOR, 5);
    out(speccy, CYLINDER_LOW, 0);
    out(speccy, CYLINDER_HIGH, 0);
    out(speccy, COMMAND, 0x20);
    assertEquals(IdeChannel.STATUS_DRQ, in(speccy, COMMAND) & (IdeChannel.STATUS_DRQ | IdeChannel.STATUS_ERR));
    byte[] sector = new byte[IdeChannel.SECTOR];
    for (int i = 0; i < sector.length; i++) {
      sector[i] = (byte) in(speccy, DATA);
    }
    assertEquals("DIVIDE", new String(sector, 0, 6, StandardCharsets.US_ASCII));
    assertEquals(0, in(speccy, COMMAND) & IdeChannel.STATUS_DRQ, "the sector should be over");

    out(speccy, SECTOR, 6);
    out(speccy, COMMAND, 0x30);
    assertEquals(IdeChannel.STATUS_DRQ, in(speccy, COMMAND) & IdeChannel.STATUS_DRQ);
    for (int i = 0; i < IdeChannel.SECTOR; i++) {
      out(speccy, DATA, i);
    }
    assertEquals(0, in(speccy, COMMAND) & IdeChannel.STATUS_DRQ);
    assertTrue(divide.drive(IdeChannel.MASTER).dirty(), "nothing reached the file yet");
    byte[] before = Files.readAllBytes(disk.toPath());
    assertEquals(0, before[HEADER + 6 * IdeChannel.SECTOR + 7], "the file is untouched until the commit");
    divide.drive(IdeChannel.MASTER).commit(disk);
    byte[] after = Files.readAllBytes(disk.toPath());
    assertEquals(7, after[HEADER + 6 * IdeChannel.SECTOR + 7]);
    assertEquals((byte) 0xff, after[HEADER + 6 * IdeChannel.SECTOR + 511]);
    assertFalse(divide.drive(IdeChannel.MASTER).dirty());

    out(speccy, COMMAND, 0xec);
    byte[] identify = new byte[IdeChannel.SECTOR];
    for (int i = 0; i < identify.length; i++) {
      identify[i] = (byte) in(speccy, DATA);
    }
    assertEquals(32, identify[120] & 0xff | (identify[121] & 0xff) << 8, "IDENTIFY's word 60 is the size in sectors");
    out(speccy, HEAD, 0xf0);
    assertEquals(0x00, in(speccy, COMMAND), "no slave: its status floats low");
  }

  /** The real firmware, which this emulator cannot ship: with the jumper on, it boots at 0x0000. */
  @Test
  void fatwareBootsThroughTheAutomapper() {
    File rom = new File(System.getProperty("user.home"), "detodo/spectrum/Roms/FATware-0-12.rom");
    Assumptions.assumeTrue(rom.isFile());
    Speccy speccy = speccy();
    speccy.settings.current.divideWp = true;
    DivIdePeripheral divide = aFortyEightWithADivIde(speccy, rom.getPath());
    assertEquals(0xF3, peek(speccy, 0x0000), "the machine's ROM boots first");
    boolean sawItPaged = false;
    long until = speccy.machine.current.frameCount() + 50;
    while (speccy.machine.current.frameCount() < until) {
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
      sawItPaged |= divide.isPaged();
    }
    assertTrue(sawItPaged, "FATware never got paged in");
  }

  @Test
  void itFitsTheSinclairsWithARomcsLine() {
    Speccy speccy = speccy();
    DivIdePeripheral divide = (DivIdePeripheral) speccy.peripherals.find(DivIdePeripheral.class);
    assertTrue(divide.fitsOn(speccy.spec48));
    assertTrue(divide.fitsOn(speccy.spec128));
    assertFalse(divide.fitsOn(speccy.specPlus3));
  }
}
