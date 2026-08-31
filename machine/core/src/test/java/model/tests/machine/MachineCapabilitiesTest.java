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
import com.fpetrola.oozx.MachineCapability;
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

import java.util.EnumSet;
import java.util.Set;

import static com.fpetrola.oozx.MachineCapability.AY;
import static com.fpetrola.oozx.MachineCapability.MEMORY_128;
import static com.fpetrola.oozx.MachineCapability.NTSC;
import static com.fpetrola.oozx.MachineCapability.PLUS3_DISK;
import static com.fpetrola.oozx.MachineCapability.PLUS3_MEMORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What each model says it can do.
 * <p>
 * This used to come from one function that answered zero for every machine, so nothing that
 * asked could tell a +3 from a 48K and several branches were unreachable. Now each model
 * declares it, which means a wrong answer is a wrong declaration rather than a missing
 * mechanism — and that is worth pinning, because nothing else fails when a capability is wrong.
 * The machine simply behaves like a different one.
 */
public class MachineCapabilitiesTest {

  private final Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));

  private SpectrumMachine model(Class<? extends SpectrumMachine> model) {
    return injector.getInstance(model);
  }

  private Set<MachineCapability> capabilitiesOf(Class<? extends SpectrumMachine> model) {
    return model(model).getCapabilities();
  }

  @Test
  public void aFortyEightHasNoneOfThem() {
    assertEquals(Set.of(), capabilitiesOf(Spec48.class));
    assertEquals(Set.of(NTSC), capabilitiesOf(Spec48Ntsc.class), "the NTSC one differs only in that");
  }

  @Test
  public void aOneTwentyEightHasSoundAndPaging() {
    assertEquals(Set.of(AY, MEMORY_128), capabilitiesOf(Spec128.class));
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
    assertTrue(model(SpecPlus3.class).has(MEMORY_128), "the +3 keeps the 128's paging port");
    assertTrue(model(SpecPlus3.class).has(PLUS3_MEMORY), "and adds its own");
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
    assertTrue(model(SpecPlus3.class).has(PLUS3_DISK), "the +3 has a drive");
    assertFalse(model(SpecPlus2A.class).has(PLUS3_DISK), "the +2A does not, which MachineTypes also says");

    Set<MachineCapability> aPlusThreeWithoutItsDrive = EnumSet.copyOf(capabilitiesOf(SpecPlus3.class));
    aPlusThreeWithoutItsDrive.remove(PLUS3_DISK);
    assertEquals(aPlusThreeWithoutItsDrive, capabilitiesOf(SpecPlus2A.class), "and is otherwise the same machine");
  }
}
