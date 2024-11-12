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

package fuse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class FuseTestParser {
  private final File inFile;
  private final File expectedFile;

  public FuseTestParser(File testDataDir) {
    this.inFile = new File(testDataDir, "tests.in");
    this.expectedFile = new File(testDataDir, "tests.expected");
  }

  public List<FuseTest> getTests() {
    try {
      List<String> inLines = Files.readAllLines(inFile.toPath()).stream()
          .filter(line -> !line.trim().isEmpty())
          .collect(Collectors.toList());

      List<FuseTest> tests = new ArrayList<>();
      Iterator<String> iterator = inLines.iterator();

      while (iterator.hasNext()) {
        String testId = iterator.next();
        String registers = iterator.next(); // AF BC DE HL AF' BC' DE' HL' IX IY SP PC
        String state = iterator.next();     // I R IFF1 IFF2 IM <halted> <tstates>

        StringBuilder memory = new StringBuilder();
        String line;
        while (!(line = iterator.next().trim()).equals("-1")) {
          memory.append("\n").append(line);
        }

        tests.add(new FuseTest(testId, registers, state, memory.toString()));
      }
      return tests;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<FuseResult> getResults() {
    try {
      List<String> inLines = Files.readAllLines(expectedFile.toPath()).stream()
          .filter(line -> !line.trim().isEmpty())
          .collect(Collectors.toList());

      List<FuseResult> results = new ArrayList<>();
      Iterator<String> iterator = inLines.iterator();
      List<String> eventTypes = Arrays.asList("MR", "MW", "MC", "PR", "PW", "PC");

      String next = iterator.hasNext() ? iterator.next() : "";
      while (iterator.hasNext()) {
        String testId = next;

        // Skip events
        while (true) {
          if (!iterator.hasNext()) {
            break;
          } else {
            next = iterator.next();
            if (eventTypes.stream().noneMatch(next::contains)) {
              break;
            }
          }
        }

        String registers = next; // AF BC DE HL AF' BC' DE' HL' IX IY SP PC
        String state = next = iterator.next();     // I R IFF1 IFF2 IM <halted> <tstates>

        StringBuilder memory = new StringBuilder();
        while (iterator.hasNext()) {
          next = iterator.next().trim();
          if (!next.endsWith("-1")) {
            break;
          }
          memory.append("\n").append(next);
        }

        results.add(new FuseResult(testId, registers, state, memory.toString()));
      }
      return results;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

// Assuming you have classes FuseTest and FuseResult defined somewhere in your codebase.
