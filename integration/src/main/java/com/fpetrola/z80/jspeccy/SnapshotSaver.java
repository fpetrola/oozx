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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.cpu.RegistersGetter;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import snapshots.*;
import machine.MachineTypes;
import z80core.IntMode;

import java.io.File;

/**
 * SnapshotSaver - Clase que permite guardar el estado actual de la emulación como un snapshot Z80.
 * Este es el inverso de SnapshotLoader: mientras que SnapshotLoader carga un snapshot y lo aplica
 * a un State, SnapshotSaver toma un State y lo guarda como snapshot.
 */
public class SnapshotSaver {

  /**
   * Guarda el estado actual de la emulación como un snapshot Z80.
   *
   * @param registersGetter Interface para leer los registros de la CPU
   * @param fileName        Nombre del archivo donde guardar el snapshot
   * @param state           Estado actual de la emulación
   * @return true si el snapshot se guardó exitosamente, false en caso contrario
   */
  public static boolean setupSnapshotWithState(RegistersGetter registersGetter, String fileName, State state) {
    try {
      File file = new File(fileName);

      // Crear el objeto SpectrumState con la información actual
      SpectrumState spectrumState = new SpectrumState();

      // Establecer el modelo de máquina (se asume 48K por defecto, podría parametrizarse)
      spectrumState.setSpectrumModel(MachineTypes.SPECTRUM48K);

      // Copiar el estado del Z80
      Z80State z80State = extractZ80State(registersGetter, state);
      spectrumState.setZ80State(z80State);

      // Copiar el estado de memoria
      MemoryState memoryState = extractMemoryState(state);
      spectrumState.setMemoryState(memoryState);

      // Copiar otros estados (AY8912, etc)
      spectrumState.setAY8912State(new AY8912State());
      spectrumState.setTstates(state.clock.getTStates());

      // Guardar usando SnapshotZ80
      SnapshotFile snapshot = SnapshotFactory.getSnapshot(file);
      if (snapshot != null) {
        return snapshot.save(file, spectrumState);
      }

      return false;
    } catch (Exception e) {
      throw new RuntimeException("Error saving snapshot: " + e.getMessage(), e);
    }
  }

  /**
   * Extrae el estado del Z80 desde RegistersGetter y State
   */
  private static Z80State extractZ80State(RegistersGetter registersGetter, State state) {
    Z80State z80State = new Z80State();

    // Copiar registros de 8 bits
    z80State.setRegA(registersGetter.getRegA());
    z80State.setRegF(registersGetter.getRegF());
    z80State.setRegB(registersGetter.getRegB());
    z80State.setRegC(registersGetter.getRegC());
    z80State.setRegD(registersGetter.getRegD());
    z80State.setRegE(registersGetter.getRegE());
    z80State.setRegH(registersGetter.getRegH());
    z80State.setRegL(registersGetter.getRegL());

    // Copiar registros alternativos
    z80State.setRegAx(registersGetter.getRegAx());
    z80State.setRegFx(registersGetter.getRegFx());
    z80State.setRegBx(registersGetter.getRegBx());
    z80State.setRegCx(registersGetter.getRegCx());
    z80State.setRegDx(registersGetter.getRegDx());
    z80State.setRegEx(registersGetter.getRegEx());
    z80State.setRegHx(registersGetter.getRegHx());
    z80State.setRegLx(registersGetter.getRegLx());

    // Copiar registros de propósito especial
    z80State.setRegPC(registersGetter.getRegPC());
    z80State.setRegSP(registersGetter.getRegSP());
    z80State.setRegIX(registersGetter.getRegIX());
    z80State.setRegIY(registersGetter.getRegIY());
    z80State.setRegI(registersGetter.getRegI());
    z80State.setRegR(registersGetter.getRegR());
    z80State.setMemPtr(registersGetter.getMemPtr());

    // Copiar flags de interrupción
    z80State.setIFF1(registersGetter.getIFF1());
    z80State.setIFF2(registersGetter.getIFF2());
    z80State.setIM(IntMode.values()[registersGetter.getModeINT()]);
    z80State.setHalted(registersGetter.isHalted());
    z80State.setPendingEI(registersGetter.isPendingEI());

    return z80State;
  }

  /**
   * Extrae el estado de memoria desde State
   */
  private static MemoryState extractMemoryState(State state) {
    MemoryState memoryState = new MemoryState();

    // Crear el array de RAM (8 páginas de 16KB cada una)
    byte[][] ram = new byte[8][0x4000];

    Memory memory = state.getMemory();

    // Copiar las páginas de RAM
    // El memory es mapeado en bloques: ROM, Página 5, Página 2, Página 0
    // Página 5 (dirección 0x4000-0x7FFF)
    for (int i = 0; i < 0x4000; i++) {
      ram[5][i] = (byte) memory.read(5, i);
    }

    // Página 2 (dirección 0x8000-0xBFFF)
    for (int i = 0; i < 0x4000; i++) {
      ram[2][i] = (byte) memory.read(2, i);
    }

    // Página 0 (dirección 0xC000-0xFFFF)
    for (int i = 0; i < 0x4000; i++) {
      ram[0][i] = (byte) memory.read(0, i);
    }

    memoryState.setRam(ram);

    return memoryState;
  }

  /**
   * Versión alternativa que guarda el snapshot en formato binario comprimido
   * para uso en configuración (retorna byte array en lugar de guardar a archivo)
   */
  public static byte[] getSnapshotAsBytes(RegistersGetter registersGetter, State state) throws SnapshotException {
    // Crear el objeto SpectrumState con la información actual
    SpectrumState spectrumState = new SpectrumState();

    // Establecer el modelo de máquina
    spectrumState.setSpectrumModel(MachineTypes.SPECTRUM48K);

    // Copiar el estado del Z80
    Z80State z80State = extractZ80State(registersGetter, state);
    spectrumState.setZ80State(z80State);

    // Copiar el estado de memoria
    MemoryState memoryState = extractMemoryState(state);
    spectrumState.setMemoryState(memoryState);

    // Copiar otros estados
    spectrumState.setAY8912State(new AY8912State());
    spectrumState.setTstates(state.clock.getTStates());

    // Convertir a bytes usando SnapshotZ80
    SnapshotZ80 snapshot = new SnapshotZ80();
    return snapshot.saveToBytes(spectrumState);
  }
}
