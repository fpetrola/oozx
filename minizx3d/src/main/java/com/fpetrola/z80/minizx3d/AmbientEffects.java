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

  /** the room changed: yesterday's drifts belong to a screen that no longer exists. */
  public void clearSnow() {
    settled.clear();
    drifts.clear();
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

  public void update(float dt, boolean mistOn, boolean fireOn, boolean rainOn, boolean snowOn) {
    time += dt;
    if (Boolean.getBoolean("fx.debug") && ++dbgTicks % 300 == 0)
      System.out.println("fx: spots=" + fireSpots.size() + " llamas=" + flames.size()
          + " gotas=" + drops.size() + " copos=" + flakes.size() + " nieve=" + settled.size());
    updateMist(dt, mistOn);
    updateFire(dt, fireOn);
    updateRain(dt, rainOn);
    updateSnow(dt, snowOn);
  }

  private void updateMist(float dt, boolean mistOn) {
    if (!mistOn)
      return;
    for (int i = 0; i < mist.size(); i++) {
      Decal d = mist.get(i);
      float x = d.getX() + mistDrift[i] * dt;
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
      f.x += (f.vx + (float) Math.sin(time * 9 + f.y * .5f) * 3) * dt;
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
        float nx = d.x + d.vx * dt, ny = d.y - d.vy * dt;
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
          f.x += (float) Math.sin(time * 1.1f + f.phase) * 9 * dt;
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

  public void render(Camera cam, boolean mistOn, boolean fireOn, boolean rainOn, boolean snowOn) {
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
        place(d, dr.x, dr.y, dr.z, cam);
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
