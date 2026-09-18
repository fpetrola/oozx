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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.z80.cpu.Core;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Which implementation of the processor this machine runs on, and how one is built for it.
 * <p>
 * Two concepts, not one: a Z80 is what the machine has, and this is the choice of what plays it -
 * the set of implementations, the one in use, how the memory and the phase processor are wired for
 * each, and who is told when the machine moves to another. The Z80 keeps the processor it was
 * handed and nothing about where it came from.
 */
@Singleton
public class Processors {

  /**
   * The one of them a machine starts on: what the settings name, or whichever this build prefers.
   * Nested here rather than a file of its own because it says nothing except which of these is
   * meant, and this is the class they belong to.
   */
  @com.google.inject.BindingAnnotation
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  public @interface StartsOn {
  }

  private final Set<Core> available;
  private final SpectrumZ80Clock zxClock;
  private final IO io;
  private final ProcessorWiring wiring;

  /** Not final: a machine can be asked to run on another implementation of the processor. */
  private Core core;
  private final MachineLoop loop;
  private Cpu cpu;
  private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

  @Inject
  public Processors(SpectrumZ80Clock zxClock, IO io, @StartsOn Core core, Set<Core> available, MachineLoop loop, ProcessorWiring wiring) {
    this.zxClock = zxClock;
    this.io = io;
    this.core = core;
    this.available = available;
    this.loop = loop;
    this.wiring = wiring;
  }

  /**
   * Builds the processor this machine starts on. The Z80 is handed in from its constructor rather
   * than injected, so that the two do not have to be built at the same time.
   */
  public void startOn(Cpu cpu) {
    this.cpu = cpu;
    runOn(core);
  }

  /**
   * The implementations of the processor this build has, in a stable order. What a machine starts
   * on is decided before it is built; this is how it is changed afterwards.
   */
  public List<Core> all() {
    List<Core> all = new ArrayList<>();
    available.stream().sorted(Comparator.comparing(Core::name))
        .filter(candidate -> all.stream().noneMatch(kept -> kept.name().equals(candidate.name())))
        .forEach(all::add);
    if (all.stream().noneMatch(c -> c.name().equals(core.name())))
      all.add(0, core);
    return all;
  }

  public String current() {
    return core.name();
  }

  private Optional<Core> forName(String name) {
    return all().stream().filter(c -> c.name().equals(name)).findFirst();
  }

  /**
   * Runs this machine on another implementation of the processor, keeping everything it is: the
   * registers and the processor's own pins move across, and the memory, the ports and the clock
   * were never the processor's to begin with.
   * <p>
   * Asked for from a window and done between instructions, because the processor cannot be
   * replaced while that thread is inside one.
   */
  public void use(String name) {
    forName(name).filter(wanted -> wanted != core).ifPresent(wanted -> loop.later(() -> runOn(wanted)));
  }

  /** Told after the machine changes processor, for whoever kept hold of the one it had. */
  public void onChanged(Runnable listener) {
    listeners.add(listener);
  }

  /**
   * Builds the processor on the first implementation that can run here, this one first. One that
   * has to be generated and compiled discovers only here that it cannot be, which is why the
   * others follow it rather than the caller having to know which is which.
   */
  private void runOn(Core wanted) {
    State previous = cpu.getOoz80() == null ? null : cpu.getOoz80().getState();
    for (Core candidate : wantedFirst(wanted)) {
      core = candidate;
      try {
        cpu.setOoz80(wiring.build(candidate, this::createState));
      } catch (RuntimeException cannotRunHere) {
        System.out.printf("oozx: the %s processor cannot run here, so another one does: %s%n", candidate.name(), cannotRunHere);
        continue;
      }
      if (previous != null)
        cpu.getOoz80().getState().takeFrom(previous);
      listeners.forEach(Runnable::run);
      return;
    }
    throw new IllegalStateException("no processor this build has can run here");
  }

  private List<Core> wantedFirst(Core wanted) {
    List<Core> order = new ArrayList<>(all());
    order.remove(wanted);
    order.add(0, wanted);
    return order;
  }

  private State createState(Memory forTheCpu) {
    var state = new State(io, core.bank(forTheCpu, io), forTheCpu) {
      public void enableInterrupt() {
        super.enableInterrupt();
        cpu.interruptsEnabled(clock.getTStates());
      }
    };
    state.clock = zxClock;
    return state;
  }
}
