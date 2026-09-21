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


package com.fpetrola.oozx.speccy.tools.io;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortsTest extends MachineTest {

  private static final int START = 0x8000;

  /** LD A,7 / OUT (0xFE),A / IN A,(0xFE) / JP 8000: the border, written and then read back. */
  private Speccy touchingThePort() {
    Speccy speccy = silentMachine();
    int[] program = {0x3e, 0x07, 0xd3, 0xfe, 0xdb, 0xfe, 0xc3, 0x00, 0x80};
    for (int i = 0; i < program.length; i++) {
      speccy.memory.poke(START + i, (byte) program[i]);
    }
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
    return speccy;
  }

  private static void steps(Speccy speccy, int many) {
    for (int i = 0; i < many; i++) {
      speccy.cpu.step();
    }
  }

  private static int accumulator(Speccy speccy) {
    return speccy.cpu.getOoz80().getState().getRegister(RegisterName.A).read();
  }

  @Test
  void everyPortTheMachineTouchedIsCounted() {
    Speccy speccy = touchingThePort();
    Ports ports = new Ports(speccy);

    steps(speccy, 4);

    List<Ports.Port> touched = ports.touched();
    assertEquals(1, touched.size(), "one port was touched");
    Ports.Port border = touched.get(0);
    assertEquals(0x07fe, border.port(), "OUT (n),A puts A on the top half of the address");
    assertEquals(1, border.writes());
    assertEquals(1, border.reads());
    assertEquals(0x07, border.lastWritten(), "the border colour it wrote");
  }

  @Test
  void itSaysWhichCodeTouchedThePort() {
    Speccy speccy = touchingThePort();
    Ports ports = new Ports(speccy);

    steps(speccy, 4);

    assertTrue(ports.touched().get(0).from().containsKey(START + 2)
            || ports.touched().get(0).from().containsKey(START + 4),
        "the OUT or the IN is where it was touched from");
  }

  /**
   * The point of the whole design: it is wired to every port but drives nothing, so whatever
   * really answers a port still wins the bus. A machine watched reads exactly what it read
   * unwatched, which is what makes this an instrument rather than a device in the way.
   */
  @Test
  void aWatchedMachineReadsWhatAnUnwatchedOneReads() {
    Speccy unwatched = touchingThePort();
    steps(unwatched, 4);

    Speccy watched = touchingThePort();
    new Ports(watched);
    steps(watched, 4);

    assertEquals(accumulator(unwatched), accumulator(watched),
        "listening to the port changed what the machine read from it");
  }

  @Test
  void lettingGoLeavesTheMachineAsItWas() {
    Speccy speccy = touchingThePort();
    Ports ports = new Ports(speccy);
    steps(speccy, 4);
    long touched = ports.touched().get(0).reads() + ports.touched().get(0).writes();

    ports.close();
    steps(speccy, 8);

    assertEquals(touched, ports.touched().get(0).reads() + ports.touched().get(0).writes(),
        "it went on counting after it unplugged");
  }
}
