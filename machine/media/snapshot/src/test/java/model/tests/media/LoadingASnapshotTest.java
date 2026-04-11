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
