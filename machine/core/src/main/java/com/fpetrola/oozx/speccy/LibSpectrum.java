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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;

public interface LibSpectrum extends Library {
  public static final int LIBSPECTRUM_ID_UNKNOWN = 0;
  public static final int LIBSPECTRUM_ID_RECORDING_RZX = 1;
  public static final int LIBSPECTRUM_ID_SNAPSHOT_SNA = 2;
  public static final int LIBSPECTRUM_ID_SNAPSHOT_Z80 = 3;
  public static final int LIBSPECTRUM_ID_TAPE_TAP = 4;
  public static final int LIBSPECTRUM_ID_TAPE_TZX = 5;

  LibSpectrum INSTANCE = Native.load("spectrum", LibSpectrum.class);

  Z80Loader.libspectrum_tape libspectrum_tape_alloc();

  void libspectrum_tape_free(Z80Loader.libspectrum_tape tape);

  boolean libspectrum_tape_present(Z80Loader.libspectrum_tape tape);

  int libspectrum_tape_read(Z80Loader.libspectrum_tape tape, byte[] buffer, int length, int type, String filename);

  int libspectrum_tape_clear(Z80Loader.libspectrum_tape tape);

  int libspectrum_tape_nth_block(Z80Loader.libspectrum_tape tape, int n);

  int libspectrum_tape_position(IntByReference n, Z80Loader.libspectrum_tape tape);

  int libspectrum_init();

  void libspectrum_end();

  Z80Loader.libspectrum_snap libspectrum_snap_alloc();

  int libspectrum_snap_free(Z80Loader.libspectrum_snap snap);

  int libspectrum_snap_read(Z80Loader.libspectrum_snap snap,
                            byte[] buffer,
                            NativeLong length,
                            int type,
                            String filename);

  // Getters de registros
  short libspectrum_snap_pc(Z80Loader.libspectrum_snap snap);

  short libspectrum_snap_sp(Z80Loader.libspectrum_snap snap);

  byte libspectrum_snap_a(Z80Loader.libspectrum_snap snap);

  byte libspectrum_snap_f(Z80Loader.libspectrum_snap snap);

  int libspectrum_snap_tstates(Z80Loader.libspectrum_snap snap);

  int identifyFileWithClass(String filename, Object o, int i, Object o1);

  public enum Class_t {
    TAPE
  }

  public enum Id_t {TAPE_TZX, UNKNOWN}
}
