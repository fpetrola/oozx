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
import com.fpetrola.oozx.speccy.devices.interface1.Interface1Peripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Interface1Test {

  private static final int DATA = 0xe7;
  private static final int CONTROL = 0xef;
  private static final int COMMS = 0xf7;
  private static final File SUCCESS = new File("../../../fuse-emulator-fuse/lib/tests/success.mdr");

  /** Eight kilobytes with a mark at the start and the RET the real one has at 0x0700. */
  private static File pretendRom() throws IOException {
    byte[] image = new byte[Interface1Peripheral.ROM_SIZE];
    image[0] = (byte) 0xA1;
    image[0x0700] = (byte) 0xC9;
    File file = File.createTempFile("if1", ".rom");
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

  private Interface1Peripheral aFortyEightWithAnInterface1(Speccy speccy) throws IOException {
    speccy.settings.current.romInterface1 = pretendRom().getPath();
    Interface1Peripheral interface1 = (Interface1Peripheral) speccy.peripherals.find(Interface1Peripheral.class);
    interface1.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    assertTrue(interface1.isAvailable());
    return interface1;
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

  /** One pulse of the clock with the data line low: the first Microdrive's motor. */
  private void motorOne(Speccy speccy) {
    out(speccy, CONTROL, 0x02);
    out(speccy, CONTROL, 0x00);
  }

  /** Ten zeros and two 0xff, which is what the ULA looks for before a block. */
  private void preamble(Speccy speccy) {
    for (int i = 0; i < 10; i++) out(speccy, DATA, 0x00);
    out(speccy, DATA, 0xff);
    out(speccy, DATA, 0xff);
  }

  private static void assertSame(byte[] image, int from, byte[] actual, String message) {
    assertEquals(Arrays.toString(Arrays.copyOfRange(image, from, from + actual.length)), Arrays.toString(actual), message);
  }

  @Test
  void itsShadowRomIsSeenTwiceAndComesAndGoesAtTheHooks() throws IOException {
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = aFortyEightWithAnInterface1(speccy);
    assertFalse(interface1.isPaged());
    speccy.z80.jump(0x0008);
    speccy.z80.step();
    assertTrue(interface1.isPaged(), "0x0008 did not page it in");
    assertEquals(0xA1, peek(speccy, 0x0000));
    assertEquals(0xA1, peek(speccy, 0x2000), "the 8K is not mirrored at 0x2000");
    speccy.z80.jump(0x0700);
    speccy.z80.step();
    assertFalse(interface1.isPaged(), "the RET at 0x0700 did not page it out");
    assertEquals(0xF3, peek(speccy, 0x0000), "the machine's ROM did not come back");
    speccy.z80.jump(0x1708);
    speccy.z80.step();
    assertTrue(interface1.isPaged(), "0x1708 did not page it in");
  }

  @Test
  void theMotorsAreAShiftRegisterAndTheHeadReadsTheCartridge() throws IOException {
    Assumptions.assumeTrue(SUCCESS.isFile());
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = aFortyEightWithAnInterface1(speccy);
    interface1.insert(0, SUCCESS);
    assertEquals(179, interface1.sectors(0));
    assertEquals(0xff, in(speccy, DATA), "a drive whose motor is off says nothing");

    motorOne(speccy);
    assertTrue(interface1.motorOn(0));
    out(speccy, CONTROL, 0x03);
    out(speccy, CONTROL, 0x01);
    assertFalse(interface1.motorOn(0), "the clock did not shift the motor along");
    assertTrue(interface1.motorOn(1));
    motorOne(speccy);
    assertTrue(interface1.motorOn(0));
    assertTrue(interface1.motorOn(2));

    byte[] image = Files.readAllBytes(SUCCESS.toPath());
    in(speccy, CONTROL);
    byte[] header = new byte[15];
    for (int i = 0; i < header.length; i++) {
      header[i] = (byte) in(speccy, DATA);
    }
    assertSame(image, 0, header, "the sector header is not what the file has");
    assertEquals("Success", new String(header, 4, 7, StandardCharsets.US_ASCII));
    in(speccy, CONTROL);
    byte[] record = new byte[528];
    for (int i = 0; i < record.length; i++) {
      record[i] = (byte) in(speccy, DATA);
    }
    assertSame(image, 15, record, "the record is not what the file has");
  }

  @Test
  void theGapAndSyncLinesComeAndGoOnAFormattedBlock() throws IOException {
    Assumptions.assumeTrue(SUCCESS.isFile());
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = aFortyEightWithAnInterface1(speccy);
    interface1.insert(0, SUCCESS);
    motorOne(speccy);
    List<Integer> seen = new ArrayList<>();
    for (int i = 0; i < 32; i++) {
      seen.add(in(speccy, CONTROL) & 0x06);
    }
    for (int i = 0; i < 15; i++) {
      assertEquals(0x06, seen.get(i), "read " + i + " should be in the gap");
      assertEquals(0x00, seen.get(15 + i), "read " + (15 + i) + " should be the sync");
    }
    assertEquals(0x06, seen.get(31), "after sixteen lows the gap comes round again");
  }

  @Test
  void aBlankCartridgeIsFormattedThroughThePortAndSavedAsAFile() throws IOException {
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = aFortyEightWithAnInterface1(speccy);
    speccy.settings.current.mdrRandomLen = false;
    speccy.settings.current.mdrLen = 10;
    interface1.insertBlank(0);
    assertEquals(10, interface1.sectors(0));
    motorOne(speccy);
    assertEquals(0x06, in(speccy, CONTROL) & 0x06, "an unformatted block has no sync marks");

    byte[] header = "   TESTING     ".getBytes(StandardCharsets.US_ASCII);
    for (int sector = 0; sector < 10; sector++) {
      preamble(speccy);
      for (byte b : header) out(speccy, DATA, b);
      in(speccy, CONTROL);
      preamble(speccy);
      for (int i = 0; i < 528; i++) out(speccy, DATA, sector);
      in(speccy, CONTROL);
    }
    int reads = 0;
    while ((in(speccy, CONTROL) & 0x06) != 0 && reads < 40) reads++;
    assertTrue(reads < 40, "round the loop, the first block never showed its sync marks");

    interface1.writeProtect(0, true);
    File saved = File.createTempFile("if1", ".mdr");
    saved.deleteOnExit();
    interface1.save(0, saved);
    byte[] image = Files.readAllBytes(saved.toPath());
    assertEquals(10 * 543 + 1, image.length);
    assertSame(header, 0, Arrays.copyOf(image, 15), "the header did not reach the tape");
    assertEquals(1, image[image.length - 1], "the write-protect byte");
    interface1.eject(0);
    interface1.insert(0, saved);
    assertTrue(interface1.writeProtected(0));
    motorOne(speccy);
    assertEquals(0, in(speccy, CONTROL) & 0x01, "protected reads back on bit 0");
  }

  @Test
  void aByteTheSpectrumBitBangsOutReachesTheTerminal() throws IOException {
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = aFortyEightWithAnInterface1(speccy);
    List<Integer> received = new ArrayList<>();
    interface1.rs232().terminal(received::add);
    out(speccy, CONTROL, 0x01);
    assertEquals(0x08, in(speccy, CONTROL) & 0x08, "DTR is up while the terminal is connected");

    int sent = 'A';
    out(speccy, COMMS, 0);
    out(speccy, COMMS, 1);
    for (int bit = 0; bit < 8; bit++) {
      out(speccy, COMMS, (sent >> bit & 1) != 0 ? 0 : 1);
    }
    out(speccy, COMMS, 0);
    out(speccy, COMMS, 0);
    out(speccy, COMMS, 1);
    assertEquals(List.of((int) 'A'), received);
  }

  @Test
  void aByteTypedOnTheTerminalComesInABitAtATime() throws IOException {
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = aFortyEightWithAnInterface1(speccy);
    interface1.rs232().terminal(b -> {
    });
    interface1.rs232().type('Z');
    out(speccy, CONTROL, 0x11);
    assertEquals(0, in(speccy, COMMS) & 0x80, "the line rests low while the byte is picked up");
    for (int i = 0; i < 4; i++) {
      assertEquals(0x80, in(speccy, COMMS) & 0x80, "start bit " + i);
    }
    int got = 0;
    for (int bit = 0; bit < 8; bit++) {
      got |= ((in(speccy, COMMS) & 0x80) == 0 ? 1 : 0) << bit;
    }
    assertEquals('Z', got);
  }

  /** The real ROM, which this emulator cannot ship: RST 8 goes through it and comes back at 0x0700. */
  @Test
  void theRealRomAnswersRst8AndHandsBackToTheMachines() throws IOException {
    File rom = new File(System.getProperty("user.home"), "detodo/spectrum/Roms/if1.rom");
    Assumptions.assumeTrue(rom.isFile());
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = (Interface1Peripheral) speccy.peripherals.find(Interface1Peripheral.class);
    speccy.settings.current.romInterface1 = rom.getPath();
    interface1.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    assertTrue(interface1.isAvailable());
    long booted = speccy.machine.current.frameCount() + 100;
    while (speccy.machine.current.frameCount() < booted) {
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
    }
    assertFalse(interface1.isPaged(), "BASIC's boot should not have called the shadow ROM");
    speccy.z80.jump(0x0008);
    speccy.z80.step();
    assertTrue(interface1.isPaged());
    assertEquals(0x2A, peek(speccy, 0x0008), "the shadow ROM's RST 8 handler starts with LD HL,(5C5D)");
    int steps = 0;
    while (interface1.isPaged() && steps++ < 100000) {
      speccy.z80.step();
    }
    assertFalse(interface1.isPaged(), "the shadow ROM never handed back at 0x0700");
  }

  @Test
  void itFitsTheSinclairsWithARomcsLine() {
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = (Interface1Peripheral) speccy.peripherals.find(Interface1Peripheral.class);
    assertTrue(interface1.fitsOn(speccy.spec48));
    assertTrue(interface1.fitsOn(speccy.spec128));
    assertFalse(interface1.fitsOn(speccy.specPlus3));
  }
}
