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

package com.fpetrola.oozx.speccy.screen;

import com.fpetrola.oozx.speccy.TvScreen;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How one machine's picture is shown, and the pipeline that does it.
 * <p>
 * ONE OF THESE PER WINDOW. Two emulators open at once are two machines, and someone watching a
 * loading screen on a simulated television while playing a game on a sharp monitor next to it is
 * a reasonable thing to want. What they share is a starting point: {@link #getDefaults()} is what
 * a new window is opened with, and a window can be told to become the default for the next ones.
 * <p>
 * It describes itself. {@link #settings()} hands out one descriptor per knob - what it is called,
 * what it does, what it may be, what it is now, and how to change it - so a window over these is
 * a loop over that list rather than a second copy of this class written in controls. Adding an
 * effect is adding it here; nothing downstream has to hear about it.
 */
public class ScreenSettings {

  /** What a window that nobody has configured is opened with. */
  private static volatile Map<String, String> defaults = new LinkedHashMap<>();

  private TvScreen tv = TvScreen.RGB_MONITOR;
  private Scaler scaler = new Scalers.Nearest();
  private double scanLines;
  private ScreenEffects.ShadowMask.Pattern mask = ScreenEffects.ShadowMask.Pattern.NONE;
  private double maskDepth = 0.35;
  private double phosphor;
  private ScreenEffects.Tint.Shade shade = ScreenEffects.Tint.Shade.NONE;
  private double brightness = 1;
  private double saturation = 1;
  private boolean border;

  private Runnable onChange = () -> { };

  /** Called whenever anything here changes, so the window it belongs to can repaint at once. */
  public void onChange(Runnable onChange) {
    this.onChange = onChange == null ? () -> { } : onChange;
  }

  private void changed() {
    onChange.run();
  }

  /**
   * Every knob, describing itself.
   * <p>
   * The order is the order they are worth meeting in, and the group is which part of a window
   * they belong to, so nothing downstream has to guess either from the names.
   */
  public List<ScreenSetting> settings() {
    List<ScreenSetting> all = new ArrayList<>();

    all.add(ScreenSetting.choice("scaler", "Scaler",
        "How 256 by 192 becomes a windowful. Repeating pixels is exact and uneven at odd sizes; "
            + "averaging is even and soft; the rest guess at the shapes the pixels suggest.",
        ScreenSetting.Group.SCALING,
        Scalers.all().stream().map(Scaler::label).toList(), new Scalers.Nearest().label(),
        () -> scaler.label(), value -> {
          scaler = Scalers.byName(String.valueOf(value));
          changed();
        }));

    all.add(ScreenSetting.switching("border", "Show border",
        "The colour around the picture. Games flash it, and a tape loading stripes it.",
        ScreenSetting.Group.SCALING, false,
        () -> border, value -> {
          border = (Boolean) value;
          changed();
        }));

    all.add(ScreenSetting.choice("tv", "Lead",
        "Which lead the picture came down. A composite signal carries colour with a quarter the "
            + "bandwidth it carries brightness, so colour smears sideways and brightness does not.",
        ScreenSetting.Group.TELEVISION,
        java.util.Arrays.stream(TvScreen.values()).map(TvScreen::label).toList(),
        TvScreen.RGB_MONITOR.label(),
        () -> tv.label(), value -> {
          tv = TvScreen.byName(String.valueOf(value));
          changed();
        }));

    all.add(ScreenSetting.number("scanlines", "Scan lines",
        "How dark the unlit gaps between the tube's lines are. Nothing below two screen pixels "
            + "to a machine pixel, where there is no gap to leave.",
        ScreenSetting.Group.TELEVISION, 0, 0.8, 0.05, 0,
        () -> scanLines, value -> {
          scanLines = ((Number) value).doubleValue();
          changed();
        }));

    all.add(ScreenSetting.choice("mask", "Phosphor layout",
        "A colour tube has no pixels: it has stripes or dots of red, green and blue phosphor, and "
            + "the picture is a beam landing across them.",
        ScreenSetting.Group.TELEVISION,
        java.util.Arrays.stream(ScreenEffects.ShadowMask.Pattern.values()).map(Enum::name).toList(),
        ScreenEffects.ShadowMask.Pattern.NONE.name(),
        () -> mask.name(), value -> {
          mask = ScreenEffects.ShadowMask.Pattern.valueOf(String.valueOf(value));
          changed();
        }));

    all.add(ScreenSetting.number("maskdepth", "Phosphor depth",
        "How strongly the stripes show. Too much and the picture goes dark; the tube did dim it.",
        ScreenSetting.Group.TELEVISION, 0, 0.8, 0.05, 0.35,
        () -> maskDepth, value -> {
          maskDepth = ((Number) value).doubleValue();
          changed();
        }));

    all.add(ScreenSetting.number("phosphor", "Persistence",
        "How much of the frame before is still glowing. A tube fades rather than going dark, "
            + "which is why a sprite has a tail on one and a flickering game is easier to watch.",
        ScreenSetting.Group.TELEVISION, 0, 0.9, 0.05, 0,
        () -> phosphor, value -> {
          phosphor = ((Number) value).doubleValue();
          changed();
        }));

    all.add(ScreenSetting.choice("shade", "Tint",
        "What the colours become on the way out: an old monochrome monitor, or something easier "
            + "on the eyes at night.",
        ScreenSetting.Group.COLOUR,
        java.util.Arrays.stream(ScreenEffects.Tint.Shade.values())
            .map(ScreenEffects.Tint.Shade::label).toList(),
        ScreenEffects.Tint.Shade.NONE.label(),
        () -> shade.label(), value -> {
          for (ScreenEffects.Tint.Shade option : ScreenEffects.Tint.Shade.values()) {
            if (option.label().equalsIgnoreCase(String.valueOf(value))
                || option.name().equalsIgnoreCase(String.valueOf(value))) {
              shade = option;
            }
          }
          changed();
        }));

    all.add(ScreenSetting.number("brightness", "Brightness", "How bright, all over.",
        ScreenSetting.Group.COLOUR, 0.4, 1.8, 0.05, 1,
        () -> brightness, value -> {
          brightness = ((Number) value).doubleValue();
          changed();
        }));

    all.add(ScreenSetting.number("saturation", "Colour depth",
        "How strong the colours are. Nothing is black and white; two is more than a Spectrum "
            + "ever managed.",
        ScreenSetting.Group.COLOUR, 0, 2, 0.05, 1,
        () -> saturation, value -> {
          saturation = ((Number) value).doubleValue();
          changed();
        }));

    return all;
  }

  /**
   * Everything set here, as text, for writing down.
   * <p>
   * Text and not the objects themselves, so a settings file written by a version with one effect
   * more or one fewer still reads: what is not recognised is skipped and what is missing keeps
   * the value it was born with.
   */
  public Map<String, String> values() {
    Map<String, String> written = new LinkedHashMap<>();
    for (ScreenSetting setting : settings()) {
      written.put(setting.key(), String.valueOf(setting.value()));
    }
    return written;
  }

  /** Takes on what was written down, ignoring anything it does not know. */
  public void apply(Map<String, String> written) {
    if (written == null) {
      return;
    }
    for (ScreenSetting setting : settings()) {
      String value = written.get(setting.key());
      if (value == null) {
        continue;
      }
      try {
        switch (setting.kind()) {
          case NUMBER -> setting.set(Double.parseDouble(value));
          case SWITCH -> setting.set(Boolean.parseBoolean(value));
          case CHOICE -> setting.set(value);
        }
      } catch (RuntimeException unreadable) {
        // A value from a file someone edited, or from a version that meant something else by it.
        // One knob at its default is a better answer than a window that will not open.
        setting.reset();
      }
    }
  }

  /** Puts every knob back to what it is when nobody has touched it. */
  public void reset() {
    settings().forEach(ScreenSetting::reset);
  }

  /** What a window opened from now on starts with. */
  public static Map<String, String> getDefaults() {
    return new LinkedHashMap<>(defaults);
  }

  public static void setDefaults(Map<String, String> newDefaults) {
    defaults = newDefaults == null ? new LinkedHashMap<>() : new LinkedHashMap<>(newDefaults);
  }

  /** These settings become what the next windows are opened with. */
  public void makeDefault() {
    setDefaults(values());
  }

  public boolean isBorder() {
    return border;
  }

  /**
   * Runs the frame through everything that is turned on, in the order it has to happen in.
   * <p>
   * Before the scaler go the things that are true of the PICTURE - the lead it came down, how
   * long the phosphor holds it, what the colours are turned into - which is also where they are
   * cheapest, on a quarter of the pixels a window has. After it go the things that are true of
   * the SCREEN, the mask and the lines, which are the same size whatever the magnification.
   */
  public BufferedImage render(BufferedImage picture, int width, int height, ScreenContext context) {
    context.beginFrame(width, height);
    BufferedImage frame = picture;
    int sourceHeight = picture.getHeight();

    frame = run(new ScreenEffects.Phosphor(phosphor), frame, context);
    frame = run(tv, frame, context);
    frame = run(new ScreenEffects.Tint(shade, brightness, saturation), frame, context);
    frame = scaler.scale(frame, width, height, context);
    frame = run(new ScreenEffects.ShadowMask(mask, maskDepth), frame, context);
    frame = run(new ScreenEffects.Scanlines(scanLines, sourceHeight), frame, context);
    return frame;
  }

  private static BufferedImage run(ScreenEffect effect, BufferedImage frame, ScreenContext context) {
    return effect.isTransparent() ? frame : effect.apply(frame, context);
  }
}
