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

package com.fpetrola.z80.minizx3d;

import com.fpetrola.z80.analysis.Tracer;
import com.fpetrola.z80.analysis.Z80OpcodeInfo;
import com.fpetrola.z80.cpu.DefaultMemorySetter;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.factory.Z80Factory;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxParser;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.emulation.DefaultEmulator;
import com.fpetrola.z80.minizx.emulation.DefaultMemory;
import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.ExecutionListener;
import snapshots.SpectrumState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * RZX replay on the OOZ80 emulator, paced to real time (50 fps), with {@link OriginTaint}
 * propagated live: per instruction the {@link Z80OpcodeInfo} footprint says which register
 * slots feed the result — only VAL-role sources propagate (an address register composing a
 * memory operand is where the value sits, not what it is; a tested flag is control) — and
 * the memory listeners carry taint through loads, stores and block copies.
 *
 * <p>At every frame boundary the screen bytes and their sprite ownership (taint leaves that
 * fall inside the {@link SpriteCatalog}) are published as an immutable {@link FrameSnapshot}
 * for the render thread. The emulator runs 15-28x faster than the Spectrum did, so pacing is
 * sleeping, not catching up.
 */
public final class TaintReplay implements Runnable {
  public static final int SCREEN = 16384, ATTRS = 22528, PIXEL_BYTES = 6144, ATTR_BYTES = 768;
  static final boolean DEBUG = Boolean.getBoolean("taint.debug");
  /** how many frames a screen byte's sprite taint stays valid; 0 = never expire. */
  static final int FRESH_FRAMES = Integer.getInteger("fresh.frames", 3);
  /** -Dlog=true turns the console chatter on; silent by default so a user-launched run
   *  doesn't interleave with whatever else shares the terminal. */
  static final boolean LOG = Boolean.getBoolean("log");

  /** what the render thread sees: one complete frame, immutable. */
  public record FrameSnapshot(int frame, byte[] pixels, byte[] attrs, int[] owner, int[] tile) {
  }

  private final String rzxPath;
  private final OriginTaint taint;
  private final Consumer<FrameSnapshot> onFrame;
  private volatile boolean stop;
  private WordNumber[] data;
  /** validation mode: no pacing, stop after maxFrames. */
  boolean paced = true;
  int maxFrames = Integer.MAX_VALUE;
  /**
   * Replay speed: 1 = the original Spectrum's 50 fps, which is the ONLY thing capping this
   * (the emulator runs ~30x faster than that, and rendering has 10x of headroom). The
   * render thread writes it live; {@code -Dspeed=N} sets the initial value.
   */
  private volatile float speed = Float.parseFloat(System.getProperty("speed", "1"));
  /** -Dseek=N: replay up to frame N unpaced, so a later room is reached in seconds. */
  private final int seekTo = Integer.getInteger("seek", 0);

  public float getSpeed() {
    return speed;
  }

  public void setSpeed(float s) {
    speed = Math.min(32f, Math.max(.125f, s));
  }

  public TaintReplay(String rzxPath, SpriteCatalog catalog, Consumer<FrameSnapshot> onFrame) {
    this.rzxPath = rzxPath;
    this.taint = new OriginTaint(catalog.baseOf, catalog.tileZone);
    this.onFrame = onFrame;
  }

