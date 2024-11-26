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

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.spy.MemptrUpdateInstructionSpy;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.AddStatesIO;
import fuse.tstates.PhaseProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class FuseTestParser<T extends WordNumber> {
  private final File inFile;
  private final File expectedFile;
  private Z80Cpu<WordNumber> cpu;
  private DefaultInstructionFetcher instructionFetcher;
  private State<T> state;

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

      Z80Cpu z80Cpu = getZ80Cpu();

      while (iterator.hasNext()) {
        String testId = iterator.next();
        String registers = iterator.next(); // AF BC DE HL AF' BC' DE' HL' IX IY SP PC
        String state = iterator.next();     // I R IFF1 IFF2 IM <halted> <tstates>

        StringBuilder memory = new StringBuilder();
        String line;
        while (!(line = iterator.next().trim()).equals("-1")) {
          memory.append("\n").append(line);
        }

        FuseTest fuseTest = new FuseTest(testId, registers, state, memory.toString(), z80Cpu);
        fuseTest.initCpu();
        fuseTest.run();
        tests.add(fuseTest);
      }
      return tests;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Z80Cpu getZ80Cpu() {
    MockedMemory<T> memory = new MockedMemory(true);

    AddStatesIO io = new AddStatesIO();
    state = new State<T>(io, memory);
    io.setState(state);
    InstructionSpy spy = new MemptrUpdateInstructionSpy(state);
    DefaultInstructionFactory instructionFactory = new DefaultInstructionFactory<WordNumber>(state);
    instructionFetcher = new MyDefaultInstructionFetcher(state, spy, instructionFactory);
    cpu = (OOZ80<WordNumber>) new OOZ80(state, instructionFetcher);

    PhaseProcessor<T> phaseProcessor = new PhaseProcessor<>((Z80Cpu<T>) cpu);
    memory.addMemoryReadListener(new AddStatesMemoryReadListener<T>(phaseProcessor));
    memory.addMemoryWriteListener(new AddStatesMemoryWriteListener<T>(phaseProcessor));
    return cpu;
  }

  public static class MyDefaultInstructionFetcher extends DefaultInstructionFetcher {
    public MyDefaultInstructionFetcher(State state, InstructionSpy spy, DefaultInstructionFactory instructionFactory) {
      super(state, new OpcodeConditions(state.getFlag(), state.getRegister(RegisterName.B)), new FetchNextOpcodeInstructionFactory(spy, state), new SpyInstructionExecutor(spy), instructionFactory);
    }

    public Instruction getLastInstruction() {
      return instruction;
    }

    public void reset() {
      super.reset();
      createOpcodeTables();
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

        results.add(new FuseResult(testId, registers, state, memory.toString(), events));
      }
      return results;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Helper to parse event in .expected file
  private Event parseEvent(String line) {
    String[] parts = line.trim().split(" ");
    int time = Integer.parseInt(parts[0]);
    String type = parts[1];
    int address = Integer.parseInt(parts[2], 16);
    Integer data = parts.length > 3 ? Integer.parseInt(parts[3], 16) : null;
    return new Event(time, type, address, data);
  }

}

// Assuming you have classes FuseTest and FuseResult defined somewhere in your codebase.
