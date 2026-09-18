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

package com.fpetrola.oozx.config;

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import java.util.List;
import java.util.Set;

/**
 * Copies config values into machine parts ({@link Speed}, {@link Sound.Output}, etc.) after the machine is built,
 * and back out before the file is written; the machine itself has no reference to this class.
 * A field is null only when neither the file nor the shipped config.json set it.
 */
public final class Settings {
  /** Lets a device's own module contribute its settings sync, since Settings can't know about jars it never imports. */
  public interface Part {
    void into();

    void from();
  }

  /**
   * What something says about the settings it has: the name they are kept under, which they are,
   * and what each one is. Said by a device about its own properties, and by anything else that has
   * settings to show without being a device - the machine itself, whose model and processor are
   * chosen the same way and are not fields of anything.
   */
  public interface Described {
    String name();

    List<String> properties();

    Class<?> typeOf(String property);

    /**
     * The values one of these can take, where it is one of a few known ones: which Spectrum this
     * is, which of its ROM sets. Empty where it is any value of its type, which is most of them.
     */
    default List<?> choicesFor(String property) {
      return List.of();
    }

    /** Whatever was said about one of these in words, for whoever shows it to a person. */
    default String saidAbout(String property) {
      return "";
    }
  }

  /**
   * What a device said about itself: which section of the file it keeps, what holds it, and which
   * of its properties are settings rather than workings. A {@link Part} that also answers what it
   * is about, so that something wanting to show these to a person - rather than copy them - does
   * not need a second list of the same thing.
   */
  public static final class Mirror implements Part, Described {
    private final String name;
    private final Class<?> device;
    private final List<String> properties;
    private final Provider<Configuration> configuration;
    private final Provider<?> held;

    private Mirror(String name, Class<?> device, List<String> properties,
        Provider<Configuration> configuration, Provider<?> held) {
      this.name = name;
      this.device = device;
      this.properties = properties;
      this.configuration = configuration;
      this.held = held;
    }

    public String name() {
      return name;
    }

    public Class<?> device() {
      return device;
    }

    public List<String> properties() {
      return properties;
    }

    /** The one in the machine this was built for, which is what a control has to change. */
    public Object held() {
      return held.get();
    }

    /** What kind of thing a setting is, which is what decides the control that shows it. */
    public Class<?> typeOf(String property) {
      try {
        return Configuration.getter(device, property).getReturnType();
      } catch (NoSuchMethodException noWayToAsk) {
        java.lang.reflect.Field field = Configuration.fieldOf(device, property);
        if (field == null) {
          throw new IllegalStateException(device.getName() + " has no " + property, noWayToAsk);
        }
        return field.getType();
      }
    }

    /** The settings of the device in this machine: changing one changes what is running. */
    public Values live() {
      return new Values() {
        public Object get(String property) {
          try {
            return Configuration.read(held.get(), property);
          } catch (ReflectiveOperationException cannot) {
            throw new IllegalStateException(name + "." + property + " cannot be read", cannot);
          }
        }

        public void set(String property, Object value) {
          configuration.get().set(held.get(), property, value);
        }
      };
    }

    /**
     * The settings as the file has them, for when there is no machine: what one will start with.
     * A property the file says nothing about answers null, and the control shows the default the
     * device itself was written with.
     */
    public Values inTheFile() {
      return new Values() {
        public Object get(String property) {
          return configuration.get().valueOf(name, property, typeOf(property));
        }

        public void set(String property, Object value) {
          configuration.get().setValue(name, property, value);
        }
      };
    }

    public void into() {
      configuration.get().fill(name, held.get(), properties.toArray(new String[0]));
    }

    public void from() {
      configuration.get().put(name, held.get(), properties.toArray(new String[0]));
    }
  }

  /** Where a device's settings sit: in the machine that is running, or in the file. */
  public interface Values {
    Object get(String property);

    void set(String property, Object value);
  }

  /** A device's settings and where they are being read from, which is all a control needs. */
  public record Configurable(Described device, Values values) {
  }

  /**
   * Settings being edited away from whatever they will end up in: every change is kept under the
   * name of the setting until somebody writes them.
   * <p>
   * Live, each change also goes straight into what is in front of it, which is what a window
   * clipped onto a machine does. Written afterwards onto something else - the file, or another
   * machine it was clipped onto since - the same changes land there, so the way to give one
   * machine the settings of another is to carry the window across and write them.
   */
  public static final class Edits {
    private final java.util.Map<String, Object> pending = new java.util.LinkedHashMap<>();

