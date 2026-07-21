/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.z80.minizx3d;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A named game bundle — RZX recording, sprite catalog, and the per-game tweaks the 3D view
 * needs (Dynamite Dan flashes its lamps, so it turns item detection off; a game with a taller
 * status area sets {@code playfield.rows}, etc.). Profiles live in {@code games.json} (bundled
 * in the jar, or {@code -Dgames.file=/path}, or a {@code games.json} in the working dir);
 * {@code -Dgame=dd} picks one, {@code -Dgame} defaults to the file's {@code "default"}.
 *
 * <p>The profile's properties are applied as system properties BEFORE {@link JSW3D} builds, so
 * every {@code -D}-driven field picks them up — but a property the user set explicitly on the
 * command line always wins, and a positional {@code rzx}/{@code db} argument overrides the
 * profile's paths.
 */
public final class GameProfile {
  public final String id, title, rzx, db;
  /**
   * Per-SPRITE 3D config for this game, keyed by catalog base ({@code "$9d00"}). The
   * {@code properties} block sets the game-wide defaults; this is for the handful of
   * sprites that want their own shape, curated with the game instead of living only in the
   * user's local tuning file.
   */
  public final JsonValue sprites;

  private GameProfile(String id, String title, String rzx, String db, JsonValue sprites) {
    this.id = id;
    this.title = title;
    this.rzx = rzx;
    this.db = db;
    this.sprites = sprites;
  }

  /** Resolve the active profile: explicit args override, then {@code -Dgame}, then default. */
  public static GameProfile resolve(String[] args) {
    JsonValue root = load();
    String wanted = System.getProperty("game",
        root != null && root.has("default") ? root.getString("default") : "jsw");
    JsonValue g = root == null ? null : root.get("games") == null ? null : root.get("games").get(wanted);
    if (g == null && root != null && System.getProperty("game") != null)
      System.out.println("perfil '" + wanted + "' no existe en games.json; usando args/defaults");

    String title = g != null ? g.getString("title", wanted) : wanted;
    // Properties in PRECEDENCE order (highest first), each only filling keys still unset:
    // explicit -D (already on the JVM) > this game's block > the top-level global block.
    // So a game overrides the global default, and a command-line flag overrides both.
    applyProps(null, g == null ? null : g.get("properties"));
    applyProps(null, root == null ? null : root.get("properties"));

    // paths: positional arg > profile candidates (first that exists / bundled) > legacy fallback
    String rzx = args.length > 0 ? args[0]
        : resolveFile("rzx", candidates(g, "rzx"),
            "Jet Set Willy - Mildly Patched.rzx",
            "/home/fernando/detodo/spectrum/oozx/Jet Set Willy - Mildly Patched.rzx");
    String db = args.length > 1 ? args[1]
        : resolveFile("db", candidates(g, "db"), "analysis/jsw-catalog.db", "analysis/jsw.db");
    // The READABLE catalogue wins when it exists (doc/catalogo-<juego>.md, or whatever the
    // profile's "md" lists): it is the one a person can read and correct, and having the
    // viewer load the very file you are looking at is the whole point of generating it.
    String md = args.length > 1 ? null
        : firstExisting(candidates(g, "md"), "doc/catalogo-" + wanted + ".md");
    if (md != null)
      db = md;
    return new GameProfile(g != null ? wanted : "custom", title, rzx, db,
        g == null ? null : g.get("sprites"));
  }

  /**
   * Apply games.json's {@code properties} into system properties for a game, WITHOUT building
   * a full profile — for the offline runners (e.g. {@link TaintDiscover}) that need the same
   * per-game + global settings the viewer gets, keyed by {@code -Dgame}. Per-game wins over
   * global; both defer to anything already set (an explicit {@code -D}, or a higher layer the
   * caller applied first). A null/unknown game still applies the global block.
   */
  public static void applyGamesJson(String game) {
    JsonValue root = load();
    if (root == null)
      return;
    JsonValue games = root.get("games");
    JsonValue g = game == null || games == null ? null : games.get(game);
    applyProps(null, g == null ? null : g.get("properties"));
    applyProps(null, root.get("properties"));
  }

  /**
   * Flattens a (possibly nested) {@code properties} object into dotted system properties —
   * so games.json can be written as structured objects ({@code "render": {"tiles": "screen",
   * "playfield": {"rows": 20}}}) instead of a flat wall of dotted-string keys, and both a
   * nested {@code render.tiles} and a legacy flat {@code tiles} entry resolve to the setting.
   * Never clobbers a value already present, which is what makes the precedence layering work:
   * an explicit {@code -D} and an earlier (higher-priority) layer both survive.
   */
  static void applyProps(String prefix, JsonValue node) {
    if (node == null)
      return;
    for (JsonValue p = node.child; p != null; p = p.next) {
      if (p.name == null)
        continue;
      String key = prefix == null ? p.name : prefix + "." + p.name;
      if (p.isObject())
        applyProps(key, p);
      else if (p.isValue() && System.getProperty(key) == null)
        System.setProperty(key, p.asString());
    }
  }

  private static String[] candidates(JsonValue g, String key) {
    if (g == null || !g.has(key))
      return new String[0];
    JsonValue v = g.get(key);
    if (v.isArray())
      return v.asStringArray();
    return new String[]{v.asString()};
  }

  private static JsonValue load() {
    try {
      String override = System.getProperty("games.file");
      if (override != null && new File(override).exists())
        return new JsonReader().parse(Files.readString(Path.of(override)));
      File local = new File("games.json");
      if (local.exists())
        return new JsonReader().parse(Files.readString(local.toPath()));
      try (InputStream in = GameProfile.class.getResourceAsStream("/games.json")) {
        if (in != null)
          return new JsonReader().parse(new String(in.readAllBytes()));
      }
    } catch (Exception e) {
      System.out.println("games.json no cargado: " + e);
    }
    return null;
  }

  /**
   * The first candidate that exists on disk wins; failing that, the profile's candidates and
   * the fallbacks are tried as BUNDLED resources unpacked to a temp dir (sqlite and the RZX
   * parser need real files). This keeps the checkout and the distributable jar on one path.
   */
  static String resolveFile(String kind, String[] candidates, String... fallbacks) {
    for (String c : candidates)
      if (new File(c).exists())
        return c;
    for (String c : fallbacks)
      if (new File(c).exists())
        return c;
    // bundled: try the profile's names first (basename inside jar), then the fallbacks
    for (String[] group : new String[][]{candidates, fallbacks})
      for (String c : group) {
        String res = c.contains("/") ? c.substring(c.lastIndexOf('/') + 1) : c;
        String unpacked = unpack(res);
        if (unpacked != null)
          return unpacked;
      }
    return candidates.length > 0 ? candidates[0]
        : fallbacks.length > 0 ? fallbacks[0] : kind;
  }

  /** the first of these paths that is really on disk, or null: no bundled-resource fallback. */
  private static String firstExisting(String[] candidates, String... more) {
    for (String[] group : new String[][]{candidates, more})
      for (String c : group)
        if (c != null && new File(c).exists())
          return c;
    return null;
  }

  private static String unpack(String resource) {
    // resources ship under their own path (RZX at root, db under analysis/): try both
    for (String path : new String[]{"/" + resource, "/analysis/" + resource})
      try (InputStream in = GameProfile.class.getResourceAsStream(path)) {
        if (in == null)
          continue;
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "jsw3d-demo");
        Files.createDirectories(dir);
        Path out = dir.resolve(resource);
        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        return out.toString();
      } catch (Exception ignored) {
      }
    return null;
  }
}
