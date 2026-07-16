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

import com.fpetrola.z80.cpu.DefaultMemorySetter;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.factory.Z80Factory;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxParser;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.BlockInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase;
import com.fpetrola.z80.minizx.emulation.DefaultEmulator;
import com.fpetrola.z80.minizx.emulation.DefaultMemory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.ExecutionListener;
import snapshots.SpectrumState;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The Z80-SIDE producer of the analysis capture: runs an RZX replay of the ORIGINAL game on
 * the OOZ80 emulator and feeds the same {@link Tracer} the instrumented-Java runner feeds —
 * so the whole detector framework (segments, screen, texts, structs, coverage, ...) works on
 * ANY game, with no Java conversion step. The Java pipeline derives each site's roles and
 * equation by parsing the decompiled source with Spoon; here they come straight from the
 * decoded opcode ({@link Z80OpcodeInfo}), which is the primary source anyway.
 *
 * <p>Faithfulness to the Java producer's aggregation semantics:
 * <ul>
 *   <li>opcode/immediate fetches never count as data reads (fetches happen outside the
 *       executor; immediates read with {@code delta > 0});</li>
 *   <li>stack machinery (push/pop/call/ret and the interrupt entry) produces no memory
 *       traffic — the Java translation turns it into native constructs; the emulator's own
 *       push/pop already run with listeners disabled;</li>
 *   <li>LDIR-family per-byte traffic is suppressed and reported as ONE {@code bulk} per
 *       burst (repeating instructions re-execute at the same pc: iterations collapse);</li>
 *   <li>routines are attributed dynamically: a site belongs to the entry address of the
 *       innermost CALL active the first time it executes.</li>
 * </ul>
 * Output: {@code analysis/analysis.db} + {@code analysis/sites-z80.json} +
 * {@code analysis/init-mem.bin}, ready for {@link AnalysisCLI}.
 */
public class Z80AnalysisRunner {

