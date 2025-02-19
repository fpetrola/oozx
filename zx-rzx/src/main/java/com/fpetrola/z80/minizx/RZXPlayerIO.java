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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.ide.rzx.InputRecordingBlock;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.minizx.emulation.OutListener;
import com.fpetrola.z80.registers.Register;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;


public class RZXPlayerIO implements MiniZXIO {
  public MiniZXKeyboard miniZXKeyboard;
  private Register pc;
  /**
   * LIVE mode: the recording is abandoned and the player takes over — INs read the real
   * keyboard matrix instead of the recorded values, and frame interrupts fire every
   * {@link #liveFetches} fetches (the recording's own average frame length, so the game
   * keeps its familiar pace). One way: once the input diverges there is no going back.
   */
  private volatile boolean live;
  /**
   * Streams recorded input as one continuous sequence instead of strict per-frame delivery.
   * Needed by the Java port of the game, whose {@code pc(addr, rdelta)} measures instruction
   * length rather than Z80 M1 cycles, so per-frame alignment starves it of input (it consumes
   * more reads than the frame recorded, gets the last value repeated, and sticks forever on
   * JSW's code-entry screen). Measured over 20M instructions: 379 lit pixels vs. 1363 streamed.
   *
   * <p>Off by default because the emulator, which does replay fetch counts, needs strict
   * alignment: carrying a frame's unconsumed reads into the next fed it a stale keyboard scan
   * and the desync compounded on its own (Abu Simbel: 26% of frames synced). The two consumers
   * need different fidelity, not one universally better policy.
   */
  private boolean streamed;
  private long liveFetches = 17000;
  /**
   * Records live play so it can be appended to the original snapshot and old frames when
   * saving. Each live frame supplies exactly the two numbers the format needs — fetch count and
   * the bytes each IN returned — both produced by this class, so the recording is reproducible
   * by construction. See {@link com.fpetrola.z80.ide.rzx.RzxWriter}.
   */
  private boolean recordLive;
  /**
   * What happens when the recording runs out: by default {@link #changeFrame()} throws
   * {@code "rzx finished"}, which some runners rely on as an end-of-run signal, so it cannot
   * simply be removed. With this flag set, running out instead starts live mode, which is how
   * a recording gets extended.
   */
  private boolean continueLiveAtEnd;
  private final List<InputRecordingBlock.Frame> recordedFrames = new ArrayList<>();
  private final java.io.ByteArrayOutputStream liveIns = new java.io.ByteArrayOutputStream();
  /** The recording being replayed: source of the snapshot and old frames when saving. */
  private RzxFile source;
  /**
   * Frame where live play took over; -1 while still replaying. Live play continues from here,
   * not from the recording's end: if it was cut midway, the frames after the cut were never
   * run, and keeping them would produce a file that replays a stretch the live session never
   * saw.
   *
   * <p>The cut frame itself is recorded whole, including its pre-cut reads and its original
   * duration, because writing only the tail produces a file real players reject ("more INs
   * during frame N than stored in RZX file"). See {@link #anotar(int)} and {@link #frameBudget}.
   */
  private int liveStartFrame = -1;
  /**
   * Fetch length of the frame in progress. Set when each frame starts — the recording's while
   * replaying, {@link #liveFetches} once live — and left untouched by {@link #goLive()}, so the
   * frame that was mid-run when the cut happened finishes with its own budget.
   *
   * <p>Without this the cut frame took the live frame's duration instead (measured on jsw-full:
   * 6621 where the recording said 8099), producing a file that misreports the duration of a
   * frame already in progress.
   */
  private long frameBudget;
  private int currentFrameIndex;
  private InputRecordingBlock.Frame currentFrame;
  private SimpleQueue<Byte> inputs = new SimpleQueue<>(1000000);
  private InputRecordingBlock inputRecordingBlock;
  private List<InputRecordingBlock.Frame> frames;
  private long lastCount;
  private byte lastPoll;
  private List<OutListener> outListeners = new ArrayList<>();
  private int fetchCounter;
  private static final boolean DEBUG_SYNC = Boolean.getBoolean("rzx.debug");
  /** -Drzx.advance=ins: advance purely on IN demand, without a fetch-count cursor. */
  private static final boolean ADVANCE_BY_INS = "ins".equals(System.getProperty("rzx.advance"));
  private static final boolean POLL_GUARD = !"false".equals(System.getProperty("rzx.pollguard"));

