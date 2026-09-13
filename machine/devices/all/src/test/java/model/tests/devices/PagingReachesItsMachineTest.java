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
 * That a write to the paging port reaches the machine that is running.
 * <p>
 * Peripherals are held in a map keyed by their class, so the machines that share a paging port
 * cannot each keep one registered: the one that answers is whichever registered last. Selecting a
 * machine calls its init, which registers its own again, and that is what keeps the port pointed
 * at the machine in front. A machine that registers only once, at construction, has its own
 * replaced before it ever runs - and its writes go to another machine's paging state, silently,
 * because the memory is shared and the pages still come out roughly right.
 * <p>
 * Asked of every machine with the 128's paging rather than of a list, so a machine added later is
 * covered by having been added.
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
   * A 128 and a +2 leave the video data on the bus, so reading the paging port is also a write of
   * whatever was floating there - a quirk of those two that games rely on. The +2A and +3 drive
   * their bus and the Pentagon has none, so on them the same read must page nothing. This used to
   * be decided by naming the two classes inside PeripheralRegistry.
   */
  @Test
  void onlyAMachineWithAFloatingBusPagesWhenItsPortIsRead() {
    for (Spectrum model : speccy().machine.getMachineTypes()) {
      if (!model.pagesThrough7ffd()) continue;

      Speccy speccy = speccy();
      Spectrum wanted = speccy.machine.getMachineTypes().stream()
          .filter(type -> type.getClass() == model.getClass()).findFirst().orElseThrow();
      speccy.machine.selectDefault();
      speccy.machine.select(wanted);

      speccy.ports.write(0x7ffd, (byte) 0x00);
      speccy.ports.read(0x7ffd);

      // 0xff is what an unattached port reads outside the screen, and bit 5 of it locks paging.
      // Said by name rather than by asking the machine the same question the code under test
      // asks, which would agree with itself no matter what either of them answered.
      boolean pages = List.of("Spectrum 128K", "Spectrum Plus 2").contains(wanted.getName());
      assertEquals(pages, wanted.paging().locked(),
          wanted.getName() + " after reading its paging port");
    }
  }

  /**
   * Switching straight from one machine to another that shares a pager, with no 48K in between.
   * The pager is one object now and binds to the machine it is switched on for, so what keeps it
   * pointed at the right one is that a machine's reset clears the bus and switches everything on
   * again. Going through the default machine would hide that; this does not.
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
      speccy.machine.select(wanted);

      asked.add(wanted.getName());
      // Bit 5 of the 128's paging port locks paging, and the machine remembers that it is locked.
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
   * A +2A saved in 48K mode: 0x7ffd carries the lock bit, 0x1ffd the high bit of the ROM number,
   * and the game reads its font from that ROM. Writing the lock first left 0x1ffd unheard, which
   * paged the syntax checker where the 48K BASIC belonged and drew every text as noise.
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
