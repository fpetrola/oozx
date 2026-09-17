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
   * What a device said about itself: which section of the file it keeps, what holds it, and which
   * of its properties are settings rather than workings. A {@link Part} that also answers what it
   * is about, so that something wanting to show these to a person - rather than copy them - does
   * not need a second list of the same thing.
   */
  public static final class Mirror implements Part {
    private final String name;
    private final Class<?> device;
    private final List<String> properties;
    private final Provider<Configuration> configuration;
    private final Provider<?> held;
    private final boolean ofTheMachine;

    private Mirror(String name, Class<?> device, List<String> properties,
        Provider<Configuration> configuration, Provider<?> held, boolean ofTheMachine) {
      this.ofTheMachine = ofTheMachine;
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

    /** Whether this is the machine's own - its speed, its sound - rather than something plugged in. */
    public boolean ofTheMachine() {
      return ofTheMachine;
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
  public record Configurable(Mirror device, Values values) {
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

  /** Binds a {@link Part} that syncs the named properties of {@code device} with the config file, so a module needn't write that out itself. */
  public static void mirror(Binder binder, String name, Class<?> device, String... properties) {
    declare(binder, name, device, false, properties);
  }

  /**
   * The same for a part of the machine itself rather than something plugged into it - the speed it
   * runs at, whether its ROMs can be written - so that what the file holds is said once, where the
   * part is bound, instead of being copied by hand in here.
   */
  public static void mirrorOfTheMachine(Binder binder, String name, Class<?> device, String... properties) {
    declare(binder, name, device, true, properties);
  }

  private static void declare(Binder binder, String name, Class<?> device, boolean ofTheMachine,
      String... properties) {
    Mirror mirror = new Mirror(name, device, List.of(properties),
        binder.getProvider(Configuration.class), binder.getProvider(device), ofTheMachine);
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
