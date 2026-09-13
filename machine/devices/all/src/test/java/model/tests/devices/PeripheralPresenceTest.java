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

import model.harness.MachineTest;
import com.fpetrola.oozx.speccy.machine.SpecPlus3E;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.devices.melodik.MelodikPeripheral;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.devices.ay.AyPlus3Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.Upd765Peripheral;
import com.fpetrola.oozx.speccy.devices.joystick.KempstonStrictPeripheral;
import com.fpetrola.oozx.speccy.devices.memory.Spec128MemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.memory.SpecPlus3MemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.ula.UlaFullDecodePeripheral;
import com.fpetrola.oozx.speccy.devices.ula.UlaPeripheral;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which devices are switched on in which machine, said once so that changing how that is decided
 * cannot change the answers by accident.
 * <p>
 * The table lives in one class today, MachinesPeriph, and is about to become each device declaring
 * where it fits. This is the net under that: every registered peripheral against every machine,
 * asked twice - with the optional ones wanted and unwanted - so a device that is built in is told
 * apart from one that is merely offered.
 */
class PeripheralPresenceTest extends MachineTest {

  private static final List<Class<? extends Peripheral>> REGISTERED = List.of(
      UlaPeripheral.class, UlaFullDecodePeripheral.class,
      Spec128MemoryPeripheral.class, SpecPlus3MemoryPeripheral.class,
      AyPeripheral.class, AyPlus3Peripheral.class, MelodikPeripheral.class,
      KempstonStrictPeripheral.class,
      Upd765Peripheral.class, Beta128Peripheral.class);

  private Speccy speccy(boolean wantOptionals) {
    Speccy speccy = silentMachine();
    ((MelodikPeripheral) speccy.peripheralRegistry.find(MelodikPeripheral.class)).setFitted(wantOptionals);
    Input.of(speccy).setup.kempstonJoystick = wantOptionals;
    return speccy;
  }

  private String activeOn(Speccy speccy, Spectrum machine) {
    speccy.machine.select(machine);
    speccy.peripheralRegistry.update();

    Set<String> active = new LinkedHashSet<>();
    for (Class<? extends Peripheral> peripheral : REGISTERED) {
      if (speccy.peripheralRegistry.isActive(peripheral)) active.add(peripheral.getSimpleName());
    }
    return String.join(" ", active);
  }

  private void has(String expected, Spectrum machine, Speccy speccy) {
    assertEquals(expected, activeOn(speccy, machine), machine.getName());
  }

  /** Built in: on whatever the settings say. */
  @Test
  void whatEachMachineComesWith() {
    Speccy speccy = speccy(false);

    has("UlaPeripheral", speccy.machine.model(Spec48.class), speccy);
    has("UlaPeripheral", speccy.machine.model(Spec48Ntsc.class), speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral", speccy.machine.model(Spec128.class), speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral", speccy.machine.model(SpecPlus2.class), speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral", speccy.machine.model(SpecPlus2A.class), speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral Upd765Peripheral", speccy.machine.model(SpecPlus3.class), speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral Upd765Peripheral", speccy.machine.model(SpecPlus3E.class), speccy);
    has("UlaFullDecodePeripheral Spec128MemoryPeripheral AyPeripheral Beta128Peripheral", speccy.machine.model(Pentagon.class), speccy);
  }

  /**
   * And what it accepts when asked for: the Melodik is a 48K box, the Kempston plugs into
   * everything except the Pentagon, which decodes its own port and is given none.
   */
  @Test
  void whatEachMachineAcceptsWhenWanted() {
    Speccy speccy = speccy(true);

    has("UlaPeripheral MelodikPeripheral KempstonStrictPeripheral", speccy.machine.model(Spec48.class), speccy);
    has("UlaPeripheral MelodikPeripheral KempstonStrictPeripheral", speccy.machine.model(Spec48Ntsc.class), speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral KempstonStrictPeripheral", speccy.machine.model(Spec128.class), speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral KempstonStrictPeripheral", speccy.machine.model(SpecPlus2.class), speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral KempstonStrictPeripheral", speccy.machine.model(SpecPlus2A.class), speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral KempstonStrictPeripheral Upd765Peripheral", speccy.machine.model(SpecPlus3.class), speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral KempstonStrictPeripheral Upd765Peripheral", speccy.machine.model(SpecPlus3E.class), speccy);
    has("UlaFullDecodePeripheral Spec128MemoryPeripheral AyPeripheral Beta128Peripheral", speccy.machine.model(Pentagon.class), speccy);
  }

  /**
   * The +3's drive, which until now was built, registered and never once switched on: its machine
   * asked for it by a name that stood for no class, so the request landed on a generic entry that
   * nothing registered. A +2A is the same machine without a drive and must not gain one.
   */
  @Test
  void theDiskControllerBelongsToTheMachineWithADrive() {
    Speccy speccy = speccy(false);

    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    speccy.peripheralRegistry.update();
    assertTrue(speccy.peripheralRegistry.isActive(Upd765Peripheral.class), "the +3 has a drive");
    assertDoesNotThrow(() -> speccy.ports.read(0x2ffd), "reading the FDC status port");
    assertDoesNotThrow(() -> speccy.ports.read(0x3ffd), "reading the FDC data port");

    speccy.machine.select(speccy.machine.model(SpecPlus2A.class));
    speccy.peripheralRegistry.update();
    assertFalse(speccy.peripheralRegistry.isActive(Upd765Peripheral.class), "a +2A has no drive");
  }

  /**
   * And that switching it on did not stop the machine: the +3's ROM asks the drive whether it is
   * ready, and used to get the same 0xff a port nothing answers gives. Now it gets the controller,
   * so this runs the machine long enough to leave the boot screen.
   */
  @Test
  void aPlus3StillRunsWithItsDrivePresent() {
    Speccy speccy = speccy(false);
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));

    long previous = speccy.zxClock.getTStates();
    int frames = 0;
    while (frames < 50) {
      step(speccy);
      long now = speccy.zxClock.getTStates();
      if (now < previous) frames++;
      previous = now;
    }

    assertTrue(speccy.peripheralRegistry.isActive(Upd765Peripheral.class), "the drive stayed on");
  }
}
