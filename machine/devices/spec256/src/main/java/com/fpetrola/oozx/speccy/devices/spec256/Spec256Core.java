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
  private final Rules rules;

  @Inject
  public Spec256Core(Planes planes, Alignment alignment, Rules rules) {
    this.planes = planes;
    this.alignment = alignment;
    this.rules = rules;
    alignment.aPictureIsThere(address -> rules.readingWhereTheMachineReads && !planes.noColoursOfItsOwn(address));
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

  /** The machine's memory, with the planes told where it writes: that is where what they write goes. */
  public Memory wrapping(Memory memory) {
    return new Memory() {
      public int read(int address, int fetching) {
        return memory.read(address, fetching);
      }

      public void write(int address, int value) {
        memory.write(address, value);
        planes.machineWroteAt(address);
      }

      public int peek(int address) {
        return memory.peek(address);
      }

      public void poke(int address, int value) {
        memory.poke(address, value);
      }

      public void reset() {
        memory.reset();
      }
    };
  }

  public OOZ80 cpu(State state, PhaseProcessor contention) {
    OOZ80[] followers = new OOZ80[Planes.PLANES];
    for (int plane = 0; plane < followers.length; plane++) {
      State own = new State(DEAF, ordinary.bank(null, DEAF), planes.plane(plane, state.getMemory()));
      followers[plane] = ordinary.cpu(own, null, new LevelledInstructions(own, rules, state, alignment));
    }
    return new LockstepZ80(ordinary.cpu(state, contention), followers, alignment, planes);
  }

}
