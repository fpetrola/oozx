package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.machine.MachineTimings;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How a model measures its frame, the facts the derivation in prototypes/tdd earned that
 * SpectrumTest did not already state: the parts and their sums, the clock as a rate, the first
 * pixel as a datum, and a late unit as a property rather than a step.
 */
class TimingsTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  private Spectrum on(Class<? extends Spectrum> model) {
    speccy.machine.select(speccy.machine.model(model));
    return speccy.machine.current;
  }

  private int firstPixel() {
    return (int) speccy.machine.current.lineStart(speccy.display.BORDER_HEIGHT) + speccy.display.BORDER_WIDTH_COLS * 4;
  }

  /** A line is its four parts end to end - border, picture, border, retrace - and so is the frame, in lines. */
  @Test
  void aLineAndAFrameAreTheirFourPartsEndToEnd() {
    var timings = on(Spec48.class).getTimings();

    assertEquals(new MachineTimings.Span(24, 128, 24, 48), timings.frame().line());
    assertEquals(224, timings.tstatesPerLine());
    assertEquals(new MachineTimings.Span(48, 192, 48, 24), timings.frame().lines());
    assertEquals(312, timings.linesPerFrame());
    assertEquals(224 * 312, timings.tstatesPerFrame());
  }

  /**
   * The clock says how long a frame takes: a 48K's is a shade over a fiftieth of a second and a
   * 128's a shade under, and a stretch of real time is that many T-states, which is what the
   * timer and a drive's motor ask.
   */
  @Test
  void theClockSaysHowLongAFrameTakes() {
    var fortyEight = on(Spec48.class).getTimings();
    assertEquals(50.08, (double) fortyEight.processorSpeed() / fortyEight.tstatesPerFrame(), 0.005);
    assertEquals(3500, fortyEight.processorSpeed() / 1000, "a millisecond");

    var oneTwentyEight = on(Spec128.class).getTimings();
    assertEquals(50.02, (double) oneTwentyEight.processorSpeed() / oneTwentyEight.tstatesPerFrame(), 0.005);
  }

  /**
   * Where the first pixel is was measured on each model and is not the border's lines times a
   * line: on a 48K it is 64 lines exactly where the border is 48, and on a 128 it is two T-states
   * before a line boundary. So it is a datum of the model, kept as measured.
   */
  @Test
  void theFirstPixelIsWhereItWasMeasured() {
    on(Spec48.class);
    assertEquals(64 * 224, firstPixel(), "a line boundary, and not the border's 48 lines");
    on(Spec128.class);
    assertEquals(63 * 228 - 2, firstPixel(), "not a line boundary");
    on(Pentagon.class);
    assertEquals(17988, firstPixel());
  }

  /** A late unit is a property, not a step: it is not later still on the next reset, and the setting can be put back. */
  @Test
  void aLateUnitIsNotLaterStillAndCanBePutBack() {
    on(Spec48.class);
    speccy.machine.unit.lateTimings = true;
    speccy.machine.reset(true);
    assertEquals(14337, firstPixel());
    speccy.machine.reset(true);
    assertEquals(14337, firstPixel(), "there is no such thing as later still");
    assertEquals(69888, speccy.machine.current.getTimings().tstatesPerFrame(), "and nothing else about it differs");

    speccy.machine.unit.lateTimings = false;
    speccy.machine.reset(true);
    assertEquals(14336, firstPixel(), "the model as measured");
  }
}
