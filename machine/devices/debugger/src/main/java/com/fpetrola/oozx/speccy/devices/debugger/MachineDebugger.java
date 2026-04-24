/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.devices.debugger;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.z80.base.ToStringInstructionVisitor;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.ReadOnlyIOImplementation;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.RegisterName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * What a debugger asks of a machine: where it is, what it holds, and permission to move.
 * <p>
 * Everything here is one machine's. Two windows on two machines are two of these, which is why
 * nothing in it is static - the debugger this replaces kept its registers, its breakpoints and
 * its tables in static fields, so a second machine would have been the first one's.
 */
public class MachineDebugger {

  /** An address, the bytes written there, and what they read as. */
  public record Line(int address, String bytes, String instruction) {
  }

  private final Speccy machine;
  private final Map<Integer, PcTraps.Watch> breakpoints = new LinkedHashMap<>();
  private final Map<Integer, Integer> routines = new ConcurrentSkipListMap<>();
  private final PcTraps.Watch calls;
  private PcTraps.Watch until;
  private Runnable onStop = () -> { };

  /**
   * The machine's memory read as instructions.
   * <p>
   * Its own state and its own registers, and a memory that answers with the machine's bytes
   * without the clock: what the processor is fetching cannot be asked for it - the generated
   * core has no instruction objects left in it - and reading through the machine's own memory
   * would charge it the T-states of every read the window makes.
   */
  private final State reading;
  private final DefaultInstructionFetcher decoder;

  public MachineDebugger(Speccy machine) {
    this.machine = machine;
    reading = new State(new ReadOnlyIOImplementation(null), new Memory() {
      public int read(int address, int fetching) {
        return machine.memory.peek(address & 0xffff) & 0xff;
      }

      public void write(int address, int value) {
      }

      public void reset() {
      }
    });
    decoder = new DefaultInstructionFetcher(reading, false, false);
    calls = machine.cpu.beforeFetch().watch(0x0000, 0xffff, pc -> {
      int target = callTarget(pc);
      if (target >= 0) {
        routines.merge(target, 1, Integer::sum);
      }
    });
  }

  /**
   * Where every CALL and RST the machine has reached points at, and how many times, lowest first.
   * <p>
   * A conditional call counts even when the condition was false: the routine is written there
   * either way. This is why one watch covers the whole address space while a debugger is open -
   * it costs a fetched instruction a read of its own opcode, which is what a machine being
   * debugged can afford and one running a game cannot.
   */
  public Map<Integer, Integer> routines() {
    return Map.copyOf(routines);
  }

  /**
   * Where a CALL or an RST at that address goes, or -1 when what is written there is neither.
   * Read out of the opcode instead of decoded: this runs for every instruction the machine fetches.
   */
  private int callTarget(int address) {
    int opcode = memory(address);
    if (opcode == 0xcd || (opcode & 0xc7) == 0xc4) {
      return memory(address + 1) | memory(address + 2) << 8;
    }
    return (opcode & 0xc7) == 0xc7 ? opcode & 0x38 : -1;
  }

  /** Told when the machine stops by itself, which is when a breakpoint is reached. */
  public void onStop(Runnable listener) {
    this.onStop = listener;
  }

  public boolean paused() {
    return machine.loop.isPaused();
  }

  public void pause() {
    machine.loop.setPaused(true);
  }

  public void run() {
    machine.loop.setPaused(false);
  }

  /**
   * One instruction, with what it was written down.
   * <p>
   * Runs on whatever thread asks, which is the window's: a machine only steps while it is paused,
   * and a paused machine's own thread is spinning on {@link com.fpetrola.oozx.speccy.modules.z80.Z80#isPaused()}
   * without touching anything.
   */
  public void step() {
    pause();
    machine.cpu.step();
  }

  /** One instruction, over whatever it calls: the machine goes and comes back. */
  public void stepOver() {
    int pc = register(RegisterName.PC);
    if (callTarget(pc) < 0) {
      step();
    } else {
      runTo(pc + at(pc).getLength() & 0xffff);
    }
  }

  /**
   * Out of the routine the machine is in, by the address on top of its stack. Which is where a
   * RET would go: a routine that has pushed something since it started goes somewhere else, and
   * this stops there instead.
   */
  public void stepOut() {
    int sp = register(RegisterName.SP);
    runTo(memory(sp) | memory(sp + 1) << 8);
  }

  /** Lets the machine go and stops it at that address, leaving no breakpoint behind. */
  public void runTo(int address) {
    forgetWhereItWasGoing();
    until = machine.cpu.beforeFetch().watch(address, pc -> {
      machine.cpu.stopHere();
      forgetWhereItWasGoing();
      onStop.run();
    });
    run();
  }

  private void forgetWhereItWasGoing() {
    if (until != null) {
      until.off();
      until = null;
    }
  }

  /** What is written at that address, and how many bytes of it there are. */
  private Instruction at(int address) {
    reading.getPc().write(address & 0xffff);
    return decoder.fetchNextInstruction();
  }

  public String instructionAt(int address) {
    return new ToStringInstructionVisitor().createToString(at(address));
  }

  private String bytesAt(int address, int length) {
    StringBuilder bytes = new StringBuilder();
    for (int i = 0; i < length; i++) {
      bytes.append(i == 0 ? "" : " ").append("%02X".formatted(memory(address + i)));
    }
    return bytes.toString();
  }

  /** The instructions from an address on, each one after the bytes of the one before. */
  public List<Line> listingFrom(int address, int lines) {
    List<Line> listing = new ArrayList<>();
    for (int at = address & 0xffff; listing.size() < lines; ) {
      Instruction instruction = at(at);
      int length = Math.max(1, instruction.getLength());
      listing.add(new Line(at, bytesAt(at, length), new ToStringInstructionVisitor().createToString(instruction)));
      at = at + length & 0xffff;
    }
    return listing;
  }

  public int register(RegisterName name) {
    return machine.cpu.getOoz80().getState().getRegister(name).read();
  }

  /** A bit of F, by the Z80's own numbering: 0 C, 1 N, 2 P/V, 4 H, 6 Z, 7 S. */
  public boolean flag(int bit) {
    return (register(RegisterName.F) & (1 << bit)) != 0;
  }

  public int memory(int address) {
    return machine.memory.peek(address & 0xffff) & 0xff;
  }

  public Set<Integer> breakpoints() {
    return Set.copyOf(breakpoints.keySet());
  }

  public boolean isBreakpoint(int address) {
    return breakpoints.containsKey(address);
  }

  /**
   * Stops the machine the next time it fetches at that address.
   * <p>
   * One watch per breakpoint rather than one over the whole address space: the traps keep a bit
   * per address, so a machine with three breakpoints costs an instruction one test of one bit,
   * and a machine with none costs it one test of one boolean.
   */
  public void breakAt(int address) {
    if (breakpoints.containsKey(address)) {
      return;
    }
    breakpoints.put(address, machine.cpu.beforeFetch().watch(address, pc -> {
      machine.cpu.stopHere();
      onStop.run();
    }));
  }

  public void clearBreak(int address) {
    PcTraps.Watch watch = breakpoints.remove(address);
    if (watch != null) {
      watch.off();
    }
  }

  /** The debugger is done with this machine: it leaves it running and watching nothing. */
  public void close() {
    breakpoints.values().forEach(PcTraps.Watch::off);
    breakpoints.clear();
    forgetWhereItWasGoing();
    calls.off();
    run();
  }
}
