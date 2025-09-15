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

package com.fpetrola.oozx.screen;

import com.sun.jna.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Z80Loader1 {

  // Tipos equivalentes a C
  public static class libspectrum_snap extends PointerType {
  }

  public interface LibSpectrum extends Library {
    LibSpectrum INSTANCE = Native.load("spectrum", LibSpectrum.class);
    // En Linux el .so suele ser "libspectrum.so"
    // En Windows "libspectrum.dll"
    // En macOS "libspectrum.dylib"

    int libspectrum_init();

    void libspectrum_end();

    libspectrum_snap libspectrum_snap_alloc();

    int libspectrum_snap_free(libspectrum_snap snap);

    int libspectrum_snap_read(libspectrum_snap snap,
                              byte[] buffer,
                              NativeLong length,
                              int type,
                              String filename);
  }

  // Constantes sacadas de libspectrum.h
  public static final int LIBSPECTRUM_ID_SNAPSHOT_Z80 = 3;

  public static void main(String[] args) throws IOException {
    args = new String[]{"/home/fernando/dynamitedan1.z80"};
    if (args.length < 1) {
      System.err.println("Uso: java Z80Loader <archivo.z80>");
      System.exit(1);
    }

    String filePath = args[0];
    byte[] data = Files.readAllBytes(Paths.get(filePath));

    LibSpectrum lib = LibSpectrum.INSTANCE;

    // Inicializar
    int err = lib.libspectrum_init();
    if (err != 0) {
      throw new RuntimeException("Error en libspectrum_init: " + err);
    }

    // Crear estructura snap
    libspectrum_snap snap = lib.libspectrum_snap_alloc();

    // Leer snapshot
    err = lib.libspectrum_snap_read(snap, data,
        new NativeLong(data.length),
        LIBSPECTRUM_ID_SNAPSHOT_Z80,
        filePath);
    if (err != 0) {
      throw new RuntimeException("Error cargando snapshot: " + err);
    }

    System.out.println("Snapshot cargado correctamente: " + filePath);

    // Liberar
    lib.libspectrum_snap_free(snap);
    lib.libspectrum_end();
  }
}
