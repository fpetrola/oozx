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

  /** Para que carpeta se armo, porque uno armado para otra miente sobre lo que hay. */
  private static Path servingFolder;

  private static synchronized PluginService service() {
    if (service != null && !folder().equals(servingFolder)) {
      // Cambio la casa: la de antes puede no existir mas, y lo que decia era de otro lugar.
      try {
        service.close();
      } catch (RuntimeException wouldNotClose) {
        TellsThePerson.that("lo anterior no se pudo cerrar: " + wouldNotClose.getMessage());
      }
      service = null;
    }
    if (service == null) {
      servingFolder = folder();
      jars();
      service = PluginService.builder()
          .cacheDirectory(Configuration.home().toPath().resolve("plugin-cache"))
          .source(new WhereAPluginComesFrom())
          .build();
      service.start();
      // Arrancar carga lo que ya estaba puesto y nada mas, a proposito: que un emulador recien
      // instalado no se baje nada es del que lo carga. Lo que hay en la carpeta es una eleccion
      // que ya se hizo, asi que eso si se enchufa.
      whatIsInTheFolder();
    }
    return service;
  }

  /**
   * Lo que esta cargado ahora mismo, dicho por quien los carga.
   * <p>
   * No es lo que hay en la carpeta: uno que arranco puede no estar mas ahi, porque quien lo
   * carga se queda con su copia. La ventana decia lo de la carpeta, asi que mostraba una lista
   * que no era la verdad - andaba lo que no figuraba.
   */
  public static java.util.List<String> pluggedIn() {
    if (!areRead()) return java.util.List.of();
    return service().plugins().stream()
        .filter(one -> String.valueOf(one.status()).equals("STARTED"))
        .map(dev.crystal.plugins.runtime.PluginInfo::id).toList();
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
  public static synchronized void add(String id) {
    plugIn(id);
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

  /**
   * Lo saca de verdad: deja de estar cargado y deja de contar en el acto, en vez de sacarlo de
   * una lista y dejarlo corriendo. Renombrar el jar no alcanzaba - el que los carga ya lo tenia
   * en su cache y arrancado, asi que el menu seguia teniendo lo que se acababa de sacar.
   *
   * @return si se pudo, que es que no, cuando algo que corre lo esta reteniendo
   */
  public static synchronized boolean takeOut(String id) {
    if (id == null || !areRead()) return false;
    // Se pregunta antes de intentar: quien lo carga sabe si algo lo esta reteniendo, y eso no es
    // una excepcion que haya que atrapar sino una respuesta.
    List<String> holding = whoIsHolding(id);
    if (holding.isEmpty()) {
      service().uninstall(id);
      generation++;
      return true;
    }
    // Una maquina de esta corrida lo uso, y eso no se suelta al cerrarla: sus clases estan en lo
    // que se construyo con ella. Se lo saca del conjunto instalado y sigue andando hasta que el
    // emulador vuelva a arrancar, que es la unica verdad que se le puede decir a alguien.
    service().uninstallOnNextStart(id);
    TellsThePerson.thisBuildCannot(nameOf(id) + " lo esta usando una maquina de esta sesion, asi"
        + " que se va cuando vuelvas a arrancar el emulador.");
    return false;
  }

  /** Si se puede sacar ahora mismo, para decirlo antes y no como el resultado de intentarlo. */
  public static boolean canBeTakenOutNow(String id) {
    return areRead() && whoIsHolding(id).isEmpty();
  }

  /**
   * Quien lo retiene, sin contar lo que ya se cerro.
   * <p>
   * Una maquina cerrada deja de alcanzarse pero sigue existiendo hasta que la recolecten, y
   * hasta entonces cuenta como que lo esta usando. Se le pide a la maquina virtual que limpie
   * antes de contestar, asi cerrar una ventana alcanza para poder sacar lo que esa ventana usaba.
   */
  private static List<String> whoIsHolding(String id) {
    List<String> holding = service().heldBy(id);
    if (holding.isEmpty()) {
      return holding;
    }
    System.gc();
    return service().heldBy(id);
  }

  /** Lo que ya no va a estar la proxima vez, aunque todavia ande. */
  public static java.util.Set<String> goingOnTheNextStart() {
    return areRead() ? service().pendingRemovals() : java.util.Set.of();
  }


  /** El archivo de la carpeta que trae ese plugin, si todavia esta ahi. */
  public static File jarOf(String id) {
    for (File jar : inFolder()) {
      if (id.equals(idOf(jar))) return jar;
    }
    return null;
  }

  /** El id sin el prefijo con que se lo publica, que es como lo nombra una persona. */
  private static String nameOf(String id) {
    return id.replaceFirst("^(device|tool)-", "");
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
    service();
    return whatIsInTheFolder();
  }

  /** @return si algo de la carpeta no estaba enchufado todavia */
  private static boolean whatIsInTheFolder() {
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
