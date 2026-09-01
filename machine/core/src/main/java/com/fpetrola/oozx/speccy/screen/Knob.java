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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One knob, describing itself well enough that a window can be built out of a list of them.
 * <p>
 * A settings window is usually written twice: once as the settings, and once as the controls that
 * show them. The second copy drifts from the first - a knob is added and nobody adds its control,
 * or a range changes in one place and not the other - and the drift is silent. Here a knob says
 * what it is, what it may be, what it is now and how to change it, and the window is a loop over
 * the list. Adding an effect is writing the effect.
 *
 * @param key      what this is called in a settings file, and never shown to anyone
 * @param label    what it is called in the window
 * @param about    one line saying what it does, for whoever did not write it
 * @param group    which part of the window it belongs in
 * @param kind     what sort of control shows it
 * @param options  for a CHOICE, what may be chosen; empty otherwise
 * @param minimum  for a NUMBER, the low end
 * @param maximum  for a NUMBER, the high end
 * @param step     for a NUMBER, how far one nudge moves it - so nobody has to guess a granularity
 * @param fallback what it is when nothing has been chosen, so a knob can be put back
 * @param read     what it is now
 * @param write    how to change it, taking effect at once
 */
public record Knob(String key, String label, String about, String group, Kind kind,
                            List<String> options, double minimum, double maximum, double step,
                            Object fallback, Supplier<Object> read, Consumer<Object> write) {

  /** What sort of control shows a knob. */
  public enum Kind { CHOICE, SWITCH, NUMBER }

  /** One of a fixed set, shown as a list or as buttons. */
  public static Knob choice(String key, String label, String about, String group,
                                     List<String> options, String fallback,
                                     Supplier<Object> read, Consumer<Object> write) {
    return new Knob(key, label, about, group, Kind.CHOICE, List.copyOf(options),
        0, 0, 0, fallback, read, write);
  }

  /** On or off. */
  public static Knob switching(String key, String label, String about, String group,
                                        boolean fallback,
                                        Supplier<Object> read, Consumer<Object> write) {
    return new Knob(key, label, about, group, Kind.SWITCH, List.of(),
        0, 1, 1, fallback, read, write);
  }

  /** A number between two ends. */
  public static Knob number(String key, String label, String about, String group,
                                     double minimum, double maximum, double step, double fallback,
                                     Supplier<Object> read, Consumer<Object> write) {
    return new Knob(key, label, about, group, Kind.NUMBER, List.of(),
        minimum, maximum, step, fallback, read, write);
  }

  /** Puts this knob back to what it is when nobody has touched it. */
  public void reset() {
    write.accept(fallback);
  }

  public Object value() {
    return read.get();
  }

  public void set(Object value) {
    write.accept(value);
  }
}
