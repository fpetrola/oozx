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
 * Frame-timing facts not already covered by SpectrumTest: line/frame part breakdowns, clock
 * rate, first-pixel position, and the late-timings flag's persistence behaviour.
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

  /** A line/frame each sum their four parts (border, picture, border, retrace) exactly. */
  @Test
  void aLineAndAFrameAreTheirFourPartsEndToEnd() {
    var timings = on(Spec48.class).getTimings();

    assertEquals(new MachineTimings.Span(24, 128, 24, 48), timings.frame().line());
    assertEquals(224, timings.tstatesPerLine());
    assertEquals(new MachineTimings.Span(48, 192, 48, 24), timings.frame().lines());
    assertEquals(312, timings.linesPerFrame());
    assertEquals(224 * 312, timings.tstatesPerFrame());
  }

  /** Frame duration in real time (48K slightly over 1/50s, 128 slightly under), as used by the
   * timer and drive motor timing. */
  @Test
  void theClockSaysHowLongAFrameTakes() {
    var fortyEight = on(Spec48.class).getTimings();
    assertEquals(50.08, (double) fortyEight.processorSpeed() / fortyEight.tstatesPerFrame(), 0.005);
    assertEquals(3500, fortyEight.processorSpeed() / 1000, "a millisecond");

    var oneTwentyEight = on(Spec128.class).getTimings();
    assertEquals(50.02, (double) oneTwentyEight.processorSpeed() / oneTwentyEight.tstatesPerFrame(), 0.005);
  }

  /** First-pixel position is a measured constant per model, not derivable from border size
   * alone (48K: exactly 64 lines despite a 48-line border; 128: 2 T-states before a boundary). */
  @Test
  void theFirstPixelIsWhereItWasMeasured() {
    on(Spec48.class);
    assertEquals(64 * 224, firstPixel(), "a line boundary, and not the border's 48 lines");
    on(Spec128.class);
    assertEquals(63 * 228 - 2, firstPixel(), "not a line boundary");
    on(Pentagon.class);
    assertEquals(17988, firstPixel());
  }

  /** The late-timings flag is idempotent across resets and reversible, not cumulative. */
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
