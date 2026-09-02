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
package com.fpetrola.z80.generate;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.Contention;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Instructions per second of each core on a small program that copies a block, adds it up and
 * loops: {@code mvn test -pl emulator -Dtest=CoreBenchmark}. Numbers, not assertions.
 * <p>
 * Each core is measured in its own JVM. Running both in one makes the second one slower: they
 * share the memory's call sites, so whoever runs first decides what those sites are profiled as.
 * The memory is a bare array for the same reason - what is being measured is the core.
 */
public class CoreBenchmark {
  static final int[] PROGRAM = {
      0x21, 0x00, 0x40,       // LD HL,4000h
      0x11, 0x00, 0x60,       // LD DE,6000h
      0x01, 0x00, 0x08,       // LD BC,0800h
      0xED, 0xB0,             // LDIR
      0x06, 0xFF,             // LD B,FFh
      0x3C,                   // INC A
      0x86,                   // ADD A,(HL)
      0x23,                   // INC HL
      0x10, 0xFB,             // DJNZ -5
      0xDD, 0x7E, 0x02,       // LD A,(IX+2)
      0xCB, 0x11,             // RL C
      0xC3, 0x00, 0x80        // JP 8000h
  };
  static final IO NO_IO = new IO() {
    public int in(int port) {
      return 0xFF;
    }

    public void out(int port, int value) {
    }
  };

  /** An array and nothing else: no listeners, no wrapper, so the number is the core's. */
  static class Ram implements Memory {
    final int[] data = new int[0x10000];

    public int read(int address, int fetching) {
      return data[address];
    }

    public void write(int address, int value) {
      data[address] = value & 0xff;
    }

    public void reset() {
    }

    public int[] getData() {
      return data;
    }
  }

  static Z80Cpu oop() {
    State state = new State(NO_IO, new Ram());
    DefaultInstructionFactory factory = new DefaultInstructionFactory(state);
    return new OOZ80(state, new DefaultInstructionFetcher(state, factory, false, false), new DefaultInstructionExecutor(state, false));
  }

  static Z80Cpu generated() {
    Memory memory = new Ram();
    GeneratedZ80 core = new GeneratedZ80(memory, NO_IO) {
      public void contend(int address, int times, int tstates, Contention.Kind kind) {
      }
    };
    return new GeneratedZ80Cpu(new State(NO_IO, core, memory), core);
  }

  static void load(Z80Cpu cpu) {
    int[] data = cpu.getState().getMemory().getData();
    for (int i = 0; i < PROGRAM.length; i++)
      data[0x8000 + i] = PROGRAM[i];
    cpu.getState().getPc().write(0x8000);
    cpu.getState().getRegister(RegisterName.IX).write(0x4000);
  }

  static double run(Z80Cpu cpu, long instructions) {
    long start = System.nanoTime();
    for (long i = 0; i < instructions; i++)
      cpu.execute();
    return instructions / ((System.nanoTime() - start) / 1e9);
  }

  /** One core, this JVM: {@code java -cp ... CoreBenchmark oop|generated}. */
  public static void main(String[] args) {
    String name = args.length > 0 ? args[0] : "generated";
    for (int round = 0; round < 3; round++) {
      Z80Cpu cpu = name.equals("oop") ? oop() : generated();
      load(cpu);
      run(cpu, 5_000_000);
      System.out.printf("core=%s round=%d rate=%.1f%n", name, round, run(cpu, 30_000_000) / 1e6);
    }
  }

  @Test
  public void instructionsPerSecond() throws Exception {
    Map<String, Double> best = new LinkedHashMap<>();
    for (String core : List.of("oop", "generated"))
      for (String line : fork(core))
        if (line.startsWith("core=")) {
          System.out.println(line);
          double rate = Double.parseDouble(line.substring(line.indexOf("rate=") + 5));
          best.merge(core, rate, Math::max);
        }
    if (best.size() == 2)
      System.out.printf("best: OOP %.1f M instr/s, generated %.1f M instr/s, ratio %.2fx%n",
          best.get("oop"), best.get("generated"), best.get("generated") / best.get("oop"));
  }

  /**
   * What the JIT did with the generated core's call sites: how many of the memory accesses in
   * each hot decode method stayed a call. The gate of the plan's step 0.
   */
  @Test
  public void inlining() throws Exception {
    Map<String, Integer> notInlined = new LinkedHashMap<>();
    List<String> compiled = new ArrayList<>();
    String method = "";
    for (String line : fork("generated", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintCompilation", "-XX:+PrintInlining")) {
      if (line.contains("GeneratedZ80::") && !line.contains("@ "))
        compiled.add(line.substring(line.indexOf("GeneratedZ80::") + 14).replaceAll(" .*", ""));
      if (line.contains("@ ") && !line.contains("inline")) {
        String callee = line.replaceAll("^.*@ \\d+\\s+", "").replaceAll("\\s+\\(.*", "");
        if (callee.contains("Memory") || callee.contains("contend") || callee.contains("Integer"))
          notInlined.merge(callee, 1, Integer::sum);
      }
    }
    System.out.println("compiled: " + compiled.stream().distinct().sorted().toList());
    System.out.println("compiled methods: " + compiled.stream().distinct().count());
    System.out.println("not inlined into it: " + (notInlined.isEmpty() ? "nothing of memory/contend/boxing" : notInlined));
  }

  private List<String> fork(String core, String... vmArgs) throws Exception {
    List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString()));
    command.addAll(List.of(vmArgs));
    command.addAll(List.of("-cp", System.getProperty("java.class.path"), CoreBenchmark.class.getName(), core));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      for (String line = reader.readLine(); line != null; line = reader.readLine())
        lines.add(line);
    }
    process.waitFor();
    return lines;
  }
}
