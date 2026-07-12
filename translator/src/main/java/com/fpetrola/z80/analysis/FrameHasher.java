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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Semantic identity verification (doc/GUIA-ANALISIS-ECUACIONES.md, section 7.1):
 * records an FNV-1a hash of the full 64K memory at every RZX frame boundary so a
 * baseline run and an instrumented run can be compared frame by frame.
 */
public class FrameHasher {
  private final StringBuilder lines = new StringBuilder();
  private int frames;

  public void onFrame(int frame, int[] mem) {
    lines.append(frame).append(' ').append(Long.toHexString(hash(mem))).append('\n');
    frames++;
  }

  public static long hash(int[] mem) {
    long h = 0xcbf29ce484222325L;
    for (int v : mem) {
      h ^= (v & 0xff);
      h *= 0x100000001b3L;
    }
    return h;
  }

  public void dump(String path) {
    try {
      Path p = Path.of(path);
      if (p.getParent() != null)
        Files.createDirectories(p.getParent());
      Files.writeString(p, lines.toString());
      System.out.println("Hashes: " + frames + " frames -> " + path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** compares two hash files; returns true when identical, printing the first divergence otherwise. */
  public static boolean compare(String baselineFile, String instrumentedFile) throws IOException {
    List<String> a = Files.readAllLines(Path.of(baselineFile));
    List<String> b = Files.readAllLines(Path.of(instrumentedFile));
    int n = Math.min(a.size(), b.size());
    for (int i = 0; i < n; i++) {
      if (!a.get(i).equals(b.get(i))) {
        System.out.println("DIVERGENCE at line " + (i + 1) + ": baseline='" + a.get(i)
            + "' instrumented='" + b.get(i) + "'");
        return false;
      }
    }
    if (a.size() != b.size()) {
      System.out.println("Same prefix but different lengths: baseline=" + a.size()
          + " frames, instrumented=" + b.size() + " frames");
      return false;
    }
    System.out.println("IDENTICAL: " + n + " frames match");
    return true;
  }
}