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
    // apply the profile's tweaks, without ever clobbering a -D the user set themselves
    if (g != null && g.has("properties"))
      for (JsonValue p = g.get("properties").child; p != null; p = p.next)
        if (System.getProperty(p.name) == null)
          System.setProperty(p.name, p.asString());

    // paths: positional arg > profile candidates (first that exists / bundled) > legacy fallback
    String rzx = args.length > 0 ? args[0]
        : resolveFile("rzx", candidates(g, "rzx"),
            "Jet Set Willy - Mildly Patched.rzx",
            "/home/fernando/detodo/spectrum/oozx/Jet Set Willy - Mildly Patched.rzx");
    String db = args.length > 1 ? args[1]
        : resolveFile("db", candidates(g, "db"), "analysis/jsw-catalog.db", "analysis/jsw.db");
    return new GameProfile(g != null ? wanted : "custom", title, rzx, db,
        g == null ? null : g.get("sprites"));
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
