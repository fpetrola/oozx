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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Measures whether the CALL TREE separates the objects a game draws — before anything in the
 * renderer is changed on the strength of it.
 *
 * <p>The premise: a game that draws a composite object does it from one routine, or from
 * several called together by one that decided to draw the thing, so every byte of that object
 * hangs off one node of the call tree. Everything tried before measured the SCREEN (adjacency,
 * colour, write order, a time window) and each one merged or split the wrong pair, because the
 * screen does not say where an object ends. The call tree is not a measurement of the picture;
 * it is what the program did.
 *
 * <p>And there is an oracle to check it against: the objects identified BY HAND in the editor.
 * Success is not an impression — it is "does the capsule come out as one node's subtree, and
 * without the player in it".
 */
public final class CallTreeProbe {
  /** what one marked object scored on one frame. */
  private static final class Score {
    int frames;
    /** per CUT LEVEL (0 = the invocation that wrote it, 1 = its caller, ...) */
    final int[] nodes = new int[LEVELS], exact = new int[LEVELS];
    final double[] contamination = new double[LEVELS];
  }

  /** how far up the tree the probe looks for the call that decided to draw the object. */
  private static final int LEVELS = 6;

  /** the ancestor {@code up} levels above {@code node}, or the outermost one there is. */
  private static int ancestor(TaintReplay r, int node, int up) {
    // the tree is rebuilt every frame, so a byte painted two frames ago carries an id that
    // means nothing now: those count as the root instead of walking off the array
    int n = node >= 0 && node < r.nodeParent.size ? node : 0;
    for (int i = 0; i < up && n > 0 && r.nodeParent.get(n) > 0; i++)
      n = r.nodeParent.get(n);
    return n;
  }

