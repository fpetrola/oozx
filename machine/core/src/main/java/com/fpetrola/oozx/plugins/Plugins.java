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

  private static final List<java.util.function.Consumer<String>> beforeTakingOut =
      new java.util.concurrent.CopyOnWriteArrayList<>();

  /**
   * Avisado con el id de cada plugin que se va, mientras sus clases todavia cargan. Sobrevive a
   * que se arme otro servicio para otra casa, y quien escucha no puede volver a preguntarle nada
   * a esta clase: quien descarga la tiene tomada.
   */
  public static void beforeTakingOut(java.util.function.Consumer<String> leaving) {
    beforeTakingOut.add(leaving);
  }

  private static final List<Runnable> whenCatalogArrives = new java.util.concurrent.CopyOnWriteArrayList<>();

  /** Avisado cuando llega lo que el archivo publica, que se pide sin hacer esperar a nadie. */
  public static void whenCatalogArrives(Runnable arrived) {
    whenCatalogArrives.add(arrived);
  }

  static void catalogArrived() {
    whenCatalogArrives.forEach(Runnable::run);
  }

  private static final List<java.util.function.Function<java.util.Set<String>, Runnable>> whenHeld =
      new java.util.concurrent.CopyOnWriteArrayList<>();

  /**
   * Preguntado cuando lo que se quiere sacar esta retenido, con todo lo que se iria: quien lo
   * retiene lo suelta y devuelve que hacer cuando se fue, o null.
   */
  public static void whenHeld(java.util.function.Function<java.util.Set<String>, Runnable> release) {
    whenHeld.add(release);
  }

  /**
   * Un plugin puede dejar algo en lo que es de toda la maquina virtual, como el formato del log, y
   * eso sigue pidiendole clases a su cargador despues de que se cerro.
   */
  private static void forgetWhatClosedLoadersLeft() {
    java.util.logging.LogManager logs = java.util.logging.LogManager.getLogManager();
    for (String name : java.util.Collections.list(logs.getLoggerNames())) {
      java.util.logging.Logger logger = logs.getLogger(name);
      if (logger == null) continue;
      for (java.util.logging.Handler handler : logger.getHandlers()) {
        if (closed(handler)) logger.removeHandler(handler);
        else if (closed(handler.getFormatter())) handler.setFormatter(new java.util.logging.SimpleFormatter());
      }
    }
  }

  private static boolean closed(Object made) {
    return made != null && made.getClass().getClassLoader() instanceof org.pf4j.PluginClassLoader loader
        && loader.isClosed();
  }

  /** El plugin que trajo esa extension, o null si la trajo la base. */
  public static String whoBrought(Object extension) {
    if (!areRead()) return null;
    String name = extension.getClass().getName();
    return service().plugins().stream()
        .filter(plugin -> plugin.extensions().stream().anyMatch(one -> one.className().equals(name)))
        .map(dev.crystal.plugins.runtime.PluginInfo::id).findFirst().orElse(null);
  }

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
      service = PluginService.builder()
          .cacheDirectory(Configuration.home().toPath().resolve("plugin-cache"))
          .source(new WhereAPluginComesFrom())
          .defaults(dev.crystal.plugins.runtime.PluginSources.bundled())
          .build();
      service.onChange(Plugins::forgetWhatClosedLoadersLeft);
      service.whenHeld(leaving -> {
        List<Runnable> afterwards = whenHeld.stream().map(release -> release.apply(leaving))
            .filter(java.util.Objects::nonNull).toList();
        return () -> afterwards.forEach(Runnable::run);
      });
      service.beforeUnload(id -> {
        System.err.println("plugin going away: " + id);
        beforeTakingOut.forEach(listener -> listener.accept(id));
      });
      service.start();
      // Arrancar pone lo que el jar trae y lo que ya estaba puesto, sin red: los plugins viajan
      // adentro. Lo que hay en la carpeta es una eleccion que alguien ya hizo, y tambien entra.
      whatIsInTheFolder();
    }
    return service;
  }

  /** Lo unico que se le da a la ventana que los muestra: quien los maneja. */
  public static PluginService managing() {
    return service();
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
        : whatThisBuildBrings();
  }

  /**
   * Con los plugins apagados contesta igual, con lo que el emulador trae adentro: apagarlos es
   * correr sin jars, no correr sin lo de uno. Sin esto, pedir cualquier forma de entrar no se
   * podia responder y no se armaba nada que las pidiera.
   */
  private static com.google.inject.Module whatThisBuildBrings() {
    return binder -> waysIn().forEach(wayIn -> bindEverythingOf(binder, wayIn));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void bindEverythingOf(com.google.inject.Binder binder, Class wayIn) {
    binder.bind((com.google.inject.Key) com.google.inject.Key.get(
        com.google.inject.util.Types.setOf(wayIn))).toProvider(() -> java.util.Set.copyOf(found(wayIn)));
  }

  /** Las formas de entrar que este build declara, dichas por cada modulo en su propio indice. */
  private static List<Class<?>> waysIn() {
    List<Class<?>> declared = new ArrayList<>();
    try {
      java.util.Enumeration<URL> indexes = Plugins.class.getClassLoader()
          .getResources("META-INF/crystal/roles.idx");
      while (indexes.hasMoreElements()) {
        for (String line : new String(indexes.nextElement().openStream().readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
          String named = line.trim();
          if (named.isEmpty() || named.startsWith("#")) continue;
          try {
            declared.add(Class.forName(named, false, Plugins.class.getClassLoader()));
          } catch (ClassNotFoundException notHere) {
            // Lo declaro otro modulo que este build no tiene: no es una forma de entrar de aca.
          }
        }
      }
    } catch (IOException cannotBeRead) {
      TellsThePerson.thisBuildCannot("no se pudo leer que formas de entrar hay: " + cannotBeRead);
    }
    return declared;
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
    // Lo que trae el emulador y lo que llego en un jar, dicho por quien los maneja: una sola
    // instancia de cada uno, armada una vez. Sumarle aca lo que ServiceLoader encuentra daba un
    // segundo lector de cada cosa, y ninguno de los dos era el que estaba andando.
    if (areRead()) return snapshot(wayIn);
    // Con los plugins apagados no hay quien conteste, y lo que el emulador trae sigue estando:
    // apagarlos es correr sin jars, no correr sin lo de uno.
    List<S> carried = new ArrayList<>();
    ServiceLoader.load(wayIn, Plugins.class.getClassLoader()).forEach(carried::add);
    return carried;
  }


  /** Whether what this came from is still in the folder. Anything from elsewhere always counts. */
  private static boolean stillHere(Class<?> type) {
    try {
      CodeSource source = type.getProtectionDomain().getCodeSource();
      if (source == null || source.getLocation() == null) return true;
      Path from = Path.of(source.getLocation().toURI());
      if (!from.startsWith(folder())) return true;
      return Files.exists(from);
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
