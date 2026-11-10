package model.tests.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fpetrola.oozx.speccy.screen.ScreenContext;
import com.fpetrola.oozx.speccy.screen.ScreenEffects;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Every shade and every mask turns the same picture into the same pixels, whichever way they are written. */
class EachShadeAndMaskPaintsAsBeforeTest {
  private static final Map<String, Integer> PAINTED = new TreeMap<>(Map.of(
      "mask APERTURE_GRILLE", -1437492094, "mask NONE", -1, "mask SLOT_MASK", 1816584523,
      "tint AMBER", 712502529, "tint GREEN", 183956679, "tint GREY", 1570991423, "tint NONE", 1309798370,
      "tint SEPIA", -1377825610, "tint WARM", -1479738544));

  @Test
  void everyShadeAndEveryMaskPaintsTheSamePixels() {
    Map<String, Integer> painted = new TreeMap<>();
    for (ScreenEffects.Tint.Shade shade : ScreenEffects.Tint.Shade.values()) {
      painted.put("tint " + shade, paint(new ScreenEffects.Tint(shade, 0.9, 1.2)));
    }
    for (ScreenEffects.ShadowMask.Pattern pattern : ScreenEffects.ShadowMask.Pattern.values()) {
      painted.put("mask " + pattern, paint(new ScreenEffects.ShadowMask(pattern, 0.4)));
    }
    assertEquals(PAINTED, painted);
  }

  private static int paint(com.fpetrola.oozx.speccy.screen.ScreenEffect effect) {
    BufferedImage picture = new BufferedImage(6, 8, BufferedImage.TYPE_INT_RGB);
    int[] pixels = ScreenContext.pixelsOf(picture);
    for (int i = 0; i < pixels.length; i++) pixels[i] = 0xFF000000 | (i * 0x2F3B47) & 0xFFFFFF;
    return effect.isTransparent() ? -1 : Arrays.hashCode(ScreenContext.pixelsOf(effect.apply(picture, new ScreenContext())));
  }
}
