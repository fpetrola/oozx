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

package com.fpetrola.oozx.speccy.modules.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.fpetrola.z80.cpu.Z80Clock;

/** Decides when each task runs: on a timetable of T-states, in order, as the clock reaches them. */
@Singleton
public class Scheduler {
  /** -Devents.trace=true prints every task as it runs. */
  private static final boolean TRACE = Boolean.getBoolean("events.trace");

  private final Z80Clock clock;
  private final Timetable timetable = new Timetable();

  @Inject
  public Scheduler(Z80Clock clock) {
    this.clock = clock;
  }

  /** From now on this task can be scheduled. Of two due together, the one registered first runs first. */
  public <T extends Task> T register(T task) {
    timetable.add(task);
    return task;
  }

  /** Runs the task at this T-state, instead of whenever it was going to run. */
  public void schedule(Task task, long due) {
    timetable.put(task, due);
  }

  /** The task is not going to run, until it is scheduled again. */
  public void cancel(Task task) {
    timetable.remove(task);
  }

  /**
   * Whether the next task is still ahead of this T-state, which is how long the processor may run
   * for. With nothing scheduled it is not: a machine with nothing to stop it does not run.
   */
  public boolean nextIsAfter(long tstates) {
    return tstates < timetable.nextDue();
  }

  /** Runs everything due by the clock, soonest first, until nothing is. */
  public void runDue() {
    while (!timetable.isEmpty() && timetable.nextDue() <= clock.getTStates()) {
      Timetable.Row row = timetable.takeFirst();
      if (TRACE) {
        System.out.printf("%s at %d (clock %d)%n", row.task.name(), row.due, clock.getTStates());
      }
      row.task.run(row.due);
    }
  }

  /** A frame went by: everything still due is that much closer, since the clock went back too. */
  public void frameEnded(int tstatesPerFrame) {
    timetable.shiftAll(tstatesPerFrame);
  }

  /** Nothing is due any more. What is registered stays registered. */
  public void clear() {
    timetable.clear();
  }
}