  /**
   * Synced against the recording itself: each RZX frame states how many port reads the
   * recording machine made, and a faithful replay must consume exactly that many.
   * {@code framesShort} = fewer reads than recorded (the leftover carries to the next frame),
   * {@code framesOver} = more (padded with a repeated value). {@code firstDesync} is the frame
   * where it started drifting.
   */
  public int framesExact, framesShort, framesOver, firstDesync = -1;
  public long insConsumed, insRecorded;
  private int consumedThisFrame;
  /** Per-frame deviation (consumed - recorded) to frame count: a systematic ±1 is a frame-edge
   *  issue, a large and varying spread is execution divergence. */
  public final java.util.Map<Integer, Integer> deltaHist = new java.util.TreeMap<>();
  /** PC that requested each port read: points at the responsible routine. */
  public final java.util.Map<Integer, Integer> inPcHist = new java.util.HashMap<>();
  /** PC where each frame was cut: a faithful replay always cuts at the same handful of spots
   *  (HALT or the game's wait loop); scattered cuts inside a routine mean the fetch count is
   *  off. */
  public final java.util.Map<Integer, Integer> intPcHist = new java.util.HashMap<>();
  /**
   * Per-frame log of the first frames as {@code {frame, delta, pc where it cut}}. The histogram
   * shows how much drift there is; this shows when it starts and what was executing — the
   * difference between "desyncs" and "desyncs on entering the ROM".
   */
  public final java.util.List<int[]> frameLog = new java.util.ArrayList<>();
  public static final int FRAME_LOG_MAX = 4000;
  /**
   * Every port read in a window of frames, as {@code {frame, order, pc, port, value}}. The
   * per-frame delta says how many reads are off; this says which one. A read missing at the end
   * of a frame is the edge falling early; one missing mid-frame is a different execution path.
   * {@code -Dzx.in.dump=305-315}.
   */
  public final java.util.List<int[]> inLog = new java.util.ArrayList<>();
  private static final int[] IN_DUMP = parseWindow(System.getProperty("zx.in.dump"));

  private static int[] parseWindow(String spec) {
    if (spec == null)
      return null;
    String[] parts = spec.split("-");
    return new int[]{Integer.parseInt(parts[0]),
        Integer.parseInt(parts[parts.length - 1])};
  }

  public RZXPlayerIO() {
    miniZXKeyboard = new MiniZXKeyboard();
  }

  private final List<OutListener> inListeners = new ArrayList<>();

  public void addInListener(OutListener inListener) {
    inListeners.add(inListener);
  }

  public void addOutListener(OutListener outListener) {
    outListeners.add(outListener);
  }

  public int getCurrentFrameIndex() {
    return currentFrameIndex;
  }

  /** The emulator's fetch counter, as seen by the interrupt predicate. */
  public int getFetchCounter() {
    return fetchCounter;
  }

  /** Fetch index where the current frame started; {@code fetchCounter - lastCount} is its height. */
  public long getFrameStart() {
    return lastCount;
  }

  /** Port reads consumed so far by the frame in progress. */
  public int getConsumedThisFrame() {
    return consumedThisFrame;
  }

  /** Last byte written to port $FE, source of the EAR bit read back. */
  private int lastFe = 0x18;

  public void out(int port, int value) {
    if ((port & 0xff) == 0xfe)
      lastFe = value & 0xff;
    for (OutListener l : outListeners)
      l.outAt(port, value);
  }

