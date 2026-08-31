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

package model.tests;

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus3E;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import static com.fpetrola.oozx.MachineCapability.AY;
import static com.fpetrola.oozx.MachineCapability.NTSC;
import static com.fpetrola.oozx.MachineCapability.PLUS3_DISK;
import static com.fpetrola.oozx.MachineCapability.PLUS3_MEMORY;
import static com.fpetrola.oozx.MachineCapability._128_MEMORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What each model says it can do.
 * <p>
 * This used to come from one function that answered zero for every machine, so nothing that
 * asked could tell a +3 from a 48K and several branches were unreachable. Now each model
 * declares it, which means a wrong answer is a wrong constant rather than a missing mechanism —
 * and that is worth pinning, because nothing else fails when a capability is wrong. The machine
 * simply behaves like a different one.
 */
public class MachineCapabilitiesTest {

  private final Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));

  private int capabilitiesOf(Class<? extends SpectrumMachine> model) {
    return injector.getInstance(model).getCapabilities();
  }

  @Test
  public void aFortyEightHasNoneOfThem() {
    assertEquals(0, capabilitiesOf(Spec48.class));
    assertEquals(NTSC, capabilitiesOf(Spec48Ntsc.class), "the NTSC one differs only in that");
  }

  @Test
  public void aOneTwentyEightHasSoundAndPaging() {
    assertEquals(AY | _128_MEMORY, capabilitiesOf(Spec128.class));
  }

  /** The +2 is a 128 in another case, so it inherits and there is nothing to declare. */
  @Test
  public void aPlusTwoIsAOneTwentyEight() {
    assertEquals(capabilitiesOf(Spec128.class), capabilitiesOf(SpecPlus2.class));
  }

  /**
   * A +3 pages through both ports, not one instead of the other: its memory peripheral
   * registers a handler for 0x7ffd and another for 0x1ffd, and restoring a snapshot writes
   * both.
   */
  @Test
  public void aPlusThreePagesThroughBothPorts() {
    int plus3 = capabilitiesOf(SpecPlus3.class);
    assertTrue((plus3 & _128_MEMORY) != 0, "the +3 keeps the 128's paging port");
    assertTrue((plus3 & PLUS3_MEMORY) != 0, "and adds its own");
    assertEquals(capabilitiesOf(SpecPlus3.class), capabilitiesOf(SpecPlus3E.class));
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
    int plus2a = capabilitiesOf(SpecPlus2A.class);
    int plus3 = capabilitiesOf(SpecPlus3.class);

    assertTrue((plus3 & PLUS3_DISK) != 0, "the +3 has a drive");
    assertFalse((plus2a & PLUS3_DISK) != 0, "the +2A does not, which MachineTypes also says");
    assertEquals(plus3 & ~PLUS3_DISK, plus2a, "and is otherwise the same machine");
  }
}