  public static void main(String[] args) throws Exception {
    String rzx = args.length > 0 ? args[0] : RzxBootstrap.DEFAULT_RZX;
    run(rzx, "analysis/analysis.db", "analysis/sites-z80.json");
    System.exit(0);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void run(String rzxPath, String dbPath, String sitesJsonPath) throws Exception {
    RZXPlayerIO<WordNumber> io = new RZXPlayerIO<>();
    State state = new State(io, new DefaultMemory(true));
    io.setPc(state.getPc());
    OOZ80 ooz80 = Z80Factory.createOOZ80(state);
    ooz80.getInstructionFetcher().setClone(false);
    ooz80.getInstructionFetcher().setPrefetch(false);

    RzxFile rzxFile = new RzxParser().parseFile(rzxPath);
    SpectrumState snapshot = RzxParser.loadSnapshot(rzxFile);
    SnapshotLoader.setupStateFromSpectrumState(snapshot, new RegistersBase(state),
        new DefaultMemorySetter(state.getMemory(), MiniZXWithEmulationBase.createROM()));
    io.setup(rzxFile);
    int totalFrames = rzxFile.getInputRecordingBlock().frames.size();
    System.out.println("RZX: " + rzxPath + " (" + totalFrames + " frames)");

    dumpInitialMemory(state.getMemory());
    Tracer.reset();
    Tracer.initDone();

    TraceListener listener = new TraceListener(state, io);
    boolean hook = !"false".equals(System.getProperty("z80.hook"));
    if (hook)
      ooz80.getInstructionExecutor().addExecutionListener(listener);
    Memory<WordNumber> memory = state.getMemory();
    memory.addMemoryReadListener((address, value, delta, fetching) -> {
      if (!listener.inExecution || listener.suppress || delta != 0 || fetching != 0)
        return;
      int a = address.intValue();
      if (a >= 0 && a <= 0xffff)
        Tracer.rd(Tracer.currentPc, a, value == null ? 0 : value.intValue());
    });
    memory.addMemoryWriteListener((address, value) -> {
      if (listener.inExecution && !listener.suppress) {
        int a = address.intValue();
        if (a >= 0 && a <= 0xffff)
          Tracer.wr(Tracer.currentPc, a, value == null ? 0 : value.intValue());
      }
      return value;
    });

    long start = System.currentTimeMillis();
    DefaultEmulator emulator = new DefaultEmulator();
    emulator.setup(ooz80, -1, 1,
        i -> io.getCurrentFrameIndex() < totalFrames
            && state.getRunState() != State.RunState.STATE_STOPPED_BREAK,
        io.getInterruptionCondition());
    try {
      emulator.emulate();
    } catch (RuntimeException e) {
      System.out.println("Run ended: " + e.getMessage());
    }
    System.out.println("Elapsed: " + (System.currentTimeMillis() - start) / 1000 + "s, frames: "
        + io.getCurrentFrameIndex() + "/" + totalFrames + ", sites: " + listener.catalog.size());

    listener.writeSites(sitesJsonPath);
    AnalysisDump.dump(dbPath, sitesJsonPath);
    System.out.println(Tracer.summary());
  }

  /** the memory image right after the snapshot load = the cassette content. */
  private static void dumpInitialMemory(Memory<WordNumber> memory) throws Exception {
    byte[] img = new byte[0x10000];
    WordNumber[] data = (WordNumber[]) memory.getData();
    for (int i = 0; i < img.length; i++)
      img[i] = data[i] == null ? 0 : (byte) data[i].intValue();
    Files.createDirectories(Path.of("analysis"));
    Files.write(Path.of("analysis/init-mem.bin"), img);
    System.out.println("init-mem.bin dumped (64K)");
  }

  /** per-instruction bridge into the Tracer + per-pc static site catalog. */
  static class TraceListener implements ExecutionListener<WordNumber> {
    final Map<Integer, Z80OpcodeInfo> catalog = new HashMap<>();
    final Map<Integer, String> equations = new HashMap<>();
    final Map<Integer, String> methodOf = new HashMap<>();
    private final Deque<Integer> callStack = new ArrayDeque<>();
    private final State<WordNumber> state;
    private final RZXPlayerIO<WordNumber> io;
    volatile boolean inExecution;
    volatile boolean suppress;
    private int prevPc = -1;

    TraceListener(State<WordNumber> state, RZXPlayerIO<WordNumber> io) {
      this.state = state;
      this.io = io;
      callStack.push(-1); // replaced by the first executed pc
    }

    @Override
    public void beforeExecution(Instruction<WordNumber> instruction) {
      int pc = state.getPc().read().intValue();
      if (callStack.peek() == -1) {
        callStack.pop();
        callStack.push(pc);
      }
      boolean continuation = pc == prevPc; // LDIR iteration / HALT re-execution
      prevPc = pc;
      Z80OpcodeInfo info = catalog.computeIfAbsent(pc, k -> Z80OpcodeInfo.of(instruction));
      suppress = info.suppressMem;
      inExecution = true;
      if (continuation)
        return;
      Tracer.currentFrame = io.getCurrentFrameIndex();
      Tracer.boundary(pc);
      Tracer.currentPc = pc;
      methodOf.computeIfAbsent(pc, k -> "$" + callStack.peek());
      for (int slot : info.reads)
        Tracer.regRead(slot);
      if (info.push)
        Tracer.pushProv();
      if (info.pop)
        Tracer.popProv();
      if (info.ioIn)
        Tracer.ioIn();
      if (info.bulk) {
        int[] r = blockRegs(instruction);
        if (r != null) {
          int len = instruction instanceof RepeatingInstruction ? (r[2] == 0 ? 0x10000 : r[2]) : 1;
          int src = info.bulkBackward ? r[0] - (len - 1) : r[0];
          int dst = info.bulkBackward ? r[1] - (len - 1) : r[1];
          Tracer.bulk(pc, src, dst, len);
        }
      }
      if (info.cpBlock) {
        int[] r = blockRegs(instruction);
        if (r != null)
          Tracer.rd(pc, r[0], 0);
      }
      for (int slot : info.writes)
        Tracer.regWrite(slot);
    }

    @Override
    public void afterExecution(Instruction<WordNumber> instruction) {
      inExecution = false;
      suppress = false;
      int pc = Tracer.currentPc;
      if (!equations.containsKey(pc))
        equations.put(pc, Z80OpcodeInfo.equation(instruction));
      // dynamic routine attribution: track CALL/RST/RET
      WordNumber next = ((AbstractInstruction<WordNumber>) instruction).getNextPC();
      if ((instruction instanceof Call || instruction instanceof RST) && next != null)
        callStack.push(next.intValue());
      else if (instruction instanceof Ret && next != null && callStack.size() > 1)
        callStack.pop();
    }

    /** {HL, DE, BC} of a block instruction (the repeating wrapper unwraps to its Ldi/Cpi). */
    private int[] blockRegs(Instruction<WordNumber> instruction) {
      Instruction<WordNumber> inner = instruction instanceof RepeatingInstruction<WordNumber> rep
          ? rep.getInstructionToRepeat() : instruction;
      if (inner instanceof Ldi<WordNumber> ldi)
        return new int[]{ldi.getHl().read().intValue(), ldi.getDe().read().intValue(),
            ldi.getBc().read().intValue()};
      if (inner instanceof BlockInstruction<WordNumber> blk)
        return new int[]{blk.getHl().read().intValue(), 0, blk.getBc().read().intValue()};
      return null;
    }

    /** sites-z80.json in the exact flat schema AnalysisDump.loadSites parses. */
    void writeSites(String path) throws Exception {
      if (Path.of(path).getParent() != null)
        Files.createDirectories(Path.of(path).getParent());
      try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of(path)))) {
        w.println("[");
        List<Integer> pcs = new ArrayList<>(catalog.keySet());
        Collections.sort(pcs);
        for (int i = 0; i < pcs.size(); i++) {
          int pc = pcs.get(i);
          Z80OpcodeInfo info = catalog.get(pc);
          String eq = equations.getOrDefault(pc, "");
          StringBuilder sb = new StringBuilder();
          sb.append("  {\"pc\": ").append(pc)
              .append(", \"method\": \"").append(esc(methodOf.getOrDefault(pc, "?"))).append('"')
              .append(", \"line\": 0")
              .append(", \"kind\": \"").append(info.kind).append('"')
              .append(", \"stmt\": \"").append(esc(eq)).append('"');
          String roles = info.rolesString();
          if (roles != null)
            sb.append(", \"roles\": \"").append(esc(roles)).append('"');
          sb.append(", \"equation\": \"").append(esc(eq)).append("\"}")
              .append(i < pcs.size() - 1 ? "," : "");
          w.println(sb);
        }
        w.println("]");
      }
      System.out.println("sites -> " + path + " (" + catalog.size() + " sites)");
    }

    private static String esc(String s) {
      return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
  }
}
