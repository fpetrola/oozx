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

import com.fpetrola.oozx.TellsThePerson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.config.Section;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The boards that are published, and bringing one in.
 * <p>
 * Each is a release of its own with one jar in it, so the list is a question to an archive and
 * taking one is a download. Nothing here decides to do either: somebody asks for the list and
 * picks what to add, which is the only consent a download needs.
 */
@Section("plugins")
public class PluginReleases implements Configuration.Saves {

  private static final String WHERE_THEY_ARE = "fpetrola/oozx-plugins";

  /**
   * Where they used to be published, before they were a repository of their own. Somebody who
   * ran this before that has it written down in their settings, and settings are read over the
   * default: without this they would go on asking the emulator's own releases, which no longer
   * carry any, and see an empty list with nothing saying why.
   */
  private static final String WHERE_THEY_USED_TO_BE = "fpetrola/oozx";

  /** Where the boards are published: the releases of this repository whose tag names a device. */
  public String repository = WHERE_THEY_ARE;

  /**
   * Which asset each jar here came from. Only says whether what is published is a different
   * build from the copy here - whether a board is here at all is the folder's to say. Kept
   * under this name because it is in the file: renaming it would quietly lose what it knows.
   */
  public Map<String, Long> brought = new LinkedHashMap<>();

  /**
   * The ones taken out while the emulator was running. The file stays where it is until the next
   * start, because the loader holds it open and a jar pulled out from under it makes every later
   * look at the services throw; it simply stops counting from the moment it is listed here.
   */
  public java.util.Set<String> takenOut = new java.util.LinkedHashSet<>();

  /** What this emulator calls itself when it asks somebody for a file. */
  private static final String WHO_IS_ASKING = "oozx (ZX Spectrum emulator)";

  /** What a tag or a jar is called when it is something to plug in: a board, or a tool over one. */
  private static final List<String> WHAT_PLUGS_IN = List.of("device-", "tool-");

  private static boolean plugsIn(String named) {
    return WHAT_PLUGS_IN.stream().anyMatch(named::startsWith);
  }

  private static final Duration PATIENCE = Duration.ofSeconds(15);

  private Configuration configuration;

  @Override
  public void savedBy(Configuration configuration) {
    this.configuration = configuration;
    if (WHERE_THEY_USED_TO_BE.equals(repository)) {
      repository = WHERE_THEY_ARE;
      configuration.save();
    }
  }

  private static PluginReleases theOne() {
    return Configuration.shared().of(PluginReleases.class);
  }

  /**
   * One published board: what it is called, which file it is, where it is, how big, and what it
   * would answer for - the machines a snapshot can ask for by name, and the kinds of file it
   * knows how to open. Both so that what this build cannot do can point at the jar that can.
   */
  public record Board(String name, String jar, String from, long asset, long size,
                      String sha256, List<String> machines, List<String> opens) {
    public Board(String name, String jar, String from, long asset, long size) {
      this(name, jar, from, asset, size, null, List.of(), List.of());
    }
  }

  /** The boards that would answer for this: a machine as a snapshot names it, or a kind of file. */
  public static List<Board> bringing(String wanted, List<Board> published) {
    return published.stream()
        .filter(board -> board.machines().contains(wanted) || board.opens().contains(wanted))
        .toList();
  }

  /**
   * What a release says it answers for. Written by whoever published it, from the sources of the
   * module, since a jar nobody has downloaded cannot be asked.
   */
  private static List<String> said(String notes, String about) {
    java.util.regex.Matcher said = java.util.regex.Pattern
        .compile("(?m)^oozx-" + about + ": (.*)$").matcher(notes == null ? "" : notes);
    return said.find() ? List.of(said.group(1).trim().split("\\s+")) : List.of();
  }

  /** Where the boards come from, for a window to say so. */
  public static String publishedAt() {
    return theOne().repository;
  }

  /**
   * Whether this board is here, which is a fact about the folder and nothing else.
   * <p>
   * It used to also ask whether the copy here came from the asset that is published now, and
   * that made every installed board vanish from the list the moment anything was pushed: the
   * build recreates every release on every push, so every asset gets a new number while the
   * jars on the disk stay exactly as they were. Being here and being the newest are two
   * questions, and only the folder answers the first one.
   */
  public static boolean isHere(Board board) {
    return isHere(board.jar());
  }

  /**
   * Matched on which board a jar is rather than on its exact file name, because the file name
   * carries the version: the day the project's version moves, every installed board would go
   * missing again for the same reason it did the first time.
   */
  public static boolean isHere(String jar) {
    if (theOne().takenOut.contains(jar)) return false;
    String board = whichBoard(jar);
    return Plugins.inFolder().stream().anyMatch(here -> whichBoard(here.getName()).equals(board));
  }

  /** Whether what is published came from a different build than the copy here. */
  public static boolean isNewerThanHere(Board board) {
    Long had = theOne().brought.get(board.jar());
    return isHere(board) && (had == null || had != board.asset());
  }

