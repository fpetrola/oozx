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

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.devices.ay.AyPlus3Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.Upd765Peripheral;
import com.fpetrola.oozx.speccy.devices.memory.Spec128MemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.memory.SpecPlus3MemoryPeripheral;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each model's onBoard() declaration, checked directly - a wrong board fails silently
 * elsewhere as behaviour drift, as when a +3 once came up with a permanently-on drive.
 */
public class OnBoardTest {

  private final Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));

  private SpectrumMachine model(Class<? extends SpectrumMachine> model) {
    return injector.getInstance(model);
  }

  private Set<Class<? extends Peripheral>> onBoardOf(Class<? extends SpectrumMachine> model) {
    return model(model).onBoard();
  }

  @Test
  public void aFortyEightHasNothingOnIt() {
    assertEquals(Set.of(), onBoardOf(Spec48.class));
    assertFalse(model(Spec48.class).pagesThrough7ffd());
  }

  @Test
  public void aOneTwentyEightHasSoundAndPaging() {
    assertEquals(Set.of(AyPeripheral.class, Spec128MemoryPeripheral.class), onBoardOf(Spec128.class));
    assertTrue(model(Spec128.class).pagesThrough7ffd());
  }

  /** The +2 shares the 128's board entirely, declaring nothing of its own. */
  @Test
  public void aPlusTwoIsAOneTwentyEight() {
    assertEquals(onBoardOf(Spec128.class), onBoardOf(SpecPlus2.class));
    assertTrue(model(SpecPlus2.class).pagesThrough7ffd());
  }

  /** The +3 pages through 0x7ffd and 0x1ffd both, registering handlers (and restoring both
   * on snapshot load), with the Amstrad-wired sound chip variant. */
  @Test
  public void aPlusThreePagesThroughBothPorts() {
    assertTrue(model(SpecPlus3.class).pagesThrough7ffd(), "the +3 keeps the 128's paging port");
    assertTrue(model(SpecPlus3.class).pagesThrough1ffd(), "and adds its own");
    assertTrue(model(SpecPlus3.class).hasOnBoard(AyPeripheral.class), "a sound chip");
    assertTrue(onBoardOf(SpecPlus3.class).contains(AyPlus3Peripheral.class), "the +3's own");
    assertTrue(onBoardOf(SpecPlus3.class).contains(SpecPlus3MemoryPeripheral.class));
  }

  /**
   * SpecPlus2A extends SpecPlus3 but must exclude the drive Upd765Peripheral, since plain
   * inheritance would wrongly give it one; a likely target for an incorrect future refactor.
   */
  @Test
  public void aPlusTwoAIsAPlusThreeWithoutTheDrive() {
    assertTrue(model(SpecPlus3.class).hasOnBoard(Upd765Peripheral.class), "the +3 has a drive");
    assertFalse(model(SpecPlus2A.class).hasOnBoard(Upd765Peripheral.class), "the +2A does not, which MachineTypes also says");

    Set<Class<? extends Peripheral>> aPlusThreeWithoutItsDrive = new HashSet<>(onBoardOf(SpecPlus3.class));
    aPlusThreeWithoutItsDrive.remove(Upd765Peripheral.class);
    assertEquals(aPlusThreeWithoutItsDrive, onBoardOf(SpecPlus2A.class), "and is otherwise the same machine");
  }
}
