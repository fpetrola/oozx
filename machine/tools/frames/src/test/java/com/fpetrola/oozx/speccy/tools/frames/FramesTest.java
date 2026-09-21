/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.fpetrola.oozx.speccy.tools.frames;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FramesTest extends MachineTest {

  private static final int START = 0x8000;

  /** NOP / JP 8000 in RAM, so the machine runs frame after frame with nothing else happening. */
  private Speccy looping() {
    Speccy speccy = silentMachine();
    int[] loop = {0x00, 0xc3, 0x00, 0x80};
    for (int i = 0; i < loop.length; i++) {
      speccy.memory.poke(START + i, (byte) loop[i]);
    }
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
    return speccy;
  }

  @Test
  void aFrameEndsWhenTheMachineSaysItDid() {
    Speccy speccy = looping();
    Frames frames = new Frames(speccy);

    runFrames(speccy, 4);

    List<Frames.Frame> ended = frames.ended();
    assertTrue(ended.size() >= 3, "frames ended: " + ended.size());
    assertEquals(ended.get(0).number() + 1, ended.get(1).number(), "and they are consecutive");
    assertTrue(ended.get(0).instructions() > 1000,
        "a frame of this loop is thousands of instructions, not " + ended.get(0).instructions());
  }

  @Test
  void itSaysTheStretchOfMemoryAFrameRanIn() {
    Speccy speccy = looping();
    Frames frames = new Frames(speccy);

    runFrames(speccy, 3);

    Frames.Frame frame = frames.ended().get(1);
    assertTrue(frame.lowest() <= START, "the loop is in it: " + Integer.toHexString(frame.lowest()));
    assertTrue(frame.highest() >= START + 1, "and so is its jump");
  }

  /** However long it is left running, it holds a couple of seconds of frames and no more. */
  @Test
  void itNeverHoldsMoreThanItKeeps() {
    Speccy speccy = looping();
    Frames frames = new Frames(speccy);

    runFrames(speccy, Frames.KEPT + 5);

    assertEquals(Frames.KEPT, frames.ended().size());
  }

  @Test
  void lettingGoLeavesTheMachineAsItWas() {
    Speccy speccy = looping();
    Frames frames = new Frames(speccy);
    runFrames(speccy, 3);
    int had = frames.ended().size();

    frames.close();
    runFrames(speccy, 3);

    assertEquals(had, frames.ended().size(), "it went on counting frames after it let go");
  }
}
