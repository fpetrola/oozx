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

import dev.crystal.plugins.api.PluginArtifact;
import dev.crystal.plugins.api.PluginSource;
import dev.crystal.plugins.runtime.PluginSources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * De donde puede salir un plugin: la carpeta donde alguien deja un jar a mano, y las releases
 * publicadas.
 * <p>
 * Las dos cosas en una sola fuente porque para quien los carga son lo mismo: un catalogo de lo
 * que se puede tener. Que este ofrecido no quiere decir que este puesto - eso lo decide una
 * persona, pidiendo que lo traigan.
 * <p>
 * Lo que esta en la carpeta gana: si alguien dejo ahi su propia version de algo publicado, esa
 * es la que quiso.
 */
final class WhereAPluginComesFrom implements PluginSource {

  private final Map<String, PluginReleases.Board> published = new LinkedHashMap<>();
  private final Map<String, File> here = new LinkedHashMap<>();

  @Override
  public List<PluginArtifact> artifacts() throws IOException {
    published.clear();
    here.clear();
    List<PluginArtifact> offered = new ArrayList<>();
    for (File jar : Plugins.inFolder()) {
      PluginArtifact one = whatThisJarIs(jar);
      if (one != null) {
        here.put(one.id(), jar);
        offered.add(one);
      }
    }
    for (PluginReleases.Board board : whatIsPublished()) {
      String id = PluginReleases.whichBoard(board.jar());
      if (board.sha256() == null || here.containsKey(id)) {
        continue;
      }
      published.put(id, board);
      offered.add(new PluginArtifact(id, versionOf(board.jar(), id), board.sha256()));
    }
    return offered;
  }

  @Override
  public InputStream open(PluginArtifact artifact) throws IOException {
    File jar = here.get(artifact.id());
    if (jar != null) {
      return new java.io.FileInputStream(jar);
    }
    PluginReleases.Board board = published.get(artifact.id());
    if (board == null) {
      throw new IOException(artifact.coordinates() + " no esta ofrecido");
    }
    return URI.create(board.from()).toURL().openStream();
  }

  @Override
  public String origin(PluginArtifact artifact) {
    return here.containsKey(artifact.id()) ? "plugins folder" : "github release";
  }

  /** Lo que responde, leido del jar de la carpeta o de lo que el archivo publica al lado del suyo. */
  @Override
  public java.util.Optional<dev.crystal.plugins.api.PluginDescription> describe(PluginArtifact artifact) {
    try {
      File jar = here.get(artifact.id());
      if (jar != null) {
        try (java.util.jar.JarFile opened = new java.util.jar.JarFile(jar)) {
          java.util.zip.ZipEntry metadata = opened.getEntry(PluginSources.METADATA);
          if (metadata == null) return java.util.Optional.empty();
          try (InputStream in = opened.getInputStream(metadata)) {
            return java.util.Optional.of(PluginSources.description(in));
          }
        }
      }
      PluginReleases.Board board = published.get(artifact.id());
      if (board == null || board.metadata() == null) return java.util.Optional.empty();
      return DESCRIBED.computeIfAbsent(board.metadata(), WhereAPluginComesFrom::described);
    } catch (IOException cannotBeRead) {
      return java.util.Optional.empty();
    }
  }

  /** Lo que dice cada release, pedido una vez: la galeria pregunta por cada juego que muestra. */
  private static final Map<String, java.util.Optional<dev.crystal.plugins.api.PluginDescription>> DESCRIBED =
      new java.util.concurrent.ConcurrentHashMap<>();

  private static java.util.Optional<dev.crystal.plugins.api.PluginDescription> described(String metadata) {
    try (InputStream in = URI.create(metadata).toURL().openStream()) {
      return java.util.Optional.of(PluginSources.description(in));
    } catch (IOException cannotBeRead) {
      return java.util.Optional.empty();
    }
  }

  /** Lo que un jar dice que es, o nada cuando no es un plugin. */
  private static PluginArtifact whatThisJarIs(File jar) {
    try (java.util.jar.JarFile opened = new java.util.jar.JarFile(jar)) {
      java.util.jar.Manifest manifest = opened.getManifest();
      if (manifest == null) {
        return null;
      }
      String id = manifest.getMainAttributes().getValue("Plugin-Id");
      String version = manifest.getMainAttributes().getValue("Plugin-Version");
      if (id == null || version == null) {
        return null;
      }
      return new PluginArtifact(id, version, PluginSources.sha256(jar.toPath()));
    } catch (IOException cannotBeRead) {
      return null;
    }
  }

  /** Lo que el archivo publica, o nada cuando no se lo puede preguntar: la carpeta alcanza. */
  private static List<PluginReleases.Board> whatIsPublished() {
    try {
      return PluginReleases.published();
    } catch (IOException | InterruptedException | RuntimeException couldNotAsk) {
      if (couldNotAsk instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return List.of();
    }
  }

  private static String versionOf(String jar, String id) {
    return jar.replaceFirst("^" + java.util.regex.Pattern.quote(id) + "-", "")
        .replaceFirst("\\.jar$", "");
  }
}
