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
 * for a prefixed opcode, so the seven-bit wrap is never ambiguous - and a difference above two
 * is not a count at all but LD R,A having written the register, which {@link #step()} says more
 * about.
 * <p>
 * It knows nothing about any particular machine: give it a processor, the port that plays the
 * recorded input back, and the recording.
 * <p>
 * One thing it does have to tell the machine, because it is running the processor in the
 * machine's place: when a frame ended and how long it was. A machine's own loop ends a frame on
 * its own count of T-states, and with that loop bypassed nobody would - the count grows without
 * bound until it runs off the end of the tables the ULA and the memory index by it. What is
 * recorded is where the frames ended, so those are the ends the machine is given.
 */
public class RzxPlayback {

  private final OOZ80 cpu;
  private final State state;
  private final Register registerR;
  private final RZXPlayerIO player;
  private final java.util.function.IntPredicate endOfFrame;
  private final int frames;
  private final com.fpetrola.z80.cpu.Z80Clock clock;
  /** Told the length of every frame the recording ends, for the machine to end its own on. */
  private final java.util.function.IntConsumer frameEnded;
  /**
   * How an instruction is run: through the machine rather than the bare processor, so that
   * whatever it hangs on a fetch is heard.
   * <p>
   * Final and given at construction, not set afterwards by a builder. It is read once per
   * instruction, and a field the compiler cannot fold to a constant is read there too: measured on
   * a whole recording, the version that set it afterwards deoptimised playFrame 62 times in a slow
   * run against 12 in a fast one, and the same recording took between 7.2 and 10.1 seconds.
   */
  private final Runnable stepper;

  private int fetchCounter;
  private int previousR;
  private int frameIndex;
  private long instructions;

  public RzxPlayback(OOZ80 cpu, RZXPlayerIO player, RzxFile recording, java.util.function.IntConsumer frameEnded, Runnable stepper) {
    this.frameEnded = frameEnded;
    this.stepper = stepper;
    this.cpu = cpu;
    this.player = player;
    this.state = cpu.getState();
    this.registerR = state.getRegisterR();
    this.frames = recording.getInputRecordingBlock().frames.size();

    player.setup(recording);
    player.setPc(state.getPc());
    // WHERE THE ACKNOWLEDGE FETCH BELONGS. Taking an interrupt is an M1 cycle like any other -
    // it bumps R - and a recorder counts it in the frame that ENDS, so the next frame's budget
    // starts after it. It only exists when the processor actually accepts, and that is what this
    // answers; a frame that runs with interrupts disabled has no acknowledge to account for.
    //
    // The player has always known how to do this and nothing outside the tests ever told it, so
    // every frame that took an interrupt ran one fetch long. One fetch is enough to carry a
    // port read across the boundary, and then the frame that lost it is one read short and the
    // next one read over, from where it drifts. Jet Set Willy is the one recording that never
    // showed it, because it runs with IFF1 at zero from beginning to end.
    // The processor's own rule, mirrored: it accepts when the line is asserted, IFF1 is set and
    // an EI is not still pending - an EI defers the interrupt by one instruction, so a frame
    // whose last instruction is EI takes no interrupt and has no acknowledge fetch, however set
    // IFF1 looks. The line is not asserted yet where this is asked, which is why it is not part
    // of it.
    player.setAcceptsInterrupt(() -> state.isIff1() && !state.isPendingEI());
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

    // The machine's frame ends here too, and lasted what the clock says it lasted: the interrupt
    // then falls where the recorded machine had it, at the top of the picture, and the frame
    // after it starts from there. Ended on the machine's own count instead, the interrupt walks
    // around inside the picture and takes every effect a game times from it along.
    frameEnded.accept(clock.getTStates());

    // The frame ends on the interrupt the recording expects, so it is taken here rather than
    // wherever the machine's own timing would have put it. After the boundary, as on a machine
    // that is not being driven: the cycles that acknowledge it belong to the frame it starts.
    state.setINTLine(true);
    step();
    state.setINTLine(false);
    frameIndex++;
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
    stepper.run();
    instructions++;
    int now = registerR.read() & 0x7F;
    int stepped = (now - previousR) & 0x7F;
    // ONE INSTRUCTION WRITES R RATHER THAN BUMPING IT: LD R,A, and after it R is whatever A
    // held, so the difference across that step is not a count of anything. Left alone it went
    // into the frame's budget as fetches that never happened - measured at 25 for a single step
    // - and the frame was then cut early by that much. A tight polling loop shows it plainly:
    // Ping Pong waits on IN A,($FE) at five fetches a turn and came up 21 turns short of the
    // 1011 the recording holds, which is those fetches to within one turn.
    //
    // Nothing else can add more than two, so more than two means R was written, and LD R,A is
    // two fetches. It can still land within two of where R was - three values out of 128 - and
    // then this counts one or two instead; that is the whole of what is left of it.
    fetchCounter += stepped > 2 ? 2 : stepped;
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
