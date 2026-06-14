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
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Paging128;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.devices.interface1.Interface1Peripheral;
import com.fpetrola.oozx.Speccy;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Interface1Test extends MachineTest {

  private static final int DATA = 0xe7;
  private static final int CONTROL = 0xef;
  private static final int COMMS = 0xf7;

  /** A formatted cartridge of 179 sectors, out of the jar and onto the disk that {@link Interface1Peripheral#insert} wants. */
  private static File successCartridge() throws IOException {
    File file = File.createTempFile("success", ".mdr");
    file.deleteOnExit();
    try (InputStream in = Interface1Test.class.getResourceAsStream("/success.mdr")) {
      Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    return file;
  }

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
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(Spec48.class));
    return speccy;
  }

  /** An Interface 1 plugged into whichever machine the caller made, with a ROM it can be recognised by. */
  private Interface1Peripheral anInterface1On(Speccy speccy) throws IOException {
    speccy.roms.choose("Interface1Peripheral", pretendRom().getPath());
    Interface1Peripheral interface1 = (Interface1Peripheral) speccy.peripheralRegistry.find(Interface1Peripheral.class);
    interface1.plugIn(true);
    assertTrue(speccy.peripheralRegistry.update());
    speccy.machine.reset(true);
    assertTrue(interface1.isAvailable());
    return interface1;
  }

  private int in(Speccy speccy, int port) {
    return speccy.ports.read(port) & 0xff;
  }

  private void out(Speccy speccy, int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  private int peek(Speccy speccy, int address) {
    return speccy.memory.peek(address) & 0xff;
  }

  /**
   * Which of a 128's two ROMs is at the bottom, asked of the machine rather than written to
   * 0x7ffd: the port belongs to the memory device, which this module does not carry, and what
   * that port does is PagingTest's to say. What is asked here is what the shadow ROM covers.
   */
  private void chooseRom(Speccy speccy, int rom) {
    ((Paging128) speccy.machine.current).memoryPortWrite(0x7ffd, (byte) (rom << 4));
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
    Interface1Peripheral interface1 = anInterface1On(speccy);
    assertFalse(interface1.isPaged());
    speccy.cpu.jump(0x0008);
    speccy.cpu.step();
    assertTrue(interface1.isPaged(), "0x0008 did not page it in");
    assertEquals(0xA1, peek(speccy, 0x0000));
    assertEquals(0xA1, peek(speccy, 0x2000), "the 8K is not mirrored at 0x2000");
    assertRamPages(speccy, 5, 2, 0);
    speccy.cpu.jump(0x0700);
    speccy.cpu.step();
    assertFalse(interface1.isPaged(), "the RET at 0x0700 did not page it out");
    assertEquals(0xF3, peek(speccy, 0x0000), "the machine's ROM did not come back");
    speccy.cpu.jump(0x1708);
    speccy.cpu.step();
    assertTrue(interface1.isPaged(), "0x1708 did not page it in");
  }

  @Test
  void theMotorsAreAShiftRegisterAndTheHeadReadsTheCartridge() throws IOException {
    File cartridge = successCartridge();
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = anInterface1On(speccy);
    interface1.insert(0, cartridge);
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

    byte[] image = Files.readAllBytes(cartridge.toPath());
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
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = anInterface1On(speccy);
    interface1.insert(0, successCartridge());
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
    Interface1Peripheral interface1 = anInterface1On(speccy);
    interface1.setRandomMicrodriveLength(false);
    interface1.setMicrodriveSectors(10);
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
    Interface1Peripheral interface1 = anInterface1On(speccy);
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
    Interface1Peripheral interface1 = anInterface1On(speccy);
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
    Interface1Peripheral interface1 = (Interface1Peripheral) speccy.peripheralRegistry.find(Interface1Peripheral.class);
    speccy.roms.choose("Interface1Peripheral", rom.getPath());
    interface1.plugIn(true);
    assertTrue(speccy.peripheralRegistry.update());
    speccy.machine.reset(true);
    assertTrue(interface1.isAvailable());
    runFrames(speccy, 100);
    assertFalse(interface1.isPaged(), "BASIC's boot should not have called the shadow ROM");
    speccy.cpu.jump(0x0008);
    speccy.cpu.step();
    assertTrue(interface1.isPaged());
    assertEquals(0x2A, peek(speccy, 0x0008), "the shadow ROM's RST 8 handler starts with LD HL,(5C5D)");
    int steps = 0;
    while (interface1.isPaged() && steps++ < 100000) {
      speccy.cpu.step();
    }
    assertFalse(interface1.isPaged(), "the shadow ROM never handed back at 0x0700");
  }

  @Test
  void itFitsTheSinclairsWithARomcsLine() {
    Speccy speccy = speccy();
    Interface1Peripheral interface1 = (Interface1Peripheral) speccy.peripheralRegistry.find(Interface1Peripheral.class);
    assertTrue(interface1.fitsOn(speccy.machine.model(Spec48.class)));
    assertTrue(interface1.fitsOn(speccy.machine.model(Spec128.class)));
    assertFalse(interface1.fitsOn(speccy.machine.model(SpecPlus3.class)));
  }

  /**
   * The shadow ROM goes over whichever of the machine's own ROMs is at the bottom, and paging it
   * out gives back that one and not the other.
   * <p>
   * A 48K has one ROM and cannot tell the two apart; a 128 has two, and the one that comes back
   * is whichever its port last chose - including when the choice was made while the shadow was
   * in the way, which is a program paging its ROM from inside a Microdrive routine.
   * <p>
   * On a 128 and not on a +3: an Interface 1 does not fit one, which is what
   * {@link #itFitsTheSinclairsWithARomcsLine} says, so the +3's four ROMs never meet a shadow.
   */
  @Test
  void itsShadowGoesOverWhicheverRomA128HasAndGivesBackThatOne() throws IOException {
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(Spec128.class));
    Interface1Peripheral interface1 = anInterface1On(speccy);

    for (int rom : new int[]{0, 1}) {
      chooseRom(speccy, rom);
      assertMap(speccy, rom, 5, 2, 0);

      speccy.cpu.jump(0x0008);
      speccy.cpu.step();
      assertTrue(interface1.isPaged(), "0x0008 did not page it in over ROM " + rom);
      assertEquals(0xA1, peek(speccy, 0x0000), "the shadow ROM is not over ROM " + rom);

      speccy.cpu.jump(0x0700);
      speccy.cpu.step();
      assertFalse(interface1.isPaged());
      assertMap(speccy, rom, 5, 2, 0); // paging the shadow out gives that ROM back, not the other
    }

    // Paged from underneath the shadow: the machine still hears its port, and what comes back
    // when the shadow goes is the ROM chosen while it was there.
    chooseRom(speccy, 1);
    speccy.cpu.jump(0x0008);
    speccy.cpu.step();
    chooseRom(speccy, 0);
    assertEquals(0xA1, peek(speccy, 0x0000), "the shadow left while the machine paged underneath it");
    speccy.cpu.jump(0x0700);
    speccy.cpu.step();
    assertMap(speccy, 0, 5, 2, 0); // the ROM chosen under the shadow is the one that comes back
  }
}