    /** These settings seen through the changes so far, taking them as they are made. */
    public List<Configurable> over(List<Configurable> targets, boolean live) {
      return targets.stream().map(target -> new Configurable(target.device(), new Values() {
        public Object get(String property) {
          String key = key(target, property);
          return pending.containsKey(key) ? pending.get(key) : target.values().get(property);
        }

        public void set(String property, Object value) {
          pending.put(key(target, property), value);
          if (live) {
            target.values().set(property, value);
            // What stuck rather than what was asked for: a part that takes the nearest value it
            // can do something with would otherwise be written the original one next time.
            pending.put(key(target, property), target.values().get(property));
          }
        }
      })).toList();
    }

    /** Writes every change made so far into these settings, leaving the rest as they are. */
    public void applyTo(List<Configurable> targets) {
      for (Configurable target : targets) {
        for (String property : target.device().properties()) {
          String key = key(target, property);
          if (pending.containsKey(key)) {
            target.values().set(property, pending.get(key));
          }
        }
      }
    }

    /**
     * Takes these settings as the changes to be written elsewhere: the window opened on a machine
     * holds that machine's settings, so unclipping it and writing them makes them the defaults,
     * and clipping it onto another machine makes that one like the first.
     */
    public void copyFrom(List<Configurable> from) {
      for (Configurable one : from) {
        for (String property : one.device().properties()) {
          pending.put(key(one, property), one.values().get(property));
        }
      }
    }

    public boolean isEmpty() {
      return pending.isEmpty();
    }

    private static String key(Configurable of, String property) {
      return of.device().name() + "." + property;
    }
  }

  /**
   * Every device that said it has settings, whichever machine it is in. The declaration belongs to
   * the build - the same modules make every machine - so it can be read with no machine at hand,
   * which is what a window showing what a new machine will start with needs.
   */
  private static final java.util.List<Mirror> DECLARED = new java.util.concurrent.CopyOnWriteArrayList<>();

  public static java.util.List<Mirror> declared() {
    return java.util.List.copyOf(DECLARED);
  }

  /**
   * Binds a {@link Part} that syncs the named properties of {@code device} with the config file, so
   * a module needn't write that out itself. The machine's own parts - the speed it runs at, whether
   * its ROMs can be written - say it the same way as anything plugged into it: there used to be a
   * second spelling of this for them, and nothing ever asked which of the two a setting came from.
   */
  public static void mirror(Binder binder, String name, Class<?> device, String... properties) {
    Mirror mirror = new Mirror(name, device, List.of(properties),
        binder.getProvider(Configuration.class), binder.getProvider(device));
    DECLARED.removeIf(declared -> declared.name().equals(name));
    DECLARED.add(mirror);
    Multibinder.newSetBinder(binder, Part.class).addBinding().toInstance(mirror);
  }

  private static final Key<Set<Part>> PARTS = Key.get(new TypeLiteral<Set<Part>>() {});

  private final Configuration configuration;

  private Settings(Configuration configuration) {
    this.configuration = configuration;
  }

  public static Settings load(Configuration configuration) {
    return new Settings(configuration);
  }

  /** Applies file values to the machine's parts (via the injector, so this needs no knowledge of which module holds which) and registers {@link #from} to run on every later save. */
  public void into(Injector injector) {
    mirrors = injector.getInstance(PARTS).stream().filter(Mirror.class::isInstance)
        .map(Mirror.class::cast).sorted(java.util.Comparator.comparing(Mirror::name)).toList();
    injector.getInstance(PARTS).forEach(Part::into);

    gather = () -> from(injector);
    configuration.beforeSave(gather);
  }

  private Runnable gather;
  private List<Mirror> mirrors = List.of();

  /** The devices of this machine that have settings, with their own instances behind them. */
  public List<Configurable> devices() {
    return mirrors.stream().map(mirror -> new Configurable(mirror, mirror.live())).toList();
  }

  /** The same, as the file has them: what a machine that nobody has configured will start with. */
  public static List<Configurable> defaults() {
    return declared().stream().map(mirror -> new Configurable(mirror, mirror.inTheFile())).toList();
  }

  public void letGo() {
    configuration.notBeforeSave(gather);
  }

  public void from(Injector injector) {
    injector.getInstance(PARTS).forEach(Part::from);
  }


}
