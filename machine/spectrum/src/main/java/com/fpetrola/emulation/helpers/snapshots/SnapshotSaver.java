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

package com.fpetrola.emulation.helpers.snapshots;

import com.fpetrola.z80.cpu.RegistersGetter;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.transformations.Base64Utils;
import com.fpetrola.emulation.SnapshotUnicodePacker;
import com.fpetrola.emulation.helpers.machine.Keyboard;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import z80core.IntMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// The inverse of SnapshotLoader: takes a running State and writes it out as a snapshot.
public class SnapshotSaver {

  public static boolean setupSnapshotWithState(RegistersGetter registersGetter, String fileName, State state) {
    try {
      File file = new File(fileName);

      SpectrumState spectrumState = new SpectrumState();
      spectrumState.setSpectrumModel(MachineTypes.SPECTRUM48K);

      Z80State z80State = extractZ80State(registersGetter, state);
      spectrumState.setZ80State(z80State);

      MemoryState memoryState = extractMemoryState(state);
      spectrumState.setMemoryState(memoryState);

      spectrumState.setAY8912State(new AY8912State());
      // No joystick is ever plugged into a machine to remember; NONE is the answer the
      // format still requires an entry for.
      spectrumState.setJoystick(com.fpetrola.emulation.helpers.machine.Keyboard.JoystickModel.NONE);
      spectrumState.setTstates(state.clock.getTStates());

      SnapshotFile snapshot = SnapshotFactory.getSnapshot(file);
      if (snapshot != null) {
        return snapshot.save(file, spectrumState);
      }

      return false;
    } catch (Exception e) {
      throw new RuntimeException("Error saving snapshot: " + e.getMessage(), e);
    }
  }

  private static Z80State extractZ80State(RegistersGetter registersGetter, State state) {
    Z80State z80State = new Z80State();

    z80State.setRegA(registersGetter.getRegA());
    z80State.setRegF(registersGetter.getRegF());
    z80State.setRegB(registersGetter.getRegB());
    z80State.setRegC(registersGetter.getRegC());
    z80State.setRegD(registersGetter.getRegD());
    z80State.setRegE(registersGetter.getRegE());
    z80State.setRegH(registersGetter.getRegH());
    z80State.setRegL(registersGetter.getRegL());

    z80State.setRegAx(registersGetter.getRegAx());
    z80State.setRegFx(registersGetter.getRegFx());
    z80State.setRegBx(registersGetter.getRegBx());
    z80State.setRegCx(registersGetter.getRegCx());
    z80State.setRegDx(registersGetter.getRegDx());
    z80State.setRegEx(registersGetter.getRegEx());
    z80State.setRegHx(registersGetter.getRegHx());
    z80State.setRegLx(registersGetter.getRegLx());

    z80State.setRegPC(registersGetter.getRegPC());
    z80State.setRegSP(registersGetter.getRegSP());
    z80State.setRegIX(registersGetter.getRegIX());
    z80State.setRegIY(registersGetter.getRegIY());
    z80State.setRegI(registersGetter.getRegI());
    z80State.setRegR(registersGetter.getRegR());
    z80State.setMemPtr(registersGetter.getMemPtr());

    z80State.setIFF1(registersGetter.getIFF1());
    z80State.setIFF2(registersGetter.getIFF2());
    z80State.setIM(IntMode.values()[registersGetter.getModeINT()]);
    z80State.setHalted(registersGetter.isHalted());
    z80State.setPendingEI(registersGetter.isPendingEI());

    return z80State;
  }

  private static MemoryState extractMemoryState(State state) {
    MemoryState memoryState = new MemoryState();

    byte[][] ram = new byte[8][0x4000];

    Memory memory = state.getMemory();

    // The memory map here is fixed: page 5 at 0x4000, page 2 at 0x8000, page 0 at 0xC000.
    int base = 0x4000;
    for (int i = 0; i < 0x4000; i++) {
      ram[5][i] = (byte) memory.read(base + i);
    }

    base += 0x4000;
    for (int i = 0; i < 0x4000; i++) {
      ram[2][i] = (byte) memory.read(base + i);
    }

    base += 0x4000;
    for (int i = 0; i < 0x4000; i++) {
      ram[0][i] = (byte) memory.read(base + i);
    }

    memoryState.setRam(ram);

    return memoryState;
  }

  public static byte[] getSnapshotAsBytes(RegistersGetter registersGetter, State state) throws SnapshotException {
    SpectrumState spectrumState = new SpectrumState();

    spectrumState.setSpectrumModel(MachineTypes.SPECTRUM48K);

    Z80State z80State = extractZ80State(registersGetter, state);
    spectrumState.setZ80State(z80State);

    MemoryState memoryState = extractMemoryState(state);
    spectrumState.setMemoryState(memoryState);

    spectrumState.setAY8912State(new AY8912State());
    spectrumState.setTstates(state.clock.getTStates());

    spectrumState.setJoystick(Keyboard.JoystickModel.NONE);

    SnapshotZ80 snapshot = new SnapshotZ80();
    return snapshot.saveToBytes(spectrumState);
  }

  public static String getSnapshotAsCompressedBase64(RegistersGetter registersGetter, State state) throws SnapshotException {
    byte[] snapshotBytes = getSnapshotAsBytes(registersGetter, state);
    return Base64Utils.gzipArrayCompressToBase64(snapshotBytes);
  }

  public static SpectrumState loadSnapshotFromCompressedBase64(String compressedBase64) throws SnapshotException {
    if (compressedBase64 == null || compressedBase64.isEmpty()) {
      throw new SnapshotException("INVALID_SNAPSHOT_DATA");
    }
    byte[] decompressedBytes = Base64Utils.gzipDecompressFromBase64(compressedBase64);
    SnapshotZ80 snapshot = new SnapshotZ80();
    return snapshot.loadFromBytes(decompressedBytes);
  }

  private static byte[] gzipCompress(byte[] data) throws Exception {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
        gzip.write(data);
      }
      return out.toByteArray();
    }
  }

  private static byte[] gzipDecompress(byte[] data) throws Exception {
    try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
      try (GZIPInputStream gzip = new GZIPInputStream(in)) {
        return gzip.readAllBytes();
      }
    }
  }

  public static String getSnapshotAsUnicodePacked(RegistersGetter registersGetter, State state) {
    try {
      byte[] snapshotBytes = getSnapshotAsBytes(registersGetter, state);
      byte[] compressedBytes = gzipCompress(snapshotBytes);
      return SnapshotUnicodePacker.packToUnicodeString(compressedBytes);
    } catch (Exception e) {
      throw new RuntimeException("Error compressing snapshot: " + e.getMessage());
    }
  }

  public static SpectrumState loadSnapshotFromUnicodePacked(String unicodePacked)  {
    try {
      if (unicodePacked == null || unicodePacked.isEmpty()) {
        throw new SnapshotException("INVALID_SNAPSHOT_DATA");
      }
      byte[] compressedBytes = SnapshotUnicodePacker.unpackFromUnicodeString(unicodePacked);
      byte[] decompressedBytes = gzipDecompress(compressedBytes);
      SnapshotZ80 snapshot = new SnapshotZ80();
      return snapshot.loadFromBytes(decompressedBytes);
    } catch (Exception e) {
      throw new RuntimeException("Error decompressing snapshot: " + e.getMessage());
    }
  }
}
