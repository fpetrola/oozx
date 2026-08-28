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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.registers.Register;

/**
 * Runs a recording against a processor: steps one instruction at a time, counts fetches, and
 * interrupts where the recording says a frame ended.
 * <p>
 * A recording is not measured in T-states but in fetches, so this drives the CPU itself rather
 * than letting a machine's own frame event decide when to interrupt. Feeding a recording into a
 * machine that keeps interrupting on its own clock desynchronises it within a frame or two.
 * <p>
 * The fetch count comes from the R register, which the processor bumps once per M1 cycle, read
 * either side of every step. R is observable, but its increment writes the field directly and
 * never tells a listener, so listening to it counts nothing; taking the difference is exact
 * either way, and needs nothing installed into the machine. Two is as much as one step can add,
 * for a prefixed opcode, so the seven-bit wrap is never ambiguous.
 * <p>
 * It knows nothing about any particular machine: give it a processor, the port that plays the
 * recorded input back, and the recording.
 * <p>
 * One thing it does have to do for the machine, because it is running the processor in the
 * machine's place: keep the clock inside a frame. A machine subtracts a frame's worth of T-states
 * at every frame boundary, and with its own loop bypassed nobody does, so the count grows without
 * bound until it runs off the end of the tables the ULA and the memory index by it - which is a
 * crash a few thousand frames in, nowhere near where the cause is.
 */
public class RzxPlayback {

  private final OOZ80 cpu;
  private final State state;
  private final Register registerR;
  private final RZXPlayerIO player;
  private final java.util.function.Predicate<Integer> endOfFrame;
  private final int frames;
  private final com.fpetrola.z80.cpu.Z80Clock clock;
  private final int frameTStates;

  private int fetchCounter;
  private int previousR;
  private int frameIndex;
  private long instructions;

  /** T-states in a frame of the machine being driven; 69888 on a 48K Spectrum. */
  public static final int SPECTRUM_48K_FRAME = 69888;

  public RzxPlayback(OOZ80 cpu, RZXPlayerIO player, RzxFile recording) {
    this(cpu, player, recording, SPECTRUM_48K_FRAME);
  }

  public RzxPlayback(OOZ80 cpu, RZXPlayerIO player, RzxFile recording, int frameTStates) {
    this.frameTStates = frameTStates;
    this.cpu = cpu;
    this.player = player;
    this.state = cpu.getState();
    this.registerR = state.getRegisterR();
    this.frames = recording.getInputRecordingBlock().frames.size();

    player.setup(recording);
    player.setPc(state.getPc());
    this.endOfFrame = player.getInterruptionCondition();
    this.previousR = registerR.read() & 0x7F;
    this.clock = state.clock;
  }

  /**
   * Runs until the recording says the frame is over.
   *
   * @return false once every recorded frame has been played
   */
  public boolean playFrame() {
    if (isFinished()) {
      return false;
    }

    while (!endOfFrame.test(fetchCounter)) {
      step();
    }

    // The frame ends on the interrupt the recording expects, so it is taken here rather than
    // wherever the machine's own timing would have put it.
    state.setINTLine(true);
    step();
    state.setINTLine(false);
    frameIndex++;

    // Stand in for the frame boundary the machine's own loop would have done.
    while (clock.getTStates() >= frameTStates) {
      clock.addTStates(-frameTStates);
    }
    return true;
  }

  /** Runs up to the given number of frames, stopping early at the end of the recording. */
  public int playFrames(int count) {
    int played = 0;
    while (played < count && playFrame()) {
      played++;
    }
    return played;
  }

  private void step() {
    cpu.execute();
    instructions++;
    int now = registerR.read() & 0x7F;
    fetchCounter += (now - previousR) & 0x7F;
    previousR = now;
  }

  public boolean isFinished() {
    return frameIndex >= frames;
  }

  /** Frames played so far. The player's own index should agree; they part company on a desync. */
  public int getFrameIndex() {
    return frameIndex;
  }

  public int getPlayerFrameIndex() {
    return player.getCurrentFrameIndex();
  }

  public int getFrameCount() {
    return frames;
  }

  public long getInstructions() {
    return instructions;
  }

  public int getFetchCounter() {
    return fetchCounter;
  }
}
