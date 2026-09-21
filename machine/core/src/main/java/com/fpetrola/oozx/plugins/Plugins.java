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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

/**
 * What this build can be beyond what it was compiled with: the jars in the plugin folder, read by
 * a loader of their own.
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

  /** The jars themselves, for whoever wants to say what was found rather than use it. */
  public static List<File> jars() {
    File[] found = folder().toFile().listFiles(file -> file.getName().endsWith(".jar"));
    if (found == null) return List.of();
    List<File> jars = new ArrayList<>(Arrays.asList(found));
    jars.sort(File::compareTo);
    return jars;
  }

  /** Whatever answers to this service, in the folder and on the classpath alike. */
  public static <S> ServiceLoader<S> found(Class<S> service) {
    return ServiceLoader.load(service, loader());
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
