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

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Atmosphere over the voxel world, all from ONE soft radial blob texture drawn as camera-
 * facing decals:
 * <ul>
 *   <li><b>mist</b>: a dozen big, faint, slowly drifting patches spread through the room's
 *       depth, alpha-blended and gently pulsing;</li>
 *   <li><b>fire</b>: additive particles born at each item, rising and shrinking while they
 *       cool from yellow-white to deep red — the item becomes a small brazier;</li>
 *   <li><b>rain</b>: fast drops that SPLASH — the {@link World} callback says which pixels
 *       are solid (tile slabs at cell resolution, moving sprites at pixel resolution), and
 *       a drop that hits anything bursts into droplets that arc away under gravity;</li>
 *   <li><b>snow</b>: slow swaying flakes that read the surface they land on: a flat top
 *       accumulates them into growing drifts, a slope or step edge makes them slide
 *       downhill until they find flat ground or fall off, and a moving character carries
 *       them slowly down its body until they drop free.</li>
 * </ul>
 */
public final class AmbientEffects implements Disposable {
  /** what the weather can collide with, in world coordinates (x right, y up, pixel units). */
  public interface World {
    /** a platform / wall / item cell — the solid slabs. */
    boolean solid(float x, float y);

    /** a lit pixel of a MOVING sprite (willy, guardians) — pixel-accurate. */
    boolean sprite(float x, float y);
  }

  private static final int MIST_PATCHES = 12;
  private static final int DROPS = 170, FLAKES = 230, MAX_SETTLED = 2600;
  private static final int FALL = 0, ONSPRITE = 1, SLIDE = 2;

  private final DecalBatch batch;
  private final Texture blobTex;
  private final TextureRegion blob;
  private final Random rnd = new Random(1234);
  private float time;
  private int dbgTicks;
  private World world;
  private float zMid = 15, zSpread = 10;

  private final List<Decal> mist = new ArrayList<>();
  private final float[] mistDrift = new float[MIST_PATCHES];
  private final float[] mistAlpha = new float[MIST_PATCHES];

  private static final class Flame {
    float x, y, z, vx, vy, age, life, size;
  }

  private final List<Flame> flames = new ArrayList<>();
  private final List<float[]> fireSpots = new ArrayList<>();

  private static final class Drop {
    float x, y, z, vx, vy;
  }

  private static final class Splash {
    float x, y, z, vx, vy, age, life;
  }

  private static final class Flake {
    float x, y, z, vy, phase, size, dir;
    int mode;
  }

  private static final class Settled {
    float x, y, z;
  }

  private final List<Drop> drops = new ArrayList<>();
  private final List<Splash> splashes = new ArrayList<>();
  private final List<Flake> flakes = new ArrayList<>();
  private final ArrayDeque<Settled> settled = new ArrayDeque<>();
  /** (surface row | column bucket) -> accumulated drift height on that spot. */
  private final Map<Integer, Float> drifts = new HashMap<>();

  // decal pools, one per particle family, grown on demand and reused every frame
  private final List<Decal> flameDecals = new ArrayList<>();
  private final List<Decal> dropDecals = new ArrayList<>();
  private final List<Decal> splashDecals = new ArrayList<>();
  private final List<Decal> flakeDecals = new ArrayList<>();
  private final List<Decal> settledDecals = new ArrayList<>();

