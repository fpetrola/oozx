/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.z80.analysis;

import com.fpetrola.z80.analysis.query.MemoryImage;

/**
 * Where a capture comes from: the game converted to Java ({@link RZXAnalysisRunner}) or the
 * ORIGINAL game replayed on the OOZ80 emulator ({@link Z80AnalysisRunner}). Both feed the same
 * {@link Tracer} — and, while tracking, the same {@link TrackLog} — so the DB build and every
 * detector downstream are ONE code path: nothing past {@link #capture} knows which one ran.
 *
 * <p>A producer's whole job is to translate its own execution into Tracer calls. What genuinely
 * differs is how each derives a site's roles and equation: the Java side parses the decompiled
 * source with Spoon offline (a checked-in {@link #sitesJson()}), the Z80 side reads them off the
 * decoded opcode as it runs ({@link Z80OpcodeInfo}). That asymmetry is the adapter's reason to
 * exist; everything else is shared.
 */
public interface CaptureSource {
  /** short id for the run messages ("java" / "z80"). */
  String name();

  /**
   * per-site catalog (method, kind, roles, equation) in the flat schema
   * {@link AnalysisDump#dump} parses — written by {@link #replay} when the source derives it
   * at runtime, or a checked-in resource when it comes from the offline extraction.
   */
  String sitesJson();

  /**
   * runs the whole RZX feeding the {@link Tracer}. With {@code track} the {@link TrackLog}
   * bridge is live as well, which requires {@link TrackLog#configure} to have run first:
   * TrackLog, not the producer, decides which sites get logged.
   */
  void replay(String rzxPath, boolean track) throws Exception;

  /** self-check of the replay that just finished (per-frame memory hashes), if the source has one. */
  default void verify(boolean track) throws Exception {
  }

  /** the game's 64K memory as it stands at the end of the replay that just ran. */
  byte[] finalMemory();

  /** the aggregate pass: replay + site catalog + analysis.db, ready for the detectors. */
  default void capture(String rzxPath, String dbPath) throws Exception {
    replay(rzxPath, false);
    verify(false);
    // the settled image + who wrote each address: a game that decompresses or decrypts itself
    // into RAM has its real static data (fonts, tables, sprites) only AFTER that runs, so the
    // cassette snapshot alone cannot decode it. See MemoryImage.
    Tracer.write(MemoryImage.FINAL_MEM, finalMemory());
    Tracer.dumpWriterMap(MemoryImage.WRITER_MAP);
    AnalysisDump.dump(dbPath, sitesJson());
    System.out.println(Tracer.summary());
  }
}
