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

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jet Set Willy in 3D: the original game replays untouched on the emulator, and the live
 * {@link OriginTaint} says which screen bytes were built from sprite-catalog data. Those
 * bytes are lifted off the 2D plane and replaced by inflated voxel models
 * ({@link VoxelSpriteBuilder}); everything else — the room, from the template records —
 * stays as the flat backdrop the sprites float in front of.
 *
 * <pre>
 *   mvn -pl minizx3d exec:java -Dexec.mainClass=com.fpetrola.z80.minizx3d.JSW3D \
 *       -Dexec.args="&lt;jsw.rzx&gt; &lt;analysis.db-con-sprites_found&gt;"
 * </pre>
 */
public class JSW3D extends ApplicationAdapter {
  private static final int W = 256, H = 192;
  private static final Color[] PALETTE = new Color[16];

  static {
    float d = 0.8f;
    for (int i = 0; i < 16; i++) {
      float v = i < 8 ? d : 1f;
      PALETTE[i] = new Color((i & 2) != 0 ? v : 0, (i & 4) != 0 ? v : 0, (i & 1) != 0 ? v : 0, 1);
    }
  }

  private final String rzxPath, dbPath;
  private volatile TaintReplay.FrameSnapshot latest;
  private int shownFrame = -1;

  private SpriteCatalog catalog;
  private TaintReplay replay;
  private Thread replayThread;
  private ModelBatch batch;
  private PerspectiveCamera cam;
  private CameraInputController camController;
  private Environment env;
  private Pixmap pixmap;
  private Texture screenTex;
  private ModelInstance backdrop;
  private Model backdropModel;
  private final Map<Integer, Model> modelCache = new HashMap<>();
  private final List<ModelInstance> spriteInstances = new ArrayList<>();
  private final List<ModelInstance> tileInstances = new ArrayList<>();
  /** smooth inflated mesh vs voxel boxes; M toggles at runtime. */
  private boolean smooth = !"voxel".equals(System.getProperty("sprites3d", "smooth"));
  /** S/X raise/lower how far the surface departs from pixel art (0 = raw voxel steps). */
  private int smoothLevel = Integer.getInteger("smooth.level", 2);
  /** D/C raise/lower the global depth multiplier; depth itself is shape-adaptive. */
  private float depthScale = Float.parseFloat(System.getProperty("depth.scale", "1"));
  /**
   * T/G raise/lower how much deeper tiles are than sprites: platforms/walls become slabs
   * the characters walk ON — sprites and items sit centered at the tiles' mid-depth.
   */
  private float tileDepth = Float.parseFloat(System.getProperty("tile.depth", "7"));
  /**
   * item detection, game-agnostic: an item is a tile whose CELL keeps changing INK over the
   * same bitmap, several times in quick succession (JSW items flash every frame). Counting
   * per cell inside a time window rejects room transitions, where every cell of the room
   * changes ink once as the reveal fills the attributes in. Latched per leaf; item leaves
   * keep 1x depth instead of tileDepth.
   */
  private final int[] prevLeafAttr = new int[24 * 32];
  private final int[] cellInkChanges = new int[24 * 32];
  private final int[] cellInkMask = new int[24 * 32];
  private final int[] cellLastChange = new int[24 * 32];
  private final java.util.Set<Integer> itemLeaves = new java.util.HashSet<>();
  /**
   * L toggles lantern mode (-Ddark=true starts in it): a faint ambient lets the whole room
   * be barely made out, each MOVING sprite carries its own small light that brightens its
   * surroundings, and items blaze with light of their own so they stand out in the gloom.
   */
  private boolean darkMode = Boolean.getBoolean("dark");
  private final float darkAmbient = Float.parseFloat(System.getProperty("dark.ambient", "0.04"));
  private final float spriteLightIntensity = Float.parseFloat(System.getProperty("dark.sprite", "300"));
  private final float itemLightIntensity = Float.parseFloat(System.getProperty("dark.item", "500"));
  /** leaf -> its bitmap is all zeros (air): no slab, no item tracking, no light — background. */
  private final Map<Integer, Boolean> emptyLeaves = new HashMap<>();
  /** the lights the CURRENT frame's sprites and items shine; env is rebuilt from them. */
  private final List<com.badlogic.gdx.graphics.g3d.environment.PointLight> frameLights = new ArrayList<>();
  /**
   * N mist, F item braziers, R rain, B snow (-Dfx.fog / -Dfx.fire / -Dfx.rain / -Dfx.snow).
   * Rain and snow collide with the world: {@link #solidCells} marks the slab cells and
   * {@link #spritePix} the moving sprites' lit pixels, both rebuilt every frame.
   */
  private AmbientEffects effects;
  private boolean mistOn = Boolean.getBoolean("fx.fog");
  private boolean fireOn = Boolean.getBoolean("fx.fire");
  private boolean rainOn = Boolean.getBoolean("fx.rain");
  private boolean snowOn = Boolean.getBoolean("fx.snow");
  private final boolean[] solidCells = new boolean[24 * 32];
  private final boolean[] prevSolidCells = new boolean[24 * 32];
  private final byte[] spritePix = new byte[TaintReplay.PIXEL_BYTES];
  /**
   * J junk (-Dfx.junk=true): household debris — balls, bottles, cans, food, boxes —
   * scattered over the floors, living in a {@link JunkPhysics} Box2D world whose statics
   * are the room's solid cells and whose kinematic bodies are the moving sprites: whoever
   * walks into a prop kicks it with their real on-screen velocity.
   */
  private JunkPhysics junk;
  private boolean junkOn = Boolean.getBoolean("fx.junk");
  private boolean junkSpawnPending = junkOn;
  /** K/H raise/lower how many pieces spawn per room (-Dfx.junk.count overrides). */
  private int junkCount = Math.max(2, Integer.getInteger("fx.junk.count", 18));
  private int junkGeneration = -1;
  private final Map<JunkPhysics.Kind, Model> junkModels = new HashMap<>();
  private final List<ModelInstance> junkInstances = new ArrayList<>();
  /** this frame's sprite blob boxes {cx, cy, halfW, halfH}, playfield only. */
  private final List<float[]> spriteBoxes = new ArrayList<>();
  /** render time elapsed between emulator frames: the sprites' kick velocity timebase. */
  private float snapDt;
  /**
   * P hanging lamps (-Dfx.lamps=true): pendulum bodies in the junk world, hung from the
   * ceiling cells. Sprites and kicked junk set them swinging; the bulb always glows, and
   * in lantern mode it carries a warm light that sways with the pendulum, moving the
   * room's shadows around.
   */
  private boolean lampsOn = Boolean.getBoolean("fx.lamps");
  private Model lampModel;
  private final List<ModelInstance> lampInstances = new ArrayList<>();
  private int lampGeneration = -1;
  /**
   * E thunderstorm (-Dfx.storm=true): random lightning strikes relight the entire room
   * for a few frames — {@link AmbientEffects#lightningLevel()} folds into the lighting
   * environment. Pairs well with rain (R) and is at its best in lantern mode.
   */
  private boolean stormOn = Boolean.getBoolean("fx.storm");

