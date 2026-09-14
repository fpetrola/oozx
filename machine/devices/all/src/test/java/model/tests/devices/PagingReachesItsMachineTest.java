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

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.emulation.helpers.snapshots.MemoryState;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.emulation.helpers.snapshots.Z80State;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression coverage: since peripherals are keyed by class, machines sharing a paging port
 * would otherwise silently share one registration, with only the last-selected machine's
 * writes landing correctly - a machine registering only at construction (not on selection)
 * loses its own port to whichever selected after it. Iterates every 128-paging model rather
 * than a fixed list, so newly added models are covered automatically.
 */
class PagingReachesItsMachineTest {

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  /**
   * On a 128/+2, reading the paging port also writes back the floating bus value (games depend
   * on this); the +2A/+3 drive their bus and the Pentagon has none, so a read must page nothing
   * there. Previously implemented by naming these two classes inside PeripheralRegistry.
   */
  @Test
  void onlyAMachineWithAFloatingBusPagesWhenItsPortIsRead() {
    for (Spectrum model : speccy().machine.getMachineTypes()) {
      if (!model.pagesThrough7ffd()) continue;

      Speccy speccy = speccy();
      Spectrum wanted = speccy.machine.getMachineTypes().stream()
          .filter(type -> type.getClass() == model.getClass()).findFirst().orElseThrow();
      speccy.machine.selectDefault();
      if (!hasItsRoms(speccy, wanted)) continue;

      speccy.ports.write(0x7ffd, (byte) 0x00);
      speccy.ports.read(0x7ffd);

      // Expected models named explicitly, not derived from the code under test, to avoid a
      // self-confirming assertion.
      boolean pages = List.of("Spectrum 128K", "Spectrum Plus 2").contains(wanted.getName());
      assertEquals(pages, wanted.paging().locked(),
          wanted.getName() + " after reading its paging port");
    }
  }

  /**
   * Switches directly between two machines sharing a pager (skipping the 48K default), which
   * would mask a binding bug that going through the default machine's reset would hide.
   */
  @Test
  void aPagerFollowsTheMachineEvenWhenSwitchedToDirectly() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.machine.model(Spec128.class));
    speccy.machine.select(speccy.machine.model(SpecPlus2.class));

    speccy.ports.write(0x7ffd, (byte) 0x20);

    assertTrue(speccy.machine.model(SpecPlus2.class).paging().locked(), "the +2 did not get its own paging write");
    assertFalse(speccy.machine.model(Spec128.class).paging().locked(), "the write reached the machine left behind");
  }

  /** A machine whose ROM is not part of this build cannot be put in, so it is not asked. */
  private static boolean hasItsRoms(Speccy speccy, Spectrum machine) {
    try {
      speccy.machine.select(machine);
      return true;
    } catch (com.fpetrola.oozx.speccy.machine.RomNotLoadedException itsRomIsNotHere) {
      return false;
    }
  }

  @Test
  void everyMachineWithAPagingPortOwnsIt() {
    List<String> deaf = new ArrayList<>();
    List<String> asked = new ArrayList<>();

    for (Spectrum model : speccy().machine.getMachineTypes()) {
      if (!model.pagesThrough7ffd()) {
        continue;
      }
      Speccy speccy = speccy();
      Spectrum wanted = speccy.machine.getMachineTypes().stream()
          .filter(type -> type.getClass() == model.getClass()).findFirst().orElseThrow();
      speccy.machine.selectDefault();
      if (!hasItsRoms(speccy, wanted)) continue;

      asked.add(wanted.getName());
      // Bit 5 of 0x7ffd is the paging lock; the machine must retain that it is now locked.
      speccy.ports.write(0x7ffd, (byte) 0x20);
      if (!speccy.machine.current.paging().locked()) {
        deaf.add(wanted.getName());
      }
    }

    assertTrue(asked.size() >= 4, "expected several machines to page this way, asked " + asked);
    if (!deaf.isEmpty()) {
      fail("the paging port did not reach " + deaf + "; it reached another machine's state instead");
    }
  }

  /**
   * Regression: applying the 0x7ffd lock bit before 0x1ffd's ROM-select bit caused 0x1ffd to be
   * ignored, paging in the wrong ROM (syntax checker instead of 48K BASIC) and corrupting text.
   */
  @Test
  void aSnapshotLockedIntoFortyEightModeComesBackWithThatRom() {
    Speccy speccy = speccy();
    SpectrumState state = new SpectrumState();
    state.setSpectrumModel(MachineTypes.SPECTRUMPLUS2A);
    state.setZ80State(new Z80State());
    state.setMemoryState(new MemoryState());
    state.setPort7ffd(0x30);
    state.setPort1ffd(0x04);

    Snapshots.of(speccy).load(state);

    byte[] glyphOfA = new byte[8];
    for (int i = 0; i < 8; i++) glyphOfA[i] = (byte) speccy.memory.peek(0x3E08 + i);
    assertArrayEquals(new byte[]{0, 0x3C, 0x42, 0x42, 0x7E, 0x42, 0x42, 0}, glyphOfA, "the font of the 48K ROM");
  }
}
