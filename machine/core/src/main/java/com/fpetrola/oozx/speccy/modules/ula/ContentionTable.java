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


package com.fpetrola.oozx.speccy.modules.ula;

import com.fpetrola.oozx.speccy.machine.Spectrum;

import java.util.Arrays;

/**
 * How long the ULA holds the processor up, for every T-state of a frame, worked out once for the
 * machine that is on.
 * <p>
 * None of it is computed here: {@link Spectrum#contendDelay} is, per model, and a Pentagon has
 * none at all. This is that answer remembered, which is a different job from being a ULA - and the
 * reason it is worth its own object is that the runs below cannot be had any other way.
 */
public final class ContentionTable {
  /** The longest frame any model has: a Pentagon's 320 lines of 224 T-states. */
  public static final int LONGEST_FRAME = 71680;

  /**
   * How many frames the tables reach. They have to outlast a frame because the clock runs past the
   * end of one while a recording plays and an index has to stay in range: measured on jsw-full.rzx
   * the highest index reached was 102846, which is 1.4 frames. Four is what is kept, and
   * {@code TheContentionTableIsBigEnoughTest} fails if a model ever needs more.
   */
  public static final int FRAMES_KEPT = 4;

  static final int SIZE = FRAMES_KEPT * LONGEST_FRAME;

  /** How much contention there is at every T-state while MREQ is active. */
  public final byte[] delay = new byte[SIZE];

  /** And how much while it is not. */
  public final byte[] delayNoMreq = new byte[SIZE];

  private static final int LAST = SIZE - 1;

  /**
   * Where in this table a T-state looks. Past its end it looks at the last entry, which is the
   * same answer the table already gives for anything past a frame: no contention, and a run of
   * plain cycles. A clock only gets there when nothing ends the machine's frame - a recording
   * says where its own frames end, and one of them can be longer than the four this table keeps.
   */
  public static int within(int tStates) {
    return tStates > LAST ? LAST : tStates;
  }

  private final byte[][] runs = new byte[8][];
  /** How far the delays reach: a frame of the machine they were filled for, nothing before the first. */
  private int frame;

  /**
   * What a run of that many one-T-state accesses to a contended address takes, from every T-state
   * it can start at.
   * <p>
   * A Z80's internal cycles come in runs - five for an indexed displacement - and each one waits
   * for the ULA from wherever the previous one left the clock, so asked one at a time they are as
   * many dependent lookups as the run is long. The whole run depends on nothing but where it
   * starts, and this is it looked up once. Built the first time a length is asked for and again
   * whenever the delays change.
   */
  public byte[] run(int times) {
    if (runs[times] == null) {
      runs[times] = new byte[SIZE];
      fillRun(times);
    }
    return runs[times];
  }

  /** The delays of this machine, and the runs built from them. */
  public void forMachine(Spectrum current) {
    frame = current.getTimings().tstatesPerFrame();
    for (int tState = 0; tState < frame; tState++) {
      delay[tState] = (byte) current.contendDelay(tState);
      delayNoMreq[tState] = (byte) current.contendDelayNoMreq(tState);
    }
    // Past the frame there is no contention, and a shorter frame than the last machine's must
    // not leave that one's tail behind.
    Arrays.fill(delay, frame, delay.length, (byte) 0);
    Arrays.fill(delayNoMreq, frame, delayNoMreq.length, (byte) 0);
    for (int times = 0; times < runs.length; times++) {
      if (runs[times] != null) {
        fillRun(times);
      }
    }
  }

  private void fillRun(int times) {
    byte[] run = runs[times];
    for (int start = 0; start < frame; start++) {
      int t = start;
      for (int i = 0; i < times; i++) {
        t += delayNoMreq[t] + 1;
      }
      run[start] = (byte) (t - start);
    }
    Arrays.fill(run, frame, run.length, (byte) times);
  }
}
