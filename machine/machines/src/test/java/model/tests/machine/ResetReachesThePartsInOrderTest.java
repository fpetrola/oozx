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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What a reset reaches, and in which order.
 * <p>
 * Resetting is three things happening to different owners - the machine puts its own ROMs and
 * paging back, the processor goes to zero, and every device that is switched on puts itself back
 * - and each of the three depends on where it falls among the others. A device is told after the
 * machine has switched it on, because what it does about a reset is decided by the machine it is
 * on: every real one here returns without doing anything when it has not been switched on for a
 * machine. And it is told after the processor has been reset, because a device that boots the
 * machine into its own ROM sends the processor there while it is being told, which a reset
 * arriving afterwards would undo.
 * <p>
 * The order is not written down anywhere: it falls out of who calls whom. This says what it is.
 */
class ResetReachesThePartsInOrderTest {

  /** Where a device sends the processor while it is being told about the reset, as a Beta does. */
  private static final int BOOT = 0x1234;

  /**
   * A device that says out loud everything a reset does to it. Bound into the build the way any
   * device arrives, and hearing about a reset the way every device here hears about one, so it
   * goes through whatever the machine does to the devices it has.
   */
  @Singleton
  public static class Probe extends AbstractPeripheral {
    private final Cpu cpu;
    final List<String> heard = new ArrayList<>();

    @Inject
    Probe(Cpu cpu) {
      super(List.of());
      this.cpu = cpu;
    }

    @Override
    public void activate(SpectrumMachine machine) {
      heard.add("switched on");
    }

    @Override
    public boolean fitsOn(SpectrumMachine machine) {
      return !machine.pagesThrough7ffd();
    }

    @Override
    public void machineWasReset(boolean hard) {
      heard.add(hard ? "reset hard" : "reset");
      cpu.jump(BOOT);
    }
  }

  private Probe probe;
  private final Speccy speccy = machineWithAProbe();

  private Speccy machineWithAProbe() {
    return MachineTest.silentMachine(
        binder -> Multibinder.newSetBinder(binder, Peripheral.class).addBinding().to(Probe.class));
  }

  /** The machine running, with the probe listening and nothing heard yet. */
  private Probe on(SpectrumMachine model) {
    speccy.machine.select(model);
    probe = (Probe) speccy.peripheralRegistry.find(Probe.class);
    probe.heard.clear();
    return probe;
  }

  @Test
  void aDeviceIsSwitchedOnForTheMachineBeforeItIsToldTheMachineReset() {
    on(speccy.machine.model(Spec48.class));

    speccy.machine.reset(true);

    assertEquals(List.of("switched on", "reset hard"), probe.heard,
        "a device has to be on the machine before it is asked what it does about a reset");
  }

  @Test
  void howHardTheResetWasIsWhatTheDeviceIsTold() {
    on(speccy.machine.model(Spec48.class));

    speccy.machine.reset(false);

    assertEquals(List.of("switched on", "reset"), probe.heard);
  }

  @Test
  void theProcessorIsResetBeforeTheDevicesAreTold() {
    on(speccy.machine.model(Spec48.class));

    speccy.machine.reset(true);

    assertEquals(BOOT, speccy.cpu.getOoz80().getState().getPc().read(),
        "the processor was reset after the device was told, so a device that boots the machine "
            + "into its own ROM lost the jump it made");
  }

  @Test
  void aDeviceThatDoesNotFitThisMachineIsNotToldAtAll() {
    on(speccy.machine.model(Spec128.class));

    speccy.machine.reset(true);

    assertEquals(List.of(), probe.heard, "a device switched off was told about a reset");
  }
}
