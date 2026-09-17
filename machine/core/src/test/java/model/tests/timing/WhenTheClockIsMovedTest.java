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
package model.tests.timing;

import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A task waits for a T-state, and a T-state only means anything against the clock it was read
 * from. Whoever moves that clock has to move what is waiting on it by the same amount.
 * <p>
 * This is what a change of speed got wrong: it put the clock at a fixed T-state and left the
 * timetable alone, so a tape edge two hundred T-states away became one due fifty thousand later -
 * or one already past, fired at once - depending on where in the frame the speed was changed. A
 * loader told that a pulse lasted a frame stops loading, which is what it looked like from outside.
 */
class WhenTheClockIsMovedTest {
  private final SpectrumZ80Clock clock = new SpectrumZ80Clock();
  private final Scheduler scheduler = new Scheduler(clock);
  private long ranAt = -1;

  private final Task edge = new Task() {
    public void run(long due) {
      ranAt = due;
    }
  };

  @Test
  void aTaskStaysAsFarAwayAsItWasWhenTheClockGoesBack() {
    clock.setTStates(12000);
    scheduler.register(edge);
    scheduler.schedule(edge, clock.getAbsTstates() + 200);

    // What a change of speed does: the clock is put somewhere else, and everything waiting on it
    // goes with it, in one call so that neither half can be forgotten.
    scheduler.moveClockTo(60000);

    
    assertFalse(scheduler.nextIsAfter(60000 + 200), "it is due 200 T-states from where the clock is now");
    scheduler.runDue();
    assertEquals(-1, ranAt, "and not before that");

    clock.addTStates(200);
    scheduler.runDue();
    assertEquals(60000 + 200, ranAt);
  }

  @Test
  void aFrameEndIsTheSameThingAndAlreadyWorked() {
    clock.setTStates(69000);
    scheduler.register(edge);
    scheduler.schedule(edge, 69000 + 500);

    scheduler.frameEnded(69888);
    clock.addTStates(-69888);

    clock.addTStates(500);
    scheduler.runDue();
    assertEquals(69000 + 500 - 69888, ranAt, "as far away as it was, counted from the new clock");
  }
}
