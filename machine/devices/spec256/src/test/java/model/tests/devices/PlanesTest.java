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

import com.fpetrola.oozx.speccy.devices.spec256.Planes;
import com.fpetrola.z80.memory.Memory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The colours of a game: eight bytes for every byte of its RAM, the rightmost pixel first, read
 * into eight memories a processor can run on.
 */
class PlanesTest {
  private static final int SOMEWHERE = 0x8000;

  private final byte[] file = new byte[Planes.LENGTH];

  /** The eight colours of one address, written the way a file has them: rightmost pixel first. */
  private void colours(int address, int... fromTheLeft) {
    for (int pixel = 0; pixel < 8; pixel++) {
      file[(address - Planes.RAM) * 8 + (7 - pixel)] = (byte) fromTheLeft[pixel];
    }
  }

  /** A byte that is no picture: every pixel the last colour where it is set, and the first where it is not. */
  private void asTheMachineHasIt(int address, int value) {
    for (int pixel = 0; pixel < 8; pixel++) {
      file[(address - Planes.RAM) * 8 + (7 - pixel)] = (byte) ((value & (0x80 >> pixel)) != 0 ? 0xff : 0);
    }
  }

  private Memory machineWith(byte... bytes) {
    return new Memory() {
      public int read(int address, int fetching) {
        return peek(address);
      }

      public int peek(int address) {
        return address < bytes.length ? bytes[address] & 0xff : 0;
      }

      public void write(int address, int value) {
      }

      public void reset() {
      }
    };
  }

  @Test
  void aPixelsColourIsOneBitOutOfEachOfTheEightPlanes() {
    colours(SOMEWHERE, 200, 1, 0, 0, 0, 0, 0, 255);
    Planes planes = Planes.of(file);

    assertEquals(200, planes.colourOf(SOMEWHERE, 0), "the leftmost pixel is the last of the eight in the file");
    assertEquals(1, planes.colourOf(SOMEWHERE, 1));
    assertEquals(255, planes.colourOf(SOMEWHERE, 7), "and the rightmost is the first");
    assertEquals(0, planes.colourOf(SOMEWHERE, 4));
  }

  @Test
  void aByteThatIsNoPictureIsEightPlanesAllEqualToIt() {
    asTheMachineHasIt(SOMEWHERE, 0b10010110);
    Planes planes = Planes.of(file);
    Memory plane = planes.plane(3, machineWith());

    assertEquals(0b10010110, plane.peek(SOMEWHERE), "every one of the eight holds the byte itself");
    assertEquals(255, planes.colourOf(SOMEWHERE, 0), "so a pixel that is set comes out the last colour");
    assertEquals(0, planes.colourOf(SOMEWHERE, 1), "and one that is not comes out the first");
  }

  @Test
  void theColoursOfAGameAreOneSizeAndNoOther() {
    assertEquals(393216, Planes.LENGTH, "49152 bytes of RAM, eight colours each");
    assertThrows(IllegalArgumentException.class, () -> Planes.of(new byte[Planes.LENGTH - 1]));
    assertThrows(IllegalArgumentException.class, () -> Planes.of(new byte[Planes.LENGTH + 1]));
  }

  @Test
  void whatAPlaneIsAskedToExecuteComesFromTheMachine() {
    colours(SOMEWHERE, 1, 1, 1, 1, 1, 1, 1, 1);
    Memory plane = Planes.of(file).plane(0, machineWith());

    assertEquals(0xff, plane.read(SOMEWHERE, 0), "read as data it is this plane's own bits");
    assertEquals(0, plane.read(SOMEWHERE, 1), "read to be executed it is the machine's byte, which here is nothing");
  }

  @Test
  void belowTheRamThereAreNoColoursAndTheMachinesByteIsAlreadyTheAnswer() {
    Memory plane = Planes.of(file).plane(5, machineWith((byte) 0x11, (byte) 0x22));

    assertEquals(0x22, plane.read(1, 0), "eight planes equal to the machine's byte say the machine's byte");
    plane.write(1, 0x99);
    assertEquals(0x22, plane.read(1, 0), "and nothing a processor writes there changes it");
  }

  @Test
  void whatAProcessorWritesToAPlaneIsWhatTheNextPixelIsPaintedFrom() {
    Planes planes = Planes.of(file);
    for (int plane = 0; plane < Planes.PLANES; plane++) {
      planes.plane(plane, machineWith()).write(SOMEWHERE, (plane & 1) == 0 ? 0x80 : 0);
    }

    assertEquals(0b01010101, planes.colourOf(SOMEWHERE, 0), "the planes that were written are the bits that are set");
    assertEquals(0, planes.colourOf(SOMEWHERE, 1), "and the pixels beside it were not written at all");
  }
}
