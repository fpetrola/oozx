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

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Household junk lying around the rooms, kicked about by whoever walks into it. A Box2D
 * world runs in the screen plane (x right, y up, pixel units / {@link #PPM}):
 * <ul>
 *   <li>the room's solid cells become static bodies (horizontal runs merged into single
 *       slabs), rebuilt only on a room switch — the transient "holes" a passing guardian
 *       punches into {@code solidCells} never reach the physics;</li>
 *   <li>each prop — ball, bottle, can, apple, cardboard box — is a small dynamic body
 *       scattered over the floor tops when the room appears (seeded by the room's solid
 *       layout, so re-entering a room finds the same junk in the same spots);</li>
 *   <li>the moving sprites are KINEMATIC boxes synced to the taint blobs every emulator
 *       frame, carrying their real on-screen velocity — Box2D's solver turns a walking
 *       Willy into kicks, and a fast guardian into a proper punt.</li>
 * </ul>
 */
public final class JunkPhysics implements Disposable {
  /** pixels per Box2D meter: cells are 1m, props well inside Box2D's sweet spot. */
  static final float PPM = 8f;
  private static final int H = 192;
  private static final float STEP = 1 / 120f;
  /** props that fall past the playfield into the status area are gone for good. */
  private static final float KILL_Y = 58;

  public enum Kind {
    //     w      h    rest  fric  dens  angDamp  palette color choices
    BALL(5.6f, 5.6f, .75f, .25f, .40f, .10f, new int[]{10, 13, 14, 11}),
    BOTTLE(3.0f, 8.0f, .12f, .40f, .90f, .60f, new int[]{4, 12, 6}),
    CAN(3.6f, 4.6f, .30f, .35f, .40f, .50f, new int[]{10, 9, 15}),
    APPLE(4.2f, 4.2f, .30f, .50f, .60f, .30f, new int[]{10, 12}),
    BOX(5.5f, 4.2f, .05f, .70f, .25f, .80f, new int[]{6, 7});

    public final float w, h;
    final float restitution, friction, density, angularDamping;
    final int[] colors;

    Kind(float w, float h, float restitution, float friction, float density,
         float angularDamping, int[] colors) {
      this.w = w;
      this.h = h;
      this.restitution = restitution;
      this.friction = friction;
      this.density = density;
      this.angularDamping = angularDamping;
      this.colors = colors;
    }
  }

  /** |zFrac| below this is the sprites' plane: only these get stepped on / kicked directly. */
  private static final float MIDDLE = .3f;
  private static final short CAT_WORLD = 1, CAT_PROP = 2, CAT_SPRITE = 4;

  public static final class Prop {
    public final Kind kind;
    public final int color; // ZX palette index
    /**
     * where the prop lives across the platform's depth, as a fraction of it (-1 back,
     * 0 the sprites' plane, +1 front). Physics stays 2D: props on ALL layers collide
     * with each other, so a middle prop kicked into a deep one knocks it around by
     * ricochet — which is exactly what sells the depth.
     */
    public final float zFrac;
    /** a few props shine with their own light, item-style. */
    public final boolean glow;
    final Body body;

    Prop(Kind kind, int color, float zFrac, boolean glow, Body body) {
      this.kind = kind;
      this.color = color;
      this.zFrac = zFrac;
      this.glow = glow;
      this.body = body;
    }

    public float x() {
      return body.getPosition().x * PPM;
    }

    public float y() {
      return body.getPosition().y * PPM;
    }

    public float angleDeg() {
      return body.getAngle() * MathUtils.radiansToDegrees;
    }
  }

  /**
   * A ceiling lamp: rod + shade + bulb as one rigid body swinging on a revolute joint
   * anchored at (px, py), the underside of a ceiling cell. Sprites brushing it and props
   * kicked into it set it swinging; the joint's limits keep it from looping the ceiling.
   */
  public static final class Lamp {
    public final float px, py, len;
    final Body body;

    Lamp(float px, float py, float len, Body body) {
      this.px = px;
      this.py = py;
      this.len = len;
      this.body = body;
    }

    public float angleDeg() {
      return body.getAngle() * MathUtils.radiansToDegrees;
    }

    /** the bulb's world position — where the swinging light shines from. */
    public float bulbX() {
      return px + (len + 2) * (float) Math.sin(body.getAngle());
    }

    public float bulbY() {
      return py - (len + 2) * (float) Math.cos(body.getAngle());
    }
  }

  private final World world;
  private final Body ground;
  private final List<Body> roomBodies = new ArrayList<>();
  private final List<Prop> props = new ArrayList<>();
  private final List<Lamp> lamps = new ArrayList<>();
  private final List<Body> spriteBodies = new ArrayList<>();
  private final List<float[]> prevBoxes = new ArrayList<>();
  private final Random rnd = new Random();
  private float accum;
  private int generation;

  public JunkPhysics() {
    Box2D.init();
    world = new World(new Vector2(0, -38f), true);
    // side walls: junk kicked hard bounces back into the room instead of leaving it
    ground = wall(-2, H / 2f, 2, H);
    wall(258, H / 2f, 2, H);
  }

  private Body wall(float cx, float cy, float halfW, float halfH) {
    BodyDef bd = new BodyDef();
    bd.position.set(cx / PPM, cy / PPM);
    Body b = world.createBody(bd);
    PolygonShape s = new PolygonShape();
    s.setAsBox(halfW / PPM, halfH / PPM);
    b.createFixture(s, 0);
    s.dispose();
    return b;
  }

  public List<Prop> props() {
    return props;
  }

  public List<Lamp> lamps() {
    return lamps;
  }

  /** bumped on every respawn: the renderer rebuilds its instances when it changes. */
  public int generation() {
    return generation;
  }

  /**
   * A new room: rebuild the static world from its solid cells and scatter {@code count}
   * pieces of junk over the floor tops (a solid cell with air above, inside the
   * playfield); when there are more props than floor cells they stack up in layers and
   * Box2D settles the pile. The seed comes from the room's solid layout, so each room
   * always gets its own arrangement.
   */
  public void roomChanged(boolean[] solidCells, long seed, int count, int lampCount) {
    for (Body b : roomBodies)
      world.destroyBody(b);
    roomBodies.clear();
    for (Prop p : props)
      world.destroyBody(p.body);
    props.clear();
    for (Lamp l : lamps)
      world.destroyBody(l.body); // takes its joint with it
    lamps.clear();
    prevBoxes.clear();
    generation++;
    rnd.setSeed(seed);

    List<int[]> floorTops = new ArrayList<>();
    for (int cy = 0; cy < 24; cy++)
      for (int col = 0; col < 32; ) {
        if (!solidCells[cy * 32 + col]) {
          col++;
          continue;
        }
        int start = col;
        while (col < 32 && solidCells[cy * 32 + col]) {
          if (cy >= 1 && cy <= 15 && !solidCells[(cy - 1) * 32 + col])
            floorTops.add(new int[]{col, cy});
          col++;
        }
        BodyDef bd = new BodyDef();
        bd.position.set((start * 8 + (col - start) * 4) / PPM, (H - cy * 8 - 4) / PPM);
        Body b = world.createBody(bd);
        PolygonShape s = new PolygonShape();
        s.setAsBox((col - start) * 4 / PPM, 4 / PPM);
        FixtureDef fd = new FixtureDef();
        fd.shape = s;
        fd.friction = .6f;
        b.createFixture(fd);
        s.dispose();
        roomBodies.add(b);
      }

    if (!floorTops.isEmpty()) {
      Collections.shuffle(floorTops, rnd);
      for (int i = 0; i < count; i++) {
        int[] cell = floorTops.get(i % floorTops.size());
        Kind k = Kind.values()[rnd.nextInt(Kind.values().length)];
        spawn(k, cell[0] * 8 + 4 + (rnd.nextFloat() - .5f) * 3,
            H - cell[1] * 8 + k.h / 2 + .5f + (i / floorTops.size()) * 9);
      }
    }

    // lamps hang from ceiling cells — a solid cell with 4+ cells of clear air straight
    // below — spread out so no two crowd the same stretch of ceiling
    if (lampCount > 0) {
      List<int[]> ceilings = new ArrayList<>();
      for (int cy = 0; cy <= 11; cy++)
        for (int col = 0; col < 32; col++) {
          if (!solidCells[cy * 32 + col])
            continue;
          boolean clear = true;
          for (int k = 1; k <= 4 && clear; k++)
            clear = !solidCells[(cy + k) * 32 + col];
          if (clear)
            ceilings.add(new int[]{col, cy});
        }
      Collections.shuffle(ceilings, rnd);
      for (int[] c : ceilings) {
        if (lamps.size() >= lampCount)
          break;
        float px = c[0] * 8 + 4, py = H - (c[1] + 1) * 8;
        boolean crowded = false;
        for (Lamp l : lamps)
          crowded |= Math.abs(l.px - px) < 48 && Math.abs(l.py - py) < 24;
        if (!crowded)
          createLamp(px, py, 13);
      }
    }
  }

  private void createLamp(float px, float py, float len) {
    BodyDef bd = new BodyDef();
    bd.type = BodyDef.BodyType.DynamicBody;
    bd.position.set(px / PPM, py / PPM);
    bd.angularDamping = .5f;
    Body b = world.createBody(bd);
    FixtureDef fd = new FixtureDef();
    fd.filter.categoryBits = CAT_PROP;
    fd.filter.maskBits = CAT_WORLD | CAT_PROP | CAT_SPRITE;
    PolygonShape rod = new PolygonShape();
    rod.setAsBox(.7f / PPM, len / 2 / PPM, new Vector2(0, -len / 2 / PPM), 0);
    fd.shape = rod;
    fd.density = .2f;
    fd.friction = .3f;
    b.createFixture(fd);
    rod.dispose();
    // most of the mass sits in the bulb at the tip: a proper pendulum, slow and heavy
    CircleShape bulb = new CircleShape();
    bulb.setRadius(2.8f / PPM);
    bulb.setPosition(new Vector2(0, -len / PPM));
    fd.shape = bulb;
    fd.density = 2f;
    fd.restitution = .3f;
    b.createFixture(fd);
    bulb.dispose();
    RevoluteJointDef jd = new RevoluteJointDef();
    jd.initialize(ground, b, new Vector2(px / PPM, py / PPM));
    jd.enableLimit = true;
    jd.lowerAngle = -1.35f;
    jd.upperAngle = 1.35f;
    world.createJoint(jd);
    b.setAngularVelocity((rnd.nextFloat() - .5f) * 3); // born mid-sway, not frozen
    lamps.add(new Lamp(px, py, len, b));
  }

  private void spawn(Kind k, float x, float y) {
    float zFrac = rnd.nextFloat() * 2 - 1;
    BodyDef bd = new BodyDef();
    bd.type = BodyDef.BodyType.DynamicBody;
    bd.position.set(x / PPM, y / PPM);
    bd.linearDamping = .15f;
    bd.angularDamping = k.angularDamping;
    Body b = world.createBody(bd);
    FixtureDef fd = new FixtureDef();
    fd.density = k.density;
    fd.friction = k.friction;
    fd.restitution = k.restitution;
    fd.filter.categoryBits = CAT_PROP;
    // deep/front props are out of the sprites' reach — the solver must not let a passing
    // guardian shove them; only other props (any layer) and the world touch them
    fd.filter.maskBits = (short) (Math.abs(zFrac) < MIDDLE
        ? CAT_WORLD | CAT_PROP | CAT_SPRITE : CAT_WORLD | CAT_PROP);
    if (k == Kind.BALL || k == Kind.APPLE) {
      CircleShape c = new CircleShape();
      c.setRadius(k.w / 2 / PPM);
      fd.shape = c;
      b.createFixture(fd);
      c.dispose();
    } else {
      PolygonShape p = new PolygonShape();
      p.setAsBox(k.w / 2 / PPM, k.h / 2 / PPM);
      fd.shape = p;
      b.createFixture(fd);
      p.dispose();
    }
    props.add(new Prop(k, k.colors[rnd.nextInt(k.colors.length)], zFrac,
        rnd.nextFloat() < .25f, b));
  }

  /**
   * Sync the kinematic sprite boxes to this emulator frame's taint blobs. Each box is
   * {cx, cy, halfW, halfH} in pixels; velocity comes from matching against last frame's
   * boxes by proximity — that velocity is what makes the solver KICK a prop instead of
   * just shoving it aside.
   */
  public void syncSprites(List<float[]> boxes, float dt) {
    for (int i = 0; i < boxes.size(); i++) {
      float[] b = boxes.get(i);
      float vx = 0, vy = 0;
      if (dt > 1e-4f) {
        float best = 24;
        for (float[] p : prevBoxes) {
          float d = Math.abs(p[0] - b[0]) + Math.abs(p[1] - b[1]);
          if (d < best) {
            best = d;
            vx = (b[0] - p[0]) / dt / PPM;
            vy = (b[1] - p[1]) / dt / PPM;
          }
        }
        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        if (speed > 40) { // a teleport (room entry, respawn) is not a kick
          vx = vy = 0;
        }
      }
      Body body;
      if (i < spriteBodies.size())
        body = spriteBodies.get(i);
      else {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.KinematicBody;
        body = world.createBody(bd);
        body.setUserData(new float[]{0, 0});
        spriteBodies.add(body);
      }
      float[] size = (float[]) body.getUserData();
      if (Math.abs(size[0] - b[2]) > .5f || Math.abs(size[1] - b[3]) > .5f) {
        while (body.getFixtureList().size > 0)
          body.destroyFixture(body.getFixtureList().first());
        PolygonShape s = new PolygonShape();
        s.setAsBox(b[2] / PPM, b[3] / PPM);
        FixtureDef fd = new FixtureDef();
        fd.shape = s;
        fd.friction = .3f;
        fd.filter.categoryBits = CAT_SPRITE;
        fd.filter.maskBits = CAT_PROP; // middle-layer props only; deep ones mask sprites out
        body.createFixture(fd);
        s.dispose();
        size[0] = b[2];
        size[1] = b[3];
      }
      body.setActive(true);
      body.setTransform(b[0] / PPM, b[1] / PPM, 0);
      body.setLinearVelocity(vx, vy);
      kickOverlaps(b, vx, vy);
    }
    for (int i = boxes.size(); i < spriteBodies.size(); i++) {
      spriteBodies.get(i).setLinearVelocity(0, 0);
      spriteBodies.get(i).setActive(false);
    }
    prevBoxes.clear();
    for (float[] b : boxes)
      prevBoxes.add(b.clone());
  }

  /**
   * A prop the sprite touches must GO somewhere: the solver alone squeezes whatever gets
   * stepped on between the kinematic body's infinite mass and the floor, and it just sits
   * there compacted. So every emulator frame each prop overlapping a sprite is granted a
   * minimum escape velocity — sideways away from the sprite, scaled by how fast the
   * sprite moves, plus an upward pop when it's underfoot — as a floor, not an impulse:
   * a prop already flying away faster is left alone, so it never winds up accelerating
   * without bound while the overlap lasts.
   */
  private void kickOverlaps(float[] b, float svx, float svy) {
    float cx = b[0] / PPM, cy = b[1] / PPM, hw = b[2] / PPM, hh = b[3] / PPM;
    float speed = (float) Math.sqrt(svx * svx + svy * svy);
    for (Prop p : props) {
      if (Math.abs(p.zFrac) >= MIDDLE) // off the sprites' plane: only ricochets move it
        continue;
      float r = Math.max(p.kind.w, p.kind.h) / 2 / PPM;
      Vector2 pos = p.body.getPosition();
      if (Math.abs(pos.x - cx) > hw + r || Math.abs(pos.y - cy) > hh + r)
        continue;
      float dir = Math.signum(pos.x - cx);
      if (dir == 0)
        dir = rnd.nextBoolean() ? 1 : -1;
      float want = dir * (5 + Math.abs(svx) * 1.4f + speed * .4f);
      Vector2 v = p.body.getLinearVelocity();
      float nvx = dir > 0 ? Math.max(v.x, want) : Math.min(v.x, want);
      float nvy = v.y;
      if (pos.y < cy && nvy < 2.5f) // underfoot: hop out, never grind under the feet
        nvy = 2.5f + speed * .35f;
      p.body.setLinearVelocity(nvx, nvy);
      p.body.setAngularVelocity(MathUtils.clamp(
          p.body.getAngularVelocity() + dir * (2 + speed * .3f), -25, 25));
      p.body.setAwake(true);
    }
  }

  /** fixed-step integration, decoupled from the render rate. */
  public void update(float dt) {
    accum += Math.min(dt, .12f);
    while (accum >= STEP) {
      world.step(STEP, 6, 2);
      accum -= STEP;
    }
    for (Iterator<Prop> it = props.iterator(); it.hasNext(); ) {
      Prop p = it.next();
      if (p.y() < KILL_Y) {
        world.destroyBody(p.body);
        it.remove();
      }
    }
  }

  @Override
  public void dispose() {
    world.dispose();
  }
}