  public AmbientEffects(Camera cam) {
    batch = new DecalBatch(new CameraGroupStrategy(cam));
    // soft radial blob: alpha falls off with the square of the distance to the center
    Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
    for (int y = 0; y < 64; y++)
      for (int x = 0; x < 64; x++) {
        float dx = (x - 31.5f) / 32f, dy = (y - 31.5f) / 32f;
        float a = Math.max(0, 1 - (float) Math.sqrt(dx * dx + dy * dy));
        pm.drawPixel(x, y, com.badlogic.gdx.graphics.Color.rgba8888(1, 1, 1, a * a));
      }
    blobTex = new Texture(pm);
    pm.dispose();
    blob = new TextureRegion(blobTex);

    for (int i = 0; i < MIST_PATCHES; i++) {
      Decal d = Decal.newDecal(60 + rnd.nextFloat() * 60, 40 + rnd.nextFloat() * 40, blob,
          GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
      mistAlpha[i] = .05f + rnd.nextFloat() * .06f;
      d.setColor(.72f, .78f, .88f, mistAlpha[i]);
      // playfield spans world y ~65..192; depth layers straddle the slabs (z 0..30)
      d.setPosition(rnd.nextFloat() * 300 - 20, 75 + rnd.nextFloat() * 105,
          rnd.nextFloat() < .5f ? 4 + rnd.nextFloat() * 12 : 32 + rnd.nextFloat() * 35);
      mistDrift[i] = (rnd.nextFloat() - .5f) * 7;
      mist.add(d);
    }
  }

  public void setWorld(World w) {
    world = w;
  }

  /** weather lives inside the slabs' depth band, so it lands ON the platforms visually. */
  public void setDepthRange(float mid, float spread) {
    zMid = mid;
    zSpread = spread;
  }

  /** the room changed: drifts, puddles and resting leaves belong to a screen that's gone. */
  public void clearSnow() {
    settled.clear();
    drifts.clear();
    puddles.clear();
    for (Leaf f : leaves)
      if (f.rest)
        respawnLeaf(f);
  }

  /** the item cells alive THIS frame; rebuilt by the caller on every emulator frame. */
  public void clearFireSpots() {
    fireSpots.clear();
  }

  public void addFireSpot(float x, float y, float z) {
    fireSpots.add(new float[]{x, y, z});
  }

  /** flame-driven flicker for the item's point light, stable per cell. */
  public float flicker(int seed) {
    return .78f + .16f * (float) Math.sin(time * 11 + seed * 3.1f)
        + .1f * (float) Math.sin(time * 23 + seed * 1.7f);
  }

  public void update(float dt, boolean mistOn, boolean fireOn, boolean rainOn, boolean snowOn,
                     boolean stormOn, boolean windOn, boolean leavesOn) {
    time += dt;
    if (Boolean.getBoolean("fx.debug") && ++dbgTicks % 300 == 0)
      System.out.println("fx: spots=" + fireSpots.size() + " llamas=" + flames.size()
          + " gotas=" + drops.size() + " copos=" + flakes.size() + " nieve=" + settled.size());
    updateWind(dt, windOn);
    updateMist(dt, mistOn);
    updateFire(dt, fireOn);
    updateRain(dt, rainOn);
    updateSnow(dt, snowOn);
    updateStorm(dt, stormOn);
    updateLeaves(dt, leavesOn);
    updatePuddles(dt, rainOn);
    updateBursts(dt);
  }

  /**
   * ONE wind signal (px/s, + rightward) that every system reads: a slow breathing base
   * with GUSTS riding on it — every few seconds the wind swells smoothly to 45-90 px/s
   * for ~2s and dies back down. Rain leans with it, snow and leaves ride it, flames and
   * mist drift, and the junk physics turns it into drag forces on the light props.
   */
  private float windCur, gustTimer = 4, gustT = -1, gustDir = 1, gustPeak;

  public float wind() {
    return windCur;
  }

  private void updateWind(float dt, boolean windOn) {
    if (!windOn) {
      windCur *= Math.max(0, 1 - 2 * dt); // dies down instead of snapping off
      return;
    }
    float base = 14f * (float) Math.sin(time * .33f) + 8f * (float) Math.sin(time * .9f);
    if (gustT < 0) {
      gustTimer -= dt;
      if (gustTimer <= 0) {
        gustT = 0;
        gustDir = rnd.nextBoolean() ? 1 : -1;
        gustPeak = 45 + rnd.nextFloat() * 45;
      }
    }
    float g = 0;
    if (gustT >= 0) {
      gustT += dt;
      float span = 2.2f;
      if (gustT >= span) {
        gustT = -1;
        gustTimer = 3 + rnd.nextFloat() * 7;
      } else
        g = gustPeak * (float) Math.sin(Math.PI * gustT / span);
    }
    windCur = base + g * gustDir;
  }

  /**
   * Autumn leaves: falling slow under heavy drag, fluttering, spinning — but the wind
   * OWNS them. They come to rest on the platforms, and lie there until a character walks
   * through (its wake flings them up spinning) or a strong gust peels them off again.
   */
  private static final int LEAVES = 42;
  private static final float[][] LEAF_COLORS =
      {{.85f, .6f, .12f}, {.75f, .28f, .06f}, {.55f, .6f, .12f}, {.9f, .78f, .25f}};

  private static final class Leaf {
    float x, y, z, vx, vy, rot, vr, phase, size;
    int color;
    boolean rest;
  }

  private final List<Leaf> leaves = new ArrayList<>();
  private final List<Decal> leafDecals = new ArrayList<>();

  private void respawnLeaf(Leaf f) {
    f.x = rnd.nextFloat() * 256;
    f.y = 150 + rnd.nextFloat() * 80;
    f.z = spawnZ();
    f.vx = 0;
    f.vy = 0;
    f.rot = rnd.nextFloat() * 360;
    f.vr = (rnd.nextFloat() - .5f) * 260;
    f.phase = rnd.nextFloat() * 6.3f;
    f.size = 2.6f + rnd.nextFloat() * 1.6f;
    f.color = rnd.nextInt(LEAF_COLORS.length);
    f.rest = false;
  }

  private void updateLeaves(float dt, boolean leavesOn) {
    if (!leavesOn || world == null) {
      leaves.clear();
      return;
    }
    while (leaves.size() < LEAVES) {
      Leaf f = new Leaf();
      respawnLeaf(f);
      f.y = 70 + rnd.nextFloat() * 120; // initial fill spread over the room
      leaves.add(f);
    }
    for (Leaf f : leaves) {
      if (f.rest) {
        boolean near = world.sprite(f.x, f.y + 2) || world.sprite(f.x - 4, f.y + 1)
            || world.sprite(f.x + 4, f.y + 1);
        if (near || (Math.abs(windCur) > 34 && rnd.nextFloat() < 1.2f * dt)) {
          f.rest = false;
          f.vy = 26 + rnd.nextFloat() * 26;
          f.vx = near ? (rnd.nextFloat() - .5f) * 70 : windCur * .8f;
          f.vr = (rnd.nextFloat() - .5f) * 420;
        }
        continue;
      }
      f.vy -= 55 * dt;            // gravity...
      f.vy *= 1 - 2.2f * dt;      // ...against heavy drag: they fall like leaves, not rocks
      f.vx += (windCur - f.vx) * 1.6f * dt;
      f.vx += (float) Math.sin(time * 2.6f + f.phase) * 30 * dt;
      f.x += f.vx * dt;
      f.y += f.vy * dt;
      f.rot += f.vr * dt;
      if (world.sprite(f.x, f.y)) { // batted away by whoever it brushed
        f.vy = 30;
        f.vr = (rnd.nextFloat() - .5f) * 400;
      } else if (world.solid(f.x, f.y)) {
        int guard = 0;
        while (world.solid(f.x, f.y) && guard++ < 8)
          f.y += 1;
        f.rest = true;
        f.vx = f.vy = 0;
        f.vr = 0;
        f.rot = (rnd.nextFloat() - .5f) * 40;
      }
      if (f.x < -6 || f.x > 262 || f.y < 4)
        respawnLeaf(f);
    }
  }

  /**
   * Puddles: each drop that strikes a platform top feeds the puddle of its 4px column
   * bucket, which widens with the rain and drains away once it stops. A character
   * stepping in one kicks up droplets and flattens it a little.
   */
  private final Map<Integer, float[]> puddles = new HashMap<>(); // key -> {x, y, z, w}
  private final List<Decal> puddleDecals = new ArrayList<>();

  private void feedPuddle(float x, float y, float z) {
    float sy = y;
    int guard = 0;
    while (world.solid(x, sy) && guard++ < 10)
      sy += 1;
    int key = (((int) sy) << 7) | (((int) x) >> 2);
    float[] p = puddles.get(key);
    if (p == null && puddles.size() < 480)
      puddles.put(key, p = new float[]{((((int) x) >> 2) << 2) + 2, sy + .4f, z, 1.4f});
    if (p != null)
      p[3] = Math.min(9, p[3] + .4f);
  }

  private void updatePuddles(float dt, boolean rainOn) {
    if (puddles.isEmpty() || world == null)
      return;
    for (Iterator<Map.Entry<Integer, float[]>> it = puddles.entrySet().iterator(); it.hasNext(); ) {
      float[] p = it.next().getValue();
      if (!rainOn)
        p[3] -= dt * .35f;
      if (p[3] <= 1.2f) {
        it.remove();
        continue;
      }
      if (world.sprite(p[0], p[1] + 2) || world.sprite(p[0], p[1] + 5)) {
        p[3] = Math.max(1.2f, p[3] - 8 * dt);
        if (rnd.nextFloat() < 14 * dt) { // stomped: water flies
          Splash s = new Splash();
          s.x = p[0] + (rnd.nextFloat() - .5f) * p[3];
          s.y = p[1] + 2;
          s.z = p[2];
          s.vx = (rnd.nextFloat() - .5f) * 60;
          s.vy = 30 + rnd.nextFloat() * 40;
          s.life = .3f;
          splashes.add(s);
        }
      }
    }
  }

  /** a balloon or bubble popping: a colored burst of droplets flying apart. */
  private static final class Burst {
    float x, y, z, vx, vy, age, life, r, g, b;
  }

  private final List<Burst> bursts = new ArrayList<>();
  private final List<Decal> burstDecals = new ArrayList<>();

  public void addBurst(float x, float y, float z, float r, float g, float b) {
    for (int i = 0; i < 10; i++) {
      Burst s = new Burst();
      s.x = x;
      s.y = y;
      s.z = z;
      double a = rnd.nextFloat() * Math.PI * 2;
      float sp = 25 + rnd.nextFloat() * 45;
      s.vx = (float) Math.cos(a) * sp;
      s.vy = (float) Math.sin(a) * sp + 10;
      s.life = .35f + rnd.nextFloat() * .25f;
      s.r = r;
      s.g = g;
      s.b = b;
      bursts.add(s);
    }
  }

  private void updateBursts(float dt) {
    for (Iterator<Burst> it = bursts.iterator(); it.hasNext(); ) {
      Burst s = it.next();
      s.age += dt;
      s.vy -= 160 * dt;
      s.x += s.vx * dt;
      s.y += s.vy * dt;
      if (s.age >= s.life)
        it.remove();
    }
  }

  /**
   * Lightning: at random intervals (mean {@code -Dstorm.period}, default 7s) a strike
   * lights the whole room through {@link #lightningLevel()} — a hard first flash and a
   * softer echo right behind it, the way a real discharge re-strikes, with a fast shimmer
   * riding on top. The caller folds the level into its lighting environment.
   */
  private final float stormPeriod = Float.parseFloat(System.getProperty("storm.period", "7"));
  private float nextStrike = 2, strikeT = -1, flashLevel;

  public float lightningLevel() {
    return flashLevel;
  }

  private void updateStorm(float dt, boolean stormOn) {
    if (!stormOn) {
      flashLevel = 0;
      strikeT = -1;
      return;
    }
    if (strikeT < 0) {
      nextStrike -= dt;
      if (nextStrike > 0)
        return;
      strikeT = 0;
    }
    strikeT += dt;
    float l = pulse(strikeT, 0f, .10f) + .7f * pulse(strikeT, .18f, .3f);
    flashLevel = Math.min(1, l) * (.82f + .18f * (float) Math.sin(time * 87));
    if (strikeT > .6f) {
      strikeT = -1;
      flashLevel = 0;
      nextStrike = stormPeriod * (.4f + rnd.nextFloat() * 1.2f);
    }
  }

  /** instant attack at {@code start}, exponential decay with time constant ~{@code dur}. */
  private static float pulse(float t, float start, float dur) {
    return t < start ? 0 : (float) Math.exp(-(t - start) / (dur * .45f));
  }

  private void updateMist(float dt, boolean mistOn) {
    if (!mistOn)
      return;
    for (int i = 0; i < mist.size(); i++) {
      Decal d = mist.get(i);
      float x = d.getX() + (mistDrift[i] + windCur * .35f) * dt;
      if (x > 300) x = -40;
      if (x < -40) x = 300;
      d.setPosition(x, d.getY() + (float) Math.sin(time * .3f + i) * dt * 1.5f, d.getZ());
      d.setColor(.72f, .78f, .88f,
          mistAlpha[i] * (.8f + .2f * (float) Math.sin(time * .5f + i * 2.1f)));
    }
  }

  private void updateFire(float dt, boolean fireOn) {
    if (fireOn)
      for (float[] s : fireSpots)
        if (rnd.nextFloat() < .8f) {
          Flame f = new Flame();
          f.x = s[0] + (rnd.nextFloat() - .5f) * 6;
          f.y = s[1] - 2;
          f.z = s[2] + (rnd.nextFloat() - .5f) * 4;
          f.vx = (rnd.nextFloat() - .5f) * 4;
          f.vy = 10 + rnd.nextFloat() * 12;
          f.life = .45f + rnd.nextFloat() * .4f;
          f.size = 5 + rnd.nextFloat() * 4;
          flames.add(f);
        }
    for (Iterator<Flame> it = flames.iterator(); it.hasNext(); ) {
      Flame f = it.next();
      f.age += dt;
      if (f.age >= f.life) {
        it.remove();
        continue;
      }
      f.x += (f.vx + (float) Math.sin(time * 9 + f.y * .5f) * 3 + windCur * .3f) * dt;
      f.y += f.vy * dt;
    }
  }

  private float spawnZ() {
    return zMid + (rnd.nextFloat() - .5f) * 1.6f * zSpread;
  }

  private boolean hitAnything(float x, float y) {
    return world.solid(x, y) || world.sprite(x, y);
  }

  private void resetDrop(Drop d) {
    d.x = rnd.nextFloat() * 256;
    d.y = 195 + rnd.nextFloat() * 40;
    d.z = spawnZ();
    d.vx = (rnd.nextFloat() - .5f) * 12;
    d.vy = 150 + rnd.nextFloat() * 90;
  }

  private void updateRain(float dt, boolean rainOn) {
    if (rainOn && world != null) {
      while (drops.size() < DROPS) {
        Drop d = new Drop();
        resetDrop(d);
        d.y = 60 + rnd.nextFloat() * 175; // initial fill spread over the sky
        drops.add(d);
      }
      for (Drop d : drops) {
        float nx = d.x + (d.vx + windCur) * dt, ny = d.y - d.vy * dt;
        // two samples per step so fast drops can't tunnel through a thin sprite line
        if (hitAnything((d.x + nx) / 2, (d.y + ny) / 2) || hitAnything(nx, ny)) {
          for (int i = 0; i < 3; i++) {
            Splash s = new Splash();
            s.x = nx;
            s.y = ny + 2;
            s.z = d.z;
            s.vx = (rnd.nextFloat() - .5f) * 70;
            s.vy = 35 + rnd.nextFloat() * 55;
            s.life = .28f + rnd.nextFloat() * .2f;
            splashes.add(s);
          }
          if (world.solid(nx, ny)) // a platform top: this drop feeds its puddle
            feedPuddle(nx, ny, d.z);
          resetDrop(d);
        } else {
          d.x = nx;
          d.y = ny;
          if (d.y < 6 || d.x < -4 || d.x > 260)
            resetDrop(d);
        }
      }
    } else if (!rainOn)
      drops.clear();
    for (Iterator<Splash> it = splashes.iterator(); it.hasNext(); ) {
      Splash s = it.next();
      s.age += dt;
      s.vy -= 300 * dt;
      s.x += s.vx * dt;
      s.y += s.vy * dt;
      if (s.age >= s.life || s.y < 4)
        it.remove();
    }
  }

  private void respawnFlake(Flake f) {
    f.x = rnd.nextFloat() * 256;
    f.y = 195 + rnd.nextFloat() * 50;
    f.z = spawnZ();
    f.vy = 13 + rnd.nextFloat() * 11;
    f.phase = rnd.nextFloat() * 6.3f;
    f.size = 1.7f + rnd.nextFloat() * 1.1f;
    f.mode = FALL;
  }

  /** the top of the solid surface near (x, yRef), NaN when there is nothing close below. */
  private float supportY(float x, float yRef) {
    for (float y = yRef + 4; y >= yRef - 12; y--)
      if (world.solid(x, y))
        return y + 1;
    return Float.NaN;
  }

  /** landing: flat ground settles into a drift, a slope or edge starts a downhill slide. */
  private void landFlake(Flake f) {
    int guard = 0;
    while (world.solid(f.x, f.y) && guard++ < 10)
      f.y += 1;
    float tl = supportY(f.x - 3.5f, f.y), tr = supportY(f.x + 3.5f, f.y);
    boolean noL = tl != tl, noR = tr != tr;
    if (noL || noR || Math.abs(tl - tr) >= 3) {
      f.mode = SLIDE;
      f.dir = noL ? -1 : noR ? 1 : tl < tr ? -1 : 1;
    } else {
      int key = (((int) f.y) << 9) | (((int) f.x) >> 1);
      float h = Math.min(5f, drifts.merge(key, .45f, Float::sum));
      Settled s = new Settled();
      s.x = f.x;
      s.y = f.y + h;
      s.z = f.z;
      settled.addLast(s);
      if (settled.size() > MAX_SETTLED)
        settled.removeFirst();
      respawnFlake(f);
    }
  }

  private void updateSnow(float dt, boolean snowOn) {
    if (!snowOn || world == null) {
      flakes.clear(); // settled snow stays until the room changes
      return;
    }
    while (flakes.size() < FLAKES) {
      Flake f = new Flake();
      respawnFlake(f);
      f.y = 70 + rnd.nextFloat() * 160; // initial fill spread over the sky
      flakes.add(f);
    }
    for (Flake f : flakes)
      switch (f.mode) {
        case FALL -> {
          f.y -= f.vy * dt;
          f.x += ((float) Math.sin(time * 1.1f + f.phase) * 9 + windCur * .55f) * dt;
          if (world.sprite(f.x, f.y))
            f.mode = ONSPRITE;
          else if (world.solid(f.x, f.y))
            landFlake(f);
          else if (f.y < 6)
            respawnFlake(f);
        }
        case ONSPRITE -> {
          // carried by a character: creep down its body until it slips off
          f.y -= 5.5f * dt;
          if (!world.sprite(f.x, f.y)) {
            if (world.solid(f.x, f.y))
              landFlake(f);
            else
              f.mode = FALL;
          }
        }
        case SLIDE -> {
          f.x += f.dir * 16 * dt;
          if (f.x < 1 || f.x > 255) {
            respawnFlake(f);
            break;
          }
          int guard = 0;
          while (world.solid(f.x, f.y) && guard++ < 6)
            f.y += 1; // climbed into the hillside: pop back out
          if (!world.solid(f.x, f.y - 2)) {
            f.y -= 24 * dt; // stepped off a ledge: drop with the slope
            if (!world.solid(f.x, f.y - 8))
              f.mode = FALL;
          } else
            landFlake(f); // re-reads the slope; settles when it finds flat ground
        }
      }
  }

  public void render(Camera cam, boolean mistOn, boolean fireOn, boolean rainOn, boolean snowOn,
                     boolean leavesOn) {
    if (mistOn)
      for (Decal d : mist) {
        d.lookAt(cam.position, cam.up);
        batch.add(d);
      }
    if (fireOn)
      for (int i = 0; i < flames.size(); i++) {
        Flame f = flames.get(i);
        Decal d = pooled(flameDecals, i, GL20.GL_ONE);
        float t = f.age / f.life;
        float s = f.size * (1 - .55f * t);
        d.setDimensions(s, s * 1.4f);          // taller than wide: tongues, not orbs
        // yellow-white core cooling to deep red as the particle rises and dies
        d.setColor(1, .85f - .6f * t, .35f - .32f * t, .85f * (1 - t));
        place(d, f.x, f.y, f.z, cam);
      }
    if (rainOn || !splashes.isEmpty()) {
      for (int i = 0; i < drops.size(); i++) {
        Drop dr = drops.get(i);
        Decal d = pooled(dropDecals, i, GL20.GL_ONE_MINUS_SRC_ALPHA);
        d.setDimensions(.8f, 5f);              // stretched: a streak, not a dot
        d.setColor(.55f, .65f, .95f, .55f);
        // the streak leans with its actual velocity, so wind visibly tilts the rain
        d.setPosition(dr.x, dr.y, dr.z);
        d.setRotationZ((float) -Math.toDegrees(Math.atan2(dr.vx + windCur, dr.vy)));
        batch.add(d);
      }
      for (int i = 0; i < splashes.size(); i++) {
        Splash sp = splashes.get(i);
        Decal d = pooled(splashDecals, i, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float t = sp.age / sp.life;
        d.setDimensions(1.5f, 1.5f);
        d.setColor(.7f, .8f, 1f, .7f * (1 - t));
        place(d, sp.x, sp.y, sp.z, cam);
      }
    }
    if (snowOn || !settled.isEmpty()) {
      for (int i = 0; i < flakes.size(); i++) {
        Flake f = flakes.get(i);
        Decal d = pooled(flakeDecals, i, GL20.GL_ONE_MINUS_SRC_ALPHA);
        d.setDimensions(f.size, f.size);
        d.setColor(1, 1, 1, .85f);
        place(d, f.x, f.y, f.z, cam);
      }
      int i = 0;
      for (Settled s : settled) {
        Decal d = pooled(settledDecals, i++, GL20.GL_ONE_MINUS_SRC_ALPHA);
        d.setDimensions(2.6f, 1.3f);           // squashed: lying snow, not a falling flake
        d.setColor(1, 1, 1, .95f);
        place(d, s.x, s.y, s.z, cam);
      }
    }
    if (leavesOn)
      for (int i = 0; i < leaves.size(); i++) {
        Leaf f = leaves.get(i);
        Decal d = pooled(leafDecals, i, GL20.GL_ONE_MINUS_SRC_ALPHA);
        d.setDimensions(f.size, f.size * .62f);
        float[] c = LEAF_COLORS[f.color];
        d.setColor(c[0], c[1], c[2], .95f);
        d.setPosition(f.x, f.y, f.z);
        d.setRotationZ(f.rot);
        batch.add(d);
      }
    if (!puddles.isEmpty()) {
      int i = 0;
      for (float[] p : puddles.values()) {
        Decal d = pooled(puddleDecals, i++, GL20.GL_ONE_MINUS_SRC_ALPHA);
        d.setDimensions(p[3], 1.1f);           // a widening sliver of standing water
        d.setColor(.5f, .65f, .95f, .34f + .08f * (float) Math.sin(time * 2.5f + p[0]));
        place(d, p[0], p[1], p[2], cam);
      }
    }
    for (int i = 0; i < bursts.size(); i++) {
      Burst s = bursts.get(i);
      Decal d = pooled(burstDecals, i, GL20.GL_ONE_MINUS_SRC_ALPHA);
      float t = s.age / s.life;
      d.setDimensions(1.7f, 1.7f);
      d.setColor(s.r, s.g, s.b, .9f * (1 - t));
      place(d, s.x, s.y, s.z, cam);
    }
    batch.flush();
  }

  private Decal pooled(List<Decal> pool, int i, int dstBlend) {
    while (pool.size() <= i)
      pool.add(Decal.newDecal(1, 1, blob, GL20.GL_SRC_ALPHA, dstBlend));
    return pool.get(i);
  }

  private void place(Decal d, float x, float y, float z, Camera cam) {
    d.setPosition(x, y, z);
    d.lookAt(cam.position, cam.up);
    batch.add(d);
  }

  @Override
  public void dispose() {
    batch.dispose();
    blobTex.dispose();
  }
}
