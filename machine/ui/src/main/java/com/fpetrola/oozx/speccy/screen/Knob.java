/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

  /** What sort of control shows a knob, and how its value is read back from where it was written down. */
  public enum Kind {
    CHOICE {
      @Override
      Object read(String written) {
        return written;
      }

      @Override
      <T> T shownBy(Controls<T> controls, Knob knob) {
        return controls.choice(knob);
      }
    },
    SWITCH {
      @Override
      Object read(String written) {
        return Boolean.parseBoolean(written);
      }

      @Override
      <T> T shownBy(Controls<T> controls, Knob knob) {
        return controls.switching(knob);
      }
    },
    NUMBER {
      @Override
      Object read(String written) {
        return Double.parseDouble(written);
      }

      @Override
      <T> T shownBy(Controls<T> controls, Knob knob) {
        return controls.number(knob);
      }
    };

    abstract Object read(String written);

    abstract <T> T shownBy(Controls<T> controls, Knob knob);
  }

  /** What makes a control for each sort of knob: a window says it once, and each knob picks its own. */
  public interface Controls<T> {
    T choice(Knob knob);

    T switching(Knob knob);

    T number(Knob knob);
  }

  /** The control for this knob, from whatever makes them. */
  public <T> T shownBy(Controls<T> controls) {
    return kind.shownBy(controls, this);
  }

  /** Sets it to a value as it was written down in a settings file. */
  public void setWritten(String written) {
    set(kind.read(written));
  }

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
