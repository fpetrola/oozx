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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * The JAVA-SIDE producer of the analysis capture: runs the full RZX with the converted and
 * instrumented game, feeding the same {@link Tracer} the emulator-side {@link Z80AnalysisRunner}
 * feeds, through the wMem/mem/ldir/pc overrides. The {@link TrackLog} bridge rides along on the
 * same overrides and stays inert until a tracked run configures it.
 * <p>
 * Per-frame memory hashes are dumped for the semantic-identity verification against
 * {@link BaselineRunner}: they prove the Spoon transformation preserved semantics.
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
    TrackLog.onRead(pc, address);
    return v;
  }

  @Override
  public void wMem(int address, int value, int pc) {
    Tracer.wr(pc, address, value);
    TrackLog.onWrite(pc, address);
    super.wMem(address, value, pc);
  }

  @Override
  public void ldir() {
    int hl = HL(), de = DE(), bc = BC();
    TrackLog.bulkCopy(hl, de, bc); // graphics-buffer reload: sprite identity source
    Tracer.bulk(Tracer.currentPc, hl, de, bc);
    super.ldir();
  }

  @Override
  public void pc(int address, int rdelta) {
    if (address >= 0) {
      Tracer.boundary(address);
      Tracer.currentPc = address;
      bootstrap.onPc(address);
    }
    super.pc(address, rdelta);
    if (address >= 0)
      TrackLog.onPc(address, mem); // after bootstrap.onPc: the FRAME marker and its cell deltas come first
  }

  // ================= F2: register provenance =================
  // 8-bit registers
  @Override public int A() { Tracer.regRead(Tracer.R_A); return super.A(); }
  @Override public void A(int v) { Tracer.regWrite(Tracer.R_A); super.A(v); }
  @Override public int F() { Tracer.regRead(Tracer.R_F); return super.F(); }
  @Override public void F(int v) { Tracer.regWrite(Tracer.R_F); super.F(v); }
  @Override public int B() { Tracer.regRead(Tracer.R_B); return super.B(); }
  @Override public void B(int v) { Tracer.regWrite(Tracer.R_B); super.B(v); }
  @Override public int C() { Tracer.regRead(Tracer.R_C); return super.C(); }
  @Override public void C(int v) { Tracer.regWrite(Tracer.R_C); super.C(v); }
  @Override public int D() { Tracer.regRead(Tracer.R_D); return super.D(); }
  @Override public void D(int v) { Tracer.regWrite(Tracer.R_D); super.D(v); }
  @Override public int E() { Tracer.regRead(Tracer.R_E); return super.E(); }
  @Override public void E(int v) { Tracer.regWrite(Tracer.R_E); super.E(v); }
  @Override public int H() { Tracer.regRead(Tracer.R_H); return super.H(); }
  @Override public void H(int v) { Tracer.regWrite(Tracer.R_H); super.H(v); }
  @Override public int L() { Tracer.regRead(Tracer.R_L); return super.L(); }
  @Override public void L(int v) { Tracer.regWrite(Tracer.R_L); super.L(v); }

  // 16-bit pairs (implemented over independent fields in SpectrumApplication, so both
  // 8-bit provenance slots are touched)
  @Override public int AF() { Tracer.regRead2(Tracer.R_A, Tracer.R_F); return super.AF(); }
  @Override public void AF(int v) { Tracer.regWrite2(Tracer.R_A, Tracer.R_F); super.AF(v); }
  @Override public int BC() { Tracer.regRead2(Tracer.R_B, Tracer.R_C); return super.BC(); }
  @Override public void BC(int v) { Tracer.regWrite2(Tracer.R_B, Tracer.R_C); super.BC(v); }
  @Override public int DE() { Tracer.regRead2(Tracer.R_D, Tracer.R_E); return super.DE(); }
  @Override public void DE(int v) { Tracer.regWrite2(Tracer.R_D, Tracer.R_E); super.DE(v); }
  @Override public int HL() { Tracer.regRead2(Tracer.R_H, Tracer.R_L); return super.HL(); }
  @Override public void HL(int v) { Tracer.regWrite2(Tracer.R_H, Tracer.R_L); super.HL(v); }
  @Override public int IX() { Tracer.regRead2(Tracer.R_IXH, Tracer.R_IXL); return super.IX(); }
  @Override public void IX(int v) { Tracer.regWrite2(Tracer.R_IXH, Tracer.R_IXL); super.IX(v); }
  @Override public int IY() { Tracer.regRead2(Tracer.R_IYH, Tracer.R_IYL); return super.IY(); }
  @Override public void IY(int v) { Tracer.regWrite2(Tracer.R_IYH, Tracer.R_IYL); super.IY(v); }

  @Override public int IXH() { Tracer.regRead(Tracer.R_IXH); return super.IXH(); }
  @Override public void IXH(int v) { Tracer.regWrite(Tracer.R_IXH); super.IXH(v); }
  @Override public int IXL() { Tracer.regRead(Tracer.R_IXL); return super.IXL(); }
  @Override public void IXL(int v) { Tracer.regWrite(Tracer.R_IXL); super.IXL(v); }
  @Override public int IYH() { Tracer.regRead(Tracer.R_IYH); return super.IYH(); }
  @Override public void IYH(int v) { Tracer.regWrite(Tracer.R_IYH); super.IYH(v); }
  @Override public int IYL() { Tracer.regRead(Tracer.R_IYL); return super.IYL(); }
  @Override public void IYL(int v) { Tracer.regWrite(Tracer.R_IYL); super.IYL(v); }

  // shadow registers
  @Override public int AFx() { Tracer.regRead2(Tracer.R_AX, Tracer.R_FX); return super.AFx(); }
  @Override public void AFx(int v) { Tracer.regWrite2(Tracer.R_AX, Tracer.R_FX); super.AFx(v); }
  @Override public int BCx() { Tracer.regRead2(Tracer.R_BX, Tracer.R_CX); return super.BCx(); }
  @Override public void BCx(int v) { Tracer.regWrite2(Tracer.R_BX, Tracer.R_CX); super.BCx(v); }
  @Override public int DEx() { Tracer.regRead2(Tracer.R_DX, Tracer.R_EX); return super.DEx(); }
  @Override public void DEx(int v) { Tracer.regWrite2(Tracer.R_DX, Tracer.R_EX); super.DEx(v); }
  @Override public int HLx() { Tracer.regRead2(Tracer.R_HX, Tracer.R_LX); return super.HLx(); }
  @Override public void HLx(int v) { Tracer.regWrite2(Tracer.R_HX, Tracer.R_LX); super.HLx(v); }

  @Override public int SP() { Tracer.regRead(Tracer.R_SP); return super.SP(); }
  @Override public void SP(int v) { Tracer.regWrite(Tracer.R_SP); super.SP(v); }
  @Override public int I() { Tracer.regRead(Tracer.R_I); return super.I(); }
  @Override public void I(int v) { Tracer.regWrite(Tracer.R_I); super.I(v); }
  @Override public int R() { Tracer.regRead(Tracer.R_R); return super.R(); }
  @Override public void R(int v) { Tracer.regWrite(Tracer.R_R); super.R(v); }

  // helpers that write the F flag internally (bypassing the F(int) setter)
  @Override public int rlc(int a) { Tracer.flagWrite(); return super.rlc(a); }
  @Override public int rrc(int a) { Tracer.flagWrite(); return super.rrc(a); }
  @Override public int rl(int a) { Tracer.flagWrite(); return super.rl(a); }
  @Override public int rr(int a) { Tracer.flagWrite(); return super.rr(a); }
  @Override public int sl(int a) { Tracer.flagWrite(); return super.sl(a); }
  @Override public int sr(int a) { Tracer.flagWrite(); return super.sr(a); }

  // provenance through the Z80 stack
  @Override public void push(int value) { Tracer.pushProv(); super.push(value); }
  @Override public int pop() { Tracer.popProv(); return super.pop(); }

  // external input (RZX recording): slice root
  @Override public int in(int port, int pc) { Tracer.ioIn(); return super.in(port, pc); }

  // cpir reads mem[] directly in SpectrumApplication: register the reads coarsely
  @Override public void cpir() {
    Tracer.rd(Tracer.currentPc, HL(), 0);
    super.cpir();
  }
  // ================= end F2 =================

  /** this runner as a {@link CaptureSource}: the converted+instrumented game. */
  public static CaptureSource source() {
    return new JavaSource();
  }

  static class JavaSource implements CaptureSource {
    static final String AGGREGATE_HASHES = "analysis/instrumented-hashes.txt";
    static final String TRACK_HASHES = "analysis/track-hashes.txt";
    private RZXAnalysisRunner game;

    @Override
    public String name() {
      return "java";
    }

    /** roles and equations come from the offline Spoon extraction ({@link EquationExtractor}). */
    @Override
    public String sitesJson() {
      return "translator/src/main/resources/analysis/sites.json";
    }

    @Override
    public void replay(String rzxPath, boolean track) {
      System.setProperty("minizx.headless", "true");
      RZXPlayerIO<WordNumber> io = new RZXPlayerIO<>();
      game = new RZXAnalysisRunner(io, io.getInterruptionCondition(), rzxPath);
      long start = System.currentTimeMillis();
      try {
        game.$34463();
      } catch (RuntimeException e) {
        System.out.println("Run ended: " + e.getMessage());
      }
      System.out.println("Elapsed: " + (System.currentTimeMillis() - start) / 1000 + "s");
    }

    @Override
    public byte[] finalMemory() {
      byte[] out = new byte[game.mem.length];
      for (int i = 0; i < out.length; i++)
        out[i] = (byte) game.mem[i];
      return out;
    }

    /** capture must be passive: a tracked run's per-frame hashes must match the aggregate's. */
    @Override
    public void verify(boolean track) throws Exception {
      game.bootstrap.hasher.dump(track ? TRACK_HASHES : AGGREGATE_HASHES);
      if (track) {
        if (Files.exists(Path.of(AGGREGATE_HASHES)))
          FrameHasher.compare(AGGREGATE_HASHES, TRACK_HASHES);
      } else {
        Tracer.dump("analysis/analysis-f1.json");
      }
    }
  }

  public static void main(String[] args) throws Exception {
    source().capture(args.length > 0 ? args[0] : RzxBootstrap.DEFAULT_RZX, "analysis/analysis.db");
    System.exit(0);
  }
}
