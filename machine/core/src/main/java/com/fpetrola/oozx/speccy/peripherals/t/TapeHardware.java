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

package com.fpetrola.oozx.speccy.peripherals.t;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which machine a tape says it was made for, asked of the tape.
 * <p>
 * A TZX carries the answer in its hardware type block: a list of machines, and for each one
 * whether the tape runs on it and whether it uses what that machine has that others do not. That
 * last part is the whole point here - a game marked as using the 128K's features has music on a
 * 128K and silence on a 48K, and loading it into the smaller machine is how somebody ends up
 * never hearing it.
 * <p>
 * A TAP carries nothing at all: it is blocks and no statement about anything, which is why the
 * caller still has a fallback for those.
 */
public class TapeHardware {

  private static final int COMPUTERS = 0x00;
  private static final int DOES_NOT_RUN = 0x03;
  private static final int USES_ITS_FEATURES = 0x01;

  /** TZX hardware ids for computers, against the machines this emulator has. */
  private static final Map<Integer, String> MACHINES = new LinkedHashMap<>() {{
    put(0x00, "Spectrum 48K");     // 16K, and this has no 16K to offer
    put(0x01, "Spectrum 48K");     // 48K and +
    put(0x02, "Spectrum 48K");     // 48K issue 1
    put(0x03, "Spectrum 128K");    // 128K
    put(0x04, "Spectrum Plus 2");  // +2
    put(0x05, "Spectrum Plus 3");  // +2A and +3
  }};

  /** Least to most, so "the best one it runs on" is a maximum over this. */
  private static final List<String> BY_CAPABILITY = List.of("Spectrum 48K", "Spectrum 128K", "Spectrum Plus 2", "Spectrum Plus 3");

  /**
   * The best machine the tape says it runs on, or nothing when it does not say.
   * <p>
   * A tape that names several is taken at its word about the biggest of them, except that one it
   * says it uses the features of wins over a bigger one it merely runs on: a game declaring it
   * uses the 128K's sound and also runs on a +3 wants the 128K, and answering +3 there would be
   * reading the list and missing what it says.
   */
  public static Optional<String> bestMachineFor(File file) {
    byte[] image;
    try {
      image = Files.readAllBytes(file.toPath());
    } catch (IOException e) {
      return Optional.empty();
    }

    String best = null;
    String bestThatUsesIt = null;
    for (TapeBlock block : TapeBlock.read(file)) {
      if (block.id() != 0x33) {
        continue;
      }
      int count = image[block.start() + 1] & 0xFF;
      for (int entry = 0; entry < count; entry++) {
        int at = block.start() + 2 + entry * 3;
        if (at + 2 >= image.length || (image[at] & 0xFF) != COMPUTERS) {
          continue;
        }
        String machine = MACHINES.get(image[at + 1] & 0xFF);
        int support = image[at + 2] & 0xFF;
        if (machine == null || support == DOES_NOT_RUN) {
          continue;
        }
        best = better(best, machine);
        if (support == USES_ITS_FEATURES) {
          bestThatUsesIt = better(bestThatUsesIt, machine);
        }
      }
    }
    return Optional.ofNullable(bestThatUsesIt != null ? bestThatUsesIt : best);
  }

  private static String better(String current, String candidate) {
    if (current == null) {
      return candidate;
    }
    return BY_CAPABILITY.indexOf(candidate) > BY_CAPABILITY.indexOf(current) ? candidate : current;
  }
}
