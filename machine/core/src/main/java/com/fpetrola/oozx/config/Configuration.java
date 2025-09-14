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
 * Everything this emulator was configured to be, in one file, a section per part.
 * <p>
 * A section is a class and is asked for by that class, so nothing reads the file as a map: what a
 * part receives is its own configuration and only its own.
 * <p>
 * What shipped is the config.json on the classpath, and the file in the home goes over it key by
 * key: a fresh home is what shipped, and adding a setting does not touch a saved file. A section
 * belonging to a build this one is not - a peripheral that is not on this classpath - is written
 * back untouched rather than lost.
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

  /**
   * The one over the file in the home, for the whole process: every machine on the desktop and
   * the desktop itself read and write the same sections, so a save by one cannot put back what
   * another had changed.
   */
  public static synchronized Configuration shared() {
    if (shared == null)
      shared = new Configuration(new File(System.getProperty("user.home"), ".oozx" + File.separator + "config.json"));
    return shared;
  }

  public Configuration(File file) {
    this.file = file;
    this.root = over(packaged(), read(file));
  }

  /** What shipped and nothing else: the config.json on the classpath, which a fresh home starts from. */
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

  /** The file's word over what shipped, key by key down through the objects; a list or a value it names replaces. */
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
      // A file that cannot be read is a file whose settings are lost, which is not worth failing
      // to start over: what it held is written again from the defaults the next time this saves.
      return json.createObjectNode();
    }
  }

  /**
   * This build's configuration for that part, made from its defaults and whatever the file had.
   * The same instance every time it is asked for, because a setting someone changes is the same
   * setting the part it belongs to reads.
   */
  @SuppressWarnings("unchecked")
  public <T> T of(Class<T> type) {
    return (T) sections.computeIfAbsent(type, this::load);
  }

  /** Handed to a section that has to save, since saving is the file's and not the section's. */
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

  /**
   * What the file says under that name into the value's setters, a property at a time: a key the
   * file leaves out keeps whatever the value was built with.
   */
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

  /** The value's properties as they stand, read from their getters, under that name for the next save. */
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

  /**
   * Where a section sits in the file. A name says the path to it, so the machines' sections are
   * inside the machine's rather than beside it under a name with a dot in it.
   */
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

  /** Writes back what was read, with every section this build has asked for as it now stands. */
  private final List<Runnable> beforeSaving = new ArrayList<>();

  /** Run before the file is written: how what a machine holds gets back into the sections. */
  public void beforeSave(Runnable gather) {
    beforeSaving.add(gather);
  }

  /** A machine that is gone stops putting its values into the file: the ones still there do. */
  public void notBeforeSave(Runnable gather) {
    beforeSaving.remove(gather);
  }

  public void save() {
    beforeSaving.forEach(Runnable::run);
    // Merged into what is there rather than put over it: a section can hold others, as the
    // machine's holds each machine's, and writing one must not take the rest with it.
    sections.forEach((type, section) -> {
      ObjectNode written = (ObjectNode) json.valueToTree(section);
      // A section that was the whole file leaves the root as it goes under its name. Keeping both
      // copies would leave the file saying everything twice, and only one of them read again.
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

  /**
   * Declares a section, so that the part it belongs to receives it and nothing else does. Each
   * module says which sections it brings, the way it says which peripherals it brings.
   */
  public static <T> void section(Binder binder, Class<T> type) {
    Provider<Configuration> configuration = binder.getProvider(Configuration.class);
    binder.bind(type).toProvider(() -> configuration.get().of(type)).in(Singleton.class);
  }
}
