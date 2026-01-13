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

package com.fpetrola.emulation.helpers.snapshots;

import com.fpetrola.z80.cpu.MemorySetter;
import com.fpetrola.z80.cpu.RegistersSetter;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase;

import java.io.File;

public class SnapshotLoader {
  /**
   * Reads a snapshot file without applying it, so that a caller can look at what it says about
   * the machine BEFORE loading it - which model it was taken on above all. The loading path that
   * flattens everything into a 48K map cannot ask that question, because by the time it has a
   * state it has already decided the answer.
   *
   * @return null for a file no loader recognises
   */
  public static SpectrumState readSnapshot(String fileName) {
    try {
      File file = new File(fileName);
      SnapshotFile snapshot = SnapshotFactory.getSnapshot(file);
      return snapshot == null ? null : snapshot.load(file);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static  byte[] setupStateWithSnapshot(RegistersSetter registersSetter, String fileName, State state) {

    try {
      File file = new File(fileName);
      byte[] result = null;

      SnapshotFile snapshot = SnapshotFactory.getSnapshot(file);

      if (snapshot != null) {
        SpectrumState snapState = snapshot.load(file);
        result= setupFromSpectrumState(registersSetter, state, snapState);
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] setupFromSpectrumState(RegistersSetter registersSetter, State state, SpectrumState snapState) {
    byte[] result = new byte[0x10000];

    state.clock.setTStates(snapState.getTstates());

    setZ80State(registersSetter, snapState.getZ80State());

    MemoryState memoryState = snapState.getMemoryState();
    byte[][] ram = memoryState.getRam();
    int position = 16384;
    position = copyPage(ram, 5, position, result);
    position = copyPage(ram, 2, position, result);
    copyPage(ram, 0, position, result);
    MemorySetter memorySetter = new MemorySetter(state.getMemory(), MiniZXWithEmulationBase.createROM(), state);
    memorySetter.setData(result);
    return result;
  }

  private static  int copyPage(byte[][] ram, int page, int position, byte[] result) {
    if (ram[page] != null)
      for (int i = 0; i < ram[page].length; i++) {
        result[position++] = ram[page][i];
      }
    return position;
  }

  private static  int copyPage2(byte[][] ram, int page, int position, State state1) {
    Memory memory = state1.getMemory();
    if (ram[page] != null)
      for (int i = 0; i < ram[page].length; i++) {
        memory.write(position++, ram[page][i]);
      }
    return position;
  }

  public static  void setZ80State(RegistersSetter registersBase, Z80State state) {
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
