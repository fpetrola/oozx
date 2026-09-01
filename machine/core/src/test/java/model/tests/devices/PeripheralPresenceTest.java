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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.Spectrum;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.devices.ay.AyPlus3Peripheral;
import com.fpetrola.oozx.speccy.devices.ay.MelodikPeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Upd765Peripheral;
import com.fpetrola.oozx.speccy.devices.joystick.KempstonLoosePeriphPeripheral;
import com.fpetrola.oozx.speccy.devices.joystick.KempstonStrictPeripheral;
import com.fpetrola.oozx.speccy.devices.memory.Spec128MemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.memory.SeMemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.memory.SpecPlus3MemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.ula.UlaFullDecodePeripheral;
import com.fpetrola.oozx.speccy.devices.ula.UlaPeripheral;
import com.fpetrola.oozx.speccy.peripherals.ZxPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which devices are switched on in which machine, said once so that changing how that is decided
 * cannot change the answers by accident.
 * <p>
 * The table lives in one class today, MachinesPeriph, and is about to become each device declaring
 * where it fits. This is the net under that: every registered peripheral against every machine,
 * asked twice - with the optional ones wanted and unwanted - so a device that is built in is told
 * apart from one that is merely offered.
 */
class PeripheralPresenceTest {

  private static final List<Class<? extends ZxPeripheral>> REGISTERED = List.of(
      UlaPeripheral.class, UlaFullDecodePeripheral.class,
      Spec128MemoryPeripheral.class, SpecPlus3MemoryPeripheral.class, SeMemoryPeripheral.class,
      AyPeripheral.class, AyPlus3Peripheral.class, MelodikPeripheral.class,
      KempstonStrictPeripheral.class, KempstonLoosePeriphPeripheral.class,
      Upd765Peripheral.class);

  private Speccy speccy(boolean wantOptionals) {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.settings.current.melodik = wantOptionals;
    speccy.settings.current.joyKempston = wantOptionals;
    return speccy;
  }

  private String activeOn(Speccy speccy, Spectrum machine) {
    speccy.machine.select(machine);
    speccy.periph.update();

    Set<String> active = new LinkedHashSet<>();
    for (Class<? extends ZxPeripheral> peripheral : REGISTERED) {
      if (speccy.periph.isActive(peripheral)) active.add(peripheral.getSimpleName());
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

    has("UlaPeripheral", speccy.spec48, speccy);
    has("UlaPeripheral", speccy.spec48Ntsc, speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral", speccy.spec128, speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral", speccy.specPlus2, speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral", speccy.specPlus2a, speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral", speccy.specPlus3, speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral", speccy.specPlus3e, speccy);
    has("UlaFullDecodePeripheral Spec128MemoryPeripheral AyPeripheral", speccy.pentagon, speccy);
  }

  /**
   * And what it accepts when asked for: the Melodik is a 48K box, the Kempston plugs into
   * everything except the Pentagon, which decodes its own port and is given none.
   */
  @Test
  void whatEachMachineAcceptsWhenWanted() {
    Speccy speccy = speccy(true);

    has("UlaPeripheral MelodikPeripheral KempstonStrictPeripheral", speccy.spec48, speccy);
    has("UlaPeripheral MelodikPeripheral KempstonStrictPeripheral", speccy.spec48Ntsc, speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral KempstonStrictPeripheral", speccy.spec128, speccy);
    has("UlaPeripheral Spec128MemoryPeripheral AyPeripheral KempstonStrictPeripheral", speccy.specPlus2, speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral KempstonStrictPeripheral", speccy.specPlus2a, speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral KempstonStrictPeripheral", speccy.specPlus3, speccy);
    has("UlaPeripheral SpecPlus3MemoryPeripheral AyPlus3Peripheral KempstonStrictPeripheral", speccy.specPlus3e, speccy);
    has("UlaFullDecodePeripheral Spec128MemoryPeripheral AyPeripheral", speccy.pentagon, speccy);
  }

  /**
   * Three peripherals this build registers and never switches on, said out loud so that turning
   * one on is a decision and not a surprise. The loose-decoding Kempston belongs to the TC2048 and
   * the SE memory to the SE, neither of which exists here - but the +3's disk controller is a
   * mistake: its machine asks for it by a Periph.Type that names no class, so the request lands
   * nowhere and no port of it has ever answered.
   */
  @Test
  void theOnesNothingEverSwitchesOn() {
    Speccy speccy = speccy(true);

    for (Spectrum machine : speccy.machine.getMachineTypes()) {
      String active = activeOn(speccy, machine);
      for (String never : List.of("SeMemoryPeripheral", "KempstonLoosePeriphPeripheral", "Upd765Peripheral")) {
        assertEquals(false, active.contains(never), never + " is now active on " + machine.getName());
      }
    }
  }
}