  /**
   * Every board this emulator actually has. The folder says which, so one built here or copied
   * in by hand counts the same as one that was downloaded; one that matches something published
   * carries that release's name, and one that matches nothing is called after its file.
   */
  public static List<Board> here(List<Board> published) {
    Map<String, Board> byBoard = new LinkedHashMap<>();
    published.forEach(board -> byBoard.put(whichBoard(board.jar()), board));
    List<Board> here = new ArrayList<>();
    // El archivo que trajo cada uno, cuando todavia esta en la carpeta: el nombre importa, que
    // inventarlo hacia que sacarlo buscara un archivo que no existe y fallara sin decir nada.
    // Lo que esta cargado, y no lo que hay en la carpeta: uno que arranco puede no estar mas
    // ahi, porque quien lo carga se queda con su copia, y la ventana mostraba una lista que no
    // era la verdad - andaba lo que no figuraba.
    for (String id : Plugins.pluggedIn()) {
      Board known = byBoard.get(id);
      java.io.File jar = Plugins.jarOf(id);
      here.add(known != null ? known
          : new Board(id, jar == null ? id + ".jar" : jar.getName(), "", 0,
              jar == null ? 0 : jar.length()));
    }
    here.sort(java.util.Comparator.comparing(Board::name));
    return here;
  }

  /**
   * Which board a jar is, whatever version it happens to be: "tool-calls-0.0.2-SNAPSHOT.jar"
   * and "tool-calls-0.0.3.jar" are both "tool-calls". A board nobody published is called this
   * too, since its file is all there is to call it after.
   */
  static String whichBoard(String jar) {
    return jar.replaceFirst("-\\d.*$", "").replaceFirst("\\.jar$", "");
  }

  /** The releases of the repository, reduced to the ones that are a device and carry a jar. */
  /**
   * The answer of a moment ago, rather than the same question again.
   * <p>
   * The archive allows sixty questions an hour to an address that does not say who it is, and
   * the list is asked for every time the window is opened or told to look again. It is also the
   * same address for everybody behind one router. What is published changes when a build runs,
   * so an answer a few minutes old is the same answer.
   */
  private static List<Board> lastAnswer;
  private static long lastAsked;
  private static final long WORTH_ASKING_AGAIN = java.time.Duration.ofMinutes(5).toMillis();

  public static List<Board> published() throws IOException, InterruptedException {
    if (lastAnswer != null && System.currentTimeMillis() - lastAsked < WORTH_ASKING_AGAIN) {
      return lastAnswer;
    }
    String repository = theOne().repository;
    HttpResponse<String> answer = client().send(
        asking("https://api.github.com/repos/" + repository + "/releases?per_page=100")
            .header("Accept", "application/vnd.github+json").build(),
        HttpResponse.BodyHandlers.ofString());
    if (answer.statusCode() != 200)
      throw new IOException(whatTheArchiveMeant(repository, answer));

    List<Board> boards = new ArrayList<>();
    for (JsonNode release : new ObjectMapper().readTree(answer.body())) {
      String tag = release.path("tag_name").asText("");
      if (!plugsIn(tag)) continue;
      for (JsonNode asset : release.path("assets")) {
        String jar = asset.path("name").asText("");
        if (!jar.endsWith(".jar")) continue;
        String named = release.path("name").asText("");
        // El sha256 lo publica el archivo por cada asset, asi que verificar no cuesta una bajada.
        String digest = asset.path("digest").asText("");
        boards.add(new Board(named.isBlank() ? tag : named, jar,
            asset.path("browser_download_url").asText(), asset.path("id").asLong(),
            asset.path("size").asLong(),
            digest.startsWith("sha256:") ? digest.substring("sha256:".length()) : null,
            said(release.path("body").asText(""), "machines"),
            said(release.path("body").asText(""), "opens")));
        break;
      }
    }
    boards.sort(java.util.Comparator.comparing(Board::name));
    lastAnswer = boards;
    lastAsked = System.currentTimeMillis();
    return boards;
  }

  /**
   * What a refusal was about, in words. A 403 with nothing left of the hour's sixty questions is
   * not a repository nobody may see: it is this address having asked too much, and it answers
   * again by itself at a time the archive says. Told as a number it reads like the plugins are
   * gone.
   */
  private static String whatTheArchiveMeant(String repository, HttpResponse<String> answer) {
    if (answer.statusCode() == 403 && "0".equals(header(answer, "x-ratelimit-remaining"))) {
      String when = header(answer, "x-ratelimit-reset");
      return "github is not answering this address for now: it allows "
          + header(answer, "x-ratelimit-limit") + " questions an hour without a name and they are"
          + " used up" + (when == null ? "" : ", so it answers again at " + java.time.LocalTime
          .ofInstant(java.time.Instant.ofEpochSecond(Long.parseLong(when)),
              java.time.ZoneId.systemDefault()).withNano(0));
    }
    return repository + " answered " + answer.statusCode();
  }

