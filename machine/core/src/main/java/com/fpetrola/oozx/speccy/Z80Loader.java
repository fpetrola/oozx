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

package com.fpetrola.oozx.speccy;

import com.sun.jna.NativeLong;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Z80Loader {

  public static class libspectrum_snap extends PointerType {
  }

  public static class libspectrum_tape extends PointerType {
  }


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

//      lib.libspectrum_tape_position(new IntByReference(1), new libspectrum_tape());
//      libspectrum_tape libspectrumTape = lib.libspectrum_tape_alloc();
      libspectrum_snap snap = lib.libspectrum_snap_alloc();

      int err = lib.libspectrum_snap_read(snap, data,
          new NativeLong(data.length),
          LibSpectrum.LIBSPECTRUM_ID_SNAPSHOT_Z80,
          filePath);
      if (err != 0) {
        throw new RuntimeException("Error cargando snapshot: " + err);
      }
      System.out.printf("Snapshot cargado: %s%n", filePath);
//      lib.libspectrum_snap_free(snap);
      return snap;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static int  getTstates(LibSpectrum lib, String url) {
    libspectrum_snap snap = Z80Loader.getLibspectrumSnap(lib, url);
    int tstates = lib.libspectrum_snap_tstates(snap);
    lib.libspectrum_snap_free(snap);
    return tstates;
  }
}