  public static void main(String[] args) throws Exception {
    GameProfile profile = GameProfile.resolve(args);
    int from = Integer.getInteger("calls.from", 500);
    int frames = Integer.getInteger("calls.frames", 4000);
    int sample = Integer.getInteger("calls.sample", 10);
    SpriteCatalog catalog = new SpriteCatalog(profile.db, 128);
    Map<String, java.util.Set<Integer>> objects = loadObjects(profile.id);
    System.out.println("sonda de arbol de llamadas: " + profile.title + " · "
        + objects.size() + " objetos marcados a mano como oraculo");

    Map<String, Score> scores = new LinkedHashMap<>();
    objects.keySet().forEach(k -> scores.put(k, new Score()));
    int[] groupsPerFrame = new int[1];
    int[] sampled = new int[1];
    long[] groupTotal = new long[1];
    TaintReplay[] holder = new TaintReplay[1];
    TaintReplay replay = new TaintReplay(profile.rzx, catalog, snap -> {
      int f = snap.frame();
      if (f < from || (f - from) % sample != 0)
        return;
      sampled[0]++;
      TaintReplay r = holder[0];
      // the bytes this frame painted, by the call-tree node that painted them
      Map<Integer, List<Integer>> byNode = new HashMap<>();
      for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
        if ((snap.pixels()[i] & 0xff) == 0 || r.lastWrite[i] != f - 1)
          continue; // exactly what the frame that just ended painted: its ids are the live tree
        byNode.computeIfAbsent(r.writeNode[i], k -> new ArrayList<>()).add(i);
      }
      if (Boolean.getBoolean("calls.dump") && sampled[0] <= 3) {
        System.out.println("  --- frame " + f + ": " + byNode.size() + " nodos escribieron");
        byNode.entrySet().stream()
            .sorted((a, b) -> b.getValue().size() - a.getValue().size()).limit(10)
            .forEach(en -> {
              int n = en.getKey();
              StringBuilder chain = new StringBuilder();
              for (int a2 : r.nodeChain(n))
                chain.append(chain.length() > 0 ? "->" : "").append('$')
                    .append(Integer.toHexString(a2));
              System.out.println("    nodo " + n + " " + en.getValue().size() + " bytes  "
                  + (chain.length() == 0 ? "(raiz, sin CALL)" : chain));
            });
      }
      groupTotal[0] += byNode.size();
      groupsPerFrame[0] = Math.max(groupsPerFrame[0], byNode.size());
      // the oracle: where do the bytes of each marked object end up?
      for (Map.Entry<String, java.util.Set<Integer>> e : objects.entrySet()) {
        List<Integer> mine = new ArrayList<>();
        for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
          if ((snap.pixels()[i] & 0xff) == 0 || r.lastWrite[i] != f - 1)
            continue;
          int addr = snap.owner()[i] != 0 ? snap.owner()[i] - 1
              : snap.tile()[i] != 0 ? snap.tile()[i] - 1 : -1;
          if (addr >= 0 && e.getValue().contains(addr))
            mine.add(i);
        }
        if (mine.size() < 4)
          continue;
        Score s = scores.get(e.getKey());
        s.frames++;
        java.util.Set<Integer> minSet = new java.util.HashSet<>(mine);
        for (int up = 0; up < LEVELS; up++) {
          java.util.Set<Integer> nodes = new java.util.HashSet<>();
          for (int i : mine)
            nodes.add(ancestor(r, r.writeNode[i], up));
          s.nodes[up] += nodes.size();
          if (nodes.size() == 1)
            s.exact[up]++;
          // contamination: of everything those nodes' SUBTREES painted, how much is not it
          int total = 0, foreign = 0;
          for (Map.Entry<Integer, List<Integer>> ne : byNode.entrySet()) {
            if (!nodes.contains(ancestor(r, ne.getKey(), up)))
              continue;
            for (int i : ne.getValue()) {
              total++;
              if (!minSet.contains(i))
                foreign++;
            }
          }
          if (total > 0)
            s.contamination[up] += foreign / (double) total;
        }
      }
    });
    holder[0] = replay;
    replay.paced = false;
    replay.maxFrames = from + frames;
    try {
      replay.run();
    } catch (RuntimeException e) {
      System.out.println("replay terminado: " + e.getMessage());
    }

    System.out.println("\n=== " + sampled[0] + " frames muestreados ===");
    System.out.printf("nodos del arbol de llamadas: %d · grupos por frame: %.1f promedio, %d maximo%n",
        replay.nodeAddr.size, groupTotal[0] / (double) Math.max(1, sampled[0]), groupsPerFrame[0]);
    scores.forEach((name, s) -> {
      if (s.frames == 0) {
        System.out.println("\n" + name + ": no aparecio");
        return;
      }
      System.out.println("\n" + name + " (" + s.frames + " frames)");
      System.out.println("  nivel   nodos/frame   1 solo nodo   contaminacion");
      for (int up = 0; up < LEVELS; up++)
        System.out.printf("  %s %11.2f %12.0f%% %14.0f%%%n",
            up == 0 ? "hoja " : "  +" + up + "  ", s.nodes[up] / (double) s.frames,
            100.0 * s.exact[up] / s.frames, 100.0 * s.contamination[up] / s.frames);
    });
    System.out.println("\nleer asi: 'nivel' es cuanto se sube desde la invocacion que escribio"
        + " el pixel.\n'nodos/frame' en 1 = el objeto sale de UNA llamada; 'contaminacion' ="
        + " cuanto de lo que\nesa llamada pinto NO es el objeto. El nivel bueno es el primero"
        + " con nodos=1 y\ncontaminacion baja: ahi esta la llamada que decidio dibujarlo.");
  }

  /** the objects marked by hand, from the one config file. */
  private static Map<String, java.util.Set<Integer>> loadObjects(String game) {
    Map<String, java.util.Set<Integer>> out = new LinkedHashMap<>();
    com.badlogic.gdx.utils.JsonValue g = GameProfile.gameNode(game, false);
    com.badlogic.gdx.utils.JsonValue list = g == null ? null : g.get("objetos");
    for (com.badlogic.gdx.utils.JsonValue o = list == null ? null : list.child; o != null;
         o = o.next) {
      java.util.Set<Integer> gfx = new java.util.HashSet<>();
      for (String p : o.getString("piezas", "").split(","))
        if (!p.isBlank())
          gfx.add(Integer.parseInt(p.trim().replace("$", ""), 16));
      if (!gfx.isEmpty())
        out.put(o.getString("nombre", "objeto" + out.size()), gfx);
    }
    return out;
  }

  private CallTreeProbe() {
  }
}
