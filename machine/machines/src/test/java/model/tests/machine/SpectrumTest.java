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

package model.tests.machine;

import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.Upd765Peripheral;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
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
 * their order are those of the derivation in prototypes/tdd, and the numbers are libspectrum's.
 */
class SpectrumTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  private Spectrum on(Class<? extends Spectrum> model) {
    speccy.machine.select(speccy.machine.model(model));
    return speccy.machine.current;
  }

  /** The T-state of the top left pixel of the picture: the first displayed line starts a border's width before it. */
  private int firstPixel() {
    return (int) speccy.machine.current.lineStart(speccy.display.BORDER_HEIGHT) + speccy.display.BORDER_WIDTH_COLS * 4;
  }

  private int floatingAt(int tState, int port) {
    speccy.zxClock.setTStates(tState);
    return speccy.machine.current.unattachedPort(port) & 0xff;
  }

  /** Every byte of the first screen line says which column it is, so the bus can be read back by eye. */
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

  static Stream<Arguments> models() {
    return Stream.of(
        Arguments.of(Spec128.class, 228, 311, 3_546_900L),
        Arguments.of(SpecPlus3.class, 228, 311, 3_546_900L),
        Arguments.of(Pentagon.class, 224, 320, 3_584_000L),
        // libspectrum times the NTSC 48K at 3527500 and this machine at 3579545: not asserted until decided.
        Arguments.of(Spec48Ntsc.class, 224, 264, 0L));
  }

  /** Nothing is assumed from the 48K: every model measures its own frame. */
  @ParameterizedTest
  @MethodSource("models")
  void everyModelMeasuresItsOwnFrame(Class<? extends Spectrum> model, int line, int lines, long hz) {
    var timings = on(model).getTimings();
    assertEquals(line, timings.tstatesPerLine());
    assertEquals(line * lines, timings.tstatesPerFrame());
    if (hz != 0) assertEquals(hz, timings.processorSpeed());
  }

  /** A frame's end takes a frame off the clock and off everything that was waiting for one. */
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
    return Stream.of(Arguments.of(Spec48.class, 32), Arguments.of(Spec128.class, 36), Arguments.of(SpecPlus3.class, 32),
        Arguments.of(Pentagon.class, 36), Arguments.of(Spec48Ntsc.class, 32));
  }

  /** How long /INT stays down at the top of a frame is the model's own. That it is taken inside and not after is TheInterruptLineTest's. */
  @ParameterizedTest
  @MethodSource("interruptLengths")
  void theInterruptLineIsDownForTheFirstTStatesOfAFrame(Class<? extends Spectrum> model, int length) {
    assertEquals(length, on(model).getTimings().interruptLength());
  }

  /** The frame is counted once it has ended, with the clock already back, and the next end comes a frame later. */
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
    return Stream.of(Arguments.of(Spec48.class, 14336), Arguments.of(Spec128.class, 14362),
        Arguments.of(SpecPlus3.class, 14365), Arguments.of(Spec48Ntsc.class, 8960), Arguments.of(Pentagon.class, 17988));
  }

  @ParameterizedTest
  @MethodSource("firstPixels")
  void thePictureStartsWhereTheModelSaysItDoes(Class<? extends Spectrum> model, int at) {
    on(model);
    assertEquals(at, firstPixel());
  }

  /**
   * Over the picture the ULA holds the processor up, and how long is the model's: a 48K and a 128
   * count six down to nothing in each group of eight, from the T-state before their first pixel;
   * a +3 counts from one, four T-states before it; and a Pentagon holds nobody up.
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

  /** With nobody driving it, a read off the picture sees the bus idle: all ones. */
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
   * Over the picture the bus carries what the ULA has just fetched: in every group of eight
   * T-states, a pixel byte and its attribute for one column and then for the next, with the bus
   * idle in the other four.
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

  /** The Amstrad ULA drives its bus, so nothing of the picture is ever left on it, on any port and at any T-state. */
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
   * The two bits of the ULA's port that nothing drives read as the model says: on a +3 bit 6 is
   * low; on a 128, and on a 48K of issue 3, it follows the speaker bit of the last write to the
   * port; on an issue 2 it follows the tape bit as well.
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

  /** What a model has is the model's to say, and a machine is asked rather than named. */
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

  /** Late timings: the whole picture is one T-state later, and so is everything timed from it. */
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
