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
package com.fpetrola.oozx.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.lang.ref.WeakReference;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The config file, one {@link Section}-annotated class per part; a part is looked up by its class, never as a raw map.
 * The home file's values overlay the classpath's shipped config.json key by key, so a fresh home matches what shipped
 * and an unrecognized section (e.g. from a peripheral not on this build's classpath) is preserved rather than dropped.
 */
@Singleton
public class Configuration {
  private static final ObjectMapper json = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .enable(SerializationFeature.INDENT_OUTPUT);


  private final File file;
  private final ObjectNode root;
  private final Map<Class<?>, Object> sections = new LinkedHashMap<>();
  private final Map<String, Class<?>> named = new LinkedHashMap<>();

  private static Configuration shared;

  /** Process-wide, so every machine and the desktop share the same sections and a save by one can't undo another's change. */
  public static synchronized Configuration shared() {
    if (shared == null)
      shared = new Configuration(new File(home(), "config.json"));
    return shared;
  }

  /** Where this emulator keeps what belongs to the person using it rather than to the build. */
  public static File home() {
    return new File(System.getProperty("user.home"), ".oozx");
  }

  public Configuration(File file) {
    this.file = file;
    this.root = over(packaged(), read(file));
  }

  /** The classpath's config.json alone, with no home file overlay. */
  public static Configuration shipped() {
    return new Configuration(null);
  }

  private static ObjectNode packaged() {
    try (InputStream packaged = Configuration.class.getResourceAsStream("/config.json")) {
      JsonNode read = packaged == null ? null : json.readTree(packaged);
      return read instanceof ObjectNode object ? object : json.createObjectNode();
    } catch (IOException cannot) {
      throw new UncheckedIOException(cannot);
    }
  }

  /** Recursive merge: nested objects merge key by key, any other value (including a list) replaces the base's. */
  private static ObjectNode over(ObjectNode base, ObjectNode file) {
    file.fields().forEachRemaining(entry -> {
      if (base.get(entry.getKey()) instanceof ObjectNode mine && entry.getValue() instanceof ObjectNode theirs) over(mine, theirs);
      else base.set(entry.getKey(), entry.getValue());
    });
    return base;
  }


  private static ObjectNode read(File file) {
    try {
      JsonNode read = file != null && file.isFile() ? json.readTree(file) : null;
      return read instanceof ObjectNode object ? object : json.createObjectNode();
    } catch (IOException unreadable) {
      // Falls back to defaults rather than failing startup; the next save rewrites the file anyway.
      return json.createObjectNode();
    }
  }

  /** Same instance on every call, so changes made through one reference are visible through another. */
  @SuppressWarnings("unchecked")
  public <T> T of(Class<T> type) {
    return (T) sections.computeIfAbsent(type, this::load);
  }

  public interface Saves {
    void savedBy(Configuration configuration);
  }

  private Object load(Class<?> type) {
    String name = nameOf(type);
    Class<?> taken = named.putIfAbsent(name, type);
    if (taken != null && taken != type)
      throw new IllegalStateException(taken.getName() + " and " + type.getName() + " both call themselves " + name);
    try {
      Object section = type.getDeclaredConstructor().newInstance();
      JsonNode saved = at(name);
      if (saved == null && type.getAnnotation(Section.class).wasTheWholeFile())
        saved = root;
      if (saved != null && !saved.isEmpty())
        section = json.readerForUpdating(section).readValue(saved);
      if (section instanceof Saves saves)
        saves.savedBy(this);
      return section;
    } catch (ReflectiveOperationException | IOException cannot) {
      throw new IllegalStateException("the " + name + " configuration cannot be read", cannot);
    }
  }

  /** Calls the setter for each listed property found under {@code name} in the file; a missing key leaves the built-in default. */
  public void fill(String name, Object value, String... properties) {
    JsonNode saved = at(name);
    if (saved == null) return;
    for (String property : properties) {
      JsonNode node = saved.get(property);
      if (node == null) continue;
      Method setter = setterOrNull(value, property);
      if (setter == null) {
        java.lang.reflect.Field field = fieldOf(value.getClass(), property);
        try {
          field.set(value, json.convertValue(node, field.getType()));
        } catch (IllegalAccessException cannot) {
          throw new IllegalStateException(name + "." + property + " cannot be set", cannot);
        }
        continue;
      }
      try {
        setter.invoke(value, json.convertValue(node, setter.getParameterTypes()[0]));
      } catch (ReflectiveOperationException cannot) {
        throw new IllegalStateException(name + "." + property + " cannot be set", cannot);
      }
    }
  }

  public void put(String name, Object value, String... properties) {
    ObjectNode node = make(name);
    for (String property : properties)
      try {
        node.set(property, json.valueToTree(read(value, property)));
      } catch (ReflectiveOperationException cannot) {
        throw new IllegalStateException(name + "." + property + " cannot be read", cannot);
      }
  }

  /** One property of a section as the file has it, converted to what the device would hold. */
  public Object valueOf(String name, String property, Class<?> type) {
    JsonNode saved = at(name);
    JsonNode node = saved == null ? null : saved.get(property);
    try {
      return node == null ? null : json.convertValue(node, type);
    } catch (IllegalArgumentException notThatKind) {
      // What the file has is not what the device holds any more. Answering nothing lets whoever
      // asked show the device's own default rather than fail over a line somebody wrote by hand.
      return null;
    }
  }

  /** Writes one property of a section, for a setting being changed where no machine is running. */
  public void setValue(String name, String property, Object value) {
    make(name).set(property, json.valueToTree(value));
  }

  /** Reads one property of a part, whether it is written as a field or as any of the usual getters. */
  public static Object read(Object held, String property) throws ReflectiveOperationException {
    try {
      return getter(held.getClass(), property).invoke(held);
    } catch (NoSuchMethodException noWayToAsk) {
      return fieldOf(held.getClass(), property).get(held);
    }
  }

  /**
   * The method a property is read by: its own name, or the is/get of a bean. Three spellings
   * because the machine, the devices and the parts that came from a schema were each written in
   * their own, and a setting is a setting in all three.
   */
  static Method getter(Class<?> type, String property) throws NoSuchMethodException {
    String capitalised = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    for (String name : new String[] {property, "is" + capitalised, "get" + capitalised}) {
      try {
        return type.getMethod(name);
      } catch (NoSuchMethodException next) {
        // The next spelling.
      }
    }
    throw new NoSuchMethodException(type.getName() + " has no " + property);
  }

  /** Sets one property on a part that is running, by the field or the setter its name says it has. */
  public void set(Object held, String property, Object value) {
    try {
      // What the part offers to be told comes first, and the field only when it offers nothing:
      // a part that has a setter has it because being set means more than the value changing.
      Method setter = setterOrNull(held, property);
      if (setter != null) {
        setter.invoke(held, value);
      } else {
        fieldOf(held.getClass(), property).set(held, value);
      }
    } catch (ReflectiveOperationException cannot) {
      throw new IllegalStateException(property + " cannot be set on " + held.getClass().getName(), cannot);
    }
  }

  private static Method setterOrNull(Object value, String property) {
    String name = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
    return Arrays.stream(value.getClass().getMethods())
        .filter(m -> m.getName().equals(name) && m.getParameterCount() == 1).findFirst().orElse(null);
  }

  /**
   * The field a property is, for the parts that are written as plain fields rather than as a pair
   * of accessors - which is most of the machine's own: {@code speed.emulation} is a field and
   * {@code covox.volume()} is a method, and a setting is a setting either way.
   */
  static java.lang.reflect.Field fieldOf(Class<?> type, String property) {
    try {
      java.lang.reflect.Field field = type.getField(property);
      return java.lang.reflect.Modifier.isStatic(field.getModifiers()) ? null : field;
    } catch (NoSuchFieldException notAField) {
      return null;
    }
  }

  private static Method setter(Object value, String property) {
    String name = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
    return Arrays.stream(value.getClass().getMethods())
        .filter(m -> m.getName().equals(name) && m.getParameterCount() == 1).findFirst()
        .orElseThrow(() -> new IllegalStateException(value.getClass().getName() + " has no " + name));
  }

  /** A dotted name is a path, so e.g. a machine's section nests inside "machine" rather than living beside it. */
  private JsonNode at(String name) {
    JsonNode node = root;
    for (String step : name.split("\\."))
      if ((node = node.get(step)) == null)
        return null;
    return node;
  }

  private ObjectNode make(String name) {
    ObjectNode node = root;
    for (String step : name.split("\\.")) {
      JsonNode next = node.get(step);
      node = next instanceof ObjectNode object ? object : node.putObject(step);
    }
    return node;
  }

  /**
   * What to pull back into the sections before saving, held weakly on purpose.
   * <p>
   * There is one configuration for the whole program and every machine ever built registers with
   * it, so holding these strongly would mean a machine nobody is using any more is kept alive by
   * having once asked to be saved. Whoever registers one keeps it for as long as it wants it to
   * run: {@link com.fpetrola.oozx.config.Settings} holds its own, and lives as long as its machine.
   */
  private final List<WeakReference<Runnable>> beforeSaving = new ArrayList<>();

  /** Registers a callback to pull a machine's live values back into its section just before {@link #save}. */
  public void beforeSave(Runnable gather) {
    letGoOfWhatIsGone();
    beforeSaving.add(new WeakReference<>(gather));
  }

  public void notBeforeSave(Runnable gather) {
    beforeSaving.removeIf(held -> held.get() == null || held.get() == gather);
  }

  private void letGoOfWhatIsGone() {
    beforeSaving.removeIf(held -> held.get() == null);
  }

  public void save() {
    letGoOfWhatIsGone();
    for (WeakReference<Runnable> held : List.copyOf(beforeSaving)) {
      Runnable gather = held.get();
      if (gather != null) gather.run();
    }
    // Merged rather than overwritten, since a section (e.g. the machine's) can nest others inside it.
    sections.forEach((type, section) -> {
      ObjectNode written = (ObjectNode) json.valueToTree(section);
      // A wasTheWholeFile section also owns the root's top-level keys; strip them so the data isn't duplicated.
      if (type.getAnnotation(Section.class).wasTheWholeFile())
        written.fieldNames().forEachRemaining(root::remove);
      make(nameOf(type)).setAll(written);
    });
    if (file == null) return;
    try {
      file.getParentFile().mkdirs();
      json.writeValue(file, root);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static String nameOf(Class<?> type) {
    Section section = type.getAnnotation(Section.class);
    if (section == null)
      throw new IllegalStateException(type.getName() + " is a configuration class and does not say what section it is");
    return section.value();
  }

  /** Binds a section type so only the module that declares it can inject it, mirroring how modules declare their peripherals. */
  public static <T> void section(Binder binder, Class<T> type) {
    Provider<Configuration> configuration = binder.getProvider(Configuration.class);
    binder.bind(type).toProvider(() -> configuration.get().of(type)).in(Singleton.class);
  }
}