  /**
   * The keyboard port's live value: bits 0-4 are the half-row, 5 and 7 are stuck high (no such
   * lines), and 6 is the EAR bit, which on a 48K issue 3 echoes the last value written to the
   * speaker.
   *
   * <p>Masking with {@code & 191} used to force bit 6 to 0 always, so live play never returned
   * $FF — the opposite of what recorded reads (the oracle) show most games expect: Abu Simbel
   * gets $FF on 95% of its reads, Wally $BF on 84%. A game comparing the whole byte to $FF to
   * mean "no key" then sees a key stuck down forever and stops responding.
   */
  public static int liveInValue(int port, MiniZXKeyboard kb, int lastFe) {
    // Odd port = Kempston, which must answer "no joystick" as bit 5 set. Returning 0 for "no
    // movement" is only correct once a joystick exists; JSW probes for one first (34968) by
    // ORing 256 reads of port 31 and ANDing with 32 — zero there means "joystick present", so
    // our all-zero reads made it always conclude one was connected (flag at 34254). Measured:
    // that flag is 0 in jsw-full (a real-machine recording) and was 1 across everything we
    // recorded live — it changes where the game reads input from, which is exactly what RZX
    // frame accounting depends on. $20 rather than $ff keeps the direction bits at zero too, so
    // a game reading Kempston without probing sees no invented movement either.
    if ((port & 1) != 0)
      return 0x20;
    int ear = (lastFe & 0x10) != 0 ? 0x40 : 0;
    return (kb.readKeyboardPort(port, true) & 0x1f) | 0xa0 | ear;
  }

  /** Enabled by a player whose engine does not count fetches like the Z80. */
  public void setStreamed(boolean streamed) {
    this.streamed = streamed;
  }

  public boolean isStreamed() {
    return streamed;
  }

  public boolean isLive() {
    return live;
  }

  /**
   * How many frames to look at when sizing a live frame: the ones next to the cut, not the
   * startup ones, since how many fetches fit in a real frame depends on what the game is
   * running, and the title screen and gameplay run different instruction mixes.
   */
  public static final int LIVE_WINDOW = 200;

  public void goLive() {
    if (live || frames == null)
      return;
    liveFetches = medianFrameLength(currentFrameIndex);
    miniZXKeyboard.reset();
    liveStartFrame = currentFrameIndex;
    live = true;
  }

  /**
   * Median length, in fetches, of the frames next to the cut.
   *
   * <p>An RZX frame is measured in fetches, but a real player also tracks T-states and flags a
   * frame that overruns one (Fuse: <em>"RZX frame is longer than 79000 tstates"</em>). This
   * emulator does not count T-states, so the live frame budget has to be estimated, and the
   * recording itself is the best estimator: made on a real machine at 50 Hz, so a typical frame
   * near the cut already is a 69888-T-state frame expressed in fetches.
   *
   * <p>Median rather than mean, since a mean is skewed by any one odd frame; and neighbors of
   * the cut rather than the first 2000 frames, since averaging startup gave 7302 fetches on
   * jsw-full where the game at the cut ran 6527 — +12%, ~78200 T-states, over Fuse's limit.
   */
  private long medianFrameLength(int cut) {
    int end = Math.min(cut, frames.size());
    int start = Math.max(0, end - LIVE_WINDOW);
    if (end - start < 1) {
      // Cut at frame zero (-Dplay=true): no prior neighborhood, so use the startup one instead.
      start = 0;
      end = Math.min(LIVE_WINDOW, frames.size());
    }
    if (end - start < 1)
      return liveFetches;
    int[] lengths = new int[end - start];
    for (int i = start; i < end; i++)
      lengths[i - start] = frames.get(i).fetchCounter;
    java.util.Arrays.sort(lengths);
    return Math.max(1000, lengths[lengths.length / 2]);
  }

  public synchronized int in(int port) {
    int value = enElPuerto(port);
    for (OutListener l : inListeners)
      l.outAt(port, value);
    return value;
  }

  private int enElPuerto(int port) {
    if (live) {
      int value = liveInValue(port, miniZXKeyboard, lastFe);
      anotar(value);
      return value;
    }
    if (currentFrame == null)
      return 0;
    return performIn(port);
  }

  private int lastPort;

  private int performIn(int port) {
    lastPort = port;
    byte value = getNextInput();
    anotar(value);
    return value;
  }

  /**
   * Reads made during replay are recorded too, not just live ones. Replayed frames themselves
   * are copied from the original rather than re-recorded, but capturing their reads is what
   * lets the cut frame come out whole: when the cut lands mid-frame, that frame already served
   * some reads from the recording, and without them the live frame would claim fewer reads than
   * the game actually made — which real players reject ("more INs during frame N than stored in
   * RZX file"). The buffer is cleared at each replayed frame's close, so only the in-progress
   * frame's reads ever survive, which is exactly what is needed.
   */
  private void anotar(int value) {
    if (recordLive)
      liveIns.write(value & 0xff);
  }

