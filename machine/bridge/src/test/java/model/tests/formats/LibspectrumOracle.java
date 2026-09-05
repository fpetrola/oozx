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

package model.tests.formats;

import com.fpetrola.oozx.speccy.bridge.LibSpectrum;
import com.fpetrola.oozx.speccy.bridge.Z80Loader;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * What libspectrum makes of a file, for a Java reader to be held to.
 * <p>
 * The scaffold, not the suite: it says what the reference implementation sees, so a Java reader
 * can be written against it and the cases nobody thought of turn up on their own. What is meant
 * to survive is the assertions it lets us write, not this class - the day every format has its
 * own tests, this asks a library the build does not otherwise need.
 */
public final class LibspectrumOracle {
  /** The type ids libspectrum knows a file by, from its own header. */
  public static final int TAP = 4, TZX = 5, SNA = 2, Z80 = 3, SZX = 15, SP = 12;

  private LibspectrumOracle() {
  }

  /** Whether the library is here at all: everything using this steps aside when it is not. */
  public static boolean present() {
    try {
      return LibSpectrum.INSTANCE != null;
    } catch (Throwable notInstalled) {
      return false;
    }
  }

  /** The type of every block of that tape, in order, as libspectrum reads it. */
  public static List<Integer> blocksOf(Path file, int type) throws Exception {
    byte[] image = Files.readAllBytes(file);
    LibSpectrum lib = LibSpectrum.INSTANCE;
    lib.libspectrum_init();
    Z80Loader.libspectrum_tape tape = lib.libspectrum_tape_alloc();
    try {
      if (lib.libspectrum_tape_read(tape, image, image.length, type, file.toString()) != 0) {
        return null;
      }
      List<Integer> blocks = new ArrayList<>();
      PointerByReference iterator = new PointerByReference();
      lib.libspectrum_tape_iterator_init(iterator, tape);
      // The iterator is a list node; the block is what the node holds.
      for (Pointer block = lib.libspectrum_tape_iterator_current(iterator.getValue());
           block != null;
           block = lib.libspectrum_tape_iterator_next(iterator)) {
        blocks.add(lib.libspectrum_tape_block_type(block));
      }
      return blocks;
    } finally {
      lib.libspectrum_tape_free(tape);
    }
  }

  /**
   * That snapshot as libspectrum writes it back out, in one chosen format.
   * <p>
   * This is what makes a snapshot comparable at all: two readers hold their state in their own
   * shapes, and there is no comparing a C struct with a Java object. Written back out by the
   * same writer, they become two byte arrays that have to be equal - and every field is in
   * them, including the ones nobody thought to check.
   */
  public static byte[] rewritten(byte[] image, int type) {
    LibSpectrum lib = LibSpectrum.INSTANCE;
    lib.libspectrum_init();
    Z80Loader.libspectrum_snap snap = lib.libspectrum_snap_alloc();
    try {
      if (lib.libspectrum_snap_read(snap, image, new NativeLong(image.length), type, null) != 0) {
        return null;
      }
      PointerByReference buffer = new PointerByReference();
      LongByReference length = new LongByReference();
      IntByReference lost = new IntByReference();
      if (lib.libspectrum_snap_write(buffer, length, lost, snap, Z80, null, 0) != 0) {
        return null;
      }
      byte[] written = buffer.getValue().getByteArray(0, (int) length.getValue());
      lib.libspectrum_free(buffer.getValue());
      return written;
    } finally {
      lib.libspectrum_snap_free(snap);
    }
  }

  /**
   * The reference's own test files of that kind. A gzipped one is unpacked to a file beside the
   * others: the archive holds two .sna that way, and skipping them would leave the format
   * uncovered for the sake of a wrapper that says nothing about it.
   */
  public static List<Path> corpus(String extension) throws Exception {
    Path here = Path.of("../../libspectrum/test");
    if (!Files.isDirectory(here)) {
      return List.of();
    }
    List<Path> found = new ArrayList<>();
    try (var files = Files.list(here)) {
      for (Path file : files.sorted().toList()) {
        String name = file.getFileName().toString();
        if (name.endsWith("." + extension)) {
          found.add(file);
        } else if (name.endsWith("." + extension + ".gz")) {
          found.add(unpacked(file, name.substring(0, name.length() - 3)));
        }
      }
    }
    return found;
  }

  private static Path unpacked(Path packed, String named) throws Exception {
    Path plain = Files.createTempDirectory("corpus").resolve(named);
    try (var gzip = new java.util.zip.GZIPInputStream(Files.newInputStream(packed))) {
      Files.write(plain, gzip.readAllBytes());
    }
    return plain;
  }
}
