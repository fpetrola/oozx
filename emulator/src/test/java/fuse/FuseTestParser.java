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

package fuse;

import com.fpetrola.z80.tstates.RecordingPhaseProcessor;
import com.fpetrola.z80.ProcessorUnderTest;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.tstates.AddStatesIO;
import com.fpetrola.z80.tstates.AddStatesMemoryReadListener;
import com.fpetrola.z80.tstates.AddStatesMemoryWriteListener;
import com.fpetrola.z80.tstates.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FuseTestParser {
  private final String testDataDir;
  private final ProcessorUnderTest under = ProcessorUnderTest.found();
  private RecordedEvents events;

  public FuseTestParser(Path testDataDir) {
    this.testDataDir = "/" + testDataDir;
  }

  /** Read as resources, so they are found in the test jar as well as on disk. */
  private List<String> lines(String file) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(FuseTestParser.class.getResourceAsStream(testDataDir + "/" + file)))) {
      return reader.lines().filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public List<FuseTest> getTests() {
    List<String> namesLines = lines("tests.names");
    List<FuseTest> tests = new ArrayList<>();
    Iterator<String> iterator = lines("tests.in").iterator();

    Z80Cpu z80Cpu = getZ80Cpu();
    int lineNumber = 0;

    while (iterator.hasNext()) {
      String testId = iterator.next();
      String registers = iterator.next(); // AF BC DE HL AF' BC' DE' HL' IX IY SP PC
      String state = iterator.next();     // I R IFF1 IFF2 IM <halted> <tstates>

      StringBuilder memory = new StringBuilder();
      String line;
      while (!(line = iterator.next().trim()).equals("-1")) {
        memory.append("\n").append(line);
      }

      tests.add(new FuseTest(testId, registers, state, memory.toString(), z80Cpu, events, namesLines, lineNumber));
      lineNumber++;
    }
    return tests;
  }

  /** The harness around whichever processor: memory and ports that record their events, and the contention reported through the same processor. */
  private Z80Cpu getZ80Cpu() {
    Memory memory = under.memory();
    events = new RecordedEvents();
    AddStatesIO io = new AddStatesIO(events);
    State state = new State(io, under.bank(memory, io), memory);
    RecordingPhaseProcessor phaseProcessor = new RecordingPhaseProcessor(state, events);
    if (!under.countsItsOwnContention()) {
      memory.addMemoryReadListener(new AddStatesMemoryReadListener(phaseProcessor));
      memory.addMemoryWriteListener(new AddStatesMemoryWriteListener(phaseProcessor));
    }
    Z80Cpu cpu = under.cpu(state, phaseProcessor);
    io.setState(state);
    events.clock = state.clock;
    return cpu;
  }

  public List<FuseResult> getResults() {
    List<FuseResult> results = new ArrayList<>();
    Iterator<String> iterator = lines("tests.expected").iterator();
    List<String> eventTypes = Arrays.asList("MR", "MW", "MC", "PR", "PW", "PC");

    String next = iterator.hasNext() ? iterator.next() : "";
    while (iterator.hasNext()) {
      String testId = next;

      List<Event> events = new ArrayList<>();

      // Skip events
      while (true) {
        if (!iterator.hasNext()) {
          break;
        } else {
          next = iterator.next();
          if (eventTypes.stream().noneMatch(next::contains)) {
            break;
          }
          events.add(parseEvent(next));
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

      results.add(new FuseResult(testId, registers, state, memory.toString(), events, under.reportedEvents()));
    }
    return results;
  }

  // Helper to parse event in .expected file
  private Event parseEvent(String line) {
    String[] parts = line.trim().split(" ");
    int time = java.lang.Integer.parseInt(parts[0]);
    String type = parts[1];
    int address = java.lang.Integer.parseInt(parts[2], 16);
    java.lang.Integer data = parts.length > 3 ? java.lang.Integer.parseInt(parts[3], 16) : null;
    return new Event(time, type, address, data);
  }

}
