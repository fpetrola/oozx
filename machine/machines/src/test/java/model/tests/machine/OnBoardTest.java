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
 * What each model has on its board, said by the model itself.
 * <p>
 * Nothing else fails when a board is wrong: the machine simply behaves like a different one,
 * which is how a +3 once came up with a drive nobody could switch off.
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

  /** The +2 is a 128 in another case, so it inherits and there is nothing to declare. */
  @Test
  public void aPlusTwoIsAOneTwentyEight() {
    assertEquals(onBoardOf(Spec128.class), onBoardOf(SpecPlus2.class));
    assertTrue(model(SpecPlus2.class).pagesThrough7ffd());
  }

  /**
   * A +3 pages through both ports, not one instead of the other: its memory peripheral
   * registers a handler for 0x7ffd and another for 0x1ffd, and restoring a snapshot writes
   * both. Its sound chip is the one wired the Amstrad way.
   */
  @Test
  public void aPlusThreePagesThroughBothPorts() {
    assertTrue(model(SpecPlus3.class).pagesThrough7ffd(), "the +3 keeps the 128's paging port");
    assertTrue(model(SpecPlus3.class).pagesThrough1ffd(), "and adds its own");
    assertTrue(model(SpecPlus3.class).hasOnBoard(AyPeripheral.class), "a sound chip");
    assertTrue(onBoardOf(SpecPlus3.class).contains(AyPlus3Peripheral.class), "the +3's own");
    assertTrue(onBoardOf(SpecPlus3.class).contains(SpecPlus3MemoryPeripheral.class));
  }

  /**
   * The one the hierarchy gets wrong on its own.
   * <p>
   * SpecPlus2A extends SpecPlus3 and is a +3 without the floppy, so inheriting would give it a
   * drive it does not have. It is also the case a reader is most likely to break later by
   * "simplifying" these into an inherit-and-add.
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
