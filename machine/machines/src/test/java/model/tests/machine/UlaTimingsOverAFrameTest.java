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
 * The ULA's timing over a whole frame, as one number per machine: the delay at every T-state, and
 * the value read off the bus at every T-state, each weighted by its position and summed. The sums
 * are measured against a reference emulator and kept as measured: a machine that agrees with them
 * agrees to the T-state.
 * <p>
 * Over 80000 T-states, in 32 bits, because that is what the numbers were made with.
 */
class UlaTimingsOverAFrameTest extends MachineTest {
  private static final int ULA_CONTENTION_SIZE = 80000;

  private final Speccy speccy = silentMachine();

  /** (machine, late timings, contention_test's sum, floating_bus_test's sum). */
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

  /** The screen holds a known pattern and the bus is read with nothing attached, at every T-state of the frame. */
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

  /** Every machine with a paging port, which is every one but the 48Ks. */
  static Stream<Class<? extends Spectrum>> machinesWithAPagingPort() {
    return Stream.of(Spec128.class, SpecPlus2.class, SpecPlus2A.class, SpecPlus3.class,
        SpecPlus3E.class, Pentagon.class);
  }

  /** Sinclair's 128 and +2, whose ULA holds the processor up on an I/O cycle as well as on a fetch. */
  static Stream<Class<? extends Spectrum>> machinesThatHoldUpAPort() {
    return Stream.of(Spec128.class, SpecPlus2.class);
  }

  /** The Amstrad machines and the Pentagon, whose I/O cycles are never held up at all. */
  static Stream<Class<? extends Spectrum>> machinesThatHoldUpNoPort() {
    return Stream.of(SpecPlus2A.class, SpecPlus3.class, SpecPlus3E.class, Pentagon.class);
  }

  /** The first T-state of a frame at which the ULA holds the processor up on a fetch. */
  private int firstContendedTState() {
    for (int t = 0; t < speccy.machine.current.getTimings().tstatesPerFrame(); t++) {
      if (speccy.ula.contention.delay[t] != 0) return t;
    }
    return 0;
  }

  /**
   * What one OUT costs, started at that T-state. The instruction is two bytes at 0x8000, which is
   * page 2 on every one of these and never contended, so what the count can differ by is the
   * port access and nothing else. Nobody answers either port here - this module carries no memory
   * device - and it makes no difference: the ULA holds the processor up before the bus is asked.
   */
  private long costOfOut(int highByte, int at) {
    speccy.memory.poke(0x8000, (byte) 0xD3); // OUT (n),A
    speccy.memory.poke(0x8001, (byte) 0xFD);
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.A).write(highByte);
    speccy.zxClock.setTStates(at);
    speccy.cpu.jump(0x8000);
    long before = speccy.zxClock.getTStates();
    speccy.cpu.step();
    long cost = speccy.zxClock.getTStates() - before;
    // The OUT reached a real pager: put the map back, or the next fetch of 0x8000 is in contended RAM.
    speccy.machine.current.paging().reset();
    speccy.machine.current.memoryMap();
    return cost;
  }

  /** Whether an OUT to that port ever costs more inside a line of the picture than off it. */
  private boolean everHeldUp(int highByte) {
    long quiet = costOfOut(highByte, 0);
    int from = firstContendedTState();
    for (int t = from; t < from + speccy.machine.current.getTimings().tstatesPerLine(); t++) {
      if (costOfOut(highByte, t) > quiet) return true;
    }
    return false;
  }

  /**
   * A port is held up when its address looks like an address in a page the ULA shares, and that is
   * the whole rule: the ULA is not asked what the port is
   * for. So the +3's own paging port is never held up on any machine, because 0x1ffd looks like
   * an address in the bottom sixteen K, where a ROM sits. That is what the deleted +3 test was
   * the only one to say.
   */
  @ParameterizedTest
  @MethodSource("machinesWithAPagingPort")
  void thePlus3sPagingPortIsNeverHeldUp(Class<? extends Spectrum> model) {
    on(model, false);
    assertEquals(0, speccy.ula.contention.delay[0], "the top of the frame should be off the picture");
    assertFalse(everHeldUp(0x1f), "0x1ffd is in the bottom sixteen K and should never be held up");
  }

  /** And the 128's own is, because 0x7ffd looks like an address in page 5. */
  @ParameterizedTest
  @MethodSource("machinesThatHoldUpAPort")
  void the128sPagingPortIsHeldUpBecauseItsAddressLooksLikePageFive(Class<? extends Spectrum> model) {
    on(model, false);
    assertTrue(everHeldUp(0x7f), "0x7ffd was never held up anywhere in a line of the picture");
  }

  /**
   * The Amstrad machines hold nothing up on an I/O cycle, whatever the address looks like: what
   * the port contention reads is the no-MREQ table, and theirs is empty even where the table for
   * a fetch is not. The Pentagon holds nothing up anywhere.
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
