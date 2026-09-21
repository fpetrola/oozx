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
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * What the machine did in each of the last frames.
 * <p>
 * A game is written against the frame: the interrupt comes, it moves everything, draws, and has
 * to be waiting for the next one. So the question that matters is not how fast the emulator
 * goes but how much of each frame the game spends - and a frame that ran far more instructions
 * than the ones around it is the frame where something happened.
 * <p>
 * The boundary is the machine's own frame count going up, not a clock this has to interpret:
 * a recording drives frames of its own length and the count still says when one ended.
 */
public class Frames {

  /** A couple of seconds of frames, which is as far back as anybody reads one of these. */
  public static final int KEPT = 100;

  /** One frame: what ran in it, over how much of the clock, and how far apart those two are. */
  public record Frame(long number, int instructions, int tStates, int lowest, int highest) {
  }

  private final PcTraps.Watch watching;
  private final Speccy machine;
  private final Deque<Frame> frames = new ArrayDeque<>();
  private long counting = -1;
  private int instructions;
  private int firstTState;
  private int lastTState;
  private int lowest = 0xffff;
  private int highest;

  public Frames(Speccy machine) {
    this.machine = machine;
    watching = machine.cpu.beforeFetch().watch(0x0000, 0xffff, this::sawFetch);
  }

  private synchronized void sawFetch(int pc) {
    long frame = machine.machine.current.frameCount();
    if (frame != counting) {
      if (counting >= 0) {
        endFrame();
      }
      counting = frame;
      instructions = 0;
      firstTState = machine.zxClock.getTStates();
      lowest = 0xffff;
      highest = 0;
    }
    instructions++;
    lastTState = machine.zxClock.getTStates();
    lowest = Math.min(lowest, pc);
    highest = Math.max(highest, pc);
  }

  private void endFrame() {
    frames.addLast(new Frame(counting, instructions, lastTState - firstTState, lowest, highest));
    while (frames.size() > KEPT) {
      frames.removeFirst();
    }
  }

  /** The frames that have ended, the most recent last. The one running is not among them. */
  public synchronized List<Frame> ended() {
    return new ArrayList<>(frames);
  }

  /** How far into the frame it is right now, in instructions: what a held machine shows. */
  public synchronized int soFar() {
    return instructions;
  }

  public synchronized void forget() {
    frames.clear();
  }

  /** Lets the machine go: from here on it runs as though this had never been watching. */
  public void close() {
    watching.off();
  }
}
