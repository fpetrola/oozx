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
  void aBlankDiskHasNoSectorsUntilItIsFormatted() throws Exception {
    Disk blank = Disk.blank(2, 80, Disk.Density.DD, Disk.Type.MGT);
    assertThrows(Exception.class, blank::toImage, "an unformatted disk has no sectors to write as MGT");
  }

  @Test
  void anImageOfTheWrongSizeIsRefused() {
    assertThrows(Exception.class, () -> Disk.openBuffer("odd.mgt", new byte[1000]));
  }
}
