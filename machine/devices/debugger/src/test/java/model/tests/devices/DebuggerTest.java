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

package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.debugger.MachineDebugger;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebuggerTest extends MachineTest {

  private static final int START = 0x8000;
  /** An infinite loop in RAM, so the machine keeps running for however long the test lets it. */
  private static final int[] LOOP = {0x3e, 0x2a, 0x00, 0xc3, 0x00, 0x80};

  private Speccy loaded() {
    Speccy speccy = silentMachine();
    for (int i = 0; i < LOOP.length; i++) {
      speccy.memory.poke(START + i, (byte) LOOP[i]);
    }
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
    return speccy;
  }

  /** A loop that calls a small subroutine (RET at 0x8010) once per iteration. */
  private Speccy calling() {
    Speccy speccy = silentMachine();
    int[] program = {0xcd, 0x10, 0x80, 0x00, 0xc3, 0x00, 0x80};
    for (int i = 0; i < program.length; i++) {
      speccy.memory.poke(START + i, (byte) program[i]);
    }
    speccy.memory.poke(START + 0x10, (byte) 0xc9);
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.SP).write(0xff00);
    return speccy;
  }

  /** Single-steps up to the given count, or until the debugger reports paused. */
  private boolean ranInto(Speccy speccy, MachineDebugger debugger, int instructions) {
    for (int i = 0; i < instructions; i++) {
      if (debugger.paused()) {
        return true;
      }
      speccy.cpu.step();
    }
    return debugger.paused();
  }

  @Test
  void readsTheMemoryOfTheMachineItIsGiven() {
    Speccy speccy = loaded();
    MachineDebugger debugger = new MachineDebugger(speccy);

    assertEquals(0x3e, debugger.memory(START));
    assertEquals(0x2a, debugger.memory(START + 1));
    assertEquals(START, debugger.register(RegisterName.PC));
  }

  @Test
  void aStepMovesTheMachineAndIsWrittenDownWhereItHappened() {
    Speccy speccy = loaded();
    MachineDebugger debugger = new MachineDebugger(speccy);

    debugger.step();

    assertEquals(0x2a, debugger.register(RegisterName.A));
    assertEquals(START + 2, debugger.register(RegisterName.PC));
  }

  @Test
  void aBreakpointStopsTheMachineBeforeTheInstructionItIsOn() {
    Speccy speccy = loaded();
    MachineDebugger debugger = new MachineDebugger(speccy);
    debugger.breakAt(START + 3);

    assertTrue(ranInto(speccy, debugger, 10), "the machine never reached the breakpoint");
    assertEquals(START + 3, debugger.register(RegisterName.PC), "it ran the instruction it was told to stop at");
  }

  @Test
  void readsTheMemoryAsInstructions() {
    MachineDebugger debugger = new MachineDebugger(loaded());

    assertEquals(List.of(
            new MachineDebugger.Line(START, "3E 2A", "LD A, 0x2A"),
            new MachineDebugger.Line(START + 2, "00", "NOP"),
            new MachineDebugger.Line(START + 3, "C3 00 80", "JP 0x8000")),
        debugger.listingFrom(START, 3));
  }

  /** The 48K ROM's opening bytes, identical across every genuine copy of it. */
  @Test
  void readsInstructionsNobodyHasRunYet() {
    MachineDebugger debugger = new MachineDebugger(loaded());

    assertEquals(List.of(
            new MachineDebugger.Line(0x0000, "F3", "DI"),
            new MachineDebugger.Line(0x0001, "AF", "XOR A, A"),
            new MachineDebugger.Line(0x0002, "11 FF FF", "LD DE, 0xFFFF"),
            new MachineDebugger.Line(0x0005, "C3 CB 11", "JP 0x11CB"),
            new MachineDebugger.Line(0x0008, "2A 5D 5C", "LD HL, (0x5C5D)"),
            new MachineDebugger.Line(0x000B, "22 5F 5C", "LD (0x5C5F), HL")),
        debugger.listingFrom(0x0000, 6));
  }

  /** Disassembles the ROM's key-wait loop, which uses an indexed (register+offset) operand. */
  @Test
  void readsAnIndexedOperand() {
    MachineDebugger debugger = new MachineDebugger(loaded());

    assertEquals(List.of(
            new MachineDebugger.Line(0x10B8, "FD CB 01 AE", "RES 5, (IY+01)"),
            new MachineDebugger.Line(0x10BC, "F5", "PUSH AF"),
            new MachineDebugger.Line(0x10BD, "FD CB 02 6E", "BIT 5, (IY+02)")),
        debugger.listingFrom(0x10B8, 3));
  }

  /** Reading the disassembly listing must not advance the machine's clock. */
  @Test
  void readingInstructionsDoesNotMoveTheMachine() {
    Speccy speccy = loaded();
    MachineDebugger debugger = new MachineDebugger(speccy);
    long tstates = speccy.zxClock.getTStates();

    debugger.listingFrom(0x0000, 64);

    assertEquals(tstates, speccy.zxClock.getTStates());
    assertEquals(START, debugger.register(RegisterName.PC));
  }

  @Test
  void aClearedBreakpointStopsNothing() {
    Speccy speccy = loaded();
    MachineDebugger debugger = new MachineDebugger(speccy);
    debugger.breakAt(START + 3);
    debugger.clearBreak(START + 3);

    assertTrue(debugger.breakpoints().isEmpty());
    assertFalse(ranInto(speccy, debugger, 40));
  }

  @Test
  void closingLeavesTheMachineRunningAndWatchingNothing() {
    Speccy speccy = loaded();
    MachineDebugger debugger = new MachineDebugger(speccy);
    debugger.breakAt(START + 3);
    assertTrue(ranInto(speccy, debugger, 10));

    debugger.close();

    assertFalse(debugger.paused());
    assertTrue(debugger.breakpoints().isEmpty());
    assertFalse(ranInto(speccy, debugger, 40));
  }

  @Test
  void steppingOverACallComesBackAfterIt() {
    Speccy speccy = calling();
    MachineDebugger debugger = new MachineDebugger(speccy);

    debugger.stepOver();

    assertTrue(ranInto(speccy, debugger, 20), "it never came back from the call");
    assertEquals(START + 3, debugger.register(RegisterName.PC));
  }

  @Test
  void steppingOutOfARoutineComesBackToWhoCalledIt() {
    Speccy speccy = calling();
    MachineDebugger debugger = new MachineDebugger(speccy);
    debugger.step();
    assertEquals(START + 0x10, debugger.register(RegisterName.PC), "the step did not go into the call");

    debugger.stepOut();

    assertTrue(ranInto(speccy, debugger, 20), "it never returned");
    assertEquals(START + 3, debugger.register(RegisterName.PC));
  }

  @Test
  void everyPlaceACallWentIsARoutine() {
    Speccy speccy = calling();
    MachineDebugger debugger = new MachineDebugger(speccy);

    for (int i = 0; i < 12; i++) {
      speccy.cpu.step();
    }

    assertEquals(Map.of(START + 0x10, 3), debugger.routines(), "three laps of the loop, three calls");
  }

  /** Regression: the previous debugger kept breakpoints in static fields, shared across machines. */
  @Test
  void oneMachinesBreakpointsAreNotAnothers() {
    Speccy one = loaded();
    Speccy other = loaded();
    MachineDebugger debugger = new MachineDebugger(one);
    MachineDebugger another = new MachineDebugger(other);

    debugger.breakAt(START + 3);

    assertTrue(debugger.isBreakpoint(START + 3));
    assertFalse(another.isBreakpoint(START + 3));
    assertFalse(ranInto(other, another, 40));
  }
}
