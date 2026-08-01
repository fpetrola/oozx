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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port facts that require a whole machine, not just the bus: timing of the bus query within
 * the instruction cycle, and contention on unanswered ports. Bus-only facts live in core's
 * SpectrumPortsTest.
 */
class PortsOnTheMachineTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  /** Records the T-state of each read/write reaching this port. */
  private static class Probe extends AbstractPeripheral {
    final List<String> reached = new ArrayList<>();

    Probe(Speccy speccy, int mask, int value) {
      super(List.of());
      ports(Wired.at(mask, value, new DefaultPortHandler(true, true) {
        public BusAnswer read(int port) {
          reached.add("read at " + speccy.zxClock.getTStates());
          return BusAnswer.of(0xbf);
        }

        public void write(int port, byte value) {
          reached.add("write at " + speccy.zxClock.getTStates());
        }
      }));
    }

    public boolean fitsOn(SpectrumMachine machine) {
      return true;
    }
  }

  private Spectrum on(Class<? extends Spectrum> model) {
    speccy.machine.select(speccy.machine.model(model));
    return speccy.machine.current;
  }

  private Probe plug(int mask, int value) {
    Probe probe = new Probe(speccy, mask, value);
    speccy.peripheralRegistry.register(probe);
    speccy.peripheralRegistry.update();
    return probe;
  }

  /** Runs one instruction from uncontended page 2, starting at a given T-state; returns its cost. */
  private long costOf(int opcode, int operand, int highByte, int at) {
    speccy.memory.poke(0x8000, (byte) opcode);
    speccy.memory.poke(0x8001, (byte) operand);
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.A).write(highByte);
    speccy.zxClock.setTStates(at);
    speccy.cpu.jump(0x8000);
    long before = speccy.zxClock.getTStates();
    speccy.cpu.step();
    return speccy.zxClock.getTStates() - before;
  }

  private static final int IN_A_N = 0xDB;
  private static final int OUT_N_A = 0xD3;

  /**
   * IN/OUT each have two built-in wait states (address then data settling); off-picture,
   * nothing else contends, so an IN samples at fetch+7+3 T-states and OUT writes at fetch+7+1,
   * both costing 11 T-states total.
   */
  @Test
  void anInWaitsTwiceBeforeTakingTheValueAndAnOutPutsItInBetween() {
    on(Spec48.class);
    Probe probe = plug(0xffff, 0x80ff);

    assertEquals(11, costOf(IN_A_N, 0xff, 0x80, 1000));
    assertEquals(List.of("read at " + (1000 + 7 + 3)), probe.reached);

    probe.reached.clear();
    assertEquals(11, costOf(OUT_N_A, 0xff, 0x80, 1000));
    assertEquals(List.of("write at " + (1000 + 7 + 1)), probe.reached);
  }

  /**
   * Contention applies before the bus is even asked which peripheral answers, so an unanswered
   * port on a 128 (address resembling page 5) still costs more within the picture than outside
   * it, purely from the ULA's I/O contention.
   */
  @Test
  void aPortNobodyAnswersIsHeldUpLikeAnyOther() {
    on(Spec128.class);
    int overThePicture = (int) speccy.machine.current.lineStart(speccy.display.BORDER_HEIGHT) + 32;

    long quietIn = costOf(IN_A_N, 0xff, 0x7f, 1000);
    long quietOut = costOf(OUT_N_A, 0xff, 0x7f, 1000);
    long heldIn = 0, heldOut = 0;
    for (int t = overThePicture; t < overThePicture + 8; t++) {
      heldIn = Math.max(heldIn, costOf(IN_A_N, 0xff, 0x7f, t));
      heldOut = Math.max(heldOut, costOf(OUT_N_A, 0xff, 0x7f, t));
    }

    assertEquals(11, quietIn);
    assertTrue(heldIn > quietIn, "an IN to 0x7fff was never held up over the picture");
    assertTrue(heldOut > quietOut, "nor an OUT");
  }

  /**
   * An IN samples the bus only after both wait states complete; an unanswered port over the
   * picture on a 48K therefore reads whatever the ULA fetched at that later moment.
   */
  @Test
  void anInTakesWhatIsOnTheBusAfterBothWaits() {
    on(Spec48.class);
    for (int column = 0; column < 4; column++) {
      speccy.memory.poke(0x4000 + column, (byte) (0x10 | column));
      speccy.memory.poke(0x5800 + column, (byte) (0x20 | column));
    }
    int firstPixel = (int) speccy.machine.current.lineStart(speccy.display.BORDER_HEIGHT) + speccy.display.BORDER_WIDTH_COLS * 4;
    int start = firstPixel - 7 - 3 + 3;

    costOf(IN_A_N, 0xff, 0x80, start);

    int read = speccy.cpu.getOoz80().getState().getRegister(RegisterName.A).read() & 0xff;
    assertEquals(0x20, read, "the attribute of column 0, which the bus carries at the first pixel plus three");
  }
}
