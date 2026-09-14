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


package com.fpetrola.oozx.speccy.devices.spec256;

import com.fpetrola.z80.cpu.Core;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.OopCore;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.tstates.PhaseProcessor;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * One more implementation of the processor, which is the ordinary one nine times over: the
 * machine's, and eight on the eight planes of colour a Spec256 game brings.
 * <p>
 * Everything about how a processor is wired stays where it was, in the ordinary core; what is
 * added here is only that there are nine of them and what they take from each other.
 */
@Singleton
public class Spec256Core implements Core {
  /** What this implementation of the processor is called, where a machine is moved onto it. */
  public static final String NAME = "Spec256";

  /** A follower has no ports: what a port answers is never a colour, and what it would say is not its to say. */
  private static final IO DEAF = new IO() {
    public int in(int port) {
      return 0xff;
    }

    public void out(int port, int value) {
    }
  };

  private final OopCore ordinary = new OopCore();
  private final Planes planes;
  private final Alignment alignment;

  @Inject
  public Spec256Core(Planes planes, Alignment alignment) {
    this.planes = planes;
    this.alignment = alignment;
  }

  public String name() {
    return NAME;
  }

  public RegisterBank bank(Memory memory, IO io) {
    return ordinary.bank(memory, io);
  }

  public boolean countsItsOwnContention() {
    return ordinary.countsItsOwnContention();
  }

  public OOZ80 cpu(State state, PhaseProcessor contention) {
    OOZ80[] followers = new OOZ80[Planes.PLANES];
    for (int plane = 0; plane < followers.length; plane++) {
      State own = new State(DEAF, ordinary.bank(null, DEAF), planes.plane(plane, state.getMemory()));
      followers[plane] = ordinary.cpu(own, null);
    }
    return new LockstepZ80(ordinary.cpu(state, contention), followers, alignment);
  }
}
