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
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus3E;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Per-machine ULA contention and floating-bus timing, collapsed to one checksum each: every
 * T-state's delay/bus value weighted by position and summed over 80000 T-states, wrapped to
 * 32 bits. Agreement to the checksum implies agreement to the individual T-state.
 */
class UlaTimingsOverAFrameTest extends MachineTest {
  private static final int ULA_CONTENTION_SIZE = 80000;

  private final Speccy speccy = silentMachine();

  /** Rows: model, late-timings flag, expected contention checksum, expected bus checksum. */
  static Stream<Arguments> machines() {
    return Stream.of(
        Arguments.of(Spec48.class, false, 2308862976L, 3427723200L),
        Arguments.of(Spec48.class, true, 2308927488L, 3426156480L),
        Arguments.of(Spec48Ntsc.class, false, 1962046464L, 3260475328L),
        Arguments.of(Spec48Ntsc.class, true, 1962110976L, 3258908608L),
        Arguments.of(Spec128.class, false, 2335183872L, 2854561728L),
        Arguments.of(Spec128.class, true, 2335248384L, 2852995008L),
        Arguments.of(SpecPlus2.class, false, 2335183872L, 2854561728L),
        Arguments.of(SpecPlus2.class, true, 2335248384L, 2852995008L),
        Arguments.of(SpecPlus2A.class, false, 3113754624L, 4261381056L),
        Arguments.of(SpecPlus2A.class, true, 3113840640L, 4261381056L),
        Arguments.of(SpecPlus3.class, false, 3113754624L, 4261381056L),
        Arguments.of(SpecPlus3.class, true, 3113840640L, 4261381056L),
        Arguments.of(SpecPlus3E.class, false, 3113754624L, 4261381056L),
        Arguments.of(SpecPlus3E.class, true, 3113840640L, 4261381056L),
        Arguments.of(Pentagon.class, false, 0L, 4261381056L),
        Arguments.of(Pentagon.class, true, 0L, 4261381056L));
  }

  private Spectrum on(Class<? extends Spectrum> model, boolean lateTimings) {
    speccy.machine.select(speccy.machine.model(model));
    speccy.machine.unit.lateTimings = lateTimings;
    speccy.machine.reset(true);
    return speccy.machine.current;
  }

  @ParameterizedTest
  @MethodSource("machines")
  void theDelayAtEveryTStateMatchesTheReference(Class<? extends Spectrum> model, boolean lateTimings, long contention, long floatingBus) {
    on(model, lateTimings);
    long sum = 0;
    for (int t = 0; t < ULA_CONTENTION_SIZE; t++) {
      sum += speccy.ula.contention.delay[t] * (t + 1L);
    }
    assertEquals(contention, sum & 0xffffffffL);
  }

  /** Fills the screen with a recognisable byte pattern before sampling the unattached bus. */
  @ParameterizedTest
  @MethodSource("machines")
  void whatFloatsOnTheBusAtEveryTStateMatchesTheReference(Class<? extends Spectrum> model, boolean lateTimings, long contention, long floatingBus) {
    Spectrum machine = on(model, lateTimings);
    byte[] screen = speccy.banks.shown().bytes;
    for (int offset = 0; offset < 8192; offset++) screen[offset] = (byte) offset;
    long sum = 0;
    for (int t = 0; t < ULA_CONTENTION_SIZE; t++) {
      speccy.zxClock.setTStates(t);
      sum += (machine.unattachedPort(0xff) & 0xff) * (t + 1L);
    }
    assertEquals(floatingBus, sum & 0xffffffffL);
  }

  /** All models except the 48Ks, i.e. those with any memory paging port. */
  static Stream<Class<? extends Spectrum>> machinesWithAPagingPort() {
    return Stream.of(Spec128.class, SpecPlus2.class, SpecPlus2A.class, SpecPlus3.class,
        SpecPlus3E.class, Pentagon.class);
  }