  public JSW3D(String rzxPath, String dbPath) {
    this.rzxPath = rzxPath;
    this.dbPath = dbPath;
  }

  @Override
  public void create() {
    // enough point-light slots for willy + guardians + every item in the room
    com.badlogic.gdx.graphics.g3d.shaders.DefaultShader.Config shaderCfg =
        new com.badlogic.gdx.graphics.g3d.shaders.DefaultShader.Config();
    shaderCfg.numPointLights = 32;
    batch = new ModelBatch(new com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider(shaderCfg));
    cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    cam.position.set(W / 2f, H / 2f + 30, 290);
    // -Dcam.pos=x,y,z overrides the viewpoint (angled screenshots for visual checks)
    String camPos = System.getProperty("cam.pos");
    if (camPos != null) {
      String[] p = camPos.split(",");
      cam.position.set(Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]));
    }
    cam.lookAt(W / 2f, H / 2f, 0);
    cam.near = 1;
    cam.far = 1000;
    cam.update();
    camController = new CameraInputController(cam);
    effects = new AmbientEffects(cam);
    junk = new JunkPhysics();
    effects.setWorld(new AmbientEffects.World() {
      @Override
      public boolean solid(float wx, float wy) {
        int x = (int) wx, sy = H - 1 - (int) Math.floor(wy);
        return x >= 0 && x < W && sy >= 0 && sy < H && solidCells[(sy >> 3) * 32 + (x >> 3)];
      }

      @Override
      public boolean sprite(float wx, float wy) {
        int x = (int) wx, sy = H - 1 - (int) Math.floor(wy);
        if (x < 0 || x >= W || sy < 0 || sy >= H)
          return false;
        int i = ((sy & 0xC0) << 5) | ((sy & 7) << 8) | ((sy & 0x38) << 2) | (x >> 3);
        return (spritePix[i] & (0x80 >> (x & 7))) != 0;
      }
    });
    com.badlogic.gdx.InputMultiplexer input = new com.badlogic.gdx.InputMultiplexer(
        new com.badlogic.gdx.InputAdapter() {
          @Override
          public boolean keyDown(int keycode) {
            // only the keys that change the VOXELS force a rebuild; lights, weather and
            // replay speed leave every cached model exactly as it is
            boolean rebuild = true;
            switch (keycode) {
              case com.badlogic.gdx.Input.Keys.M -> smooth = !smooth;
              case com.badlogic.gdx.Input.Keys.S -> smoothLevel = Math.min(10, smoothLevel + 1);
              case com.badlogic.gdx.Input.Keys.X -> smoothLevel = Math.max(0, smoothLevel - 1);
              case com.badlogic.gdx.Input.Keys.D -> depthScale = Math.min(3f, depthScale * 1.25f);
              case com.badlogic.gdx.Input.Keys.C -> depthScale = Math.max(.3f, depthScale / 1.25f);
              case com.badlogic.gdx.Input.Keys.T -> tileDepth = Math.min(20f, tileDepth * 1.25f);
              case com.badlogic.gdx.Input.Keys.G -> tileDepth = Math.max(1f, tileDepth / 1.25f);
              case com.badlogic.gdx.Input.Keys.L -> {
                darkMode = !darkMode;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.N -> {
                mistOn = !mistOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F -> {
                fireOn = !fireOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.R -> {
                rainOn = !rainOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.B -> {
                snowOn = !snowOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.J -> {
                junkOn = !junkOn;
                junkSpawnPending = true;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.P -> {
                lampsOn = !lampsOn;
                junkSpawnPending = true;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.E -> {
                stormOn = !stormOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.K -> {
                junkCount = Math.min(150, junkCount + 6);
                junkSpawnPending = junkOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.H -> {
                junkCount = Math.max(2, junkCount - 6);
                junkSpawnPending = junkOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.PERIOD -> {
                replay.setSpeed(replay.getSpeed() * 2);
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.COMMA -> {
                replay.setSpeed(replay.getSpeed() / 2);
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.NUM_0 -> {
                replay.setSpeed(1);
                rebuild = false;
              }
              default -> {
                return false;
              }
            }
            if (rebuild) {
              try {
                  Thread.sleep(100);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
              modelCache.values().forEach(Model::dispose);
              modelCache.clear();
            }
            printStatus();
            return true;
          }
        });
    // screenshot runs must render from the fixed default camera: a stray mouse drag over
    // the window would rotate the view and invalidate the visual check
    if (System.getProperty("shot") == null)
      input.addProcessor(camController);
    Gdx.input.setInputProcessor(input);

    rebuildEnv();

    pixmap = new Pixmap(W, H, Pixmap.Format.RGBA8888);
    screenTex = new Texture(W, H, Pixmap.Format.RGBA8888);
    Material mat = new Material(TextureAttribute.createDiffuse(screenTex),
        TextureAttribute.createEmissive(screenTex));
    // the backdrop is subdivided because lighting is per-vertex: on a single 4-corner
    // quad a sprite's point light in the middle of the room would light NOTHING
    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    mb.part("backdrop", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat)
        .patch(0, 0, 0, W, 0, 0, W, H, 0, 0, H, 0, 0, 0, 1, 64, 48);
    backdropModel = mb.end();
    backdrop = new ModelInstance(backdropModel);

    try {
      catalog = new SpriteCatalog(dbPath, 128);
      replay = new TaintReplay(rzxPath, catalog, snap -> latest = snap);
      replayThread = new Thread(replay, "taint-replay");
      replayThread.setDaemon(true);
      replayThread.start();
      printStatus();
    } catch (Exception e) {
      throw new RuntimeException("No pude cargar el catalogo de sprites de " + dbPath, e);
    }
  }

  /**
   * normal: bright ambient + a sun-like directional. Lantern mode: near-darkness where the
   * only real light comes from {@link #frameLights} — what this frame's sprites and items
   * shine on their surroundings.
   */
  private void rebuildEnv() {
    env = new Environment();
    // a lightning strike floods the room: ambient jumps toward daylight-white and a hard
    // top-down directional slams in, then both decay with the flash envelope
    float flash = effects == null ? 0 : effects.lightningLevel();
    if (darkMode) {
      float a = darkAmbient + flash * .85f;
      env.set(new ColorAttribute(ColorAttribute.AmbientLight, a, a, a * 1.15f, 1));
      frameLights.forEach(env::add);
    } else {
      float a = .5f + flash * .4f;
      env.set(new ColorAttribute(ColorAttribute.AmbientLight, a, a, a * 1.05f, 1));
      env.add(new DirectionalLight().set(1f, 1f, 1f, -0.4f, -0.6f, -1f));
    }
    if (flash > .01f)
      env.add(new DirectionalLight().set(flash, flash, flash * 1.08f, .25f, -.9f, -.35f));
  }

  @Override
  public void render() {
    snapDt += Gdx.graphics.getDeltaTime();
    TaintReplay.FrameSnapshot snap = latest;
    if (snap != null && snap.frame() != shownFrame) {
      shownFrame = snap.frame();
      frameLights.clear();
      effects.clearFireSpots();
      long t0 = perf ? System.nanoTime() : 0;
      updateBackdrop(snap);
      long t1 = perf ? System.nanoTime() : 0;
      updateSprites(snap);
      if (junkOn || lampsOn)
        junk.syncSprites(spriteBoxes, snapDt);
      snapDt = 0;
      long t2 = perf ? System.nanoTime() : 0;
      updateTiles(snap);
      long t3 = perf ? System.nanoTime() : 0;
      // glowing junk casts its own small pool of light in the dark, like the items do;
      // capped so a big junk count can't starve the shader's point-light slots
      if (junkOn && darkMode) {
        int lit = 0;
        for (JunkPhysics.Prop p : junk.props())
          if (p.glow && lit++ < 6) {
            Color c = PALETTE[p.color];
            frameLights.add(new com.badlogic.gdx.graphics.g3d.environment.PointLight().set(
                c.r, c.g, c.b, p.x(), p.y(), junkZ(p) + 6, itemLightIntensity * .5f));
          }
      }
      // each lamp's bulb carries a warm swinging light: as the pendulum moves, so do
      // the shadows it throws around the room
      if (lampsOn && darkMode)
        for (JunkPhysics.Lamp l : junk.lamps())
          frameLights.add(new com.badlogic.gdx.graphics.g3d.environment.PointLight().set(
              1f, .9f, .68f, l.bulbX(), l.bulbY(), midZ() + 8, itemLightIntensity * 1.2f));
      rebuildEnv();
      if (perf) {
        nsBackdrop += t1 - t0;
        nsSprites += t2 - t1;
        nsTiles += t3 - t2;
        perfFrames++;
      }
    }
    if (junkOn || lampsOn) {
      junk.update(Gdx.graphics.getDeltaTime());
      if (junkOn)
        updateJunkInstances();
      if (lampsOn)
        updateLampInstances();
    }
    camController.update();
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    Gdx.gl.glClearColor(.05f, .05f, .1f, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    batch.begin(cam);
    batch.render(backdrop, env);
    for (ModelInstance t : tileInstances)
      batch.render(t, env);
    for (ModelInstance s : spriteInstances)
      batch.render(s, env);
    if (junkOn)
      for (ModelInstance j : junkInstances)
        batch.render(j, env);
    if (lampsOn)
      for (ModelInstance l : lampInstances)
        batch.render(l, env);
    batch.end();
    // blended decals go after the opaque world so mist, flames and weather layer over it
    effects.setDepthRange(midZ(), slabDepth() / 2f + 3);
    effects.update(Gdx.graphics.getDeltaTime(), mistOn, fireOn, rainOn, snowOn, stormOn);
    effects.render(cam, mistOn, fireOn, rainOn, snowOn);
    reportPerf();
    screenshotIfAsked();
  }

  private void printStatus() {
    System.out.printf("modo=%s smooth=%d (S/X) profundidad=%.2f (D/C) tiles=%.2fx (T/G) "
            + "luz=%s (L) niebla=%s (N) fuego=%s (F) lluvia=%s (R) nieve=%s (B) "
            + "basura=%s x%d (J, K/H) lamparas=%s (P) tormenta=%s (E) velocidad=%sx (,/./0)%n",
        smooth ? "suave" : "voxel", smoothLevel, depthScale, tileDepth,
        darkMode ? "linterna" : "normal", mistOn ? "si" : "no", fireOn ? "si" : "no",
        rainOn ? "si" : "no", snowOn ? "si" : "no", junkOn ? "si" : "no", junkCount,
        lampsOn ? "si" : "no", stormOn ? "si" : "no",
        replay == null ? "?" : String.valueOf(replay.getSpeed()));
  }

  /** -Dperf=true: once a second, where the frame time actually goes. */
  private final boolean perf = Boolean.getBoolean("perf");
  private long nsBackdrop, nsSprites, nsTiles, perfLast;
  private int perfFrames;

  private void reportPerf() {
    if (!perf)
      return;
    long now = System.nanoTime();
    if (perfLast == 0)
      perfLast = now;
    if (now - perfLast < 1_000_000_000L)
      return;
    int tris = 0;
    for (ModelInstance i : tileInstances)
      tris += triangles(i);
    for (ModelInstance i : spriteInstances)
      tris += triangles(i);
    int n = Math.max(1, perfFrames);
    System.out.printf(
        "perf: %d fps | draws=%d tris=%dk | cpu/frame backdrop=%.2fms sprites=%.2fms tiles=%.2fms%n",
        Gdx.graphics.getFramesPerSecond(), tileInstances.size() + spriteInstances.size() + 1,
        tris / 1000, nsBackdrop / 1e6 / n, nsSprites / 1e6 / n, nsTiles / 1e6 / n);
    nsBackdrop = nsSprites = nsTiles = 0;
    perfFrames = 0;
    perfLast = now;
  }

  private static int triangles(ModelInstance inst) {
    int t = 0;
    for (com.badlogic.gdx.graphics.g3d.model.NodePart np : inst.nodes.first().parts)
      t += np.meshPart.size / 3;
    return t;
  }

  /** -Dshot=/path.png captures the framebuffer once sprites are on screen (visual check). */
  private void screenshotIfAsked() {
    String path = System.getProperty("shot");
    if (path == null || shotTaken || spriteInstances.isEmpty()
        || shownFrame < Integer.getInteger("shot.frame", 400))
      return;
    shotTaken = true;
    Pixmap shot = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(
        0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.absolute(path), shot, 0, true);
    shot.dispose();
    System.out.println("screenshot -> " + path + " (frame " + shownFrame + ")");
  }

  private boolean shotTaken;

  /** the 2D room: every screen byte decoded normally, sprite-owned bytes erased to paper. */
  private void updateBackdrop(TaintReplay.FrameSnapshot snap) {
    // emissive makes the backdrop glow on its own — exactly what lantern mode must NOT do:
    // there it only reflects the ambient gloom and whatever sprite light reaches it
    Material mat = backdrop.materials.first();
    if (darkMode)
      mat.remove(TextureAttribute.Emissive);
    else if (!mat.has(TextureAttribute.Emissive))
      mat.set(TextureAttribute.createEmissive(screenTex));
    for (int y = 0; y < H; y++) {
      int rowAddr = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2);
      for (int col = 0; col < 32; col++) {
        int attr = snap.attrs()[(y >> 3) * 32 + col] & 0xff;
        Color ink = PALETTE[(attr & 7) | ((attr >> 3) & 8)];
        Color paper = PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)];
        int i = rowAddr | col;
        // the moving sprites' actual pixels, for pixel-accurate weather collision
        spritePix[i] = (byte) (snap.owner()[i] != 0 ? snap.pixels()[i] : 0);
        int bits = snap.owner()[i] != 0 || snap.tile()[i] != 0 ? 0 : snap.pixels()[i] & 0xff;
        for (int bit = 0; bit < 8; bit++)
          pixmap.drawPixel(col * 8 + bit, y, Color.rgba8888(
              (bits & (0x80 >> bit)) != 0 ? ink : paper));
      }
    }
    screenTex.draw(pixmap, 0, 0);
  }

  /**
   * taint-owned screen bytes -> one voxel instance per CONNECTED blob of the same sprite.
   * Same-base blobs apart in space are different entities: Willy's animation frame 40256 is
   * also what the lives row draws, and two guardians share one sheet — merging them by base
   * put Willy's box halfway to the lives row.
   */
  private void updateSprites(TaintReplay.FrameSnapshot snap) {
    int[][] grid = new int[H][32];
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7);
      grid[y][i & 31] = snap.owner()[i];
    }
    List<int[]> blobs = new ArrayList<>(); // base, minCol, minRow, maxCol, maxRow, paletteIdx
    java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
    int[] inkVotes = new int[16];
    for (int y0 = 0; y0 < H; y0++)
      for (int c0 = 0; c0 < 32; c0++) {
        int base = grid[y0][c0];
        if (base == 0)
          continue;
        int[] b = {base, c0, y0, c0, y0, 7};
        int bytes = 0;
        java.util.Arrays.fill(inkVotes, 0);
        queue.add(new int[]{c0, y0});
        grid[y0][c0] = 0;
        while (!queue.isEmpty()) {
          int[] p = queue.poll();
          bytes++;
          b[1] = Math.min(b[1], p[0]);
          b[2] = Math.min(b[2], p[1]);
          b[3] = Math.max(b[3], p[0]);
          b[4] = Math.max(b[4], p[1]);
          // the sprite's color: every owned byte votes for the INK of its attribute cell —
          // the ink IS what the game paints the sprite's pixels with at that spot
          int attr = snap.attrs()[(p[1] >> 3) * 32 + p[0]] & 0xff;
          int ink = (attr & 7) | ((attr >> 3) & 8);
          if ((ink & 7) != ((attr >> 3) & 7)) // ink == paper would be invisible: no vote
            inkVotes[ink]++;
          for (int dy = -1; dy <= 1; dy++)
            for (int dc = -1; dc <= 1; dc++) {
              int c = p[0] + dc, y = p[1] + dy;
              if (c >= 0 && c < 32 && y >= 0 && y < H && grid[y][c] == base) {
                grid[y][c] = 0;
                queue.add(new int[]{c, y});
              }
            }
        }
        if (bytes >= 4) {
          int bestInk = 7;
          for (int i = 0; i < 16; i++)
            if (inkVotes[i] > inkVotes[bestInk] || (inkVotes[bestInk] == 0 && inkVotes[i] > 0))
              bestInk = i;
          b[5] = bestInk;
          blobs.add(b);
        }
      }
    spriteInstances.clear();
    spriteBoxes.clear();
    for (int[] blob : blobs) {
      int base = blob[0] - 1;
      int[] b = {blob[1], blob[2], blob[3], blob[4]};
      int bytes = catalog.sizeOf.getOrDefault(base, 32);
      Model model = modelCache.computeIfAbsent(base, k -> smooth
          ? SmoothSpriteBuilder.build(k, bytes, 2, replay::memByte, smoothLevel, depthScale)
          : VoxelSpriteBuilder.build(k, bytes, 2, replay::memByte, smoothLevel, depthScale));
      ModelInstance inst = new ModelInstance(model);
      float cx = (b[0] + b[2] + 1) * 8 / 2f;          // byte cols -> pixels
      float cy = H - (b[1] + b[3] + 1) / 2f;          // screen y down -> world y up
      inst.transform.setToTranslation(cx, cy, midZ());
      // playfield blobs only: the lives-row Willys must not kick junk around
      if (cy > 66)
        spriteBoxes.add(new float[]{cx, cy, (b[2] - b[0] + 1) * 4f, (b[3] - b[1] + 1) / 2f});
      Color c = PALETTE[blob[5]];
      inst.materials.first().set(ColorAttribute.createDiffuse(c));
      if (darkMode) {
        // the sprite IS a light source: it glows a little itself and casts a small pool
        // of its own color around it — enough to make out its surroundings, no more
        inst.materials.first().set(ColorAttribute.createEmissive(
            c.r * .4f, c.g * .4f, c.b * .4f, 1));
        frameLights.add(new com.badlogic.gdx.graphics.g3d.environment.PointLight().set(
            .5f + c.r * .5f, .5f + c.g * .5f, .5f + c.b * .5f,
            cx, cy, midZ() + 14, spriteLightIntensity));
      }
      spriteInstances.add(inst);
    }
  }

  /** the tile slabs' full extent in z: tileDepth is a multiplier over half a cell (4px). */
  private float slabDepth() {
    return 4f * depthScale * tileDepth;
  }

  /**
   * The world's mid-depth plane: tiles, sprites and items all center here, so the slabs'
   * back faces rest just off the backdrop and the characters walk INSIDE the platforms
   * instead of floating in front of them. The 1.5 gap keeps the slab backs (and the 1px
   * ink lip behind them) clear of the backdrop plane — coplanar faces shimmer.
   */
  private float midZ() {
    return slabDepth() / 2f + 1.5f;
  }

  /**
   * 8x8 tile cells: a screen byte whose origin lands in a tile-template zone belongs to a
   * platform / wall / conveyor / item. The row-0 leaf address IS the tile bitmap's start
   * (rows are consecutive in the template), so the model reads its 8 bytes straight from
   * static memory — no knowledge of the record layout needed. One instance per cell; the
   * cell's own attribute colors it (items flash exactly like in the game).
   *
   * <p>Platform/wall tiles become SOLID slabs ({@link TileSlabBuilder}): ink and paper
   * both extrude the full {@code tileDepth}, so the platform is a massive two-color block,
   * not ink bumps over a flat paper. Item leaves — recognized because their cells keep
   * changing ink over the same bitmap — stay inflated at 1x, floating at mid-depth like
   * the characters do.
   */
  private void updateTiles(TaintReplay.FrameSnapshot snap) {
    tileInstances.clear();
    System.arraycopy(solidCells, 0, prevSolidCells, 0, solidCells.length);
    java.util.Arrays.fill(solidCells, false);
    for (int cellY = 0; cellY < 24; cellY++)
      for (int col = 0; col < 32; col++) {
        int y0 = cellY * 8;
        int i0 = ((y0 & 0xC0) << 5) | ((y0 & 7) << 8) | ((y0 & 0x38) << 2) | col;
        int t = snap.tile()[i0];
        if (t == 0 || snap.owner()[i0] != 0)
          continue;
        int leaf = t - 1;
        // AIR (all-zero bitmap) is background, full stop: no slab, and crucially no item
        // tracking — guardians crossing air cells eventually latch the air leaf as "item",
        // and in lantern mode hundreds of invisible air-items would flood the room with
        // light until the darkness is gone
        if (emptyLeaves.computeIfAbsent(leaf, k -> {
          for (int r = 0; r < 8; r++)
            if (replay.memByte(k + r) != 0)
              return false;
          return true;
        }))
          continue;
        int attr = snap.attrs()[cellY * 32 + col] & 0xff;
        int cell = cellY * 32 + col;
        // ink-change tracking only when NO row of the cell is sprite-owned: a guardian
        // overlapping rows 1-7 (row 0 free) leaves the cell classified as tile but colors
        // it with its own attr, which would latch every platform leaf as "item"
        boolean clean = true;
        for (int r = 1; r < 8 && clean; r++)
          clean = snap.owner()[i0 | (r << 8)] == 0;
        if (clean) {
          int prev = prevLeafAttr[cell];
          if (prev != 0 && (prev >> 8) == t) {
            if (((prev ^ attr) & 7) != 0) {
              // an item's cell CYCLES through 3+ inks in a steady burst; everything else
              // is quieter AND poorer: the room reveal changes each cell once, and the
              // one-frame attr lag a passing guardian leaves behind only ever toggles
              // between two inks (the room's and the guardian's)
              int bits = (1 << (attr & 7)) | (1 << (prev & 7));
              if (shownFrame - cellLastChange[cell] <= 25) {
                cellInkChanges[cell]++;
                cellInkMask[cell] |= bits;
              } else {
                cellInkChanges[cell] = 1;
                cellInkMask[cell] = bits;
              }
              cellLastChange[cell] = shownFrame;
              if (cellInkChanges[cell] >= 4 && Integer.bitCount(cellInkMask[cell]) >= 3
                  && itemLeaves.add(leaf))
                System.out.println("item detectado: leaf $" + Integer.toHexString(leaf));
            }
          } else
            cellInkChanges[cell] = 0;
          prevLeafAttr[cell] = (t << 8) | attr;
        }
        boolean item = itemLeaves.contains(leaf);
        // an air cell (empty bitmap) builds no model; computeIfAbsent leaves null uncached,
        // so the cheap 8-byte mask check re-runs — fine
        Model model = modelCache.computeIfAbsent(item ? -leaf - 0x10000 : -leaf, k -> item
            ? (smooth
               ? SmoothSpriteBuilder.build(leaf, 8, 1, replay::memByte, smoothLevel, depthScale)
               : VoxelSpriteBuilder.build(leaf, 8, 1, replay::memByte, smoothLevel, depthScale))
            : TileSlabBuilder.build(leaf, replay::memByte, slabDepth()));
        if (model == null)
          continue;
        solidCells[cell] = true;
        ModelInstance inst = new ModelInstance(model);
        inst.transform.setToTranslation(col * 8 + 4, H - (y0 + 4), midZ());
        Color inkColor = PALETTE[(attr & 7) | ((attr >> 3) & 8)];
        if (item) {
          if (Boolean.getBoolean("fx.debug") && shownFrame % 250 == 0)
            System.out.println("celda item f=" + shownFrame + " (" + col + "," + cellY
                + ") fireOn=" + fireOn);
          inst.materials.first().set(ColorAttribute.createDiffuse(inkColor));
          if (fireOn)
            effects.addFireSpot(col * 8 + 4, H - (y0 + 4), midZ() + 6);
          if (darkMode) {
            // a treasure glinting in its corner: clear self-glow plus a small pool of
            // light in the item's OWN color, cycling as the ink flashes — noticeable
            // from afar without turning its corner into daylight. With fire on, the
            // light turns ember-warm and dances with the flames.
            inst.materials.first().set(ColorAttribute.createEmissive(
                inkColor.r * .75f, inkColor.g * .75f, inkColor.b * .75f, 1));
            float flick = fireOn ? effects.flicker(cell) : 1;
            frameLights.add(new com.badlogic.gdx.graphics.g3d.environment.PointLight().set(
                fireOn ? 1 : inkColor.r, fireOn ? .55f : inkColor.g, fireOn ? .2f : inkColor.b,
                col * 8 + 4, H - (y0 + 4), midZ() + 8, itemLightIntensity * flick));
          }
        } else {
          inst.getMaterial(TileSlabBuilder.INK).set(ColorAttribute.createDiffuse(inkColor));
          Material paper = inst.getMaterial(TileSlabBuilder.PAPER);
          if (paper != null) // an all-ink bitmap has no paper part
            paper.set(ColorAttribute.createDiffuse(
                PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)]));
        }
        tileInstances.add(inst);
      }
    // a room switch redraws most of the screen at once — the accumulated snow drifts
    // belong to platforms that no longer exist
    int changed = 0;
    for (int i = 0; i < solidCells.length; i++)
      if (solidCells[i] != prevSolidCells[i])
        changed++;
    if (changed > 60) {
      effects.clearSnow();
      junkSpawnPending = true;
    }
    // the junk's static world rebuilds ONLY here — the transient holes a passing sprite
    // punches into solidCells (its cell rows read as "not tile") never reach the physics
    if ((junkOn || lampsOn) && junkSpawnPending) {
      junk.roomChanged(solidCells, java.util.Arrays.hashCode(solidCells),
          junkOn ? junkCount : 0, lampsOn ? 3 : 0);
      junkSpawnPending = false;
    }
  }

  /** one shared model per junk kind; each prop instance is tinted its own ZX color. */
  private Model junkModel(JunkPhysics.Kind k) {
    return junkModels.computeIfAbsent(k, kind -> {
      ModelBuilder mb = new ModelBuilder();
      Material m = new Material(ColorAttribute.createDiffuse(Color.WHITE));
      long attrs = Usage.Position | Usage.Normal;
      return switch (kind) {
        case BALL -> mb.createSphere(kind.w, kind.h, kind.w, 12, 8, m, attrs);
        case APPLE -> mb.createSphere(kind.w, kind.h, kind.w, 10, 8, m, attrs);
        case BOTTLE, CAN -> mb.createCylinder(kind.w, kind.h, kind.w, 10, m, attrs);
        case BOX -> mb.createBox(kind.w, kind.h, kind.w * .8f, m, attrs);
      };
    });
  }

  /** how far a prop's zFrac displaces it off the sprites' plane, inside the slab depth. */
  private float junkZ(JunkPhysics.Prop p) {
    return midZ() + p.zFrac * Math.max(0, slabDepth() / 2 - 3);
  }

  /**
   * the lamp, origin at its pivot so the Box2D angle rotates the whole thing around the
   * hanging point: a thin metal rod, a cone shade, and a glowing bulb at the tip.
   */
  private Model lampModel() {
    if (lampModel != null)
      return lampModel;
    float len = 13;
    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    long attrs = Usage.Position | Usage.Normal;
    Material rodMat = new Material(ColorAttribute.createDiffuse(.45f, .45f, .5f, 1));
    com.badlogic.gdx.graphics.g3d.model.Node rod = mb.node();
    rod.translation.set(0, -len / 2f, 0);
    com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder.build(
        mb.part("rod", GL20.GL_TRIANGLES, attrs, rodMat), 1.4f, len, 1.4f);
    Material shadeMat = new Material(ColorAttribute.createDiffuse(.16f, .32f, .16f, 1));
    com.badlogic.gdx.graphics.g3d.model.Node shade = mb.node();
    shade.translation.set(0, -len + 1f, 0);
    com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder.build(
        mb.part("shade", GL20.GL_TRIANGLES, attrs, shadeMat), 8, 5, 8, 12);
    Material bulbMat = new Material(ColorAttribute.createDiffuse(1, .95f, .8f, 1),
        ColorAttribute.createEmissive(.9f, .8f, .55f, 1));
    com.badlogic.gdx.graphics.g3d.model.Node bulb = mb.node();
    bulb.translation.set(0, -len - 2f, 0);
    com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder.build(
        mb.part("bulb", GL20.GL_TRIANGLES, attrs, bulbMat), 3.4f, 3.4f, 3.4f, 10, 8);
    lampModel = mb.end();
    return lampModel;
  }

  /** mirror the swinging lamps; the pivot is fixed, the Box2D angle does the swaying. */
  private void updateLampInstances() {
    List<JunkPhysics.Lamp> lamps = junk.lamps();
    if (junk.generation() != lampGeneration || lamps.size() != lampInstances.size()) {
      lampGeneration = junk.generation();
      lampInstances.clear();
      for (int i = 0; i < lamps.size(); i++)
        lampInstances.add(new ModelInstance(lampModel()));
    }
    for (int i = 0; i < lamps.size(); i++) {
      JunkPhysics.Lamp l = lamps.get(i);
      lampInstances.get(i).transform.setToTranslation(l.px, l.py, midZ())
          .rotate(0, 0, 1, l.angleDeg());
    }
  }

  /** mirror the physics props into render instances; position + spin come from Box2D. */
  private void updateJunkInstances() {
    List<JunkPhysics.Prop> props = junk.props();
    if (junk.generation() != junkGeneration || props.size() != junkInstances.size()) {
      junkGeneration = junk.generation();
      junkInstances.clear();
      for (JunkPhysics.Prop p : props) {
        ModelInstance inst = new ModelInstance(junkModel(p.kind));
        Color c = PALETTE[p.color];
        inst.materials.first().set(ColorAttribute.createDiffuse(c));
        // a glowing prop shines from within, item-style: visibly brighter than anything
        // merely lit, in normal mode and in the dark alike
//        if (p.glow)
//          inst.materials.first().set(ColorAttribute.createEmissive(
//              c.r * .6f, c.g * .6f, c.b * .6f, 1));
        junkInstances.add(inst);
      }
    }
    for (int i = 0; i < props.size(); i++) {
      JunkPhysics.Prop p = props.get(i);
      junkInstances.get(i).transform.setToTranslation(p.x(), p.y(), junkZ(p))
          .rotate(0, 0, 1, p.angleDeg());
    }
  }

  @Override
  public void dispose() {
    if (replay != null)
      replay.stop();
    batch.dispose();
    pixmap.dispose();
    screenTex.dispose();
    backdropModel.dispose();
    modelCache.values().forEach(Model::dispose);
    junkModels.values().forEach(Model::dispose);
    if (lampModel != null)
      lampModel.dispose();
    junk.dispose();
    effects.dispose();
  }

  public static void main(String[] args) {
//    System.setProperty("sprites3d", "voxel");
    String rzx = args.length > 0 ? args[0]
        : "/home/fernando/detodo/spectrum/oozx/Jet Set Willy - Mildly Patched.rzx";
    // the catalog must be THIS game's: analysis/analysis.db rotates between games (it held
    // Dynamite Dan's once, and JSW replayed with DD's catalog shows almost no sprites)
    String db = args.length > 1 ? args[1] : "analysis/jsw.db";
    Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
    cfg.setTitle("JSW 3D — sprites por taint de origenes");
    cfg.setWindowedMode(1024, 768);
    // 4x MSAA: 1px-wide voxel columns 28 units deep shimmer badly at oblique angles without it
    cfg.setBackBufferConfig(8, 8, 8, 8, 16, 0, Integer.getInteger("msaa", 4));
    if (!Boolean.parseBoolean(System.getProperty("vsync", "true")))
      cfg.useVsync(false); // -Dvsync=false uncaps the framerate (perf measurements)
    new Lwjgl3Application(new JSW3D(rzx, db), cfg);
  }
}
