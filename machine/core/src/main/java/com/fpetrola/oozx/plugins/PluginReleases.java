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
   * One published board: what it is called, which file it is, where it is, how big, and where
   * what it answers for is published, so that it can be asked without bringing the jar.
   */
  public record Board(String name, String jar, String from, long asset, long size,
                      String sha256, String metadata) {
    public Board(String name, String jar, String from, long asset, long size) {
      this(name, jar, from, asset, size, null, null);
    }
  }

  /**
   * The boards that would answer for this under any of these roles: a machine as a snapshot
   * names it, or a kind of file. Each board says it itself, in what it was compiled with.
   */
  public static List<Board> bringing(String wanted, List<String> roles) throws IOException, InterruptedException {
    java.util.Set<String> ids = new java.util.HashSet<>();
    for (String role : roles) {
      Plugins.managing().availableAnswering(role, wanted).forEach(artifact -> ids.add(artifact.id()));
    }
    return published().stream().filter(board -> ids.contains(whichBoard(board.jar()))).toList();
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
      String metadata = null;
      for (JsonNode asset : release.path("assets")) {
        if (asset.path("name").asText("").endsWith("plugin-metadata.json")) {
          metadata = asset.path("browser_download_url").asText();
        }
      }
      for (JsonNode asset : release.path("assets")) {
        String jar = asset.path("name").asText("");
        if (!jar.endsWith(".jar")) continue;
        String named = release.path("name").asText("");
        // El sha256 lo publica el archivo por cada asset, asi que verificar no cuesta una bajada.
        String digest = asset.path("digest").asText("");
        boards.add(new Board(named.isBlank() ? tag : named, jar,
            asset.path("browser_download_url").asText(), asset.path("id").asLong(),
            asset.path("size").asLong(),
            digest.startsWith("sha256:") ? digest.substring("sha256:".length()) : null, metadata));
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
    return Plugins.folder().resolve(board.jar());
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

  /** Lo que dice cada release, pedido una vez y todos a la vez: el panel describe cada uno que ofrece. */
  private static final java.util.Map<String, java.util.concurrent.CompletableFuture<java.util.Optional<dev.crystal.plugins.api.PluginDescription>>> DESCRIBED =
      new java.util.concurrent.ConcurrentHashMap<>();

  static java.util.concurrent.CompletableFuture<java.util.Optional<dev.crystal.plugins.api.PluginDescription>> describing(String metadata) {
    return DESCRIBED.computeIfAbsent(metadata, url -> client()
        .sendAsync(asking(url).build(), HttpResponse.BodyHandlers.ofInputStream())
        .thenApply(answer -> {
          try (java.io.InputStream in = answer.body()) {
            return answer.statusCode() == 200
                ? java.util.Optional.of(dev.crystal.plugins.runtime.PluginSources.description(in))
                : java.util.Optional.<dev.crystal.plugins.api.PluginDescription>empty();
          } catch (IOException unreadable) {
            return java.util.Optional.<dev.crystal.plugins.api.PluginDescription>empty();
          }
        })
        .exceptionally(couldNotAsk -> java.util.Optional.empty()));
  }

  /** La descripcion de un release, esperada lo que se espera cualquier respuesta del archivo. */
  static java.util.Optional<dev.crystal.plugins.api.PluginDescription> described(String metadata) {
    try {
      return describing(metadata).get(PATIENCE.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (Exception tooSlow) {
      return java.util.Optional.empty();
    }
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
