package model.tests.ui;

import com.fpetrola.oozx.speccy.SpeccyScreen;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The size the screen asks for is what a window packed around it gets, so it has to be the
 * picture it shows at its zoom: 320 by 236 with the border, 256 by 192 without. It used to ask
 * for the bare picture whatever the defaults said about the border.
 */
class SpeccyScreenSizeTest {
  @Test
  void itPrefersThePictureItShowsAtItsZoom() {
    Map<String, String> previous = ScreenSettings.getDefaults();
    Map<String, String> withBorder = new LinkedHashMap<>(previous);
    withBorder.put("border", "true");
    ScreenSettings.setDefaults(withBorder);
    try {
      SpeccyScreen screen = new SpeccyScreen(new byte[1000][1000]);
      assertEquals(new Dimension(640, 472), screen.getPreferredSize(), "with the border, at zoom 2");
      screen.setBorderVisible(false);
      assertEquals(new Dimension(512, 384), screen.getPreferredSize(), "without the border, at zoom 2");
    } finally {
      ScreenSettings.setDefaults(previous);
    }
  }
}