  private static String header(HttpResponse<String> answer, String named) {
    return answer.headers().firstValue(named).orElse(null);
  }

  /**
   * Brings one in and puts it where the emulator will find it, which is the same as plugging it
   * in: from here on it is one more thing the machine can be built with.
   */
  public static Path bring(Board board) throws IOException, InterruptedException {
    String id = whichBoard(board.jar());
    // Pedirlo por su nombre: quien los carga lo baja del catalogo, verifica el sha256 que el
    // archivo publica, y trae lo que necesite. Bajarlo nosotros y despues mirar de que depende
    // era instalar sin su dependencia, fallar, y hacer falta otra vuelta para cada pieza.
    Plugins.add(id);
    PluginReleases them = theOne();
    them.brought.put(board.jar(), board.asset());
    if (them.configuration != null) them.configuration.save();
    return Plugins.folder().resolve(board.jar());
  }


  /**
   * Takes one out: written down as out rather than deleted, and deleted when the emulator starts
   * again. The file has to stay where it is while this runs - the loader holds it open, and one
   * pulled out from under it makes every later look at the services throw - so being out is a
   * fact about the list and not about the disk.
   * <p>
   * What was already loaded stays loaded: a class cannot be unloaded from a machine that is using
   * it, so a board taken out now is one that machine keeps until the emulator starts again.
   */
  public static void takeOut(Board board) {
    PluginReleases them = theOne();
    them.brought.remove(board.jar());
    // Primero deja de estar cargado, y recien despues se corre el archivo: al reves, el jar
    // desaparecia de la lista y lo que traia seguia en los menus y andando.
    String id = whichBoard(board.jar());
    Plugins.takeOut(id);
    // Y el archivo se corre de la carpeta, o al arrancar se volveria a enchufar solo.
    java.io.File jar = Plugins.jarOf(id);
    if (jar != null) {
      movedAside(jar.getName());
    }
    if (them.configuration != null) them.configuration.save();
  }

  /** Out of the way of the loader that is holding it open, for the next start to throw away. */
  private static boolean movedAside(String jar) {
    try {
      Path here = Plugins.folder().resolve(jar);
      Files.move(here, here.resolveSibling(jar + OUT), StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException itIsHeldOpen) {
      return false;
    }
  }

  /** What a board taken out is called until the next start throws it away. */
  private static final String OUT = ".out";

  /** Whether this jar was taken out and is only waiting for the next start to go. */

  public static boolean isOut(String jar) {
    return theOne().takenOut.contains(jar);
  }

  /** The ones taken out, thrown away now that nothing has them open. Called once, on the way up. */
  public static void sweep() {
    try (java.util.stream.Stream<Path> aside = Files.list(Plugins.folder())) {
      for (Path thrownAway : aside.filter(file -> file.getFileName().toString().endsWith(OUT)).toList()) {
        try {
          Files.delete(thrownAway);
        } catch (IOException wouldNotGo) {
          TellsThePerson.thisBuildCannot(thrownAway.getFileName()
              + " could not be thrown away: " + wouldNotGo.getMessage());
        }
      }
    } catch (IOException noFolder) {
      // Nothing was ever put in it, so there is nothing to sweep.
    }
    PluginReleases them = theOne();
    if (them.takenOut.isEmpty()) return;
    // What an older build wrote down. The file is not deleted on the strength of a note: one
    // that is there now was put there on purpose, and taking somebody's file away is not this.
    them.takenOut.clear();
    if (them.configuration != null) them.configuration.save();
  }

  /**
   * The other boards this one was built against, read from its own manifest: a board that uses
   * another's code says so in the jar, so bringing one can bring what it cannot work without.
   */
  public static List<String> needs(Path jar) {
    List<String> boards = new ArrayList<>();
    try (java.util.jar.JarFile opened = new java.util.jar.JarFile(jar.toFile())) {
      java.util.jar.Manifest manifest = opened.getManifest();
      if (manifest == null) return boards;
      String classpath = manifest.getMainAttributes().getValue("Class-Path");
      if (classpath == null) return boards;
      for (String named : classpath.split("\\s+")) {
        if (plugsIn(named) && named.endsWith(".jar")) boards.add(named);
      }
    } catch (IOException cannotBeRead) {
      TellsThePerson.thisBuildCannot(jar.getFileName() + " does not say what it needs: " + cannotBeRead.getMessage());
    }
    return boards;
  }

  private static HttpClient client() {
    return HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(PATIENCE)
        .build();
  }

  /** Saying who is asking, because an archive that publishes these hangs up on a request that does not. */
  private static HttpRequest.Builder asking(String url) {
    return HttpRequest.newBuilder(URI.create(url)).timeout(PATIENCE).header("User-Agent", WHO_IS_ASKING);
  }
}
