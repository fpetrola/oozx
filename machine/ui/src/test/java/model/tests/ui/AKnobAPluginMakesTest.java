package model.tests.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fpetrola.oozx.speccy.screen.Knob;
import org.junit.jupiter.api.Test;

/**
 * A number knob made the way the tube-curve plugin makes its own: its own range, its own step, its own fallback.
 * The plugins are built against this, so what they ask of it is said here too.
 */
class AKnobAPluginMakesTest {
  private double curve = 3;

  @Test
  void aPluginsKnobKeepsItsOwnRangeAndStep() {
    Knob knob = Knob.number("curve", "Tube curve", "How much the glass bulges.", "Television", 0, 10, 0.5, 0,
        () -> curve, value -> curve = ((Number) value).doubleValue());

    assertEquals(3.0, knob.value());
    knob.reset();
    assertEquals(0.0, curve);
  }
}
