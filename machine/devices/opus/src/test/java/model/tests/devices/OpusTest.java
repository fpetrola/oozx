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
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.devices.opus.OpusPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpusTest {

  private static File pretendRom() throws IOException {
    byte[] image = new byte[OpusPeripheral.ROM_SIZE];
    image[0] = (byte) 0xAA;
    File file = File.createTempFile("opus", ".rom");
    file.deleteOnExit();
    Files.write(file.toPath(), image);
    return file;
  }

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.machine.select(speccy.spec48);
    return speccy;
  }

  private OpusPeripheral aFortyEightWithAnOpus(Speccy speccy) throws IOException {
    speccy.settings.current.romOpus = pretendRom().getPath();
    OpusPeripheral opus = (OpusPeripheral) speccy.peripherals.find(OpusPeripheral.class);
    opus.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    assertTrue(opus.isAvailable());
    return opus;
  }

  private void wait(Speccy speccy, int tstates) {
    while (tstates > 0) {
      int step = Math.min(tstates, 1000);
      speccy.zxClock.addTStates(step);
      speccy.eventManager.eventDoEvents();
      tstates -= step;
    }
  }

  private int peek(Speccy speccy, int address) {
    return speccy.memory.readByteInternal(address) & 0xff;
  }

  private void poke(Speccy speccy, int address, int value) {
    speccy.memory.writeByteInternal2(address, (byte) value);
  }

  @Test
  void itPagesInAfterTheInstructionAtItsHooksAndOutAt0x1748() throws IOException {
    Speccy speccy = speccy();
    OpusPeripheral opus = aFortyEightWithAnOpus(speccy);
    assertFalse(opus.isPaged());
    speccy.z80.jump(0x0008);
    speccy.z80.step();
    assertTrue(opus.isPaged(), "0x0008 did not page the Opus in");
    assertEquals(0xAA, peek(speccy, 0));
    poke(speccy, 0x2001, 0x5a);
    assertEquals(0x5a, peek(speccy, 0x2001), "its RAM is not at 0x2000");
    speccy.z80.jump(0x1748);
    speccy.z80.step();
    assertFalse(opus.isPaged(), "0x1748 did not page it out");
    assertEquals(0xF3, peek(speccy, 0));
  }

  @Test
  void theControllerIsReadThroughMemoryAndEveryByteKnocksOnNmi() throws Exception {
    Speccy speccy = speccy();
    OpusPeripheral opus = aFortyEightWithAnOpus(speccy);
    byte[] image = new byte[80 * 18 * 256];
    System.arraycopy("OPUSDISK".getBytes(StandardCharsets.US_ASCII), 0, image, 3 * 256, 8);
    opus.insert(0, Disk.openBuffer("test.opd", image));
    speccy.z80.jump(0x0008);
    speccy.z80.step();

    poke(speccy, 0x3001, 0x04);                 // PIA control A: port A is data
    poke(speccy, 0x3000, 0x00);                 // drive 1, side 0
    poke(speccy, 0x2800, 0x08);                 // restore, with the head loaded
    for (int i = 0; i < 5000 && (peek(speccy, 0x2800) & WdFdc.SR_BUSY) != 0; i++) wait(speccy, 3500);
    assertEquals(0, peek(speccy, 0x2800) & WdFdc.SR_BUSY);

    int nmisBefore = nmis(speccy);
    poke(speccy, 0x2801, 0);                    // track
    poke(speccy, 0x2802, 3);                    // sector
    poke(speccy, 0x2800, 0x80);                 // read sector
    byte[] data = new byte[256];
    int got = 0;
    for (int i = 0; i < 200000 && got < 256; i++) {
      int status = peek(speccy, 0x2800);
      if ((status & WdFdc.SR_BUSY) == 0) break;
      if ((status & WdFdc.SR_IDX_DRQ) != 0) {
        data[got++] = (byte) peek(speccy, 0x2803);
      }
      wait(speccy, 120);
    }
    assertEquals(256, got, "the sector did not come out whole");
    assertEquals("OPUSDISK", new String(data, 0, 8, StandardCharsets.US_ASCII));
    assertTrue(nmis(speccy) - nmisBefore >= 256, "the Opus pulls /NMI for every byte, and did not");
  }

  private int nmiCount;

  /** How many NMIs were taken: the processor tells its listeners as it takes each one. */
  private int nmis(Speccy speccy) {
    if (!counting) {
      counting = true;
      speccy.z80.onNmi(() -> nmiCount++);
    }
    return nmiCount;
  }

  private boolean counting;

  @Test
  void itGoesOnTheSinclairMachines() {
    Speccy speccy = speccy();
    OpusPeripheral opus = (OpusPeripheral) speccy.peripherals.find(OpusPeripheral.class);
    assertTrue(opus.fitsOn(speccy.spec48));
    assertTrue(opus.fitsOn(speccy.spec128));
    assertFalse(opus.fitsOn(speccy.specPlus3));
  }
}
