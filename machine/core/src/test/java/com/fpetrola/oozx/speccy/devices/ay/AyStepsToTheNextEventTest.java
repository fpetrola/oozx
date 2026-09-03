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
package com.fpetrola.oozx.speccy.devices.ay;

import com.fpetrola.oozx.speccy.sound.AudioOutput;
import com.fpetrola.oozx.speccy.sound.blip.BlipBuffer;
import com.fpetrola.oozx.speccy.sound.blip.BlipSynth;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chip that goes from one event to the next has to put out exactly what the one that takes
 * every step puts out: the same levels at the same T-states, for any program of register writes.
 * <p>
 * The one that takes every step is the same chip told there is never a quiet step, so the two
 * differ in nothing but the jumping; the writes are random, with tones on and off, envelopes of
 * every shape and noise of every period, over frames whose length is not a multiple of a step.
 */
class AyStepsToTheNextEventTest {

  /** A synth that writes down every level it is handed, and when. */
  private static class Recording extends BlipSynth {
    final List<String> levels = new ArrayList<>();

    Recording() {
      super(BlipBuffer.BLIP_HIGH_QUALITY, 44100, 1000, 7_000_000, 200, 1.0, -37.0);
    }

    @Override
    public void update(long time, int amplitude) {
      levels.add(time + ":" + amplitude);
      super.update(time, amplitude);
    }
  }

  private static class Output implements AudioOutput {
    final Recording synth = new Recording();

    public BlipSynth newSynth(int volumePercent) {
      return synth;
    }

    public BlipSynth newFlatSynth(int volumePercent) {
      return synth;
    }

    public int frameSize() {
      return 447;
    }
  }

  @Test
  void theSameLevelsAtTheSameTStatesWhetherEveryStepIsTakenOrOnlyTheEventfulOnes() {
    Output everyStepOutput = new Output(), eventfulOutput = new Output();
    Ay everyStep = new Ay(everyStepOutput) {
      @Override
      int quietSteps(long f, long frameTstates, long nextChange, int audible) {
        return 0;
      }
    };
    Ay eventful = new Ay(eventfulOutput);
    Random random = new Random(7);
    int frameTstates = 70908;
    for (int frame = 0; frame < 60; frame++) {
      long at = 0;
      for (int writes = random.nextInt(10); writes > 0; writes--) {
        int register = random.nextInt(14);
        // Short periods, so that tones, envelopes and noise all have their edges inside a frame.
        int value = register < 6 || register == 11 ? random.nextInt(random.nextBoolean() ? 8 : 256) : random.nextInt(256);
        at += random.nextInt(frameTstates / 4);
        everyStep.write(register, value, at);
        eventful.write(register, value, at);
      }
      everyStep.endFrame(frameTstates);
      eventful.endFrame(frameTstates);
      assertEquals(everyStepOutput.synth.levels, eventfulOutput.synth.levels, "frame " + frame);
    }
    assertTrue(everyStepOutput.synth.levels.size() > 1000,
        "the program has to make the chip change level a great many times, or this proves little");
  }
}
