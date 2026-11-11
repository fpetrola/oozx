package model.tests.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fpetrola.oozx.speccy.screen.SpeccyScreen;
import org.junit.jupiter.api.Test;

/**
 * The zoom tool cycles the screen's zoom from what it reads of it, and a session mapping writes that zoom down; both
 * are plugins built against getZoom and setZoom, so the cycle they make is said here.
 */
class TheZoomToolZoomsTheScreenTest {
  @Test
  void theZoomToolGoesUpToThreeAndBackToOne() {
    SpeccyScreen screen = new SpeccyScreen(new int[320 * 240]);

    for (int expected : new int[] {2, 3, 1}) {
      screen.setZoom(screen.getZoom() >= 3 ? 1 : screen.getZoom() + 1);
      assertEquals(expected, screen.getZoom());
    }
  }
}
