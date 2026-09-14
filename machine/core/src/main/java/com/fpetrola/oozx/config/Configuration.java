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
      Method setter = setter(value, property);
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
        node.set(property, json.valueToTree(value.getClass().getMethod(property).invoke(value)));
      } catch (ReflectiveOperationException cannot) {
        throw new IllegalStateException(name + "." + property + " cannot be read", cannot);
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

  private final List<Runnable> beforeSaving = new ArrayList<>();

  /** Registers a callback to pull a machine's live values back into its section just before {@link #save}. */
  public void beforeSave(Runnable gather) {
    beforeSaving.add(gather);
  }

  public void notBeforeSave(Runnable gather) {
    beforeSaving.remove(gather);
  }

  public void save() {
    beforeSaving.forEach(Runnable::run);
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
