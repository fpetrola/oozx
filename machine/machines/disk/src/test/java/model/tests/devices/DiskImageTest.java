/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.oozx.speccy.devices.disk.Disk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiskImageTest {

  @Test
  void anMgtImageComesBackAsItWentIn() throws Exception {
    byte[] image = new byte[2 * 80 * 10 * 512];
    for (int i = 0; i < image.length; i++) {
      image[i] = (byte) (i * 7 + (i >> 9));
    }
    Disk disk = Disk.openBuffer("round.mgt", image);
    assertEquals(2, disk.sides);
    assertEquals(80, disk.cylinders);
    assertEquals(Disk.Type.MGT, disk.type);
    assertArrayEquals(image, disk.toImage(), "the tracks were made up around the sectors and written back differently");
  }

  @Test
  void anImgImageIsTheSameSectorsInTheOtherOrder() throws Exception {
    byte[] image = new byte[80 * 10 * 512];
    for (int i = 0; i < image.length; i++) {
      image[i] = (byte) (i * 13);
    }
    assertArrayEquals(image, Disk.openBuffer("round.img", image).toImage());
  }

  @Test
  void aTrdImageComesBackAsItWentIn() throws Exception {
    byte[] image = new byte[2 * 80 * 16 * 256];
    for (int i = 0; i < image.length; i++) {
      image[i] = (byte) (i * 3 + (i >> 8));
    }
    // The specification sector: TR-DOS's id and a disk type that says 80 tracks, two sides.
    int spec = 8 * 256;
    image[spec] = 0;
    image[spec + 227] = 0x16;
    image[spec + 231] = 0x10;
    Disk disk = Disk.openBuffer("round.trd", image);
    assertEquals(2, disk.sides);
    assertEquals(80, disk.cylinders);
    assertArrayEquals(image, disk.toImage());
  }

  @Test
  void aBlankDiskHasNoSectorsUntilItIsFormatted() throws Exception {
    Disk blank = Disk.blank(2, 80, Disk.Density.DD, Disk.Type.MGT);
    assertThrows(Exception.class, blank::toImage, "an unformatted disk has no sectors to write as MGT");
  }

  @Test
  void anImageOfTheWrongSizeIsRefused() {
    assertThrows(Exception.class, () -> Disk.openBuffer("odd.mgt", new byte[1000]));
  }
}