  /** The two Sinclair 128-family models, which contend I/O cycles as well as fetches. */
  static Stream<Class<? extends Spectrum>> machinesThatHoldUpAPort() {
    return Stream.of(Spec128.class, SpecPlus2.class);
  }

  /** Amstrad models plus the Pentagon, none of which ever contend an I/O cycle. */
  static Stream<Class<? extends Spectrum>> machinesThatHoldUpNoPort() {
    return Stream.of(SpecPlus2A.class, SpecPlus3.class, SpecPlus3E.class, Pentagon.class);
  }

  /** Finds the earliest T-state in the frame with a nonzero fetch-contention delay. */
  private int firstContendedTState() {
    for (int t = 0; t < speccy.machine.current.getTimings().tstatesPerFrame(); t++) {
      if (speccy.ula.contention.delay[t] != 0) return t;
    }
    return 0;
  }

  /** T-states one OUT instruction takes from a given start point; runs from uncontended page 2
   * so any difference in cost comes from port contention alone, not memory access. */
  private long costOfOut(int highByte, int at) {
    speccy.memory.poke(0x8000, (byte) 0xD3); // opcode for OUT (n),A
    speccy.memory.poke(0x8001, (byte) 0xFD);
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.A).write(highByte);
    speccy.zxClock.setTStates(at);
    speccy.cpu.jump(0x8000);
    long before = speccy.zxClock.getTStates();
    speccy.cpu.step();
    long cost = speccy.zxClock.getTStates() - before;
    // Undoes any paging side effect from the OUT, or the next 0x8000 fetch hits contended RAM.
    speccy.machine.current.paging().reset();
    speccy.machine.current.memoryMap();
    return cost;
  }

  /** True if any T-state within one picture line costs more than the same OUT off-picture. */
  private boolean everHeldUp(int highByte) {
    long quiet = costOfOut(highByte, 0);
    int from = firstContendedTState();
    for (int t = from; t < from + speccy.machine.current.getTimings().tstatesPerLine(); t++) {
      if (costOfOut(highByte, t) > quiet) return true;
    }
    return false;
  }

  /**
   * Port contention keys only on whether the address resembles a shared-page memory address,
   * never on what the port actually does; 0x1ffd resembles a ROM address (bottom 16K), so it is
   * never held up on any model.
   */
  @ParameterizedTest
  @MethodSource("machinesWithAPagingPort")
  void thePlus3sPagingPortIsNeverHeldUp(Class<? extends Spectrum> model) {
    on(model, false);
    assertEquals(0, speccy.ula.contention.delay[0], "the top of the frame should be off the picture");
    assertFalse(everHeldUp(0x1f), "0x1ffd is in the bottom sixteen K and should never be held up");
  }

  /** By contrast 0x7ffd resembles a page-5 address, so it is held up on 128-family machines. */
  @ParameterizedTest
  @MethodSource("machinesThatHoldUpAPort")
  void the128sPagingPortIsHeldUpBecauseItsAddressLooksLikePageFive(Class<? extends Spectrum> model) {
    on(model, false);
    assertTrue(everHeldUp(0x7f), "0x7ffd was never held up anywhere in a line of the picture");
  }

  /**
   * Amstrad models' no-MREQ contention table is entirely empty, so no port address is ever
   * held up regardless of resemblance to memory; the Pentagon likewise contends nothing.
   */
  @ParameterizedTest
  @MethodSource("machinesThatHoldUpNoPort")
  void theAmstradMachinesHoldUpNoPortAtAll(Class<? extends Spectrum> model) {
    on(model, false);
    assertFalse(everHeldUp(0x7f), "0x7ffd was held up on a machine that contends no I/O");

    int frame = speccy.machine.current.getTimings().tstatesPerFrame();
    for (int t = 0; t < frame; t++) {
      assertEquals(0, speccy.ula.contention.delayNoMreq[t], "a cycle with no MREQ was held up at " + t);
    }
  }
}
