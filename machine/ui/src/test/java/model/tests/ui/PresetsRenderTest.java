package model.tests.ui;

import com.fpetrola.oozx.speccy.screen.ScreenProfile;
import com.fpetrola.oozx.speccy.screen.SpeccyScreen;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Every look that comes with the program paints without touching the machine's frame. The
 * effects work in place, and a television look with the border on was handed the frame itself:
 * every repaint blurred and faded what the machine was still drawing, and the picture stopped,
 * washed out. The window paints from a copy now; this is what says so.
 */
class PresetsRenderTest {
  @Test
  void everyPresetPaintsWithoutTouchingTheMachinesFrame() {
    for (ScreenProfile profile : ScreenProfile.presets()) {
      int[] pixels = new int[320 * 240];
      for (int i = 0; i < pixels.length; i++) pixels[i] = (i * 7 % 13) < 6 ? 0xD7D700 : 0x0000D7;
      int[] before = pixels.clone();
      SpeccyScreen screen = new SpeccyScreen(pixels);
      screen.getScreenSettings().apply(profile.values());
      screen.setSize(960, 720);
      BufferedImage canvas = new BufferedImage(960, 720, BufferedImage.TYPE_INT_RGB);
      for (int frame = 0; frame < 3; frame++) {
        screen.paint(canvas.getGraphics());
      }
      assertArrayEquals(before, pixels, profile.name() + " changed the machine's frame");
    }
  }
}
