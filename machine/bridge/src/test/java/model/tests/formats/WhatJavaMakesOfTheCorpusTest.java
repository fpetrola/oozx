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

import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.oozx.speccy.modules.tape.TapeBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Where the Java readers stand against the reference, over the files libspectrum keeps for its
 * own regressions - the invalid ones, the loops, the zero-pilot turbo, the block with no data.
 * <p>
 * It prints a table rather than asserting: this is the first look, and what it is for is to say
 * which formats are read at all and which files are read differently, so the work has an order.
 * The assertions come one at a time, as each format gets its own tests.
 */
class WhatJavaMakesOfTheCorpusTest {
  @BeforeAll
  static void needsLibspectrum() {
    assumeTrue(LibspectrumOracle.present(), "libspectrum is not installed here");
  }

  @Test
  void tapesAsBothSeeThem() throws Exception {
    System.out.printf("%-40s %8s %8s  %s%n", "file", "libspec", "java", "");
    for (String extension : new String[]{"tap", "tzx"}) {
      int type = extension.equals("tap") ? LibspectrumOracle.TAP : LibspectrumOracle.TZX;
      for (Path file : LibspectrumOracle.corpus(extension)) {
        List<Integer> reference = LibspectrumOracle.blocksOf(file, type);
        List<TapeBlock> java = TapeBlock.read(file.toFile());
        String verdict = reference == null
            ? (java.isEmpty() ? "both refuse it" : "LIBSPECTRUM REFUSES IT, JAVA TAKES IT")
            : java.isEmpty() && !reference.isEmpty() ? "JAVA READS NOTHING"
            : reference.size() != java.size() ? "DIFFERENT COUNT"
            : reference.equals(java.stream().map(TapeBlock::id).toList()) ? "same blocks"
            : "SAME COUNT, DIFFERENT IDS";
        System.out.printf("%-34s %-28s %s%n    libspectrum %s%n    java        %s%n", file.getFileName(), verdict, "",
            reference == null ? "-" : reference.stream().map(id -> String.format("%02X", id)).toList(),
            java.stream().map(block -> String.format("%02X:%s", block.id(), block.type())).toList());
      }
    }
  }

  @Test
  void snapshotsJavaCanRead() throws Exception {
    System.out.printf("%-40s %s%n", "file", "java");
    for (String extension : new String[]{"z80", "szx", "sna", "sp"}) {
      for (Path file : LibspectrumOracle.corpus(extension)) {
        String verdict;
        try {
          SpectrumState state = new com.fpetrola.emulation.helpers.snapshots.SnapshotFactory()
              .getSnapshot(file.toFile()).load(file.toFile());
          verdict = state == null ? "READS NOTHING" : "pc=" + state.getZ80State().getRegPC();
        } catch (Throwable refused) {
          verdict = "REFUSED: " + refused.getClass().getSimpleName();
        }
        System.out.printf("%-40s %s%n", file.getFileName(), verdict);
      }
    }
  }
}