  /**
   * Headless validation against the oracle: replays without pacing and prints, for a few
   * frames, the sprites the TAINT sees on screen (base + pixel bbox) so they can be checked
   * against the {@code sprite_draws} the track pipeline recorded for those same frames.
   */
  public static void main(String[] args) throws Exception {
    String rzx = args.length > 0 ? args[0]
        : "/home/fernando/detodo/spectrum/oozx/Jet Set Willy - Mildly Patched.rzx";
    String db = args.length > 1 ? args[1] : "analysis/analysis.db";
    int from = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
    int to = args.length > 3 ? Integer.parseInt(args[3]) : 1010;
    SpriteCatalog catalog = new SpriteCatalog(db, 128);
    TaintReplay[] holder = new TaintReplay[1];
    // -Ddebug.scan=true: histogram of single-leaf origins of LIT, UNCLASSIFIED playfield
    // bytes across the sampled frames — finds sprite zones the catalog is missing
    java.util.Map<Integer, Integer> orphanLeaves = new java.util.TreeMap<>();
    TaintReplay replay = new TaintReplay(rzx, catalog, snap -> {
      if (snap.frame() < from || snap.frame() > to)
        return;
      StringBuilder sb = new StringBuilder("frame " + snap.frame() + ":");
      java.util.Map<Integer, int[]> boxes = new java.util.TreeMap<>();
      for (int i = 0; i < PIXEL_BYTES; i++) {
        int base = snap.owner()[i];
        if (base == 0)
          continue;
        int col = i & 31, y = ((i >> 11) & 3) * 64 + ((i >> 5) & 7) * 8 + ((i >> 8) & 7);
        int[] b = boxes.computeIfAbsent(base, k -> new int[]{col, y, col, y, 0});
        b[0] = Math.min(b[0], col);
        b[1] = Math.min(b[1], y);
        b[2] = Math.max(b[2], col);
        b[3] = Math.max(b[3], y);
        b[4]++;
      }
      boxes.forEach((base, b) -> sb.append(String.format(" [gfx=%d x=%d y=%d w=%d h=%d bytes=%d]",
          base - 1, b[0] * 8, b[1], (b[2] - b[0] + 1) * 8, b[3] - b[1] + 1, b[4])));
      System.out.println(sb);
      // -Ddebug.xy=x,y: the taint LEAVES of that screen byte, to see where a pixel that
      // should be a sprite actually traces to
      // -Ddebug.mem=hexAddr[,hexAddr...]: taint leaves of MEMORY bytes (staging probes)
      String dm = System.getProperty("debug.mem");
      if (dm != null)
        for (String one : dm.split(",")) {
          int a = Integer.parseInt(one, 16);
          java.util.Set<Integer> lv = holder[0].taint.leaves(holder[0].taint.mem[a], 20);
          StringBuilder ls = new StringBuilder("  mem[$" + one + "]="
              + holder[0].memByte(a) + " hojas:");
          lv.forEach(x -> ls.append(" $").append(Integer.toHexString(x)));
          System.out.println(ls);
        }
      String xy = System.getProperty("debug.rect");
      if (xy != null) {
        String[] p = xy.split(",");
        for (int y = Integer.parseInt(p[1]); y <= Integer.parseInt(p[3]); y += 4)
          for (int x = Integer.parseInt(p[0]); x <= Integer.parseInt(p[2]); x += 8) {
            int i = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2) | (x >> 3);
            if (snap.pixels()[i] == 0 || snap.owner()[i] != 0)
              continue; // only lit bytes the taint did NOT classify
            java.util.Set<Integer> lv = holder[0].taint.leaves(holder[0].taint.mem[SCREEN + i], 12);
            StringBuilder ls = new StringBuilder("  sin-clasificar(" + x + "," + y + "):");
            lv.forEach(a -> ls.append(" $").append(Integer.toHexString(a)));
            System.out.println(ls);
          }
      }
      if (snap.frame() == to && Boolean.getBoolean("dump")) {
        // the screen as ascii (one char per cell, # = any pixel set, * = taint-owned):
        // tells desync (menu vs gameplay) apart from stale taint at a glance
        for (int cy = 0; cy < 24; cy++) {
          StringBuilder line = new StringBuilder();
          for (int cx = 0; cx < 32; cx++) {
            boolean on = false, owned = false;
            for (int r = 0; r < 8; r++) {
              int i = ((cy >> 3) << 11) | (r << 8) | ((cy & 7) << 5) | cx;
              on |= snap.pixels()[i] != 0;
              owned |= snap.owner()[i] != 0;
            }
            line.append(owned ? '*' : on ? '#' : '.');
          }
          System.out.println(line);
        }
      }
    });
    holder[0] = replay;
    if (Boolean.getBoolean("debug.scan")) {
      TaintReplay scanner = holder[0] = new TaintReplay(rzx, catalog, snap -> {
        if (snap.frame() % 25 != 0 || snap.frame() < from)
          return;
        for (int i = 0; i < PIXEL_BYTES; i++) {
          if (snap.pixels()[i] == 0 || snap.owner()[i] != 0 || snap.tile()[i] != 0)
            continue;
          java.util.Set<Integer> lv = holder[0].taint.leaves(holder[0].taint.mem[SCREEN + i], 3);
          if (lv.size() <= 2)
            for (int a : lv)
              if (a >= 0x8000) // static game RAM only: ROM and screen/attr noise out
                orphanLeaves.merge(a, 1, Integer::sum);
        }
      });
      scanner.paced = false;
      scanner.maxFrames = to + 1;
      scanner.run();
      // merge contiguous-ish leaves into ranges, report the strong ones
      int lo = -1, hi = -1, hits = 0;
      System.out.println("=== zonas huerfanas (origen de pixeles sin clasificar) ===");
      java.util.List<int[]> ranges = new java.util.ArrayList<>();
      for (Map.Entry<Integer, Integer> e : orphanLeaves.entrySet()) {
        if (lo < 0 || e.getKey() > hi + 8) {
          if (lo >= 0)
            ranges.add(new int[]{lo, hi, hits});
          lo = e.getKey();
          hits = 0;
        }
        hi = e.getKey();
        hits += e.getValue();
      }
      if (lo >= 0)
        ranges.add(new int[]{lo, hi, hits});
      ranges.stream().filter(r -> r[2] >= 40)
          .forEach(r -> System.out.printf("  $%x..$%x (%d bytes) hits=%d%n",
              r[0], r[1], r[1] - r[0] + 1, r[2]));
      return;
    }
    replay.paced = false;
    replay.maxFrames = to + 1;
    long start = System.currentTimeMillis();
    replay.run();
    System.out.println("Validation run: " + (System.currentTimeMillis() - start) / 1000 + "s");
  }

  public void stop() {
    stop = true;
  }

  /** the RZX player, published once the replay thread builds it. */
  private volatile RZXPlayerIO<WordNumber> io;

  /** cut the recording NOW and hand the input over to the live keyboard. One way. */
  public void goLive() {
    RZXPlayerIO<WordNumber> i = io;
    if (i != null)
      i.goLive();
  }

  public boolean isLive() {
    RZXPlayerIO<WordNumber> i = io;
    return i != null && i.isLive();
  }

  /** the Spectrum keyboard matrix live mode reads; feed it AWT key events. */
  public com.fpetrola.z80.minizx.MiniZXKeyboard keyboard() {
    RZXPlayerIO<WordNumber> i = io;
    return i == null ? null : i.getMiniZXKeyboard();
  }

  /** a static graphics byte, readable once the snapshot is loaded (for the voxel builder). */
  public int memByte(int addr) {
    WordNumber[] d = data;
    return d == null || d[addr] == null ? 0 : d[addr].intValue() & 0xff;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void run() {
    try {
      RZXPlayerIO<WordNumber> io = new RZXPlayerIO<>();
      this.io = io;
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
      if (Boolean.getBoolean("play"))
        io.goLive(); // -Dplay=true: skip the recording entirely, play from frame one
      int totalFrames = rzxFile.getInputRecordingBlock().frames.size();
      if (LOG)
        System.out.println("TaintReplay: " + rzxPath + " (" + totalFrames + " frames)");

      Memory<WordNumber> memory = state.getMemory();
      data = (WordNumber[]) memory.getData();

      Listener listener = new Listener(state, io);
      ooz80.getInstructionExecutor().addExecutionListener(listener);
      memory.addMemoryReadListener((address, value, delta, fetching) -> {
        if (!listener.inExecution || delta != 0 || fetching != 0)
          return;
        int a = address.intValue();
        if (a >= listener.curPc && a < listener.curPc + listener.curLen)
          return; // immediate operand bytes: instruction encoding, not data
        if (a >= 0 && a <= 0xffff) {
          listener.lastRead = taint.read(a);
          listener.pendingRead = taint.union(listener.pendingRead, listener.lastRead);
        }
      });
      memory.addMemoryWriteListener((address, value) -> {
        int a = address.intValue();
        if (a >= 0 && a <= 0xffff) {
          if (a >= SCREEN && a < SCREEN + PIXEL_BYTES)
            listener.lastWrite[a - SCREEN] = listener.curFrame;
          if (listener.inExecution && !listener.suppress)
            // a block copy pairs each write with the read just before it; anything else
            // combines what the instruction read from registers and memory
            taint.mem[a] = listener.bulk ? listener.lastRead
                : taint.union(listener.srcTaint, listener.pendingRead);
          if (DEBUG && a >= SCREEN && a < SCREEN + PIXEL_BYTES) {
            listener.dbgScreenWrites++;
            if (!listener.inExecution)
              listener.dbgOutside++;
            else if (listener.suppress) {
              listener.dbgSuppressed++;
              listener.dbgSuppressedPcs.merge(listener.curPc, 1L, Long::sum);
            } else if (taint.mem[a] == OriginTaint.NONE)
              listener.dbgUntainted++;
          }
        }
        return value;
      });

      DefaultEmulator emulator = new DefaultEmulator();
      emulator.setup(ooz80, -1, 1,
          i -> !stop
              && (io.isLive() || io.getCurrentFrameIndex() < Math.min(totalFrames, maxFrames))
              && state.getRunState() != State.RunState.STATE_STOPPED_BREAK,
          io.getInterruptionCondition());
      emulator.emulate();
      if (LOG)
        System.out.println("TaintReplay done: " + io.getCurrentFrameIndex() + "/" + totalFrames
            + " frames, " + taint.nodeCount() + " union nodes");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private class Listener implements ExecutionListener<WordNumber> {
    final Map<Integer, Z80OpcodeInfo> catalog = new HashMap<>();
    private final State<WordNumber> state;
    private final RZXPlayerIO<WordNumber> io;
    volatile boolean inExecution, suppress, bulk, regSwap;
    volatile int curPc = -1, curLen, curFrame;
    /** frame each screen byte was last WRITTEN: sprite taint older than a couple of
     * frames is a stale trail a dirty-region engine (Dynamite Dan) never re-erased. */
    final int[] lastWrite = new int[PIXEL_BYTES];
    int srcTaint, pendingRead, lastRead;
    long dbgScreenWrites, dbgOutside, dbgSuppressed, dbgUntainted;
    final Map<Integer, Long> dbgSuppressedPcs = new HashMap<>();
    private int prevPc = -1, lastFrame = -1, pacedFrom;
    private long t0 = -1;
    private float pacedSpeed = -1;
    private Z80OpcodeInfo info;

    Listener(State<WordNumber> state, RZXPlayerIO<WordNumber> io) {
      this.state = state;
      this.io = io;
    }

    @Override
    public void beforeExecution(Instruction<WordNumber> instruction) {
      int pc = state.getPc().read().intValue();
      boolean continuation = pc == prevPc; // LDIR iteration / HALT re-execution
      prevPc = pc;
      if (!continuation) {
        info = catalog.computeIfAbsent(pc, k -> Z80OpcodeInfo.of(instruction));
        // taint sources: VAL-role register reads only. ADDR tells where a value lives,
        // COND steers control flow — neither is what the value is made of.
        int t = OriginTaint.NONE;
        for (int slot : info.reads)
          if (info.roles.getOrDefault(Tracer.CH_NAME[slot], "").indexOf('V') >= 0)
            t = taint.union(t, taint.reg[slot]);
        srcTaint = t;
        int frame = io.getCurrentFrameIndex();
        curFrame = frame;
        if (frame != lastFrame) {
          lastFrame = frame;
          if (DEBUG && frame % 1000 == 0) {
            System.out.println("frame " + frame + ": screenWrites=" + dbgScreenWrites
                + " outside=" + dbgOutside + " suppressed=" + dbgSuppressed
                + " untainted=" + dbgUntainted + " suppressedPcs=" + dbgSuppressedPcs.entrySet()
                .stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(4)
                .map(e -> {
                  Z80OpcodeInfo i = catalog.get(e.getKey());
                  return "$" + e.getKey() + " x" + e.getValue() + " [push=" + i.push
                      + " pop=" + i.pop + " sup=" + i.suppressMem + "]";
                }).toList());
            dbgScreenWrites = dbgOutside = dbgSuppressed = dbgUntainted = 0;
            dbgSuppressedPcs.clear();
          }
          publish(frame);
          if (paced)
            pace(frame);
        }
      }
      // EXX / EX DE,HL / EX AF,AF' are register SWAPS: the generic src-union flow would
      // smear every involved register with the union of all of them, and in an EXX copy
      // loop (Dynamite Dan's engine) the compounded unions hit the depth cap and drop
      // the sprite terms — the screen came out patchily classified. Swap the taints
      // exactly like the silicon swaps the values, and skip the generic write pass.
      regSwap = false;
      if (instruction instanceof com.fpetrola.z80.instructions.impl.Exx
          && info.writes.size() == 12) {
        for (int k = 0; k < 6; k++) {
          int a = info.writes.get(k), b = info.writes.get(k + 6);
          int t1 = taint.reg[a];
          taint.reg[a] = taint.reg[b];
          taint.reg[b] = t1;
        }
        regSwap = true;
      } else if (instruction instanceof com.fpetrola.z80.instructions.impl.Ex
          && !info.suppressMem && info.writes.size() == 4) {
        for (int k = 0; k < 2; k++) {
          int a = info.writes.get(k), b = info.writes.get(k + 2);
          int t1 = taint.reg[a];
          taint.reg[a] = taint.reg[b];
          taint.reg[b] = t1;
        }
        regSwap = true;
      }
      // suppressMem exists so the TRACER can collapse per-byte traffic; taint needs the
      // opposite: PUSH/POP move game data (the classic stack blit) and the LDIR family is
      // handled per byte right here — only call/ret return-address machinery is suppressed
      suppress = info.suppressMem && !info.push && !info.pop && !info.bulk;
      bulk = info.bulk;
      curPc = pc;
      curLen = Math.max(1, instruction.getLength());
      pendingRead = OriginTaint.NONE;
      lastRead = OriginTaint.NONE;
      inExecution = true;
    }

    @Override
    public void afterExecution(Instruction<WordNumber> instruction) {
      inExecution = false;
      if (!bulk && !regSwap)
        for (int slot : info.writes)
          taint.reg[slot] = taint.union(srcTaint, pendingRead);
    }

    private void publish(int frame) {
      byte[] pixels = new byte[PIXEL_BYTES];
      byte[] attrs = new byte[ATTR_BYTES];
      int[] owner = new int[PIXEL_BYTES];
      int[] tile = new int[PIXEL_BYTES];
      for (int i = 0; i < PIXEL_BYTES; i++) {
        WordNumber w = data[SCREEN + i];
        pixels[i] = (byte) (w == null ? 0 : w.intValue());
        int node = taint.mem[SCREEN + i];
        // sprite ownership must be FRESH: moving sprites get redrawn every frame, so
        // taint on a byte nothing wrote for a while is a stale trail, not a sprite.
        // -Dfresh.frames=0 disables the gate (JSW/MM never leave stale sprite trails).
        owner[i] = FRESH_FRAMES <= 0 || frame - lastWrite[i] <= FRESH_FRAMES
            ? taint.spriteOf(node) : 0;
        tile[i] = taint.tileOf(node);
      }
      for (int i = 0; i < ATTR_BYTES; i++) {
        WordNumber w = data[ATTRS + i];
        attrs[i] = (byte) (w == null ? 0 : w.intValue());
      }
      onFrame.accept(new FrameSnapshot(frame, pixels, attrs, owner, tile));
    }

    /**
     * The emulator outruns the Spectrum by ~30x, so the game's speed is ENTIRELY this
     * sleep: frame N shows 20ms/{@link #speed} after the last rebase. Rebasing (instead of
     * targeting t0 + N*20ms absolutely) is what lets the speed change mid-replay without
     * the clock demanding a catch-up burst for time it "owes".
     */
    private void pace(int frame) {
      if (frame < seekTo)
        return; // seeking: run flat out until the frame the viewer asked for
      float s = speed;
      if (t0 < 0 || s != pacedSpeed) {
        t0 = System.nanoTime();
        pacedFrom = frame;
        pacedSpeed = s;
      }
      long target = t0 + (long) ((frame - pacedFrom) * (20_000_000L / s));
      long wait = target - System.nanoTime();
      if (wait > 1_000_000)
        try {
          Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
    }
  }
}
