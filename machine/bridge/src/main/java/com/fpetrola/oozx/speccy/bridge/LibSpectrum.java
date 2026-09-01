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

package com.fpetrola.oozx.speccy.bridge;

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

  /* Walking a tape's blocks, and writing a tape or a snapshot back out: what an oracle needs to
     say what it read, without an accessor per field. */

  int libspectrum_tape_iterator_init(com.sun.jna.ptr.PointerByReference iterator, Z80Loader.libspectrum_tape tape);

  com.sun.jna.Pointer libspectrum_tape_iterator_current(com.sun.jna.Pointer iterator);

  com.sun.jna.Pointer libspectrum_tape_iterator_next(com.sun.jna.ptr.PointerByReference iterator);

  int libspectrum_tape_block_type(com.sun.jna.Pointer block);

  int libspectrum_tape_write(com.sun.jna.ptr.PointerByReference buffer, com.sun.jna.ptr.IntByReference length,
                             Z80Loader.libspectrum_tape tape, int type);

  int libspectrum_snap_write(com.sun.jna.ptr.PointerByReference buffer, com.sun.jna.ptr.LongByReference length,
                             com.sun.jna.ptr.IntByReference outFlags, Z80Loader.libspectrum_snap snap,
                             int type, com.sun.jna.Pointer creator, int inFlags);

  void libspectrum_free(com.sun.jna.Pointer buffer);

  public enum Class_t {
    TAPE
  }

  public enum Id_t {TAPE_TZX, UNKNOWN}
}
