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

package model.tests;

import com.fpetrola.oozx.speccy.screen.Scaler;
import com.fpetrola.oozx.speccy.screen.ScreenContext;
import com.fpetrola.oozx.speccy.screen.ScreenSetting;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import com.fpetrola.oozx.speccy.screen.Scalers;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What each scaler promises, and what a window built out of the settings can rely on. */
class ScreenPipelineTest {

  /** Diagonals and single-pixel detail in a Spectrum's fifteen colours: what these are for. */
  private static BufferedImage picture() {
    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    int[] palette = {0x000000, 0x0000D7, 0xD70000, 0x00D700, 0xD7D700, 0xFFFFFF};
    for (int y = 0; y < 64; y++) {
      for (int x = 0; x < 64; x++) {
        boolean diagonal = ((x + y) / 3) % 2 == 0;
        boolean speck = (x % 7 == 0) && (y % 5 == 0);
        image.setRGB(x, y, palette[speck ? 5 : diagonal ? (x / 8) % 5 : 0]);
      }
    }
    return image;
  }

  private static Set<Integer> coloursOf(BufferedImage image) {
    Set<Integer> colours = new HashSet<>();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        colours.add(image.getRGB(x, y) & 0xFFFFFF);
      }
    }
    return colours;
  }

  private static BufferedImage scaled(Scaler scaler, BufferedImage source, int times) {
    ScreenContext context = new ScreenContext();
    int width = source.getWidth() * times, height = source.getHeight() * times;
    context.beginFrame(width, height);
    return scaler.scale(source, width, height, context);
  }

  @Test
  void everyScalerHandsBackThePictureAtTheSizeItWasAskedFor() {
    BufferedImage source = picture();
    for (Scaler scaler : Scalers.all()) {
      // Odd numbers on purpose: a window is whatever size someone dragged it to, and a scaler
      // that works in whole steps still has to land exactly on it.
      ScreenContext context = new ScreenContext();
      context.beginFrame(301, 219);
      BufferedImage out = scaler.scale(source, 301, 219, context);
      assertEquals(301, out.getWidth(), scaler.label() + " came out the wrong width");
      assertEquals(219, out.getHeight(), scaler.label() + " came out the wrong height");
    }
  }

  @Test
  void thePixelArtScalersNeverInventAColour() {
    // The promise that makes them worth having on this machine: a Spectrum's picture is drawn
    // from fifteen colours a pixel at a time, and a scaler that blends turns the palette into a
    // gradient. These enlarge by choosing among the colours that are already there.
    BufferedImage source = picture();
    Set<Integer> original = coloursOf(source);
    for (Scaler scaler : List.of(new Scalers.Nearest(), new Scalers.Scale2x(), new Scalers.Scale3x())) {
      Set<Integer> after = coloursOf(scaled(scaler, source, 2));
      after.removeAll(original);
      assertTrue(after.isEmpty(), scaler.label() + " invented " + after.size() + " colours");
    }
  }

  @Test
  void theSmoothingOnesDoInventColours() {
    // The other half of the same statement: bilinear is not a pixel-art scaler and should not
    // quietly become one, and the edge-directed one blends corners on purpose.
    BufferedImage source = picture();
    Set<Integer> original = coloursOf(source);
    for (Scaler scaler : List.of(new Scalers.Bilinear(), new Scalers.EdgeDirected())) {
      Set<Integer> after = coloursOf(scaled(scaler, source, 2));
      after.removeAll(original);
      assertFalse(after.isEmpty(), scaler.label() + " blended nothing");
    }
  }

  @Test
  void aScalerSmoothsTheStaircaseOffADiagonal() {
    // What Scale2x is for, measured rather than looked at: walking down a diagonal, a scaler
    // that rounds it changes colour in smaller steps than one that repeats pixels.
    BufferedImage source = picture();
    assertTrue(steps(scaled(new Scalers.Scale2x(), source, 2))
            > steps(scaled(new Scalers.Nearest(), source, 2)),
        "Scale2x should break a diagonal into more, smaller steps than repeating pixels does");
  }

  private static int steps(BufferedImage image) {
    int changes = 0;
    for (int i = 1; i < Math.min(image.getWidth(), image.getHeight()); i++) {
      if (image.getRGB(i, i) != image.getRGB(i - 1, i - 1)) {
        changes++;
      }
    }
    return changes;
  }

  @Test
  void everySettingDescribesItselfWellEnoughToBuildAControlFrom() {
    // A window over these is a loop over the list, so anything the loop needs has to be there:
    // without a step it has to invent a granularity, without a default it cannot offer a reset,
    // without a group it has to guess the layout from the names.
    for (ScreenSetting setting : new ScreenSettings().settings()) {
      assertNotNull(setting.label(), setting.key() + " has no label");
      assertFalse(setting.about().isBlank(), setting.key() + " says nothing about what it does");
      assertNotNull(setting.group(), setting.key() + " belongs to no part of the window");
      assertNotNull(setting.fallback(), setting.key() + " cannot be put back");
      assertNotNull(setting.value(), setting.key() + " will not say what it is");
      if (setting.kind() == ScreenSetting.Kind.NUMBER) {
        assertTrue(setting.step() > 0, setting.key() + " has no step, so a slider must guess one");
        assertTrue(setting.maximum() > setting.minimum(), setting.key() + " has no range");
      }
      if (setting.kind() == ScreenSetting.Kind.CHOICE) {
        assertFalse(setting.options().isEmpty(), setting.key() + " offers nothing to choose");
        assertTrue(setting.options().contains(String.valueOf(setting.fallback())),
            setting.key() + " falls back to something it does not offer");
      }
    }
  }

  @Test
  void whatOneWindowIsSetToCanBeGivenToAnother() {
    // How "use these as the default" works: read one window's knobs, hand them to the next.
    ScreenSettings one = new ScreenSettings();
    one.settings().forEach(setting -> {
      switch (setting.kind()) {
        case NUMBER -> setting.set(setting.maximum());
        case SWITCH -> setting.set(true);
        case CHOICE -> setting.set(setting.options().get(setting.options().size() - 1));
      }
    });
    Map<String, String> written = one.values();

    ScreenSettings other = new ScreenSettings();
    assertNotEquals(written, other.values(), "the two started out the same, so this proves nothing");
    other.apply(written);
    assertEquals(written, other.values(), "a window did not take on what another was set to");
  }

  @Test
  void aSettingsFileFromAnotherVersionStillOpens() {
    ScreenSettings settings = new ScreenSettings();
    Map<String, String> before = settings.values();
    // A knob that no longer exists, one that does with a value that no longer means anything,
    // and nothing at all for the rest. None of that is worth a window that will not open.
    settings.apply(Map.of("scanlines", "not a number", "curvature", "0.5"));
    assertEquals(before.get("scaler"), settings.values().get("scaler"),
        "an unreadable value for one knob disturbed another");
    assertEquals(String.valueOf(0.0), settings.values().get("scanlines"),
        "an unreadable value should leave its knob at the default");
  }

  @Test
  void changingAnythingSaysSo() {
    // What makes the window show its effect at once instead of when it is closed.
    ScreenSettings settings = new ScreenSettings();
    int[] told = {0};
    settings.onChange(() -> told[0]++);
    settings.settings().forEach(setting -> setting.set(setting.value()));
    assertTrue(told[0] >= new ScreenSettings().settings().size(),
        "setting a knob did not report the change, so nothing would repaint");
  }
}
