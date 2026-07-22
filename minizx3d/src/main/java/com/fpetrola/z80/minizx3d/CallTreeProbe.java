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
    /** and how coherent the node is in SPACE, which does not depend on the crop being whole */
    final double[] blobs = new double[LEVELS], ratio = new double[LEVELS];
    final long[] w = new long[LEVELS], h = new long[LEVELS];
    long objW, objH;
    /** why the call left more than one blob, at the cut level that matters */
    int multi, instances, repaint, mixed;
  }

  /** how far up the tree the probe looks for the call that decided to draw the object. */
  private static final int LEVELS = 6;

  /**
   * How coherent, in SPACE, is what one call painted. This is the measurement that does not
   * depend on the hand-made definitions being complete: a call that drew one object leaves a
   * single connected patch of object size, and one that drew three leaves three patches or a
   * box the size of the room. Counting "foreign bytes" against a crop cannot tell those apart
   * — most of the foreign bytes there are the rest of the same object, left outside the crop.
   */
  private static final class Coherence {
    long groups, oneBlob, objectSized, blobs, wSum, hSum, fillNum, fillDen;
  }

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
    int[] blobDumps = new int[1];
    Coherence[] coh = new Coherence[LEVELS];
    for (int i = 0; i < LEVELS; i++)
      coh[i] = new Coherence();
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
      // spatial coherence per cut level: what does each call leave on screen?
      for (int up = 0; up < LEVELS; up++) {
        Map<Integer, List<Integer>> byAnc = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> ne : byNode.entrySet())
          byAnc.computeIfAbsent(ancestor(r, ne.getKey(), up), k -> new ArrayList<>())
              .addAll(ne.getValue());
        Coherence co = coh[up];
        for (List<Integer> bytes : byAnc.values()) {
          co.groups++;
          int minC = 31, maxC = 0, minR = 191, maxR = 0;
          java.util.Set<Integer> set = new java.util.HashSet<>(bytes);
          for (int i : bytes) {
            int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
            minC = Math.min(minC, c);
            maxC = Math.max(maxC, c);
            minR = Math.min(minR, y);
            maxR = Math.max(maxR, y);
          }
          int w = maxC - minC + 1, h = maxR - minR + 1;
          co.wSum += w;
          co.hSum += h;
          co.fillNum += bytes.size();
          co.fillDen += (long) w * h;
          int comps = components(set);
          co.blobs += comps;
          if (comps == 1)
            co.oneBlob++;
          if (w <= 16 && h <= 96)
            co.objectSized++;
        }
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
        int onc = 31, oxc = 0, onr = 191, oxr = 0;
        for (int i : mine) {
          int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
          onc = Math.min(onc, c);
          oxc = Math.max(oxc, c);
          onr = Math.min(onr, y);
          oxr = Math.max(oxr, y);
        }
        s.objW += oxc - onc + 1;
        s.objH += oxr - onr + 1;
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
          // the SPATIAL reading: what those nodes painted, as a shape. A call that drew this
          // object and nothing else leaves one blob about the size of the object; the crop
          // being partial does not affect either number
          java.util.Set<Integer> paintedAll = new java.util.HashSet<>();
          for (Map.Entry<Integer, List<Integer>> ne : byNode.entrySet())
            if (nodes.contains(ancestor(r, ne.getKey(), up)))
              paintedAll.addAll(ne.getValue());
          // why does this call leave more than one blob? Two objects in one call look like
          // blobs far apart; one object drawn in two passes looks like blobs that touch or
          // sit inside the same box
          String want = System.getProperty("calls.blobs", "");
          if (up == 1 && want.equals(e.getKey()) && !paintedAll.isEmpty()
              && blobDumps[0] < 8 && components(paintedAll) > 1) {
            blobDumps[0]++;
            List<java.util.Set<Integer>> bl = blobsOf(paintedAll);
            System.out.println("  " + e.getKey() + " frame " + f + ": objeto en " + box(minSet)
                + ", la llamada dejo " + bl.size() + " blobs");
            for (java.util.Set<Integer> b : bl) {
              int mineHere = 0;
              for (int i : b)
                if (minSet.contains(i))
                  mineHere++;
              System.out.println("      blob " + box(b) + " " + b.size() + " bytes, "
                  + mineHere + " del objeto");
            }
          }
          if (up == 1 && !paintedAll.isEmpty()) {
            List<java.util.Set<Integer>> bl = blobsOf(paintedAll);
            if (bl.size() > 1) {
              s.multi++;
              // a blob is "the object" if it is mostly the object's own bytes; the rest is
              // something else the same call drew
              boolean foreignBlob = false;
              int far = 0;
              int[] prev = null;
              for (java.util.Set<Integer> b : bl) {
                int mineHere = 0;
                for (int i : b)
                  if (minSet.contains(i))
                    mineHere++;
                if (mineHere < .5 * b.size())
                  foreignBlob = true;
                int[] bx = bbox(b);
                if (prev != null && (Math.abs(bx[0] - prev[0]) > 4 || Math.abs(bx[1] - prev[1]) > 24))
                  far++;
                prev = bx;
              }
              if (foreignBlob)
                s.mixed++;
              else if (far > 0)
                s.instances++;
              else
                s.repaint++;
            }
          }
          if (!paintedAll.isEmpty()) {
            int nc = 31, xc = 0, nr = 191, xr = 0;
            for (int i : paintedAll) {
              int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
              nc = Math.min(nc, c);
              xc = Math.max(xc, c);
              nr = Math.min(nr, y);
              xr = Math.max(xr, y);
            }
            s.w[up] += xc - nc + 1;
            s.h[up] += xr - nr + 1;
            s.blobs[up] += components(paintedAll);
            s.ratio[up] += paintedAll.size() / (double) mine.size();
          }
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
    System.out.println("\n=== coherencia espacial de lo que pinta cada llamada ===");
    System.out.println("  nivel   llamadas  1 solo blob  tamaño objeto  blobs/llamada  bbox medio  llenado");
    for (int up = 0; up < LEVELS; up++) {
      Coherence c = coh[up];
      if (c.groups == 0)
        continue;
      System.out.printf("  %s %9d %10.0f%% %13.0f%% %14.2f %7dx%-4d %6.0f%%%n",
          up == 0 ? "hoja " : "  +" + up + "  ", c.groups, 100.0 * c.oneBlob / c.groups,
          100.0 * c.objectSized / c.groups, c.blobs / (double) c.groups,
          Math.round(c.wSum / (double) c.groups), Math.round(c.hSum / (double) c.groups),
          100.0 * c.fillNum / Math.max(1, c.fillDen));
    }
    scores.forEach((name, s) -> {
      if (s.frames == 0) {
        System.out.println("\n" + name + ": no aparecio");
        return;
      }
      System.out.println("\n" + name + " (" + s.frames + " frames)");
      System.out.printf("  el objeto ocupa %dx%d bytes%n",
          Math.round(s.objW / (double) s.frames), Math.round(s.objH / (double) s.frames));
      System.out.println("  nivel   nodos/frame   1 solo nodo   bbox de la llamada   blobs"
          + "   bytes vs objeto");
      for (int up = 0; up < LEVELS; up++)
        System.out.printf("  %s %11.2f %12.0f%% %15dx%-4d %6.2f %13.1fx%n",
            up == 0 ? "hoja " : "  +" + up + "  ", s.nodes[up] / (double) s.frames,
            100.0 * s.exact[up] / s.frames, Math.round(s.w[up] / (double) s.frames),
            Math.round(s.h[up] / (double) s.frames), s.blobs[up] / s.frames,
            s.ratio[up] / s.frames);
    });
    System.out.println("\n=== por que la llamada dominante deja mas de un blob ===");
    System.out.println("objeto                 frames  con >1 blob   instancias   repintado   mezclado");
    scores.forEach((name, s) -> {
      if (s.frames == 0)
        return;
      System.out.printf("%-22s %6d %11.0f%% %11.0f%% %11.0f%% %10.0f%%%n", name, s.frames,
          100.0 * s.multi / s.frames, 100.0 * s.instances / Math.max(1, s.multi),
          100.0 * s.repaint / Math.max(1, s.multi), 100.0 * s.mixed / Math.max(1, s.multi));
    });
    System.out.println("'instancias' = la misma llamada dibujo el objeto VARIAS VECES en"
        + " lugares distintos;\n'repintado' = un solo objeto que quedo en pedazos porque el"
        + " frame repinto una parte;\n'mezclado' = la llamada tambien dibujo otra cosa. Los"
        + " dos primeros ya los resuelve el\nagrupador actual; el tercero es el unico que"
        + " seria un problema del criterio.");
    System.out.println("\nleer asi: el nivel bueno es el primero con 'nodos/frame' en 1, un"
        + " bbox parecido al del\nobjeto, 'blobs' en 1 y 'bytes vs objeto' cerca de 1. Esa"
        + " medicion no depende de que el\nrecorte hecho a mano este completo: dice si la"
        + " llamada pinto UNA cosa y del tamaño de\nla que buscamos.");
  }

  /** the blobs themselves, for looking at WHY a call left more than one. */
  private static List<java.util.Set<Integer>> blobsOf(java.util.Set<Integer> bytes) {
    List<java.util.Set<Integer>> out = new ArrayList<>();
    java.util.Set<Integer> left = new java.util.HashSet<>(bytes);
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    while (!left.isEmpty()) {
      java.util.Set<Integer> blob = new java.util.HashSet<>();
      Integer start = left.iterator().next();
      left.remove(start);
      queue.add(start);
      while (!queue.isEmpty()) {
        int i = queue.poll();
        blob.add(i);
        int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
        for (int dy = -1; dy <= 1; dy++)
          for (int dc = -1; dc <= 1; dc++) {
            int yy = y + dy, cc = c + dc;
            if (yy < 0 || yy >= 192 || cc < 0 || cc > 31)
              continue;
            int q = ((yy & 0xC0) << 5) | ((yy & 7) << 8) | ((yy & 0x38) << 2) | cc;
            if (left.remove(q))
              queue.add(q);
          }
      }
      out.add(blob);
    }
    return out;
  }

  private static int[] bbox(java.util.Set<Integer> b) {
    int nc = 31, xc = 0, nr = 191, xr = 0;
    for (int i : b) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      nc = Math.min(nc, c);
      xc = Math.max(xc, c);
      nr = Math.min(nr, y);
      xr = Math.max(xr, y);
    }
    return new int[]{nc, nr, xc, xr};
  }

  private static String box(java.util.Set<Integer> b) {
    int nc = 31, xc = 0, nr = 191, xr = 0;
    for (int i : b) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      nc = Math.min(nc, c);
      xc = Math.max(xc, c);
      nr = Math.min(nr, y);
      xr = Math.max(xr, y);
    }
    return "r" + nr + "c" + nc + " " + (xc - nc + 1) + "x" + (xr - nr + 1);
  }

  /** 8-connected blobs of a set of screen bytes: one object is one blob. */
  private static int components(java.util.Set<Integer> bytes) {
    java.util.Set<Integer> left = new java.util.HashSet<>(bytes);
    int n = 0;
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    while (!left.isEmpty()) {
      n++;
      Integer start = left.iterator().next();
      left.remove(start);
      queue.add(start);
      while (!queue.isEmpty()) {
        int i = queue.poll();
        int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
        for (int dy = -1; dy <= 1; dy++)
          for (int dc = -1; dc <= 1; dc++) {
            int yy = y + dy, cc = c + dc;
            if (yy < 0 || yy >= 192 || cc < 0 || cc > 31)
              continue;
            int q = ((yy & 0xC0) << 5) | ((yy & 7) << 8) | ((yy & 0x38) << 2) | cc;
            if (left.remove(q))
              queue.add(q);
          }
      }
    }
    return n;
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
