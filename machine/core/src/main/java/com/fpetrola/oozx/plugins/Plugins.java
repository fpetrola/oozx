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

package com.fpetrola.oozx.plugins;

import com.fpetrola.oozx.config.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * What this build can be beyond what it was compiled with: the jars in the plugin folder, read by
 * a loader of their own.
 * <p>
 * What can arrive that way is whatever answers to an interface marked {@link Plugin}, and
 * {@link #found} is the only way to ask for them.
 * <p>
 * Everything the emulator finds rather than names - a device's {@code Extension}, its window's
 * {@code Equipment} - is looked up through here instead of through the application's own loader,
 * so a peripheral is something dropped in rather than something this jar carries. The parent is
 * this class's own loader, so a plugin sees the machine it plugs into and the machine never sees
 * two of anything: the core classes come from one place whoever asks.
 * <p>
 * A build that still carries its devices keeps working: a service on the application's classpath
 * is found through the parent like it always was, and the folder merely adds to it.
 */
public final class Plugins {

  private static Boards loader;

  private Plugins() {
  }

  /** Where a jar is dropped for this emulator to find it. */
  public static Path folder() {
    return Configuration.home().toPath().resolve("plugins");
  }

  /**
   * The loader over whatever is in the folder right now. Made once: the classes a plugin brings
   * are bound into machines and hung on windows, and two loaders would make two of each.
   */
  public static synchronized ClassLoader loader() {
    if (loader == null) {
      loader = new Boards(urlsOf(jars()));
    }
    return loader;
  }

  /**
   * One more jar, while the emulator is running. What it brings is in the Equipment menu at once
   * and in every machine built from here on; a machine that was already made was made without it.
   */
  public static synchronized void add(Path jar) {
    try {
      ((Boards) loader()).take(jar.toUri().toURL());
    } catch (MalformedURLException notAUrl) {
      System.err.println("oozx: " + jar + " could not be read: " + notAUrl.getMessage());
    }
  }

  /** A loader that can be given something after it was made, since a board can arrive at any time. */
  private static final class Boards extends URLClassLoader {
    Boards(URL[] jars) {
      super("plugins", jars, Plugins.class.getClassLoader());
    }

    void take(URL jar) {
      for (URL had : getURLs()) {
        if (had.equals(jar)) return;
      }
      addURL(jar);
    }
  }

  /**
   * Whether plugins are looked at at all. Off while the build specialises the model and while the
   * tests run: what those two produce is this tree, never whatever somebody has installed.
   */
  public static boolean areRead() {
    return !"off".equals(System.getProperty("oozx.plugins"));
  }

  /**
   * The jars themselves, on the way up: what was taken out is thrown away first, so this is
   * both what is installed and the moment the folder is tidied.
   */
  public static List<File> jars() {
    if (!areRead()) return List.of();
    PluginReleases.sweep();
    return inFolder();
  }

  /**
   * What is in the folder, asked without tidying it. For whoever wants to say what this
   * emulator has while it is running: sweeping then would delete a jar the loader still holds
   * open, which is the one thing {@link PluginReleases#takeOut} exists to put off.
   */
  public static List<File> inFolder() {
    if (!areRead()) return List.of();
    File[] found = folder().toFile().listFiles(file -> file.getName().endsWith(".jar"));
    if (found == null) return List.of();
    List<File> jars = new ArrayList<>(Arrays.asList(found));
    jars.sort(File::compareTo);
    return jars;
  }

  /**
   * One thing that is plugged in: what it answers to, what that kind is called, which class it
   * is, and which jar it came in.
   */
  public record WhatIsIn(String wayIn, String kind, String implementation, String from) {
  }

  /**
   * Everything that is plugged in right now, read from what each jar says it brings rather than
   * by loading any of it.
   * <p>
   * The service files are the answer: one per way in, naming what answers to it. Reading the
   * names is enough to say what a jar brought, and nothing is built to find out - which matters,
   * because half of what is listed would want a machine to be built with.
   */
  public static List<WhatIsIn> everythingPluggedIn() {
    List<WhatIsIn> everything = new ArrayList<>();
    for (File jar : inFolder()) {
      everything.addAll(whatIsIn(jar));
    }
    for (File jar : whatThisBuildCarries()) {
      everything.addAll(whatIsIn(jar));
    }
    return everything;
  }

  /** What one jar says it brings: its service files, read as names and not as classes. */
  static List<WhatIsIn> whatIsIn(File jar) {
    List<WhatIsIn> inside = new ArrayList<>();
    String where = jar.getName();
    try (java.util.jar.JarFile opened = new java.util.jar.JarFile(jar)) {
      for (java.util.Enumeration<java.util.jar.JarEntry> entries = opened.entries();
           entries.hasMoreElements(); ) {
        java.util.jar.JarEntry entry = entries.nextElement();
        String named = entry.getName();
        if (!named.startsWith("META-INF/services/") || entry.isDirectory()) {
          continue;
        }
        String wayIn = named.substring("META-INF/services/".length());
        String kind = kindOf(wayIn);
        if (kind == null) {
          continue;
        }
        try (java.io.BufferedReader lines = new java.io.BufferedReader(
            new java.io.InputStreamReader(opened.getInputStream(entry)))) {
          for (String line = lines.readLine(); line != null; line = lines.readLine()) {
            String answering = line.split("#")[0].trim();
            if (!answering.isEmpty()) {
              inside.add(new WhatIsIn(wayIn, kind, answering, where));
            }
          }
        }
      }
    } catch (IOException cannotBeRead) {
      System.err.println("oozx: " + where + " could not be read: " + cannotBeRead.getMessage());
    }
    return inside;
  }

  /**
   * What this kind of thing is called, or null when the interface is not a way in at all - a
   * jar may serve anything through META-INF/services, and only these are plugins.
   */
  private static String kindOf(String wayIn) {
    try {
      Class<?> type = Class.forName(wayIn, false, loader());
      Plugin mark = type.getAnnotation(Plugin.class);
      return mark == null ? null : mark.value();
    } catch (ClassNotFoundException | LinkageError notHere) {
      return null;
    }
  }

  /** The jars this build was made of, which is where what ships rather than arrives comes from. */
  private static List<File> whatThisBuildCarries() {
    List<File> jars = new ArrayList<>();
    for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
      File file = new File(entry);
      if (file.isFile() && entry.endsWith(".jar")) {
        jars.add(file);
      }
    }
    return jars;
  }

  /**
   * Whatever answers to this service, in the folder and on the classpath alike - less whatever
   * came out of a jar that is not in the folder any more.
   * <p>
   * A jar taken out cannot be taken out of the loader: the classes it brought are in machines and
   * on windows, and a loader made again would hand out second copies of them to whatever is built
   * next. So it stays loaded and stops counting, which is the difference between what this build
   * can do and what it happens to be holding.
   */
  public static <S> List<S> found(Class<S> service) {
    // A way in says so. Asking for anything else is a mistake worth hearing about: the answer
    // would be an empty list, which is also what a misspelt service file gives, and that was
    // twenty minutes of looking for a board that was there all along.
    if (!service.isAnnotationPresent(Plugin.class)) {
      throw new IllegalArgumentException(service.getName() + " is not a way in: it is not @Plugin");
    }
    List<S> answering = new ArrayList<>();
    Iterator<ServiceLoader.Provider<S>> providers =
        ServiceLoader.load(service, loader()).stream().iterator();
    // A jar deleted by hand while this runs is still in the loader, and reading its service file
    // throws: one that cannot be read is skipped rather than allowed to take everything with it.
    for (int guard = 0; guard < 10_000; guard++) {
      try {
        if (!providers.hasNext()) break;
        ServiceLoader.Provider<S> provider = providers.next();
        if (stillHere(provider.type())) answering.add(provider.get());
      } catch (ServiceConfigurationError cannotBeRead) {
        System.err.println("oozx: a plugin could not be read: " + cannotBeRead.getMessage());
      }
    }
    return answering;
  }

  /** Whether what this came from is still in the folder. Anything from elsewhere always counts. */
  private static boolean stillHere(Class<?> type) {
    try {
      CodeSource source = type.getProtectionDomain().getCodeSource();
      if (source == null || source.getLocation() == null) return true;
      Path from = Path.of(source.getLocation().toURI());
      if (!from.startsWith(folder())) return true;
      return Files.exists(from) && !PluginReleases.isOut(from.getFileName().toString());
    } catch (URISyntaxException | IllegalArgumentException notAFile) {
      return true;
    }
  }

  private static URL[] urlsOf(List<File> jars) {
    List<URL> urls = new ArrayList<>();
    for (File jar : jars) {
      try {
        urls.add(jar.toURI().toURL());
      } catch (MalformedURLException notAUrl) {
        System.err.println("oozx: " + jar + " could not be read: " + notAUrl.getMessage());
      }
    }
    return urls.toArray(new URL[0]);
  }
}
