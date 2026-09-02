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
import com.fpetrola.oozx.speccy.devices.ide.MmcCard;
import com.fpetrola.oozx.speccy.devices.zxmmc.ZxmmcPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZxmmcTest {

  private static final int SELECT = 0x1f;
  private static final int DATA = 0x3f;

  private static File aCard() throws IOException {
    File file = File.createTempFile("zxmmc", ".mmc");
    file.deleteOnExit();
    MmcCard.createImage(file, 16);
    try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
      out.seek(3L * MmcCard.SECTOR);
      out.write("ZXMMC".getBytes(StandardCharsets.US_ASCII));
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

  private int in(Speccy speccy, int port) {
    return speccy.peripherals.readPort(port) & 0xff;
  }

  private void out(Speccy speccy, int port, int value) {
    speccy.peripherals.writePort(port, (byte) value);
  }

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
  void theCardOnlyAnswersWhileItIsChosen() throws IOException {
    Speccy speccy = speccy();
    ZxmmcPeripheral zxmmc = (ZxmmcPeripheral) speccy.peripherals.find(ZxmmcPeripheral.class);
    zxmmc.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    zxmmc.insert(0, aCard());
    assertFalse(zxmmc.isPaged(), "it has no memory of its own");

    command(speccy, 0, 0);
    assertEquals(0xff, untilNot(speccy, 0xff), "nothing answers while no card is chosen");
    out(speccy, SELECT, 0x02);
    command(speccy, 0, 0);
    assertEquals(0x01, untilNot(speccy, 0xff));
    command(speccy, 1, 0);
    assertEquals(0x00, untilNot(speccy, 0xff));

    command(speccy, 17, 3 * MmcCard.SECTOR);
    untilNot(speccy, 0xff);
    assertEquals(0xfe, untilNot(speccy, 0xff));
    byte[] sector = new byte[MmcCard.SECTOR];
    for (int i = 0; i < sector.length; i++) {
      sector[i] = (byte) in(speccy, DATA);
    }
    assertEquals("ZXMMC", new String(sector, 0, 5, StandardCharsets.US_ASCII));

    out(speccy, SELECT, 0x00);
    assertEquals(0xff, in(speccy, DATA), "unchosen, it says nothing again");
  }
}
