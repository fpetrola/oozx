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
  /** smooth inflated mesh vs voxel boxes; M toggles at runtime. */
  private boolean smooth = !"voxel".equals(System.getProperty("sprites3d", "smooth"));

  public JSW3D(String rzxPath, String dbPath) {
    this.rzxPath = rzxPath;
    this.dbPath = dbPath;
  }

  @Override
  public void create() {
    batch = new ModelBatch();
    cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    cam.position.set(W / 2f, H / 2f + 30, 290);
    cam.lookAt(W / 2f, H / 2f, 0);
    cam.near = 1;
    cam.far = 1000;
    cam.update();
    camController = new CameraInputController(cam);
    Gdx.input.setInputProcessor(new com.badlogic.gdx.InputMultiplexer(
        new com.badlogic.gdx.InputAdapter() {
          @Override
          public boolean keyDown(int keycode) {
            if (keycode == com.badlogic.gdx.Input.Keys.M) {
              smooth = !smooth;
              modelCache.values().forEach(Model::dispose);
              modelCache.clear();
              System.out.println("modo sprites: " + (smooth ? "suave" : "voxel"));
              return true;
            }
            return false;
          }
        }, camController));
    System.out.println("modo sprites: " + (smooth ? "suave" : "voxel") + " (tecla M alterna)");

    env = new Environment();
    env.set(new ColorAttribute(ColorAttribute.AmbientLight, .5f, .5f, .5f, 1));
    env.add(new DirectionalLight().set(1f, 1f, 1f, -0.4f, -0.6f, -1f));

    pixmap = new Pixmap(W, H, Pixmap.Format.RGBA8888);
    screenTex = new Texture(W, H, Pixmap.Format.RGBA8888);
    Material mat = new Material(TextureAttribute.createDiffuse(screenTex),
        TextureAttribute.createEmissive(screenTex));
    backdropModel = new ModelBuilder().createRect(
        0, 0, 0, W, 0, 0, W, H, 0, 0, H, 0, 0, 0, 1,
        mat, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
    backdrop = new ModelInstance(backdropModel);

    try {
      SpriteCatalog catalog = new SpriteCatalog(dbPath, 128);
      replay = new TaintReplay(rzxPath, catalog, snap -> latest = snap);
      replayThread = new Thread(replay, "taint-replay");
      replayThread.setDaemon(true);
      replayThread.start();
    } catch (Exception e) {
      throw new RuntimeException("No pude cargar el catalogo de sprites de " + dbPath, e);
    }
  }

  @Override
  public void render() {
    TaintReplay.FrameSnapshot snap = latest;
    if (snap != null && snap.frame() != shownFrame) {
      shownFrame = snap.frame();
      updateBackdrop(snap);
      updateSprites(snap);
    }
    camController.update();
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    Gdx.gl.glClearColor(.05f, .05f, .1f, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    batch.begin(cam);
    batch.render(backdrop, env);
    for (ModelInstance s : spriteInstances)
      batch.render(s, env);
    batch.end();
    screenshotIfAsked();
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
    for (int y = 0; y < H; y++) {
      int rowAddr = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2);
      for (int col = 0; col < 32; col++) {
        int attr = snap.attrs()[(y >> 3) * 32 + col] & 0xff;
        Color ink = PALETTE[(attr & 7) | ((attr >> 3) & 8)];
        Color paper = PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)];
        int i = rowAddr | col;
        int bits = snap.owner()[i] != 0 ? 0 : snap.pixels()[i] & 0xff;
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
    for (int[] blob : blobs) {
      int base = blob[0] - 1;
      int[] b = {blob[1], blob[2], blob[3], blob[4]};
      Model model = modelCache.computeIfAbsent(base, k -> smooth
          ? SmoothSpriteBuilder.build(k, replay::memByte)
          : VoxelSpriteBuilder.build(k, replay::memByte));
      ModelInstance inst = new ModelInstance(model);
      float cx = (b[0] + b[2] + 1) * 8 / 2f;          // byte cols -> pixels
      float cy = H - (b[1] + b[3] + 1) / 2f;          // screen y down -> world y up
      inst.transform.setToTranslation(cx, cy, 6);
      inst.materials.first().set(ColorAttribute.createDiffuse(PALETTE[blob[5]]));
      spriteInstances.add(inst);
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
  }

  public static void main(String[] args) {
    String rzx = args.length > 0 ? args[0]
        : "/home/fernando/detodo/spectrum/oozx/Jet Set Willy - Mildly Patched.rzx";
    // the catalog must be THIS game's: analysis/analysis.db rotates between games (it held
    // Dynamite Dan's once, and JSW replayed with DD's catalog shows almost no sprites)
    String db = args.length > 1 ? args[1] : "analysis/jsw.db";
    Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
    cfg.setTitle("JSW 3D — sprites por taint de origenes");
    cfg.setWindowedMode(1024, 768);
    new Lwjgl3Application(new JSW3D(rzx, db), cfg);
  }
}