  private byte getNextInput() {
    if (inputs.isEmpty()) {
      // The emulator asked for more reads than this frame recorded. The next frame's values
      // belong to the next frame: serving them now would advance input early and decouple the
      // frame index from the fetch counter (which still measures against the old frame). The
      // last value is repeated instead, and the frame only advances once its fetch budget is met.
      if (DEBUG_SYNC)
        System.out.println("rzx-sync: frame " + currentFrameIndex
            + " ran out of inputs BEFORE its interrupt (emulator consumed too many INs)");
      if (streamed) {
        if (ADVANCE_BY_INS) {
          // Original advance model: the recording walks purely on IN demand, skipping frames
          // that recorded no reads until one with material is found; pc() plays no part. The
          // loop always terminates, since past the last frame changeFrame throws "rzx finished".
          while (inputs.isEmpty()) {
            ++currentFrameIndex;
            changeFrame();
          }
        } else {
          ++currentFrameIndex;
          changeFrame();
        }
      } else {
        consumedThisFrame++;
        if (pc != null)
          inPcHist.merge(pc.read(), 1, Integer::sum);
        return lastPoll;
      }
    }
    consumedThisFrame++;
    if (pc != null)
      inPcHist.merge(pc.read(), 1, Integer::sum);
    // Poll only when non-empty: SimpleQueue#poll has no guard and decrements its counter
    // regardless, so polling an empty queue drives the counter negative and isEmpty()
    // (counter == 0) never becomes true again. A single frame recorded with zero reads is
    // enough to trigger this, after which exhaustion-driven advance — the only option once
    // there is no fetch-count frame edge — stays dead and stale ring-buffer values get served.
    // Measured without pc(): the recording walks on its own to frame 592 and sticks there with
    // the counter at -44044.
    //
    // -Drzx.pollguard=false restores the old behavior, to measure against it.
    Byte poll = (POLL_GUARD && inputs.isEmpty()) ? null : inputs.poll();
    if (poll == null)
      return lastPoll;
    else
      lastPoll = poll;
    if (IN_DUMP != null && currentFrameIndex >= IN_DUMP[0] && currentFrameIndex <= IN_DUMP[1])
      inLog.add(new int[]{currentFrameIndex, consumedThisFrame,
          pc == null ? -1 : pc.read(), lastPort, poll & 0xff});
    return poll;
  }

  /**
   * THE RECORDING IS PAST ITS LAST FRAME. A harness that answers some port on the recording's
   * behalf (absent hardware, floating bus) must stop answering HERE and let every later
   * {@code in()} reach {@link #getNextInput}: the queue may still hold the final frame's
   * leftover values, draining them is the tail's job, and "rzx finished" is thrown from there
   * and nowhere else. Both halves are measured on JSW -- answering past this point left the
   * replay spinning at the final frame until the 2-billion-step cap (twice: once waiting for
   * the queue to empty on its own, which nothing does), with the end state already correct.
   */
  public boolean finished() {
    return frames != null && currentFrameIndex >= frames.size();
  }

  public MiniZXKeyboard getMiniZXKeyboard() {
    return miniZXKeyboard;
  }

  /**
   * Whether the CPU can accept the interrupt at the moment the frame closes. The acknowledge is
   * an M1 cycle and therefore a fetch: a frame where the interrupt is accepted costs one more
   * than one where it was disabled. If the recorder and this replay don't count that fetch on
   * the same side of the edge, the drift shows up only in frames where that changes — which is
   * exactly the shape Abu Simbel's desync had, and why shifting every frame by a fixed amount
   * didn't fix it.
   */
  private java.util.function.BooleanSupplier acceptsInterrupt;

  public void setAcceptsInterrupt(java.util.function.BooleanSupplier acceptsInterrupt) {
    this.acceptsInterrupt = acceptsInterrupt;
  }

  public void setPc(Register pc) {
    this.pc = pc;
  }

