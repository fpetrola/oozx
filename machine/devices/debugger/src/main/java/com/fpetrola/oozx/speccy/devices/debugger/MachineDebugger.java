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
import com.fpetrola.oozx.speccy.modules.z80.Disassembly;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
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

  private final Speccy machine;
  private final Map<Integer, PcTraps.Watch> breakpoints = new LinkedHashMap<>();
  private final Map<Integer, Integer> routines = new ConcurrentSkipListMap<>();
  private final PcTraps.Watch calls;
  private PcTraps.Watch until;
  private Runnable onStop = () -> { };

  private final Disassembly listing;

  public MachineDebugger(Speccy machine) {
    this.machine = machine;
    listing = new Disassembly(machine);
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
      runTo(pc + listing.lengthAt(pc) & 0xffff);
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

  public String instructionAt(int address) {
    return listing.instructionAt(address);
  }

  public List<Disassembly.Line> listingFrom(int address, int lines) {
    return listing.from(address, lines);
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
