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

import com.fpetrola.z80.bytecode.tests.JetSetWilly2;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.function.Predicate;

/**
 * Reference run: the ORIGINAL JetSetWilly2 (direct mem[] accesses, no Tracer) over the
 * same RZX, recording the same per-frame memory hashes. Comparing its output with
 * {@link RZXAnalysisRunner} proves the Spoon transformation preserved semantics.
 */
public class BaselineRunner extends JetSetWilly2 {
  final RzxBootstrap bootstrap;

  public BaselineRunner(MiniZXIO<WordNumber> io, Predicate<Integer> interruptionCondition, String rzxPath) {
    super(io, interruptionCondition);
    bootstrap = new RzxBootstrap(this, (RZXPlayerIO<?>) io, rzxPath, null);
  }

  @Override
  public void pc(int address, int rdelta) {
    if (address >= 0)
      bootstrap.onPc(address);
    super.pc(address, rdelta);
  }

  public static void main(String[] args) {
    System.setProperty("minizx.headless", "true");
    String rzxPath = args.length > 0 ? args[0] : RzxBootstrap.DEFAULT_RZX;
    RZXPlayerIO<WordNumber> io = new RZXPlayerIO<>();
    BaselineRunner game = new BaselineRunner(io, io.getInterruptionCondition(), rzxPath);
    long start = System.currentTimeMillis();
    try {
      game.$34463();
    } catch (RuntimeException e) {
      System.out.println("Run ended: " + e.getMessage());
    }
    System.out.println("Elapsed: " + (System.currentTimeMillis() - start) / 1000 + "s");
    game.bootstrap.hasher.dump("analysis/baseline-hashes.txt");
    System.exit(0);
  }
}
