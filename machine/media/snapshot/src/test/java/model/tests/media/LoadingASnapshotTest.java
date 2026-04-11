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

package model.tests.media;

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.emulation.helpers.snapshots.MemoryState;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.emulation.helpers.snapshots.Z80State;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** What putting a snapshot into a machine does to the machine, beyond the bytes it carries. */
class LoadingASnapshotTest extends MachineTest {

  /** The five keys of a half row, asked of the matrix rather than of the port: which port reads it is the ULA's, and that is the ULA's test. */
  private static final int KEYS = 0x1F;

  private static SpectrumState aFortyEight() {
    SpectrumState state = new SpectrumState();
    state.setSpectrumModel(MachineTypes.SPECTRUM48K);
    state.setZ80State(new Z80State());
    state.setMemoryState(new MemoryState());
    return state;
  }

  /**
   * Whoever loads a snapshot is holding a key while they do it - the one that opened the menu -
   * and what a program reads next has nothing to do with it. Letting go of the keyboard is for
   * the same reason: a game that arrives with a key held walks into a wall or never leaves its
   * title screen, and nothing the person does explains it.
   */
  @Test
  void itLetsGoOfEveryKeyThatWasBeingHeld() {
    Speccy speccy = silentMachine();
    speccy.keys.press(SpectrumKey.A);
    assertNotEquals(KEYS, speccy.keys.read(0xFD) & KEYS, "A should be down to begin with");

    Snapshots.of(speccy).load(aFortyEight());

    assertEquals(KEYS, speccy.keys.read(0xFD) & KEYS, "the snapshot arrived with A still held");
    assertEquals(KEYS, speccy.keys.read(0x00) & KEYS, "and nothing is held on any other row either");
  }
}
