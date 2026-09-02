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
import com.fpetrola.oozx.speccy.devices.divmmc.DivMmcPeripheral;
import com.fpetrola.oozx.speccy.devices.ide.DivPeripheral;
import com.fpetrola.oozx.speccy.devices.ide.MmcCard;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DivMmcTest {

  private static final int CONTROL = 0xe3;
  private static final int SELECT = 0xe7;
  private static final int DATA = 0xeb;

  private static File pretendEprom() throws IOException {
    byte[] image = new byte[DivPeripheral.PAGE_SIZE];
    image[0] = (byte) 0xA1;
    File file = File.createTempFile("divmmc", ".rom");
    file.deleteOnExit();
    Files.write(file.toPath(), image);
    return file;
  }

  private static File aCard(int sectors) throws IOException {
    File file = File.createTempFile("divmmc", ".mmc");
    file.deleteOnExit();
    MmcCard.createImage(file, sectors);
    try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
      out.seek(5L * MmcCard.SECTOR);
      out.write("DIVMMC".getBytes(StandardCharsets.US_ASCII));
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

  private DivMmcPeripheral aFortyEightWithADivMmc(Speccy speccy, String eprom) {
    speccy.settings.current.romDivmmc = eprom;
    DivMmcPeripheral divmmc = (DivMmcPeripheral) speccy.peripherals.find(DivMmcPeripheral.class);
    divmmc.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    return divmmc;
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

  /** A command is six bytes, and the card answers on the accesses that follow. */
  private void command(Speccy speccy, int which, long argument) {
    out(speccy, DATA, 0x40 | which);
    out(speccy, DATA, (int) (argument >> 24) & 0xff);
    out(speccy, DATA, (int) (argument >> 16) & 0xff);
    out(speccy, DATA, (int) (argument >> 8) & 0xff);
    out(speccy, DATA, (int) argument & 0xff);
    out(speccy, DATA, 0x95);
  }

  private int untilNot(Speccy speccy, int idle) {
    for (int i = 0; i < 16; i++) {
      int b = in(speccy, DATA);
      if (b != idle) {
        return b;
      }
    }
    return idle;
  }

  @Test
  void theCardIsWokenUpAndASectorRead() throws IOException {
    Speccy speccy = speccy();
    DivMmcPeripheral divmmc = aFortyEightWithADivMmc(speccy, null);
    divmmc.insert(0, aCard(32));
    assertEquals(32, divmmc.drive(0).sectors());

    assertEquals(0xff, in(speccy, DATA), "no card is chosen yet");
    out(speccy, SELECT, 0x02);
    command(speccy, 0, 0);
    assertEquals(0x01, untilNot(speccy, 0xff), "the card should say it is idle");
    command(speccy, 55, 0);
    untilNot(speccy, 0xff);
    command(speccy, 41, 0);
    assertEquals(0x00, untilNot(speccy, 0xff), "the card should be awake now");

    command(speccy, 17, 5 * MmcCard.SECTOR);
    assertEquals(0x00, untilNot(speccy, 0xff), "the read was not accepted");
    assertEquals(0xfe, untilNot(speccy, 0xff), "no token before the data");
    byte[] sector = new byte[MmcCard.SECTOR];
    for (int i = 0; i < sector.length; i++) {
      sector[i] = (byte) in(speccy, DATA);
    }
    assertEquals("DIVMMC", new String(sector, 0, 6, StandardCharsets.US_ASCII));
  }

  @Test
  void aSectorWrittenReachesTheFileOnlyOnCommit() throws IOException {
    Speccy speccy = speccy();
    DivMmcPeripheral divmmc = aFortyEightWithADivMmc(speccy, null);
    File card = aCard(32);
    divmmc.insert(0, card);
    out(speccy, SELECT, 0x02);
    command(speccy, 1, 0);
    untilNot(speccy, 0xff);

    command(speccy, 24, 6 * MmcCard.SECTOR);
    assertEquals(0x00, untilNot(speccy, 0xff), "the write was not accepted");
    out(speccy, DATA, 0xfe);
    for (int i = 0; i < MmcCard.SECTOR; i++) {
      out(speccy, DATA, i);
    }
    out(speccy, DATA, 0xff);
    out(speccy, DATA, 0xff);
    assertEquals(0x05, untilNot(speccy, 0xff), "the card did not accept the data");
    assertTrue(divmmc.drive(0).dirty());
    assertEquals(0, Files.readAllBytes(card.toPath())[6 * MmcCard.SECTOR + 7], "the file is untouched");
    divmmc.drive(0).commit(card);
    assertEquals(7, Files.readAllBytes(card.toPath())[6 * MmcCard.SECTOR + 7]);
    assertFalse(divmmc.drive(0).dirty());

    command(speccy, 17, 6 * MmcCard.SECTOR);
    untilNot(speccy, 0xff);
    assertEquals(0xfe, untilNot(speccy, 0xff));
    for (int i = 0; i < 8; i++) {
      assertEquals(i, in(speccy, DATA), "byte " + i + " of what was written");
    }
  }

  @Test
  void itHasTheSameAutomapperWithSixteenPagesOfRam() throws IOException {
    Speccy speccy = speccy();
    DivMmcPeripheral divmmc = aFortyEightWithADivMmc(speccy, pretendEprom().getPath());
    speccy.settings.current.divmmcWp = true;
    divmmc.refresh();
    speccy.z80.jump(0x3d00);
    speccy.z80.step();
    assertTrue(divmmc.isPaged(), "the automapper did not page it in");
    assertEquals(0xA1, peek(speccy, 0x0000));
    out(speccy, CONTROL, 0x40);
    out(speccy, CONTROL, 0x3f);
    assertEquals(0x7f, divmmc.control(), "MAPRAM stays set");
    assertEquals("MAPRAM page 15, EPROM protected", divmmc.status(), "sixteen pages, so 0x3f is page 15");
  }
}
