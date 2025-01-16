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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.cpu.MemorySetter;
import com.fpetrola.z80.cpu.RegistersSetter;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase;
import com.fpetrola.z80.opcodes.references.WordNumber;
import snapshots.*;

import java.io.File;

public class SnapshotLoader {
  public static <T extends WordNumber> byte[] setupStateWithSnapshot(RegistersSetter registersSetter, String fileName, State<T> state) {
    try {
      File file = new File(fileName);
      SnapshotFile snap = new SnapshotZ80();
      SpectrumState spectrumState = snap.load(file);

      return setupStateFromSpectrumState(spectrumState, registersSetter, state);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T extends WordNumber> byte[] setupStateFromSpectrumState(SpectrumState spectrumState, RegistersSetter registersSetter, State<T> state) {
    MemorySetter memorySetter = new MemorySetter(state.getMemory(), MiniZXWithEmulationBase.createROM());

    setZ80State(registersSetter, spectrumState.getZ80State());
//      registersBase.setZ80State(spectrumState.getZ80State());

    MemoryState memoryState = spectrumState.getMemoryState();

    byte[][] ram = memoryState.getRam();
    byte[] result = new byte[0x10000];

    int position = 16384;
    position = copyPage(ram, 5, position, result);
    position = copyPage(ram, 2, position, result);
    copyPage(ram, 0, position, result);

    memorySetter.setData(result);
    return result;
  }

  private static <T extends WordNumber> int copyPage(byte[][] ram, int page, int position, byte[] result) {
    if (ram[page] != null)
      for (int i = 0; i < ram[page].length; i++) {
        result[position++] = ram[page][i];
      }
    return position;
  }

  private static <T extends WordNumber> int copyPage2(byte[][] ram, int page, int position, State<T> state1) {
    Memory<T> memory = state1.getMemory();
    if (ram[page] != null)
      for (int i = 0; i < ram[page].length; i++) {
        memory.write(WordNumber.createValue(position++), WordNumber.createValue(ram[page][i]));
      }
    return position;
  }

  public static <T extends WordNumber> void setZ80State(RegistersSetter<T> registersBase, Z80State state) {
    registersBase.setRegA(state.getRegA());
    registersBase.setFlags(state.getRegF());
    registersBase.setRegB(state.getRegB());
    registersBase.setRegC(state.getRegC());
    registersBase.setRegD(state.getRegD());
    registersBase.setRegE(state.getRegE());
    registersBase.setRegH(state.getRegH());
    registersBase.setRegL(state.getRegL());
    registersBase.setRegAx(state.getRegAx());
    registersBase.setRegFx(state.getRegFx());
    registersBase.setRegBx(state.getRegBx());
    registersBase.setRegCx(state.getRegCx());
    registersBase.setRegDx(state.getRegDx());
    registersBase.setRegEx(state.getRegEx());
    registersBase.setRegHx(state.getRegHx());
    registersBase.setRegLx(state.getRegLx());
    registersBase.setRegIX(state.getRegIX());
    registersBase.setRegIY(state.getRegIY());
    registersBase.setRegSP(state.getRegSP());
    registersBase.setRegPC(state.getRegPC());
    registersBase.setRegI(state.getRegI());
    registersBase.setRegR(state.getRegR());
    registersBase.setMemptr(state.getMemPtr());
    registersBase.setHalted(state.isHalted());
    registersBase.setFfIFF1(state.isIFF1());
    registersBase.setFfIFF2(state.isIFF2());
    registersBase.setModeINT(state.getIM().ordinal());
    registersBase.setActiveINT(state.isINTLine());
    registersBase.setPendingEI(state.isPendingEI());
    registersBase.setActiveNMI(state.isNMI());
    registersBase.setFlagQ(false);
    registersBase.setLastFlagQ(state.isFlagQ());
  }
}
