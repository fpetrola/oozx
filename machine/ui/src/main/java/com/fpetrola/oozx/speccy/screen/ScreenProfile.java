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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A whole way of showing the picture, under one name.
 * <p>
 * Ten knobs is enough that getting to a look worth having means moving several of them together,
 * and enough that nobody wants to do it twice. What someone actually wants is "the way it looked
 * on a telly" or "sharp, get out of the way", and those are combinations, not settings: a
 * composite lead alone is not a television, it wants the scan lines and the phosphor with it.
 * <p>
 * The ones here are the combinations worth having; a person's own go beside them, and neither
 * kind is treated differently once it is in the list.
 *
 * @param name   what it is called
 * @param values the knobs it sets, by key; anything it leaves out keeps whatever it had
 * @param builtIn whether it came with the program, which is the only thing that cannot be deleted
 */
public record ScreenProfile(String name, Map<String, String> values, boolean builtIn) {

  public ScreenProfile {
    values = Map.copyOf(values);
  }

  /** A profile made from what a window is set to now. */
  public static ScreenProfile of(String name, ScreenSettings settings) {
    return new ScreenProfile(name, settings.values(), false);
  }

  /**
   * The combinations that come with the program.
   * <p>
   * Deliberately few and far apart. A list of twenty near-identical looks is a list nobody reads;
   * these are six that are recognisably different from each other across the room.
   */
  public static List<ScreenProfile> presets() {
    List<ScreenProfile> all = new ArrayList<>();

    all.add(preset("Sharp monitor",
        "scaler", "Nearest neighbour", "tv", "RGB Monitor",
        "scanlines", "0", "mask", "NONE", "phosphor", "0",
        "shade", "None", "brightness", "1", "saturation", "1", "border", "false"));

    all.add(preset("Smooth monitor",
        "scaler", "xBRZ 4x", "tv", "RGB Monitor",
        "scanlines", "0", "mask", "NONE", "phosphor", "0",
        "shade", "None", "brightness", "1", "saturation", "1", "border", "false"));

    // A television in reasonable condition, plugged in the good way: the picture is soft and the
    // colour runs, and the lines are there because the tube had them.
    all.add(preset("Television",
        "scaler", "Sharp bilinear", "tv", "Composite Video",
        "scanlines", "0.35", "mask", "APERTURE_GRILLE", "maskdepth", "0.25",
        "phosphor", "0.25", "shade", "None", "brightness", "1.1", "saturation", "1.05",
        "border", "true"));

    // The one in the corner of the room in 1985, on the aerial socket, with the game on channel
    // 36 and the tuning never quite right.
    all.add(preset("Portable telly, 1985",
        "scaler", "Sharp bilinear", "tv", "Aerial (RF)",
        "scanlines", "0.45", "mask", "SLOT_MASK", "maskdepth", "0.4",
        "phosphor", "0.4", "shade", "Warm (less blue)", "brightness", "1.15", "saturation", "0.9",
        "border", "true"));

    all.add(preset("Green screen",
        "scaler", "Sharp bilinear", "tv", "Scart (RGB)",
        "scanlines", "0.4", "mask", "NONE", "phosphor", "0.35",
        "shade", "Green phosphor", "brightness", "1.1", "saturation", "1", "border", "false"));

    all.add(preset("Amber screen",
        "scaler", "Sharp bilinear", "tv", "Scart (RGB)",
        "scanlines", "0.4", "mask", "NONE", "phosphor", "0.35",
        "shade", "Amber phosphor", "brightness", "1.1", "saturation", "1", "border", "false"));

    return all;
  }

  private static ScreenProfile preset(String name, String... pairs) {
    Map<String, String> values = new LinkedHashMap<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      values.put(pairs[i], pairs[i + 1]);
    }
    return new ScreenProfile(name, values, true);
  }

  /**
   * Which of these a window is showing, or null if it is showing something of its own.
   * <p>
   * By what it looks like rather than by remembering what was chosen: someone who picks a
   * television and then turns the scan lines off is not watching a television any more, and a
   * combo still saying so would be lying about the picture in front of them.
   */
  public static ScreenProfile matching(ScreenSettings settings, List<ScreenProfile> among) {
    Map<String, String> now = settings.values();
    for (ScreenProfile profile : among) {
      boolean same = true;
      for (Map.Entry<String, String> knob : profile.values().entrySet()) {
        String is = now.get(knob.getKey());
        if (is == null || !sameValue(is, knob.getValue())) {
          same = false;
          break;
        }
      }
      if (same) {
        return profile;
      }
    }
    return null;
  }

  /** "0" and "0.0" are the same number, and a profile is written by hand. */
  private static boolean sameValue(String one, String other) {
    if (one.equalsIgnoreCase(other)) {
      return true;
    }
    try {
      return Double.compare(Double.parseDouble(one), Double.parseDouble(other)) == 0;
    } catch (NumberFormatException notNumbers) {
      return false;
    }
  }

  @Override
  public String toString() {
    return name;
  }
}
