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
 * Pins which peripherals are active per machine, independent of how that decision is
 * implemented (currently MachinesPeriph, moving toward each device declaring its own fit).
 * Every registered peripheral is checked against every machine with optionals both wanted and
 * unwanted, distinguishing built-in from merely-offered.
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

  /** Peripherals active with optional devices switched off. */
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

  /** Peripherals active with optionals requested: Melodik fits only the 48K family; Kempston
   * fits everything except the Pentagon, which decodes that port itself. */
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
   * Regression: the +3's drive was built and registered but never activated, because its
   * machine referenced it by a name matching no class, landing on an unregistered generic
   * entry. A +2A (the same machine minus the drive) must not activate one either.
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
   * Regression: the +3 ROM's drive-ready check used to see an unanswered-port 0xff instead of
   * the real controller; this runs long enough to confirm the boot screen is reached.
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
