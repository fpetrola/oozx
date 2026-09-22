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
import com.fpetrola.oozx.speccy.devices.scld.TimexMemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.ulaplus.UlaPlusPeripheral;
import com.fpetrola.oozx.speccy.machine.Chloe140Se;
import com.fpetrola.oozx.speccy.machine.Chloe280Se;
import com.fpetrola.oozx.speccy.machine.SpecSe;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SE as its designer went on with it: the same memory and the same ROM, plus the sixty-four
 * colours a program picks itself and a register that tells the machine to run faster than it was
 * built to. The smaller of the two is that machine without the slots.
 */
class ChloeTest extends MachineTest {
  /** The port the second chip's registers are named and given a value through, in one byte. */
  private static final int ULA2 = 0x8e3b;

  private final Speccy speccy = silentMachine();

  private Spectrum on(Class<? extends Spectrum> model) {
    speccy.machine.select(speccy.machine.model(model));
    return speccy.machine.current;
  }

  private void out(int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  @Test
  void bothOfThemRunTheRomTheSeRuns() {
    assertEquals(speccy.roms.running(speccy.machine.model(SpecSe.class)),
        speccy.roms.running(speccy.machine.model(Chloe280Se.class)), "the same two ROM images");
    assertEquals(speccy.roms.running(speccy.machine.model(SpecSe.class)),
        speccy.roms.running(speccy.machine.model(Chloe140Se.class)));
  }

  /**
   * The first of the second chip's registers says how many of the processor's cycles fit where one
   * fitted. Even numbers double it up to sixteen times; an odd one is the machine as it was built.
   */
  @Test
  void theFirstRegisterOfTheSecondChipSaysHowFastToRun() {
    Spectrum chloe = on(Chloe280Se.class);
    int line = chloe.getTimings().tstatesPerLine();

    out(ULA2, 0x02);
    assertEquals(2, chloe.timesFaster());
    assertEquals(line * 2, chloe.getTimings().tstatesPerLine(), "twice the cycles in the same line");

    out(ULA2, 0x06);
    assertEquals(8, chloe.timesFaster(), "six is eight times, which is what the chip was given");

    out(ULA2, 0x03);
    assertEquals(1, chloe.timesFaster(), "an odd one is the machine as it was built");
    assertEquals(line, chloe.getTimings().tstatesPerLine());
  }

  /** Only the first of those registers is one: a byte naming any other says nothing about speed. */
  @Test
  void aByteNamingAnotherRegisterSaysNothingAboutSpeed() {
    Spectrum chloe = on(Chloe280Se.class);
    out(ULA2, 0x02);

    out(ULA2, 0x14);

    assertEquals(2, chloe.timesFaster(), "a register that is not the speed changed the speed");
  }

  /** Two of its pages are contended, where the SE contends every odd one of them. */
  @Test
  void twoOfItsPagesAreContended() {
    on(Chloe280Se.class);

    for (int page = 0; page < 8; page++) {
      assertEquals(page == 5 || page == 7, speccy.banks.ram(page).contended, "page " + page);
    }
  }

  /** The smaller one has no slots, so nothing of it can be covered by a cartridge that is not there. */
  @Test
  void theSmallerOneHasNoSlots() {
    on(Chloe140Se.class);
    assertFalse(speccy.peripheralRegistry.isActive(TimexMemoryPeripheral.class), "the small one grew slots");

    on(Chloe280Se.class);
    assertTrue(speccy.peripheralRegistry.isActive(TimexMemoryPeripheral.class), "the big one lost them");
  }

  /** Both come with the sixty-four colours: nobody has to fit what a machine was sold with. */
  @Test
  void bothComeWithTheSixtyFourColours() {
    on(Chloe140Se.class);
    assertTrue(speccy.peripheralRegistry.isActive(UlaPlusPeripheral.class));

    on(Chloe280Se.class);
    assertTrue(speccy.peripheralRegistry.isActive(UlaPlusPeripheral.class));
  }
}
