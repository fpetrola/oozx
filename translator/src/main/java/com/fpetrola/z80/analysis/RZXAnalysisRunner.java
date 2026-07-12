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

import com.fpetrola.z80.analysis.generated.JetSetWilly2Instrumented;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.function.Predicate;

/**
 * Runs the full RZX with the instrumented game, feeding the Tracer through the
 * wMem/mem/ldir/pc overrides, and dumps per-site aggregates plus per-frame memory
 * hashes for the semantic-identity verification against {@link BaselineRunner}.
 */
public class RZXAnalysisRunner extends JetSetWilly2Instrumented {
  final RzxBootstrap bootstrap;

  public RZXAnalysisRunner(MiniZXIO<WordNumber> io, Predicate<Integer> interruptionCondition, String rzxPath) {
    super(io, interruptionCondition);
    bootstrap = new RzxBootstrap(this, (RZXPlayerIO<?>) io, rzxPath, Tracer::reset);
  }

  @Override
  public int mem(int address, int pc) {
    int v = super.mem(address, pc);
    Tracer.rd(pc, address, v);
    return v;
  }

  @Override
  public void wMem(int address, int value, int pc) {
    Tracer.wr(pc, address, value);
    super.wMem(address, value, pc);
  }

  @Override
  public void ldir() {
    Tracer.bulk(Tracer.currentPc, HL(), DE(), BC());
    super.ldir();
  }

  @Override
  public void pc(int address, int rdelta) {
    if (address >= 0) {
      Tracer.currentPc = address;
      bootstrap.onPc(address);
    }
    super.pc(address, rdelta);
  }

  public static void main(String[] args) {
    String rzxPath = args.length > 0 ? args[0] : RzxBootstrap.DEFAULT_RZX;
    RZXPlayerIO<WordNumber> io = new RZXPlayerIO<>();
    RZXAnalysisRunner game = new RZXAnalysisRunner(io, io.getInterruptionCondition(), rzxPath);
    long start = System.currentTimeMillis();
    try {
      game.$34463();
    } catch (RuntimeException e) {
      System.out.println("Run ended: " + e.getMessage());
    }
    System.out.println("Elapsed: " + (System.currentTimeMillis() - start) / 1000 + "s");
    game.bootstrap.hasher.dump("analysis/instrumented-hashes.txt");
    Tracer.dump("analysis/analysis-f1.json");
    System.out.println(Tracer.summary());
    System.exit(0);
  }
}