  public void setup(RzxFile rzxFile) {
    source = rzxFile;
    inputRecordingBlock = rzxFile.getInputRecordingBlock();
    frames = inputRecordingBlock.frames;
    currentFrameIndex = 0;
    // The counter here is fetches since replay start (from 0); the block's tStates are a
    // different unit and using them delayed the first interrupt by that many fetches.
    lastCount = 0;
    lastPoll = 0;
    fetchCounter = 0;
    inputs.clear();
    changeFrame();
  }

  private void changeFrame() {
    if (currentFrameIndex < frames.size()) {
      printFrameCount();

      // IN values belong to their frame: whatever this frame didn't consume is discarded.
      // Carrying it over fed the next frame a stale keyboard scan, and the drift compounded
      // on its own (Abu Simbel: 26% of frames synced, with symmetric -4/+4 swings between
      // neighboring frames).
      if (!streamed)
        inputs.clear();
      currentFrame = frames.get(currentFrameIndex);
      frameBudget = currentFrame.fetchCounter;
      for (int i = 0; i < currentFrame.returnValues.length; i++) {
        inputs.add(currentFrame.returnValues[i]);
      }
    } else {
      if (inputs.isEmpty()) {
        if (continueLiveAtEnd) {
          // Printed once, unconditionally: this is a mode change, and without it the switch
          // to live is unobservable from outside since the frame counter keeps advancing
          // either way.
          System.out.println("rzx terminado en el frame " + currentFrameIndex
              + ": el teclado es tuyo, se graba lo que juegues (Ctrl+G guarda)");
          goLive();
          frameBudget = liveFetches;
          return;
        }
        throw new RuntimeException("rzx finished");
      }
      inputs.add((byte) 0);
    }
  }

  private void printFrameCount() {
    if (DEBUG_SYNC && currentFrameIndex % 1000 == 0)
      System.out.println(currentFrameIndex);
  }

  /** Records each live-played frame, so the extended recording can later be saved. */
  public void setRecordLive(boolean recordLive) {
    this.recordLive = recordLive;
  }

  public boolean isRecordLive() {
    return recordLive;
  }

  /** Once frames run out, continue live instead of throwing {@code "rzx finished"}. */
  public void setContinueLiveAtEnd(boolean continueLiveAtEnd) {
    this.continueLiveAtEnd = continueLiveAtEnd;
  }

  public boolean isContinueLiveAtEnd() {
    return continueLiveAtEnd;
  }

  /** Frame where the player took control, or -1 while still replaying. */
  public int getLiveStartFrame() {
    return liveStartFrame;
  }

  /** Frames played live so far, in order. */
  public List<InputRecordingBlock.Frame> getRecordedFrames() {
    return recordedFrames;
  }

  private void closeLiveFrame(int fetches) {
    InputRecordingBlock.Frame frame = new InputRecordingBlock.Frame();
    frame.fetchCounter = fetches;
    frame.returnValues = liveIns.toByteArray();
    frame.inCounter = frame.returnValues.length;
    liveIns.reset();
    recordedFrames.add(frame);
  }

  /**
   * Writes the extended recording: the original's snapshot and frames, then what was played.
   * The writer validates every new frame, so one that couldn't be read back — too many reads,
   * a fetchCounter over 16 bits — fails here instead of silently.
   */
  public void saveExtendedTo(java.nio.file.Path out) throws java.io.IOException {
    saveExtendedTo(out, com.fpetrola.z80.ide.rzx.RzxWriter.Mode.CONTINUE_BLOCK);
  }

  public void saveExtendedTo(java.nio.file.Path out, com.fpetrola.z80.ide.rzx.RzxWriter.Mode mode)
      throws java.io.IOException {
    if (source == null)
      throw new IllegalStateException("no hay grabacion de origen: falto setup(RzxFile)");
    com.fpetrola.z80.ide.rzx.RzxWriter.writeExtended(source, liveStartFrame, recordedFrames, out, mode);
  }

  /**
   * Saves the splice: what was replayed up to the cut, what was played live, and the recording
   * from {@code resume} on. See {@link com.fpetrola.z80.ide.rzx.RzxWriter#writeSpliced}.
   */
  public void saveSplicedTo(java.nio.file.Path out, int resume) throws java.io.IOException {
    if (source == null)
      throw new IllegalStateException("no hay grabacion de origen: falto setup(RzxFile)");
    if (liveStartFrame < 0)
      throw new IllegalStateException("todavia no se jugo nada");
    com.fpetrola.z80.ide.rzx.RzxWriter.writeSpliced(source, liveStartFrame, recordedFrames, resume, out);
  }

