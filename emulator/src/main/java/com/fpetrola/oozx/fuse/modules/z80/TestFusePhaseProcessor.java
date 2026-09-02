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

package com.fpetrola.oozx.fuse.modules.z80;

import com.fpetrola.z80.cpu.State;
import fuse.tstates.Contention.Kind;
import fuse.tstates.Event;
import fuse.tstates.PhaseProcessor;

import java.util.function.Consumer;

/** The contention as Fuse's test harness records it: one MC event per cycle, and the memory accesses beside them. */
public class TestFusePhaseProcessor extends PhaseProcessor {
  private final Consumer<Event> events;

  public TestFusePhaseProcessor(State state, Consumer<Event> events) {
    super(state);
    this.events = events;
  }

  public void addMw(int address, int value) {
    events.accept(new Event(0, "MW", address, value));
  }

  public void addMr(int address, int value) {
    events.accept(new Event(0, "MR", address, value));
  }

  public void contend(int address, int times, int tstates, Kind kind) {
    for (int i = 0; i < times; i++)
      events.accept(new Event(tstates, "MC", address, null, kind.description));
  }
}
