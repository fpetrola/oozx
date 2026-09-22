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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Chrome;
import model.harness.MachineTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A 128 with two more sixteen K, two more ROMs and a port of its own that says what to do with
 * them - and one bit of that port that puts all of it away and leaves a 128 behind.
 * <p>
 * Given the 128's ROMs to read rather than its own, which are not here: what is asked about below
 * is which of them is read and what is where, not what any of them says.
 */
class ChromeTest extends MachineTest {
  private static final int PAGING = 0x7ffd, ITS_OWN = 0x1ffd;

  private final Speccy speccy = silentMachine();
  private List<String> itsOwn;

  @BeforeEach
  void withSomeRomsToRead() {
    itsOwn = speccy.roms.files.get("Chrome");
    speccy.roms.files.put("Chrome", List.of("128-0.rom", "128-1.rom", "128-0.rom", "128-1.rom"));
    speccy.machine.select(speccy.machine.model(Chrome.class));
  }

  @AfterEach
  void backToItsOwn() {
    speccy.roms.files.put("Chrome", itsOwn);
  }

  private void out(int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  private Object at(int address) {
    return speccy.memory.reading(address).memory();
  }

  /** Four ROMs where a 128 has two: the bit a 128 chooses with, and one of this machine's own above it. */
  @Test
  void itChoosesBetweenFourRomsAndNotTwo() {
    out(PAGING, 0x00);
    assertSame(speccy.banks.rom(0), at(0x0000));

    out(PAGING, 0x10);
    assertSame(speccy.banks.rom(1), at(0x0000), "the bit a 128 chooses with");

    out(ITS_OWN, 0x02);
    assertSame(speccy.banks.rom(3), at(0x0000), "and its own above that one");

    out(PAGING, 0x00);
    assertSame(speccy.banks.rom(2), at(0x0000));
  }

  /** One of the two pages that are not a 128's goes where the ROM is, and its own bit says which. */
  @Test
  void oneOfItsOwnPagesGoesWhereTheRomIs() {
    out(ITS_OWN, 0x01);
    assertSame(speccy.banks.ram(8), at(0x0000));

    out(ITS_OWN, 0x03);
    assertSame(speccy.banks.ram(9), at(0x0000), "the other one, which the same bit names as the high bit of the ROM");

    out(ITS_OWN, 0x00);
    assertSame(speccy.banks.rom(0), at(0x0000), "and the ROM back");
  }

  /** The other goes where the screen is, and the screen goes on being shown from where it was. */
  @Test
  void theOtherGoesWhereTheScreenIs() {
    assertSame(speccy.banks.ram(5), at(0x4000));

    out(ITS_OWN, 0x04);

    assertSame(speccy.banks.ram(9), at(0x4000));
    assertSame(speccy.banks.ram(5), speccy.banks.shown(), "the screen is still shown from where it was");
  }

  /** One bit and it runs at twice the speed it was built at. */
  @Test
  void oneBitRunsItAtTwiceTheSpeed() {
    int line = speccy.machine.current.getTimings().tstatesPerLine();

    out(ITS_OWN, 0x08);

    assertEquals(2, speccy.machine.current.timesFaster());
    assertEquals(line * 2, speccy.machine.current.getTimings().tstatesPerLine());
  }

  /**
   * And one bit puts everything of its own away: the four ROMs go back to two, the extra pages
   * stay where they are, and the speed goes back to what the machine was built at. It is how a
   * machine like this runs what will not have it.
   */
  @Test
  void oneBitLeavesA128Behind() {
    out(ITS_OWN, 0x08 | 0x02 | 0x01);
    assertSame(speccy.banks.ram(9), at(0x0000));

    out(ITS_OWN, 0x20 | 0x08 | 0x02 | 0x01);

    assertSame(speccy.banks.rom(0), at(0x0000), "the ROM is back where a 128 has it");
    assertEquals(1, speccy.machine.current.timesFaster(), "and it is running at what it was built at");
  }

  /** Two of its pages are held up, where a 128 holds up the odd ones. */
  @Test
  void twoOfItsPagesAreContended() {
    for (int page = 0; page < 10; page++) {
      assertEquals(page == 2 || page == 5, speccy.banks.ram(page).contended, "page " + page);
    }
  }
}
