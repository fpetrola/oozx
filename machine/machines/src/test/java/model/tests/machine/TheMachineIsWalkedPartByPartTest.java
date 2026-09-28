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
import com.fpetrola.oozx.speccy.machine.Paging;
import com.fpetrola.oozx.speccy.modules.display.Border;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.parts.Visitable;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The machine walked part by part: what a file format meets when it reads a snapshot into the
 * machine or writes one out of it. Each part presents itself, in an order that does not change,
 * and a peripheral only while it is switched on.
 */
class TheMachineIsWalkedPartByPartTest {

  /** A device of the kind a plugin brings, and nothing else. */
  static class Gadget extends AbstractPeripheral {
    Gadget() {
      super(List.of());
    }
  }

  Speccy speccy;

  @BeforeEach
  void aMachine() {
    speccy = Speccy.create(new SpectrumZ80Clock(), binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
  }

  List<Visitable> walked() {
    List<Visitable> parts = new ArrayList<>();
    speccy.accept(parts::add);
    return parts;
  }

  @Test
  void theCoreComesFirstAndAlwaysInTheSameOrder() {
    List<Visitable> parts = walked();
    assertSame(speccy.machine, parts.get(0));
    assertSame(speccy.cpu, parts.get(1));
    assertSame(speccy.banks, parts.get(2));
    assertSame(speccy.machine.current.paging(), parts.get(3));
    assertSame(speccy.display.border, parts.get(4));
    assertSame(speccy.zxClock, parts.get(5));
    assertEquals(List.of(Machine.class, Cpu.class, SpectrumMemory.class, Paging.class, Border.class, SpectrumZ80Clock.class),
        parts.subList(0, 6).stream().map(Object::getClass).toList());
  }

  @Test
  void eachPartIsMetOnce() {
    List<Visitable> parts = walked();
    assertEquals(parts.size(), parts.stream().distinct().count());
  }

  @Test
  void aPeripheralIsMetWhileItIsSwitchedOn() {
    Gadget gadget = new Gadget();
    speccy.peripheralRegistry.register(gadget);
    assertFalse(walked().contains(gadget));

    speccy.peripheralRegistry.activateType(Gadget.class, true);
    assertTrue(walked().contains(gadget));

    speccy.peripheralRegistry.activateType(Gadget.class, false);
    assertFalse(walked().contains(gadget));
  }

  @Test
  void peripheralsComeAfterTheCoreInTheOrderTheyWereRegistered() {
    Gadget first = new Gadget();
    Gadget second = new Gadget() {
    };
    speccy.peripheralRegistry.register(first);
    speccy.peripheralRegistry.register(second);
    speccy.peripheralRegistry.activateType(second.getClass(), true);
    speccy.peripheralRegistry.activateType(Gadget.class, true);
    List<Visitable> parts = walked();
    assertTrue(parts.indexOf(first) > 5);
    assertTrue(parts.indexOf(first) < parts.indexOf(second));
  }

  @Test
  void theBorderSaysItsColour() {
    speccy.display.border.becomes(5);
    assertEquals(5, speccy.display.border.colour());
  }
}
