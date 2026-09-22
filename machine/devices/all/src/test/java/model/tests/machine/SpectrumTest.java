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

import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.Upd765Peripheral;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec16;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Tc2048;
import com.fpetrola.oozx.speccy.machine.Tc2068;
import com.fpetrola.oozx.speccy.machine.Ts2068;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus3E;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One fact about a Spectrum as a machine per test, asked of the real machines. The facts and
 * their order are those of the derivation in prototypes/tdd.
 */
class SpectrumTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  private Spectrum on(Class<? extends Spectrum> model) {
    speccy.machine.select(speccy.machine.model(model));
    return speccy.machine.current;
  }

  /** T-state of the picture's top-left pixel, one border-width into the first displayed line. */
  private int firstPixel() {
    return (int) speccy.machine.current.lineStart(speccy.display.BORDER_HEIGHT) + speccy.display.BORDER_WIDTH_COLS * 4;
  }

  private int floatingAt(int tState, int port) {
    speccy.zxClock.setTStates(tState);
    return speccy.machine.current.unattachedPort(port) & 0xff;
  }

  /** Marks each screen/attribute byte on the top line with its column, for reading the bus by eye. */
  private void markTheScreen() {
    for (int column = 0; column < 4; column++) {
      speccy.memory.poke(0x4000 + column, (byte) (0x10 | column));
      speccy.memory.poke(0x5800 + column, (byte) (0x20 | column));
    }
  }

  private static String waits(Spectrum machine, int from, int count) {
    StringBuilder text = new StringBuilder();
    for (int t = from; t < from + count; t++) text.append(machine.contendDelay(t)).append(' ');
    return text.toString().trim();
  }

  @Test
  void aFortyEightsFrameIs312LinesOf224TStatesAt3Point5MHz() {
    var timings = on(Spec48.class).getTimings();
    assertEquals(224, timings.tstatesPerLine());
    assertEquals(69888, timings.tstatesPerFrame());
    assertEquals(312, timings.tstatesPerFrame() / timings.tstatesPerLine());
    assertEquals(3_500_000, timings.processorSpeed());
  }

  /**
   * The first machine sold had a quarter of the memory and no way to tell the difference from
   * inside except by looking: above the sixteen K that is there, every address reads as 0xff and
   * a write to it is gone as soon as it is made.
   */
  @Test
  void aSixteenKHasNothingAboveItsSixteenK() {
    on(Spec16.class);
    for (int address : new int[]{0x8000, 0xbfff, 0xc000, 0xffff}) {
      speccy.memory.poke(address, (byte) 0x42);
      assertEquals(0xff, speccy.memory.peek(address) & 0xff, "there is no chip at " + Integer.toHexString(address));
    }
    on(Spec48.class);
    speccy.memory.poke(0x8000, (byte) 0x42);
    assertEquals(0x42, speccy.memory.peek(0x8000) & 0xff, "a 48K does have one there");
  }

  static Stream<Arguments> models() {
    return Stream.of(
        Arguments.of(Spec16.class, 224, 312, 3_500_000L),
        Arguments.of(Tc2048.class, 224, 312, 3_500_000L), Arguments.of(Tc2068.class, 224, 312, 3_500_000L), Arguments.of(Ts2068.class, 224, 262, 3_528_000L),
        Arguments.of(Spec128.class, 228, 311, 3_546_900L),
        Arguments.of(SpecPlus3.class, 228, 311, 3_546_900L),
        Arguments.of(Pentagon.class, 224, 320, 3_584_000L),
        // Two conflicting NTSC clock figures exist (3527500 vs 3579545); left unchecked pending a decision.
        Arguments.of(Spec48Ntsc.class, 224, 264, 0L));
  }

  /** Each model's own line/frame/clock figures, asserted independently of the 48K's. */
  @ParameterizedTest
  @MethodSource("models")
  void everyModelMeasuresItsOwnFrame(Class<? extends Spectrum> model, int line, int lines, long hz) {
    var timings = on(model).getTimings();
    assertEquals(line, timings.tstatesPerLine());
    assertEquals(line * lines, timings.tstatesPerFrame());
    if (hz != 0) assertEquals(hz, timings.processorSpeed());
  }

  /** On frame end, the clock and every pending scheduled task shift back by one frame length. */
  @Test
  void aFrameEndsAndTheClockGoesBackByItsLengthWithEverythingThatWasDue() {
    Spectrum machine = on(Spec48.class);
    List<Long> ranAt = new ArrayList<>();
    Task task = speccy.scheduler.register(new Task() {
      public void run(long due) {
        ranAt.add((long) speccy.zxClock.getTStates());
      }
    });
    speccy.zxClock.setTStates(69900);
    speccy.scheduler.schedule(task, 69950);
    machine.spectrumFrame();
    assertEquals(12, speccy.zxClock.getTStates());
    advance(speccy, 49);
    assertTrue(ranAt.isEmpty(), "due fifty T-states after the end, it should not have run yet");
    advance(speccy, 1);
    assertEquals(List.of(62L), ranAt, "what was due after the end is due that much sooner");
  }

  static Stream<Arguments> interruptLengths() {
    return Stream.of(Arguments.of(Spec16.class, 32), Arguments.of(Tc2048.class, 32), Arguments.of(Tc2068.class, 32), Arguments.of(Ts2068.class, 32), Arguments.of(Spec48.class, 32), Arguments.of(Spec128.class, 36), Arguments.of(SpecPlus3.class, 32),
        Arguments.of(Pentagon.class, 36), Arguments.of(Spec48Ntsc.class, 32));
  }

  /** Interrupt-line duration is model-specific; where in the frame it fires is covered separately in TheInterruptLineTest. */
  @ParameterizedTest
  @MethodSource("interruptLengths")
  void theInterruptLineIsDownForTheFirstTStatesOfAFrame(Class<? extends Spectrum> model, int length) {
    assertEquals(length, on(model).getTimings().interruptLength());
  }

  /** Frame count increments only after the clock has wrapped, exactly once per frame length. */
  @Test
  void aFrameIsCountedOnceItEndedAndTheNextEndComesAFrameLater() {
    Spectrum machine = on(Spec48.class);
    long frames = machine.frameCount();
    speccy.zxClock.setTStates(69890);
    machine.spectrumFrame();
    assertEquals(frames + 1, machine.frameCount());
    assertEquals(2, speccy.zxClock.getTStates());
    advance(speccy, 69888 - 2 - 1);
    assertEquals(frames + 1, machine.frameCount(), "one T-state short of a frame, it has not ended again");
    advance(speccy, 1);
    assertEquals(frames + 2, machine.frameCount(), "the end of a frame comes round a frame later");
  }

  static Stream<Arguments> firstPixels() {
    return Stream.of(Arguments.of(Spec16.class, 14336), Arguments.of(Tc2048.class, 14321), Arguments.of(Tc2068.class, 14321), Arguments.of(Ts2068.class, 9169), Arguments.of(Spec48.class, 14336), Arguments.of(Spec128.class, 14362),
        Arguments.of(SpecPlus3.class, 14365), Arguments.of(Spec48Ntsc.class, 8960), Arguments.of(Pentagon.class, 17988));
  }

  @ParameterizedTest
  @MethodSource("firstPixels")
  void thePictureStartsWhereTheModelSaysItDoes(Class<? extends Spectrum> model, int at) {
    on(model);
    assertEquals(at, firstPixel());
  }

  /**
   * Contention pattern is model-specific: 48K/128 count 6 down to 0 per 8 T-states starting
   * one before the first pixel; +3 counts from 1 starting four T-states earlier; Pentagon
   * contends nothing.
   */
  @Test
  void overThePictureAnAccessWaitsForTheUlaAndTheModelSaysHowLong() {
    Spectrum fortyEight = on(Spec48.class);
    assertEquals("6 5 4 3 2 1 0 0 6", waits(fortyEight, 14335, 9));
    assertEquals(0, fortyEight.contendDelay(1000), "the top border");
    assertEquals(0, fortyEight.contendDelay(14336 + 128 + 4), "the right border of a picture line");
    assertEquals(0, fortyEight.contendDelay(14336 + 192 * 224), "the line after the picture");
    assertEquals("6 5", waits(on(Spec128.class), 14361, 2));
    assertEquals("1 0 7 6 5 4 3 2 1", waits(on(SpecPlus3.class), 14361, 9));
    assertEquals("0 0 0", waits(on(Pentagon.class), 17987, 3));
  }

  /** Outside the picture area, an unattached port reads as all ones. */
  @Test
  void nothingFloatsOnTheBusOffThePicture() {
    on(Spec48.class);
    markTheScreen();
    assertEquals(0xff, floatingAt(1000, 0xff), "the top border");
    assertEquals(0xff, floatingAt(14336 - 4, 0xff), "the left border of the first picture line");
    assertEquals(0xff, floatingAt(14336 + 128, 0xff), "the right border");
    assertEquals(0xff, floatingAt(14336 + 192 * 224, 0xff), "below the picture");
  }

  /**
   * Within the picture, the floating bus shows the ULA's last fetch: pixel then attribute byte
   * for one column, then the next, idle for the remaining four of each eight T-states.
   */
  @Test
  void overThePictureTheBusCarriesWhatTheUlaJustFetched() {
    on(Spec48.class);
    markTheScreen();
    assertEquals(0xff, floatingAt(14336, 0xff));
    assertEquals(0xff, floatingAt(14336 + 1, 0xff));
    assertEquals(0x10, floatingAt(14336 + 2, 0xff), "the pixels of column 0");
    assertEquals(0x20, floatingAt(14336 + 3, 0xff), "and their attribute");
    assertEquals(0x11, floatingAt(14336 + 4, 0xff), "the pixels of column 1");
    assertEquals(0x21, floatingAt(14336 + 5, 0xff), "and its attribute");
    assertEquals(0xff, floatingAt(14336 + 6, 0xff));
    assertEquals(0xff, floatingAt(14336 + 7, 0xff));
    assertEquals(0x12, floatingAt(14336 + 10, 0xff), "the next group is the next two columns");
    assertEquals(0x23, floatingAt(14336 + 13, 0xff));
  }

  static Stream<Class<? extends Spectrum>> amstrads() {
    return Stream.of(SpecPlus2A.class, SpecPlus3.class, SpecPlus3E.class);
  }

  /** Amstrad-ULA machines never show floating-bus values, on any port or T-state. */
  @ParameterizedTest
  @MethodSource("amstrads")
  void theAmstradUlaDrivesItsBusSoNothingFloats(Class<? extends Spectrum> model) {
    on(model);
    markTheScreen();
    for (int port : new int[]{0xff, 0x0001, 0x0005, 0x0ffd}) {
      for (int i = 0; i < 16; i++) {
        assertEquals(0xff, floatingAt(14365 + i, port), "port " + port + " at " + (14365 + i));
      }
    }
  }

  /**
   * Undriven ULA-port bits are model-specific: always low on a +3; on a 128 or issue-3 48K they
   * mirror the last speaker-bit write; an issue-2 48K also mirrors the tape bit.
   */
  @Test
  void theBitsNobodyDrivesReadAsTheModelSays() {
    assertEquals((byte) 0xbf, on(SpecPlus3.class).ulaPortIdleValue((byte) 0x10));
    assertEquals((byte) 0xbf, on(SpecPlus3.class).ulaPortIdleValue((byte) 0x18));
    assertEquals((byte) 0xff, on(Spec128.class).ulaPortIdleValue((byte) 0x10));
    assertEquals((byte) 0xbf, on(Spec128.class).ulaPortIdleValue((byte) 0x08));
    Spectrum fortyEight = on(Spec48.class);
    assertEquals((byte) 0xff, fortyEight.ulaPortIdleValue((byte) 0x10), "issue 3: the speaker bit");
    assertEquals((byte) 0xbf, fortyEight.ulaPortIdleValue((byte) 0x08), "issue 3: the tape bit alone does nothing");
    speccy.machine.unit.issue2 = true;
    assertEquals((byte) 0xff, fortyEight.ulaPortIdleValue((byte) 0x08), "issue 2: the tape bit counts too");
    assertEquals((byte) 0xbf, fortyEight.ulaPortIdleValue((byte) 0x00));
  }

  /** Board contents are queried per model, not hard-coded by machine name. */
  @Test
  void aModelSaysWhatItHas() {
    assertFalse(on(Spec48.class).hasOnBoard(AyPeripheral.class));
    assertTrue(on(Spec128.class).hasOnBoard(AyPeripheral.class));
    assertTrue(on(Spec128.class).pagesThrough7ffd());
    assertFalse(on(Spec128.class).pagesThrough1ffd());
    assertTrue(on(SpecPlus3.class).pagesThrough1ffd());
    assertTrue(on(SpecPlus3.class).hasOnBoard(Upd765Peripheral.class));
    assertTrue(on(Pentagon.class).hasOnBoard(Beta128Peripheral.class));
  }

  /** The late-timings flag shifts the whole picture, and everything timed from it, by one T-state. */
  @Test
  void lateTimingsPutEverythingOneTStateLater() {
    Spectrum fortyEight = on(Spec48.class);
    speccy.machine.unit.lateTimings = true;
    speccy.machine.reset(true);
    assertEquals(14337, firstPixel());
    assertEquals(6, fortyEight.contendDelay(14336));
    assertEquals(0, fortyEight.contendDelay(14335));
    markTheScreen();
    assertEquals(0x10, floatingAt(14337 + 2, 0xff));
  }
}
