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

import dev.crystal.plugins.api.RoleInterface;
import dev.crystal.plugins.runtime.PluginService;
import dev.crystal.plugins.runtime.PluginSources;

import com.fpetrola.oozx.TellsThePerson;

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

  /**
   * Lo que los carga, que ya no es nuestro. Este es el unico lugar del programa que lo nombra:
   * de aca para abajo, quien necesita algo que llego en un jar lo recibe inyectado y no sabe de
   * donde salio.
   */
  private static PluginService service;

  /**
   * How many times the folder has grown. Whoever keeps something worked out from what is plugged
   * in compares this against what it had, and works it out again when it moved.
   */
  private static volatile int generation;

  /** Which lot of plugins this is: it changes when a jar arrives while the emulator runs. */
  public static int generation() {
    return generation;
  }

  private Plugins() {
  }

  /** Where a jar is dropped for this emulator to find it. */
  public static Path folder() {
    return Configuration.home().toPath().resolve("plugins");
  }

  private static synchronized PluginService service() {
    if (service == null) {
      jars();
      service = PluginService.builder()
          .cacheDirectory(Configuration.home().toPath().resolve("plugin-cache"))
          .source(PluginSources.directory(folder()))
          .build();
      service.start();
    }
    return service;
  }

  /**
   * Lo que hay de esa clase ahora mismo, para quien esta armando un injector y no puede esperar.
   * <p>
   * Apagados, nada de esto llega a existir: lo que el build usa para especializar el modelo, y lo
   * que corre en los tests, es este arbol y nunca lo que alguien tenga instalado.
   */
  public static <T> java.util.List<T> snapshot(Class<T> role) {
    return areRead() ? service().snapshot(role) : java.util.List.of();
  }

  /** Lo que hace que un injector nuestro sepa de los roles sin que nadie los vaya a buscar. */
  public static com.google.inject.Module asModule() {
    return areRead() ? dev.crystal.plugins.guice.PluginsModule.of(service())
        : binder -> { };
  }

  /** Arma algo que retiene plugins, para que no se los saque de abajo mientras corre. */
  public static <T> T building(java.util.function.Supplier<T> build) {
    return areRead() ? service().building(build) : build.get();
  }

  /**
   * One more jar, while the emulator is running. What it brings is in the Equipment menu at once
   * and in every machine built from here on; a machine that was already made was made without it.
   */
  public static synchronized void add(Path jar) {
    plugIn(idOf(jar.toFile()));
  }

  /** Como se llama un jar, que es por lo que se lo pide. */
  private static String idOf(File jar) {
    try (java.util.jar.JarFile opened = new java.util.jar.JarFile(jar)) {
      java.util.jar.Manifest manifest = opened.getManifest();
      return manifest == null ? null : manifest.getMainAttributes().getValue("Plugin-Id");
    } catch (IOException cannotBeRead) {
      return null;
    }
  }

  /** @return si no estaba ya puesto */
  private static boolean plugIn(String id) {
    if (id == null || !areRead()) return false;
    try {
      if (service().install(id).isEmpty()) return false;
      generation++;
      return true;
    } catch (RuntimeException wouldNotGo) {
      TellsThePerson.thisBuildCannot(id + " could not be plugged in: " + wouldNotGo.getMessage());
      return false;
    }
  }

  /**
   * Whatever turned up in the folder and nobody has read yet: a jar copied in by hand is plugged
   * in the same as one brought through the window, and it used to sit there listed as being in
   * while nothing it brought was in any menu until the emulator was started again.
   *
   * @return whether anything was read, so that whoever shows a menu can build it again
   */
  public static synchronized boolean readWhatArrived() {
    if (!areRead()) return false;
    boolean anythingNew = false;
    for (File jar : inFolder()) {
      anythingNew |= plugIn(idOf(jar));
    }
    return anythingNew;
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
  public record WhatIsIn(String wayIn, String implementation, String from) {
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
        if (!isAWayIn(wayIn)) {
          continue;
        }
        try (java.io.BufferedReader lines = new java.io.BufferedReader(
            new java.io.InputStreamReader(opened.getInputStream(entry)))) {
          for (String line = lines.readLine(); line != null; line = lines.readLine()) {
            String answering = line.split("#")[0].trim();
            if (!answering.isEmpty()) {
              inside.add(new WhatIsIn(wayIn, answering, where));
            }
          }
        }
      }
    } catch (IOException cannotBeRead) {
      TellsThePerson.thisBuildCannot(where + " could not be read: " + cannotBeRead.getMessage());
    }
    return inside;
  }

  /**
   * What this kind of thing is called, or null when the interface is not a way in at all - a
   * jar may serve anything through META-INF/services, and only these are plugins.
   */
  private static boolean isAWayIn(String wayIn) {
    try {
      return Class.forName(wayIn, false, Plugins.class.getClassLoader())
          .isAnnotationPresent(RoleInterface.class);
    } catch (ClassNotFoundException | LinkageError notHere) {
      return false;
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
   * Lo que responde a esa forma de entrar: lo que el build trae adentro y lo que trajeron los
   * jars, que no son la misma fuente. Queda para lo que todavia no recibe por inyeccion.
   */
  public static <S> List<S> found(Class<S> wayIn) {
    // Una forma de entrar lo dice. Pedir otra cosa es un error que conviene escuchar: la
    // respuesta seria una lista vacia, que es lo mismo que da un service file mal escrito.
    if (!wayIn.isAnnotationPresent(RoleInterface.class)) {
      throw new IllegalArgumentException(
          wayIn.getName() + " is not a way in: it is not @RoleInterface");
    }
    List<S> answering = new ArrayList<>();
    java.util.Set<Class<?>> already = new java.util.HashSet<>();
    // Lo que viene adentro del emulador esta en su classpath y no en ningun jar enchufado.
    for (S carried : ServiceLoader.load(wayIn, Plugins.class.getClassLoader())) {
      if (already.add(carried.getClass())) answering.add(carried);
    }
    for (S pluggedIn : snapshot(wayIn)) {
      if (already.add(pluggedIn.getClass())) answering.add(pluggedIn);
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
        TellsThePerson.thisBuildCannot(jar + " could not be read: " + notAUrl.getMessage());
      }
    }
    return urls.toArray(new URL[0]);
  }
}
