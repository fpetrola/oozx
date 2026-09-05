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
    assumeTrue(ReferenceOracle.present(), "libspectrum is not installed here");
  }

  @Test
  void tapesAsBothSeeThem() throws Exception {
    System.out.printf("%-40s %8s %8s  %s%n", "file", "libspec", "java", "");
    for (String extension : new String[]{"tap", "tzx"}) {
      int type = extension.equals("tap") ? ReferenceOracle.TAP : ReferenceOracle.TZX;
      for (Path file : ReferenceOracle.corpus(extension)) {
        List<Integer> reference = ReferenceOracle.blocksOf(file, type);
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
      for (Path file : ReferenceOracle.corpus(extension)) {
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
