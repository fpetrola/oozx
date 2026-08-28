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

package com.fpetrola.oozx.fuse.modules;

import cern.colt.list.ObjectArrayList;
import com.fpetrola.oozx.MachineChangeListener;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.z80.cpu.Z80Clock;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

import java.util.*;

// When will the next event happen?
public class EventManager implements ZxModule, MachineChangeListener {

  final int EVENT_NO_EVENTS = 0xffffffff;

  // A null event type
  public int eventTypeNull;

  public long eventNextEvent;

  // The actual list of events
//  private final List<Event> eventList = new LinkedList<>();

  private final Z80Clock z80Clock;
  private final SortedSet<Event> events;
  private SpectrumMachine spectrumMachine;

  public EventManager(Z80Clock z80Clock) {
    this.z80Clock = z80Clock;
    Comparator<Event> customComparator = this::eventAddCmp; // Placeholder, not used
    events = new ObjectAVLTreeSet<>(customComparator);
  }

  @Override
  public int init(Object initContext) {
    registeredEvents = new ObjectArrayList();
    eventTypeNull = eventRegister(null, "[Deleted event]");
    eventNextEvent = EVENT_NO_EVENTS;
    return 0;
  }

  @Override
  public void end() {
    reset();
    registeredEventsFree();
  }

  @Override
  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
  }

  class EventDescriptor {
    EventFn fn;
    String description;
  }

  ObjectArrayList registeredEvents;

  public int eventRegister(EventFn fn, String description) {
    EventDescriptor descriptor = new EventDescriptor();
    descriptor.fn = fn;
    descriptor.description = description; // Assume Utils.safeStrdup returns String

    registeredEvents.add(descriptor);

    return registeredEvents.size() - 1;
  }

  private int eventAddCmp(Event a, Event b) {
    if (a.tstates != b.tstates) {
      return Long.compare(a.tstates, b.tstates);
    } else {
      return Integer.compare(a.type, b.type);
    }
  }

  // Add an event at the correct place in the event list
  public void eventAddWithData(long eventTime, int type, Object userData) {
    if (eventTime < eventNextEvent) {
      eventNextEvent = eventTime;
    }
    events.add(new Event(eventTime, type, userData));
  }

  public void changeEventTime(long eventTime, int type) {
    Event found = null;
    for (Event event : events) {
      if (event.type == type) {
        found = event;
        break;
      }
    }
    if (found == null) return;

    // tstates is the sort key, so the event has to leave the tree before it
    // changes and go back in afterwards.
    events.remove(found);
    found.tstates = eventTime;
    events.add(found);

    eventNextEvent = events.isEmpty() ? EVENT_NO_EVENTS : events.first().tstates;
  }

  public void eventAdd(long eventTime, int type) {
    eventAddWithData(eventTime, type, null);
  }

  // Do all events which have passed
  public int eventDoEvents() {
    while (eventNextEvent <= z80Clock.getTStates()) {
      Event firstEvent = events.removeFirst();
      EventDescriptor descriptor = (EventDescriptor) registeredEvents.get(firstEvent.type);

      if (events.isEmpty()) {
        eventNextEvent = EVENT_NO_EVENTS;
      } else {
        eventNextEvent = events.getFirst().tstates;
      }

      if (descriptor.fn != null) {
        descriptor.fn.apply(firstEvent.tstates, firstEvent.type, firstEvent.userData);
      }
    }

    return 0;
  }

  private void eventReduceTstates(Event ptr, int tstatesPerFrame) {
    ptr.tstates -= tstatesPerFrame;
  }

  // Called at end of frame to reduce T-state count of all entries
  public void eventFrame(int tstatesPerFrame) {
    for (Event event : events) {
      eventReduceTstates(event, tstatesPerFrame);
    }

    eventNextEvent = events.isEmpty() ? EVENT_NO_EVENTS : events.getFirst().tstates;
  }

  // Force all events between now and the next interrupt to happen
  public void eventForceEvents() {
    while (eventNextEvent < spectrumMachine.getTimings().tstatesPerFrame) { // Assume Machine.current
      z80Clock.setTStates((int) eventNextEvent);
      eventDoEvents();
    }
  }

  private void setEventNull(Event ptr, int type) {
    if (ptr.type == type) {
      ptr.type = eventTypeNull;
    }
  }

  private void setEventNullWithUserData(Event event, Event template) {
    if (event.type == template.type && event.userData == template.userData) {
      event.type = eventTypeNull;
    }
  }

  // Remove all events of a specific type from the stack
  public void eventRemoveType(int type) {
    for (Event event : events) {
      setEventNull(event, type);
    }
  }

  // Remove all events of a specific type and user data from the stack
  public void eventRemoveTypeUserData(int type, Object userData) {
    Event template = new Event(-1, type, userData);
    for (Event event : events) {
      setEventNullWithUserData(event, template);
    }
  }

  public void reset() {
    events.clear();
    eventNextEvent = EVENT_NO_EVENTS;
  }

  // A textual representation of each event type
  public String eventName(int type) {
    return ((EventDescriptor) registeredEvents.get(type)).description;
  }

  void registeredEventsFree() {
    if (registeredEvents == null) return;

    registeredEvents.clear();
    registeredEvents = null;
  }
}
