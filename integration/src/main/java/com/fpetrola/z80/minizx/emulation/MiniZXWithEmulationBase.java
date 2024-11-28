/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.Base64Utils;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;
import snapshots.*;

import java.io.File;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class MiniZXWithEmulationBase {
  protected static void loadSnapshotFromBase64(final State<WordNumber> state, String base64Z80File) {
    try {
      SpectrumState snapState = MiniZXWithEmulationBase.getSpectrumStateFromBase64(base64Z80File);
      loadIntoMemory(state, snapState);
    } catch (SnapshotException e) {
      throw new RuntimeException(e);
    }
  }
  protected void loadSnapshotFromFile(String fileName, final State<WordNumber> state) {
    try {
      File file = new File(fileName);
      SnapshotFile snap = SnapshotFactory.getSnapshot(file);
      SpectrumState snapState = snap.load(file);
      loadIntoMemory(state, snapState);
    } catch (SnapshotException e) {
      throw new RuntimeException(e);
    }
  }
  private static void loadIntoMemory(State<WordNumber> state, SpectrumState snapState) {
    RegistersBase registersBase = new RegistersBase<>(state) {
      public VirtualRegisterFactory getVirtualRegisterFactory() {
        return new VirtualRegisterFactory(null, null, null);
      }
    };

    registersBase.setZ80State(snapState.getZ80State());

    MemoryState memoryState = snapState.getMemoryState();
    Memory memory = state.getMemory();
    Object[] data = memory.getData();

    byte[][] ram = memoryState.getRam();
    int position = 16384;

    position = MiniZXWithEmulationBase.copyPage(ram, 5, position, data);
    position = MiniZXWithEmulationBase.copyPage(ram, 2, position, data);
    MiniZXWithEmulationBase.copyPage(ram, 0, position, data);
  }

  private static SpectrumState getSpectrumStateFromBase64(String t1) throws SnapshotException {
    return new SnapshotZ80().loadFromString(Base64Utils.gzipDecompressFromBase64(t1));
  }

  private static int copyPage(byte[][] ram, int page, int position, Object[] data) {
    for (int i = 0; i < ram[page].length; i++) {
      data[position++] = createValue(ram[page][i]);
    }
    return position;
  }
}
