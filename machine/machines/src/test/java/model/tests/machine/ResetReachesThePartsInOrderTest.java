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
 * Pins the required reset order: machine resets its ROMs/paging, then the CPU resets to 0, then
 * each active device is told. Devices must hear it only after activation (so fitsOn logic
 * applies) and after the CPU reset (so a device that jumps the CPU into its own ROM is not
 * immediately undone by a later CPU reset). This ordering exists implicitly in the call graph
 * and is not documented elsewhere.
 */
class ResetReachesThePartsInOrderTest {

  /** Address the probe jumps to on reset, mimicking a boot-ROM device like the Beta 128. */
  private static final int BOOT = 0x1234;

  /** Test peripheral that logs every reset-related callback it receives, wired in exactly as a
   * real device would be. */
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

  /** Selects the machine and resets the probe's log to empty. */
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

  /**
   * A machine put in finds memory as a machine just switched on finds it. It used to find whatever
   * the last one left, and the ROMs that write over everything they use hid it: a clone whose ROM
   * reads memory to decide whether it has run before then booted differently depending on which
   * machine had been running, which looked like chance and was not.
   */
  @Test
  void aMachinePutInDoesNotFindWhatTheLastOneLeft() {
    on(speccy.machine.model(Spec48.class));
    for (int page = 0; page < 8; page++) java.util.Arrays.fill(speccy.banks.ram(page).bytes, (byte) 0xa5);

    on(speccy.machine.model(Spec128.class));

    for (int page = 0; page < 8; page++) {
      for (byte at : speccy.banks.ram(page).bytes) {
        assertEquals(0, at, "page " + page + " still had what the machine before it wrote");
      }
    }
  }

  @Test
  void aDeviceThatDoesNotFitThisMachineIsNotToldAtAll() {
    on(speccy.machine.model(Spec128.class));

    speccy.machine.reset(true);

    assertEquals(List.of(), probe.heard, "a device switched off was told about a reset");
  }
}
