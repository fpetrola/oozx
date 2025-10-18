/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.function.Supplier;

// When will the next event happen?
public class EventManager implements ZxModule {

  final int EVENT_NO_EVENTS = 0xffffffff;

  // A null event type
  public int eventTypeNull;

  public int eventNextEvent;

  // The actual list of events
  private LinkedList<Event> eventList = new LinkedList<>();

  // An event ready to be reused
  private Event eventFree = null;
  private Supplier<SpectrumMachine> fuseMachineInfoSupplier;
  private Z80Clock z80Clock;

  public EventManager(Supplier<SpectrumMachine> machine, Z80Clock z80Clock) {
    this.fuseMachineInfoSupplier = machine;
    this.z80Clock = z80Clock;
  }

  @Override
  public int init(Object initContext) {
    registeredEvents = new ArrayList<>();
    eventTypeNull = eventRegister(null, "[Deleted event]");
    eventNextEvent = EVENT_NO_EVENTS;
    return 0;
  }

  @Override
  public void end() {
    reset();
    registeredEventsFree();
  }

  class EventDescriptor {
    EventFn fn;
    String description;
  }

  ArrayList<EventDescriptor> registeredEvents;

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
  public void eventAddWithData(int eventTime, int type, Object userData) {
    Event ptr;

    if (eventFree != null) {
      ptr = eventFree;
      eventFree = null;
    } else {
      ptr = new Event();
    }

    ptr.tstates = eventTime;
    ptr.type = type;
    ptr.userData = userData;

    if (eventTime < eventNextEvent) {
      eventNextEvent = eventTime;
      eventList.addFirst(ptr);
    } else {
      // Insert sorted
      ListIterator<Event> iterator = eventList.listIterator();
      boolean added = false;
      while (iterator.hasNext()) {
        Event current = iterator.next();
        if (eventAddCmp(ptr, current) < 0) {
          iterator.previous();
          iterator.add(ptr);
          added = true;
          break;
        }
      }
      if (!added) {
        eventList.addLast(ptr);
      }
    }
  }

  public void eventAdd(int eventTime, int type) {
    eventAddWithData(eventTime, type, null);
  }

  // Do all events which have passed
  public int eventDoEvents() {
    while (eventNextEvent <= z80Clock.getTstates()) { // Assume Fuse.tstates
      Event ptr = eventList.getFirst();
      EventDescriptor descriptor = registeredEvents.get(ptr.type);

      // Remove the event from the list *before* processing
      eventList.removeFirst();

      if (eventList.isEmpty()) {
        eventNextEvent = EVENT_NO_EVENTS;
      } else {
        eventNextEvent = eventList.getFirst().tstates;
      }

      if (descriptor.fn != null) {
        descriptor.fn.apply(ptr.tstates, ptr.type, ptr.userData);
      }

      if (eventFree != null) {
        // No free in Java, GC handles
      } else {
        eventFree = ptr;
      }
    }

    return 0;
  }

  private void eventReduceTstates(Event ptr, long tstatesPerFrame) {
    ptr.tstates -= tstatesPerFrame;
  }

  // Called at end of frame to reduce T-state count of all entries
  public void eventFrame(long tstatesPerFrame) {
    for (Event event : eventList) {
      eventReduceTstates(event, tstatesPerFrame);
    }

    eventNextEvent = eventList.isEmpty() ? EVENT_NO_EVENTS : eventList.getFirst().tstates;
  }

  // Force all events between now and the next interrupt to happen
  public void eventForceEvents() {
    while (eventNextEvent < fuseMachineInfoSupplier.get().getTimings().tstatesPerFrame) { // Assume Machine.current
      z80Clock.setTstates(eventNextEvent);
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
    for (Event event : eventList) {
      setEventNull(event, type);
    }
  }

  // Remove all events of a specific type and user data from the stack
  public void eventRemoveTypeUserData(int type, Object userData) {
    Event template = new Event();
    template.type = type;
    template.userData = userData;
    for (Event event : eventList) {
      setEventNullWithUserData(event, template);
    }
  }

  // Clear the event stack
  public void reset() {
    eventList.clear();

    eventNextEvent = EVENT_NO_EVENTS;

    eventFree = null; // GC
  }

  // A textual representation of each event type
  public String eventName(int type) {
    return registeredEvents.get(type).description;
  }

  void registeredEventsFree() {
    if (registeredEvents == null) return;

    registeredEvents.clear();
    registeredEvents = null;
  }

  // Tidy-up function called at end of emulation

}
