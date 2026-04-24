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
 * Per-machine debugger state: position, memory, breakpoints, and stepping. All instance fields,
 * not static, so two machines debugged at once do not share breakpoints or registers.
 */
public class MachineDebugger {

  public record Line(int address, String bytes, String instruction) {
  }

  private final Speccy machine;
  private final Map<Integer, PcTraps.Watch> breakpoints = new LinkedHashMap<>();
  private final Map<Integer, Integer> routines = new ConcurrentSkipListMap<>();
  private final PcTraps.Watch calls;
  private PcTraps.Watch until;
  private Runnable onStop = () -> { };

  /** A separate decoding-only CPU state: the real one has no surviving Instruction objects to
   * inspect, and decoding through it would bill T-states for every debugger read. */
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
   * Call counts per target address, including untaken conditional calls (the routine address
   * is fixed regardless). Costs an opcode read per fetch, acceptable only while debugging.
   */
  public Map<Integer, Integer> routines() {
    return Map.copyOf(routines);
  }

  /** Decodes just enough of the raw opcode byte to find a CALL/RST target, or -1; kept cheap
   * since it runs on every fetch. */
  private int callTarget(int address) {
    int opcode = memory(address);
    if (opcode == 0xcd || (opcode & 0xc7) == 0xc4) {
      return memory(address + 1) | memory(address + 2) << 8;
    }
    return (opcode & 0xc7) == 0xc7 ? opcode & 0x38 : -1;
  }

  /** Registers a callback fired whenever a breakpoint halts the machine. */
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

  /** Executes on the calling (UI) thread: safe because the machine's own thread just spins on
   * {@link com.fpetrola.oozx.speccy.modules.z80.Z80#isPaused()} while paused. */
  public void step() {
    pause();
    machine.cpu.step();
  }

  /** Steps past a CALL/RST as one unit by running to the instruction right after it. */
  public void stepOver() {
    int pc = register(RegisterName.PC);
    if (callTarget(pc) < 0) {
      step();
    } else {
      runTo(pc + at(pc).getLength() & 0xffff);
    }
  }

  /** Runs to the address on top of the stack - where a RET would return to - though an
   * intervening push means that is not necessarily the caller. */
  public void stepOut() {
    int sp = register(RegisterName.SP);
    runTo(memory(sp) | memory(sp + 1) << 8);
  }

  /** Resumes execution and halts at the given address without adding a persistent breakpoint. */
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

  /** Decodes the instruction at an address using the read-only debugger memory view. */
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

  /** Decodes a run of consecutive instructions starting at an address. */
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

  /** F-register bit positions: 0=C, 1=N, 2=P/V, 4=H, 6=Z, 7=S. */
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

  /** Adds a fetch-time breakpoint; one watch per address costs a single bit-test per fetch,
   * regardless of how many breakpoints exist. */
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

  /** Detaches from the machine entirely: clears breakpoints, watches, and resumes it. */
  public void close() {
    breakpoints.values().forEach(PcTraps.Watch::off);
    breakpoints.clear();
    forgetWhereItWasGoing();
    calls.off();
    run();
  }
}
