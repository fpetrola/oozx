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

package fuse;

import com.fpetrola.z80.cpu.Z80Clock;
import fuse.tstates.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What the Fuse expectations are compared against: every bus event, stamped with the clock as
 * it was when the event began, and the clock moved on by what the event took.
 */
public class RecordedEvents implements Consumer<Event> {
  private final List<Event> events = new ArrayList<>();
  public Z80Clock clock;

  public void accept(Event event) {
    int time = event.getTime();
    event.setTime(clock.getTStates());
    clock.addTStates(time);
    events.add(event);
  }

  public List<Event> list() {
    return events;
  }

  public void clear() {
    events.clear();
  }
}
