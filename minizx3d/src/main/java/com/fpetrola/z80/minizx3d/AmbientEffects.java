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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Atmosphere over the voxel world, all from ONE soft radial blob texture drawn as camera-
 * facing decals:
 * <ul>
 *   <li><b>mist</b>: a dozen big, faint, slowly drifting patches spread through the room's
 *       depth (some behind the characters, some in front), alpha-blended and gently
 *       pulsing — fog that has volume without any shader work;</li>
 *   <li><b>fire</b>: additive particles born at each item, rising and shrinking while they
 *       cool from yellow-white to deep red — the item becomes a small brazier. The caller
 *       reports the CURRENT frame's item cells; when an item is collected its spot stops
 *       spawning and the leftover flames burn out on their own.</li>
 * </ul>
 * Smoke or waterfalls would be more of the same: different palette, drift and blending.
 */
public final class AmbientEffects implements Disposable {
  private static final int MIST_PATCHES = 12;

  private final DecalBatch batch;
  private final Texture blobTex;
  private final TextureRegion blob;
  private final Random rnd = new Random(1234);
  private float time;
  private int dbgTicks;

  private final List<Decal> mist = new ArrayList<>();
  private final float[] mistDrift = new float[MIST_PATCHES];
  private final float[] mistAlpha = new float[MIST_PATCHES];

  private static final class Flame {
    float x, y, z, vx, vy, age, life, size;
  }

  private final List<Flame> flames = new ArrayList<>();
  private final List<Decal> flameDecals = new ArrayList<>();
  private final List<float[]> fireSpots = new ArrayList<>();

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

  public void update(float dt, boolean mistOn, boolean fireOn) {
    time += dt;
    if (Boolean.getBoolean("fx.debug") && ++dbgTicks % 300 == 0)
      System.out.println("fx: spots=" + fireSpots.size() + " llamas=" + flames.size());
    if (mistOn)
      for (int i = 0; i < mist.size(); i++) {
        Decal d = mist.get(i);
        float x = d.getX() + mistDrift[i] * dt;
        if (x > 300) x = -40;
        if (x < -40) x = 300;
        d.setPosition(x, d.getY() + (float) Math.sin(time * .3f + i) * dt * 1.5f, d.getZ());
        d.setColor(.72f, .78f, .88f,
            mistAlpha[i] * (.8f + .2f * (float) Math.sin(time * .5f + i * 2.1f)));
      }
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
    for (java.util.Iterator<Flame> it = flames.iterator(); it.hasNext(); ) {
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

  public void render(Camera cam, boolean mistOn, boolean fireOn) {
    if (mistOn)
      for (Decal d : mist) {
        d.lookAt(cam.position, cam.up);
        batch.add(d);
      }
    if (fireOn) {
      while (flameDecals.size() < flames.size())
        flameDecals.add(Decal.newDecal(1, 1, blob, GL20.GL_SRC_ALPHA, GL20.GL_ONE));
      for (int i = 0; i < flames.size(); i++) {
        Flame f = flames.get(i);
        Decal d = flameDecals.get(i);
        float t = f.age / f.life;
        float s = f.size * (1 - .55f * t);
        d.setDimensions(s, s * 1.4f);          // taller than wide: tongues, not orbs
        // yellow-white core cooling to deep red as the particle rises and dies
        d.setColor(1, .85f - .6f * t, .35f - .32f * t, .85f * (1 - t));
        d.setPosition(f.x, f.y, f.z);
        d.lookAt(cam.position, cam.up);
        batch.add(d);
      }
    }
    if (mistOn || fireOn)
      batch.flush();
  }

  @Override
  public void dispose() {
    batch.dispose();
    blobTex.dispose();
  }
}