  public void savePrependedTo(java.nio.file.Path out) throws java.io.IOException {
    savePrependedTo(out, 0);
  }

  /**
   * @param fromFrame recording frame to resume from. See
   *                  {@link com.fpetrola.z80.ide.rzx.RzxWriter#writePrepended(RzxFile, java.util.List, int, java.nio.file.Path)}:
   *                  the splice point is a code zone, not a frame number.
   */
  public void savePrependedTo(java.nio.file.Path out, int fromFrame) throws java.io.IOException {
    if (source == null)
      throw new IllegalStateException("no hay grabacion de origen: falto setup(RzxFile)");
    if (liveStartFrame != 0)
      throw new IllegalStateException("anteponer exige haber jugado desde el frame cero"
          + " (-Dplay=true), y el vivo arranco en el " + liveStartFrame
          + ": ese tramo continua el estado de ahi, no el del snapshot");
    com.fpetrola.z80.ide.rzx.RzxWriter.writePrepended(source, recordedFrames, fromFrame, out);
  }

  /** Asked once per instruction, so it takes the count itself and not a boxed one. */
  public IntPredicate getInterruptionCondition() {
    return (i) -> {
      fetchCounter = i;
      if (live) {
        if (i - lastCount + 1 > frameBudget) {
          // Same closing rule as the recorded branch, not `lastCount = i`: the acknowledge
          // fetch belongs to the frame that got interrupted, so the next budget starts after
          // it. Closing live frames one way and replaying them the other is a one-fetch-per-
          // frame cumulative drift, the same shape measured on Abu Simbel below. Pinned by
          // LiveRecordingTest.
          if (recordLive)
            closeLiveFrame((int) (i - lastCount));
          ++currentFrameIndex;
          frameBudget = liveFetches;
          lastCount = i + (acceptsInterrupt != null && acceptsInterrupt.getAsBoolean() ? 1 : 0);
          return true;
        }
        return false;
      }
      if (currentFrame != null)
        if (i - lastCount + 1 > frameBudget) {
          // Frame close: did we consume the same number of reads as the recording?
          insConsumed += consumedThisFrame;
          insRecorded += currentFrame.inCounter;
          deltaHist.merge(consumedThisFrame - currentFrame.inCounter, 1, Integer::sum);
          int intPc = pc == null ? -1 : pc.read();
          if (pc != null)
            intPcHist.merge(intPc, 1, Integer::sum);
          if (frameLog.size() < FRAME_LOG_MAX)
            frameLog.add(new int[]{currentFrameIndex,
                consumedThisFrame - currentFrame.inCounter, intPc,
                acceptsInterrupt == null ? -1 : acceptsInterrupt.getAsBoolean() ? 1 : 0});
          if (consumedThisFrame == currentFrame.inCounter)
            framesExact++;
          else {
            if (consumedThisFrame < currentFrame.inCounter)
              framesShort++;
            else
              framesOver++;
            if (firstDesync < 0)
              firstDesync = currentFrameIndex;
          }
          consumedThisFrame = 0;
          liveIns.reset();
          if (DEBUG_SYNC && !inputs.isEmpty())
            System.out.println("rzx-sync: frame " + currentFrameIndex
                + " reached its interrupt with unconsumed inputs (emulator consumed too few INs)");
          ++currentFrameIndex;
          changeFrame();
          // The acknowledge is the last fetch of the interrupting frame, not the first of the
          // next: it's an M1 cycle, incrementing R like any fetch, and the recorder counts it
          // on the closing frame's side, so the next budget starts after it. It only exists
          // when the CPU accepts — a frame with interrupts disabled has none, which is why a
          // fixed per-frame shift didn't work and why JSW (IFF1 zero throughout) never showed
          // the bug. Measured on Abu Simbel: first desync moved from frame 309 to 13704, and
          // exact frames from 33.4% to 76.5% over 20000 frames. JSW stayed at 100%.
          lastCount = i + (acceptsInterrupt != null && acceptsInterrupt.getAsBoolean() ? 1 : 0);
          return true;
        } else
          return false;

      return false;
    };
  }
}
