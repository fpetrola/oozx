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

package com.fpetrola.oozx.fuse;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.PointerType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Z80Loader {

  public static class libspectrum_snap extends PointerType {
  }

  public interface LibSpectrum extends Library {
    LibSpectrum INSTANCE = Native.load("spectrum", LibSpectrum.class);

    int libspectrum_init();

    void libspectrum_end();

    libspectrum_snap libspectrum_snap_alloc();

    int libspectrum_snap_free(libspectrum_snap snap);

    int libspectrum_snap_read(libspectrum_snap snap,
                              byte[] buffer,
                              NativeLong length,
                              int type,
                              String filename);

    // Getters de registros
    short libspectrum_snap_pc(libspectrum_snap snap);

    short libspectrum_snap_sp(libspectrum_snap snap);

    byte libspectrum_snap_a(libspectrum_snap snap);

    byte libspectrum_snap_f(libspectrum_snap snap);

    int libspectrum_snap_tstates(libspectrum_snap snap);

  }

  public static final int LIBSPECTRUM_ID_SNAPSHOT_Z80 = 3;

  public static void main(String[] args) throws IOException {

    LibSpectrum lib = LibSpectrum.INSTANCE;
    libspectrum_snap snap = getLibspectrumSnap(lib, "/home/fernando/detodo/desarrollo/m/zx/roms/jsw.z80");

    // Mostrar algunos registros procesados por la librería
    int pc = lib.libspectrum_snap_pc(snap) & 0xFFFF;
    int sp = lib.libspectrum_snap_sp(snap) & 0xFFFF;
    int a = lib.libspectrum_snap_a(snap) & 0xFF;
    int f = lib.libspectrum_snap_f(snap) & 0xFF;
    int tstates = lib.libspectrum_snap_tstates(snap);

    System.out.printf("PC = 0x%04X%n", pc);
    System.out.printf("SP = 0x%04X%n", sp);
    System.out.printf("A  = 0x%02X%n", a);
    System.out.printf("F  = 0x%02X%n", f);
    System.out.printf("TStates = %d%n", tstates);

    lib.libspectrum_snap_free(snap);
    lib.libspectrum_end();
  }

  public static libspectrum_snap getLibspectrumSnap(LibSpectrum lib, String filePath) {
    try {
      byte[] data = Files.readAllBytes(Paths.get(filePath));
      if (lib.libspectrum_init() != 0) {
        throw new RuntimeException("Error en libspectrum_init");
      }

      libspectrum_snap snap = lib.libspectrum_snap_alloc();

      int err = lib.libspectrum_snap_read(snap, data,
          new NativeLong(data.length),
          LIBSPECTRUM_ID_SNAPSHOT_Z80,
          filePath);
      if (err != 0) {
        throw new RuntimeException("Error cargando snapshot: " + err);
      }
      System.out.printf("Snapshot cargado: %s%n", filePath);
      return snap;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
