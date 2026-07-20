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
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

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

  /** the profile id ("jsw"/"mm"/"dd"/...); scopes the persisted config file per game. */
  private static String activeGame = "jsw";
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
  /**
   * -Dblobs=adjacent (composite engines: Exolon): sprite blobs group ANY adjacent owned
   * byte regardless of base, and the model is inflated from the pixels already composited
   * on screen (doc DETECCION-SPRITES-3D §5, atajo de render) instead of from the memory
   * bitmap — a multi-piece object has no single memory layout to read. Default "base"
   * keeps the JSW/MM/DD behavior untouched.
   */
  private final boolean blobsAdjacent = "adjacent".equals(System.getProperty("blobs", "base"));
  /**
   * -Dtiles=false: leave background zones in the flat 2D backdrop instead of building slabs
   * for them. JSW/MM want the slabs — their backgrounds ARE tidy 8x8 tiles and the room
   * reads as architecture. Exolon's "tiles" are a dithered rock texture spread over the
   * whole screen: slabbing them turns the picture into noise, and terrain is scenery the
   * player never interacts with in 3D anyway.
   */
  private final boolean tilesOn = !"false".equals(System.getProperty("tiles"));
  /** screen-pixel sprite models, keyed by content hash (animation frames each get one). */
  private final Map<Long, Model> pixModelCache = new HashMap<>();
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
   * leaf -> last frame its ink cycled. A REAL item flashes relentlessly, every frame it
   * is on screen; a platform leaf that got mis-latched (sprite traffic can fake the ink
   * burst over time) goes quiet as soon as the sprites leave — one silent second and the
   * latch is dropped, so the slab comes back instead of staying an ink ghost forever.
   */
  private final Map<Integer, Integer> leafLastFlash = new HashMap<>();
  /**
   * L toggles lantern mode (-Ddark=true starts in it): a faint ambient lets the whole room
   * be barely made out, each MOVING sprite carries its own small light that brightens its
   * surroundings, and items blaze with light of their own so they stand out in the gloom.
   */
  private boolean darkMode = Boolean.getBoolean("dark");
  private float darkAmbient = Float.parseFloat(System.getProperty("dark.ambient", "0.04"));
  private float spriteLightIntensity = Float.parseFloat(System.getProperty("dark.sprite", "300"));
  private float itemLightIntensity = Float.parseFloat(System.getProperty("dark.item", "500"));
  /** leaf -> its bitmap is all zeros (air): no slab, no item tracking, no light — background. */
  private final Map<Integer, Boolean> emptyLeaves = new HashMap<>();
  /**
   * per cell, the last tile (leaf + 1) and attr the screen showed there while sprite-free:
   * when a sprite walks THROUGH the cell and steals its bytes, the cached tile is drawn as
   * a half-transparent ghost instead of letting the platform vanish around the character.
   */
  private final int[] cellLeaf = new int[24 * 32];
  private final int[] cellAttr = new int[24 * 32];
  /**
   * tiles (slabs, ghosts, item detection) exist only in the PLAYFIELD — the top 16 cell
   * rows in JSW and Manic Miner alike (-Dplayfield.rows overrides). Below lives the
   * status area: its text and bars are drawn from data that happens to be inside the
   * catalogued zones (MM keeps the cavern NAME in the same record as the tiles), and
   * slab-ifying those bytes shreds them — the status belongs to the 2D backdrop.
   */
  private final int playfieldRows = Integer.getInteger("playfield.rows", 16);
  /**
   * Q steps the ghosts' opacity down (.95 → .80 → … → .20, then wraps back up);
   * -Dghost.alpha sets where it starts — nearly opaque by default.
   */
  private float ghostAlpha = Float.parseFloat(System.getProperty("ghost.alpha", "0.85"));
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
  /**
   * O cast shadows (-Dfx.shadows=false disables): the sun becomes a
   * {@link DirectionalShadowLight} and a depth pre-pass renders the CASTERS — sprites,
   * junk, lamps, rope — so they throw real shadows onto the slabs and the backdrop.
   * Normal mode only: lantern mode has no sun to cast them.
   */
  private boolean shadowsOn = !"false".equals(System.getProperty("fx.shadows"));
  private DirectionalShadowLight shadowLight;
  private ModelBatch shadowBatch;
  /**
   * while it rains (R) the world soaks: specular sheen fades in over ~6s on slabs,
   * sprites and junk — light sources glint off them as if wet — and drains away once
   * the rain stops. The rain also feeds standing PUDDLES on the platform tops, which
   * characters splash through and which drain when the rain ends.
   */
  private float wetness;
  /**
   * V wind (-Dfx.wind=true): one gusting wind signal ({@link AmbientEffects#wind()})
   * that tilts the rain, rides the snow and leaves, bends flames and mist, and leans on
   * the junk through Box2D drag. U balloons+bubbles (-Dfx.balloons): buoyant props that
   * pool under the platforms, get batted around, and POP into colored bursts when hit
   * hard. Y autumn leaves (-Dfx.leaves): flutter down, rest on floors, and fly up again
   * when a character runs through or a gust peels them off.
   */
  private boolean windOn = Boolean.getBoolean("fx.wind");
  private boolean balloonsOn = Boolean.getBoolean("fx.balloons");
  private boolean leavesOn = Boolean.getBoolean("fx.leaves");
  /**
   * Z dust (-Dfx.dust): moving sprites shed motes that drift down and pile into little
   * dirt mounds on the floor below their path — the room soils as time passes.
   */
  private boolean dustOn = Boolean.getBoolean("fx.dust");
  /** 0 toggles the on-screen key guide (-Dhelp=true starts with it open). */
  private boolean helpOn = Boolean.getBoolean("help");
  private com.badlogic.gdx.graphics.g2d.SpriteBatch uiBatch;
  private com.badlogic.gdx.graphics.g2d.BitmapFont uiFont;
  private Texture whitePix;
  /**
   * Everything the user calibrates — every effect toggle, counts, ghost opacity, camera
   * angle/pan/zoom, replay speed — persists to a JSON (~/.jsw3d-config.json, or
   * -Dconfig.file=...) loaded at startup and saved ~1s after the last change. Values in
   * the file win over -D defaults; -Dconfig=false or a -Dshot run disables the whole
   * thing so scripted runs stay deterministic.
   */
  private final boolean configEnabled = System.getProperty("shot") == null
      && !"false".equals(System.getProperty("config"));
  private boolean configDirty;
  private long configDirtyAt;
  private final Vector3 savedCamPos = new Vector3(), savedCamDir = new Vector3();
  private float cfgSpeed = Float.parseFloat(System.getProperty("speed", "1"));
  /**
   * The tuning menu: TAB cycles the effect groups, up/down picks a parameter, left/right
   * adjusts it (held keys repeat), ESC closes, and 6 quiet seconds hide it. Every value
   * lives in {@link #params} as a get/set pair over the real live field, so changes apply
   * instantly and persist in the config JSON under their {@code id}.
   */
  private record Param(String id, String legacy, String group, String name, float min, float max,
                       float step, boolean integer, java.util.function.Supplier<Float> get,
                       java.util.function.Consumer<Float> set) {
  }

  /** an effect's on/off switch, addressed by its nested JSON path (+ legacy flat key). */
  private record Toggle(String path, String legacy, java.util.function.Supplier<Boolean> get,
                        java.util.function.Consumer<Boolean> set) {
  }

  private final List<Param> params = new ArrayList<>();
  private final List<Toggle> toggles = new ArrayList<>();
  private final List<String> paramGroups = new ArrayList<>();
  private int tuneGroup = Integer.getInteger("tune", 0) - 1, tuneParam;
  /**
   * F1 (-Dplay=true from frame one) CUTS the RZX replay and hands the input to the
   * player: every letter/digit/space/enter/shift goes to the Spectrum keyboard matrix
   * (Alt = symbol shift), so the game is played live from that exact moment. One way —
   * once the input diverges, the recording can't resume. While playing, the effect
   * hotkeys stay reachable as Ctrl+key, the guide moves to F2, and TAB/arrows/ESC keep
   * driving the tuning menu.
   */
  private boolean playMode = Boolean.getBoolean("play");
  private java.awt.Component keyEventSource;
  private long tuneShownAt, lastAdjustAt;
  /** counts the tuning menu edits live; lamps/balloons/junk regenerate on change. */
  private int lampCount = 3, balloonCount = 6, bubbleCount = 6;
  private float lampLightScale = 1, junkGravity = 1;

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
    shadowLight = new DirectionalShadowLight(2048, 2048, 320, 260, 1, 400);
    // tilted well off the z axis so shadows STRETCH across the slab tops beside their
    // casters, instead of falling straight back onto the backdrop where they vanish
    shadowLight.set(1f, 1f, 1f, -0.55f, -0.75f, -0.5f);
    shadowBatch = new ModelBatch(new DepthShaderProvider());
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
            // playing live: game keys go straight to the Spectrum matrix; Ctrl+key keeps
            // naming a preset: keys build the name, Enter confirms, Esc cancels — nothing
            // else fires while the little prompt is up
            if (namingPreset) {
              if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                savePreset(presetTyping.toString());
                namingPreset = false;
              } else if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                namingPreset = false;
                flashPreset("cancelado");
              } else if (keycode == com.badlogic.gdx.Input.Keys.BACKSPACE
                  && presetTyping.length() > 0) {
                presetTyping.setLength(presetTyping.length() - 1);
              }
              return true;
            }
            // the effect toggles reachable, and TAB/arrows/ESC still drive the tuning menu
            boolean ctrl = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_RIGHT);
            if (playMode && !ctrl && (tuneGroup < 0 || !isTuningKey(keycode))) {
              int vk = vkFor(keycode);
              if (vk >= 0) {
                spectrumKey(vk, true);
                return true;
              }
            }
            // only the keys that change the VOXELS force a rebuild; lights, weather and
            // replay speed leave every cached model exactly as it is
            boolean rebuild = true;
            switch (keycode) {
              case com.badlogic.gdx.Input.Keys.F1 -> {
                if (!playMode) {
                  playMode = true;
                  replay.goLive();
                }
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F2 -> {
                helpOn = !helpOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F3 -> {
                // save current state as a NAMED ambience preset
                namingPreset = true;
                presetTyping.setLength(0);
                if (presetIdx >= 0)
                  presetTyping.append(presets.get(presetIdx)); // seed with current for re-save
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F4 -> {
                cyclePreset(1); // next ambience preset (Shift = previous)
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F5 -> {
                cyclePreset(-1);
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F6 -> {
                deleteCurrentPreset();
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.M -> smooth = !smooth;
              case com.badlogic.gdx.Input.Keys.S -> smoothLevel = Math.min(10, smoothLevel + 1);
              case com.badlogic.gdx.Input.Keys.X -> smoothLevel = Math.max(0, smoothLevel - 1);
              case com.badlogic.gdx.Input.Keys.D -> depthScale = Math.min(10f, depthScale * 1.25f);
              case com.badlogic.gdx.Input.Keys.C -> depthScale = Math.max(.05f, depthScale / 1.25f);
              case com.badlogic.gdx.Input.Keys.T -> tileDepth = Math.min(60f, tileDepth * 1.25f);
              case com.badlogic.gdx.Input.Keys.G -> tileDepth = Math.max(.2f, tileDepth / 1.25f);
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
              case com.badlogic.gdx.Input.Keys.O -> {
                shadowsOn = !shadowsOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.V -> {
                windOn = !windOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.U -> {
                balloonsOn = !balloonsOn;
                junkSpawnPending = true;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.Y -> {
                leavesOn = !leavesOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.Q -> {
                ghostAlpha -= .15f;
                if (ghostAlpha < .18f)
                  ghostAlpha = .95f;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.Z -> {
                dustOn = !dustOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.K -> {
                junkCount = Math.min(500, junkCount + 6);
                junkSpawnPending = junkOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.H -> {
                junkCount = Math.max(0, junkCount - 6);
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
              case com.badlogic.gdx.Input.Keys.ENTER -> {
                replay.setSpeed(1);
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.NUM_0, com.badlogic.gdx.Input.Keys.NUMPAD_0 -> {
                helpOn = !helpOn;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.TAB -> {
                tuneGroup = (tuneGroup + 1) % paramGroups.size();
                tuneParam = 0;
                tuneShownAt = System.currentTimeMillis();
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.ESCAPE -> {
                tuneGroup = -1;
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.UP, com.badlogic.gdx.Input.Keys.DOWN -> {
                if (tuneGroup < 0)
                  return false;
                int n = groupParams().size();
                tuneParam = (tuneParam
                    + (keycode == com.badlogic.gdx.Input.Keys.UP ? n - 1 : 1)) % n;
                tuneShownAt = System.currentTimeMillis();
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.LEFT, com.badlogic.gdx.Input.Keys.RIGHT -> {
                if (tuneGroup < 0)
                  return false;
                adjustParam(keycode == com.badlogic.gdx.Input.Keys.RIGHT ? 1 : -1);
                rebuild = false;
              }
              default -> {
                return false;
              }
            }
            configDirty = true;
            configDirtyAt = System.currentTimeMillis();
            return afterKey(true);
          }

          @Override
          public boolean keyTyped(char c) {
            // build the preset name; letters/digits/space/dash/underscore, cap the length
            if (namingPreset && presetTyping.length() < 40
                && (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_')) {
              presetTyping.append(c);
              return true;
            }
            return false;
          }

          @Override
          public boolean keyUp(int keycode) {
            if (playMode && !namingPreset) {
              int vk = vkFor(keycode);
              if (vk >= 0) {
                spectrumKey(vk, false); // releases always reach the game, Ctrl or not
                return true;
              }
            }
            return false;
          }

          private boolean afterKey(boolean rebuild) {
            if (rebuild) {
              try {
                  Thread.sleep(100);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
              modelCache.values().forEach(Model::dispose);
              modelCache.clear();
              pixModelCache.values().forEach(Model::dispose);
              pixModelCache.clear();
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

    uiBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
    uiFont = new com.badlogic.gdx.graphics.g2d.BitmapFont();
    Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    px.drawPixel(0, 0, 0xffffffff);
    whitePix = new Texture(px);
    px.dispose();
    registerParams();
    loadConfig();
    loadPresetList();
    // -Dpreset=<name> starts in a named ambience instead of the auto-saved config
    String startPreset = System.getProperty("preset");
    if (startPreset != null && presets.contains(startPreset))
      loadPreset(presets.indexOf(startPreset));
    savedCamPos.set(cam.position);
    savedCamDir.set(cam.direction);
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
      replay.setSpeed(cfgSpeed);
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
      if (shadowsOn) {
        // the sun and the shadow caster are the SAME light, so lit and shadowed sides agree
        env.add(shadowLight);
        env.shadowMap = shadowLight;
      } else
        env.add(new DirectionalLight().set(1f, 1f, 1f, -0.55f, -0.75f, -0.5f));
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
      if (blobsAdjacent)
        demoteOversizedBlobs(snap); // must precede updateBackdrop: it reads owner()
      updateBackdrop(snap);
      long t1 = perf ? System.nanoTime() : 0;
      updateSprites(snap);
      if (junkOn || lampsOn || balloonsOn)
        junk.syncSprites(spriteBoxes, snapDt);
      if (dustOn)
        effects.spriteDust(spriteBoxes, snapDt);
      snapDt = 0;
      long t2 = perf ? System.nanoTime() : 0;
      if (tilesOn)
        updateTiles(snap);
      long t3 = perf ? System.nanoTime() : 0;
      // glowing junk casts its own small pool of light in the dark, like the items do;
      // capped so a big junk count can't starve the shader's point-light slots
      if ((junkOn || balloonsOn) && darkMode) {
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
              1f, .9f, .68f, l.bulbX(), l.bulbY(), midZ() + 8,
              itemLightIntensity * 1.2f * lampLightScale));
      rebuildEnv();
      if (perf) {
        nsBackdrop += t1 - t0;
        nsSprites += t2 - t1;
        nsTiles += t3 - t2;
        perfFrames++;
      }
    }
    if (junkOn || lampsOn || balloonsOn) {
      junk.setWind(windOn ? effects.wind() : 0);
      junk.update(Gdx.graphics.getDeltaTime());
      if (junkOn || balloonsOn)
        updateJunkInstances();
      if (lampsOn)
        updateLampInstances();
      if (!junk.pops().isEmpty()) {
        for (float[] p : junk.pops()) {
          Color c = PALETTE[(int) p[3]];
          effects.addBurst(p[0], p[1],
              midZ() + p[2] * Math.max(0, slabDepth() / 2 - 3), c.r, c.g, c.b);
        }
        junk.pops().clear();
      }
    }
    // rain soaks the world in and dries it out; the sheen is applied where the
    // instances get their materials, so it follows every rebuilt slab and sprite
    wetness = Math.max(0, Math.min(1,
        wetness + (rainOn ? Gdx.graphics.getDeltaTime() / 6 : -Gdx.graphics.getDeltaTime() / 15)));
    camController.update();
    // depth pre-pass from the sun's viewpoint: only the movable things cast (sprites,
    // junk, lamps, rope) — slabs and backdrop receive without shadowing themselves
    if (shadowsOn && !darkMode) {
      shadowLight.begin(new Vector3(W / 2f, H / 2f, 10), shadowLight.direction);
      shadowBatch.begin(shadowLight.getCamera());
      for (ModelInstance s : spriteInstances)
        shadowBatch.render(s);
      for (int i = 0; i < ropeCount; i++)
        shadowBatch.render(ropePool.get(i));
      if (junkOn)
        for (ModelInstance j : junkInstances)
          shadowBatch.render(j);
      if (lampsOn)
        for (ModelInstance l : lampInstances)
          shadowBatch.render(l);
      shadowBatch.end();
      shadowLight.end();
    }
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    Gdx.gl.glClearColor(.05f, .05f, .1f, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    batch.begin(cam);
    batch.render(backdrop, env);
    for (ModelInstance t : tileInstances)
      batch.render(t, env);
    for (ModelInstance s : spriteInstances)
      batch.render(s, env);
    for (int i = 0; i < ropeCount; i++)
      batch.render(ropePool.get(i), env);
    if (junkOn || balloonsOn)
      for (ModelInstance j : junkInstances)
        batch.render(j, env);
    if (lampsOn)
      for (ModelInstance l : lampInstances)
        batch.render(l, env);
    batch.end();
    // blended decals go after the opaque world so mist, flames and weather layer over it
    effects.setDepthRange(midZ(), slabDepth() / 2f + 3);
    effects.update(Gdx.graphics.getDeltaTime(), mistOn, fireOn, rainOn, snowOn, stormOn,
        windOn, leavesOn);
    effects.render(cam, mistOn, fireOn, rainOn, snowOn, leavesOn);
    renderHelp();
    // held arrows keep adjusting; 6 quiet seconds close the tuning panel on their own
    if (tuneGroup >= 0) {
      long now = System.currentTimeMillis();
      boolean l = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT);
      boolean r = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT);
      if ((l || r) && now - lastAdjustAt > 70)
        adjustParam(r ? 1 : -1);
      if (System.getProperty("tune") != null)
        tuneShownAt = now; // pinned open for screenshot verification
      if (now - tuneShownAt > 6000)
        tuneGroup = -1;
    }
    renderTuning();
    renderPresets();
    // camera drags/zooms mark the config dirty; a quiet second later it hits disk
    if (configEnabled) {
      if (!cam.position.epsilonEquals(savedCamPos, .4f)
          || !cam.direction.epsilonEquals(savedCamDir, .005f)) {
        savedCamPos.set(cam.position);
        savedCamDir.set(cam.direction);
        configDirty = true;
        configDirtyAt = System.currentTimeMillis();
      }
      if (configDirty && System.currentTimeMillis() - configDirtyAt > 1000) {
        configDirty = false;
        saveConfig();
      }
    }
    reportPerf();
    screenshotIfAsked();
  }

  /** keys the tuning menu owns while it is open — they must not reach the game. */
  private static boolean isTuningKey(int keycode) {
    return keycode == com.badlogic.gdx.Input.Keys.UP
        || keycode == com.badlogic.gdx.Input.Keys.DOWN
        || keycode == com.badlogic.gdx.Input.Keys.LEFT
        || keycode == com.badlogic.gdx.Input.Keys.RIGHT;
  }

  /** Gdx keycode -> AWT VK code for the Spectrum keyboard; -1 = not a game key. */
  private static int vkFor(int keycode) {
    if (keycode >= com.badlogic.gdx.Input.Keys.A && keycode <= com.badlogic.gdx.Input.Keys.Z)
      return java.awt.event.KeyEvent.VK_A + (keycode - com.badlogic.gdx.Input.Keys.A);
    if (keycode >= com.badlogic.gdx.Input.Keys.NUM_0
        && keycode <= com.badlogic.gdx.Input.Keys.NUM_9)
      return java.awt.event.KeyEvent.VK_0 + (keycode - com.badlogic.gdx.Input.Keys.NUM_0);
    return switch (keycode) {
      case com.badlogic.gdx.Input.Keys.SPACE -> java.awt.event.KeyEvent.VK_SPACE;
      case com.badlogic.gdx.Input.Keys.ENTER -> java.awt.event.KeyEvent.VK_ENTER;
      case com.badlogic.gdx.Input.Keys.SHIFT_LEFT, com.badlogic.gdx.Input.Keys.SHIFT_RIGHT ->
          java.awt.event.KeyEvent.VK_SHIFT;
      // the Spectrum's symbol shift: Alt, because Ctrl is the effects modifier
      case com.badlogic.gdx.Input.Keys.ALT_LEFT, com.badlogic.gdx.Input.Keys.ALT_RIGHT ->
          java.awt.event.KeyEvent.VK_CONTROL;
      case com.badlogic.gdx.Input.Keys.LEFT -> java.awt.event.KeyEvent.VK_LEFT;
      case com.badlogic.gdx.Input.Keys.RIGHT -> java.awt.event.KeyEvent.VK_RIGHT;
      case com.badlogic.gdx.Input.Keys.UP -> java.awt.event.KeyEvent.VK_UP;
      case com.badlogic.gdx.Input.Keys.DOWN -> java.awt.event.KeyEvent.VK_DOWN;
      default -> -1;
    };
  }

  /** feed a synthesized AWT key event into the live Spectrum keyboard matrix. */
  private void spectrumKey(int vk, boolean down) {
    com.fpetrola.z80.minizx.MiniZXKeyboard kb = replay == null ? null : replay.keyboard();
    if (kb == null)
      return;
    if (keyEventSource == null)
      keyEventSource = new java.awt.Container();
    java.awt.event.KeyEvent e = new java.awt.event.KeyEvent(keyEventSource,
        down ? java.awt.event.KeyEvent.KEY_PRESSED : java.awt.event.KeyEvent.KEY_RELEASED,
        System.currentTimeMillis(), 0, vk, java.awt.event.KeyEvent.CHAR_UNDEFINED);
    if (down)
      kb.keyPressed(e);
    else
      kb.keyReleased(e);
  }

  private void addParam(String id, String legacy, String group, String name, float min,
                        float max, float step, boolean integer,
                        java.util.function.Supplier<Float> get,
                        java.util.function.Consumer<Float> set) {
    params.add(new Param(id, legacy, group, name, min, max, step, integer, get, set));
    if (!paramGroups.contains(group))
      paramGroups.add(group);
  }

  private void addToggle(String path, String legacy, java.util.function.Supplier<Boolean> get,
                         java.util.function.Consumer<Boolean> set) {
    toggles.add(new Toggle(path, legacy, get, set));
  }

  /**
   * The config schema: every effect is an object under {@code effects} holding its own
   * {@code on} switch and properties (nested where it makes sense, like the wind's
   * vortex); scene-wide values live under {@code general}, the viewpoint under
   * {@code camera}. Params carry their pre-nesting flat id so old files still load.
   */
  private void registerParams() {
    addToggle("effects.mist.on", "mist", () -> mistOn, v -> mistOn = v);
    addToggle("effects.fire.on", "fire", () -> fireOn, v -> fireOn = v);
    addToggle("effects.rain.on", "rain", () -> rainOn, v -> rainOn = v);
    addToggle("effects.snow.on", "snow", () -> snowOn, v -> snowOn = v);
    addToggle("effects.storm.on", "storm", () -> stormOn, v -> stormOn = v);
    addToggle("effects.wind.on", "wind", () -> windOn, v -> windOn = v);
    addToggle("effects.leaves.on", "leaves", () -> leavesOn, v -> leavesOn = v);
    addToggle("effects.junk.on", "junk", () -> junkOn, v -> junkOn = v);
    addToggle("effects.balloons.on", "balloons", () -> balloonsOn, v -> balloonsOn = v);
    addToggle("effects.lamps.on", "lamps", () -> lampsOn, v -> lampsOn = v);
    addToggle("effects.dust.on", "dust", () -> dustOn, v -> dustOn = v);
    addToggle("effects.lantern.on", "dark", () -> darkMode, v -> darkMode = v);

    addParam("effects.rain.drops", "rain.drops", "Lluvia", "gotas", 0, 5000, 50, true,
        () -> (float) effects.dropCount, v -> effects.dropCount = Math.round(v));
    addParam("effects.rain.fallSpeed", "rain.speed", "Lluvia", "velocidad caida",
        .05f, 10, .15f, false, () -> effects.rainSpeed, v -> effects.rainSpeed = v);
    addParam("effects.rain.puddleMax", "rain.puddle", "Lluvia", "charco maximo", 1, 60, 2, true,
        () -> effects.puddleMax, v -> effects.puddleMax = v);
    addParam("effects.snow.flakes", "snow.flakes", "Nieve", "copos", 0, 5000, 50, true,
        () -> (float) effects.flakeCount, v -> effects.flakeCount = Math.round(v));
    addParam("effects.snow.fallSpeed", "snow.speed", "Nieve", "velocidad caida",
        .05f, 10, .15f, false, () -> effects.snowSpeed, v -> effects.snowSpeed = v);
    addParam("effects.snow.settledMax", "snow.settled", "Nieve", "acumulacion max",
        0, 50000, 500, true,
        () -> (float) effects.settledMax, v -> effects.settledMax = Math.round(v));
    addParam("effects.wind.base", "wind.base", "Viento", "fuerza base", 0, 20, .25f, false,
        () -> effects.windBase, v -> effects.windBase = v);
    addParam("effects.wind.gust", "wind.gust", "Viento", "fuerza rafagas", 0, 20, .25f, false,
        () -> effects.windGust, v -> effects.windGust = v);
    addParam("effects.wind.vortex.power", "wind.vortex", "Viento", "fuerza remolino",
        0, 20, .25f, false, () -> effects.vortexPower, v -> effects.vortexPower = v);
    addParam("effects.wind.vortex.radius", "wind.vortexRadius", "Viento", "radio remolino",
        100, 30000, 300, true, () -> effects.vortexRadius, v -> effects.vortexRadius = v);
    addParam("effects.leaves.count", "leaves.count", "Hojas", "cantidad", 0, 2000, 20, true,
        () -> (float) effects.leafCount, v -> effects.leafCount = Math.round(v));
    addParam("effects.leaves.paperFraction", "leaves.paper", "Hojas", "fraccion papeles",
        0, 1, .05f, false, () -> effects.paperFrac, v -> effects.paperFrac = v);
    addParam("effects.junk.count", "junk.count", "Basura", "cantidad", 0, 500, 5, true,
        () -> (float) junkCount, v -> {
          junkCount = Math.round(v);
          junkSpawnPending = true;
        });
    addParam("effects.junk.kickStrength", "junk.kick", "Basura", "fuerza patada",
        0, 20, .25f, false, () -> junk.kickScale, v -> junk.kickScale = v);
    addParam("effects.junk.gravity", "junk.gravity", "Basura", "gravedad", -5, 10, .25f, false,
        () -> junkGravity, v -> {
          junkGravity = v;
          junk.setGravityFactor(v);
        });
    addParam("effects.balloons.balloons", "balloons.count", "Globos", "globos", 0, 200, 2, true,
        () -> (float) balloonCount, v -> {
          balloonCount = Math.round(v);
          junkSpawnPending = true;
        });
    addParam("effects.balloons.bubbles", "balloons.bubbles", "Globos", "burbujas",
        0, 200, 2, true, () -> (float) bubbleCount, v -> {
          bubbleCount = Math.round(v);
          junkSpawnPending = true;
        });
    addParam("effects.balloons.buoyancy", "balloons.buoyancy", "Globos", "flotacion",
        -5, 10, .25f, false, () -> junk.buoyancy, v -> {
          junk.buoyancy = v;
          junkSpawnPending = true;
        });
    addParam("effects.lamps.count", "lamps.count", "Lamparas", "cantidad", 0, 40, 1, true,
        () -> (float) lampCount, v -> {
          lampCount = Math.round(v);
          junkSpawnPending = true;
        });
    addParam("effects.lamps.lightIntensity", "lamps.light", "Lamparas", "intensidad luz",
        0, 20, .25f, false, () -> lampLightScale, v -> lampLightScale = v);
    addParam("effects.fire.rate", "fire.rate", "Fuego", "emision", 0, 20, .25f, false,
        () -> effects.fireRate, v -> effects.fireRate = v);
    addParam("effects.fire.flameSize", "fire.size", "Fuego", "tamano llamas",
        .1f, 10, .2f, false, () -> effects.fireSize, v -> effects.fireSize = v);
    addParam("effects.mist.patches", "mist.count", "Niebla", "parches", 0, 300, 5, true,
        () -> (float) effects.mistCount, v -> effects.mistCount = Math.round(v));
    addParam("effects.mist.density", "mist.density", "Niebla", "densidad", 0, 10, .2f, false,
        () -> effects.mistDensity, v -> effects.mistDensity = v);
    addParam("effects.storm.period", "storm.period", "Tormenta", "periodo (seg)",
        .2f, 120, .5f, false, () -> effects.stormPeriod, v -> effects.stormPeriod = v);
    addParam("effects.storm.intensity", "storm.intensity", "Tormenta", "intensidad",
        0, 5, .1f, false, () -> effects.stormIntensity, v -> effects.stormIntensity = v);
    addParam("effects.dust.rate", "dust.rate", "Polvo", "emision", 0, 20, .25f, false,
        () -> effects.dustRate, v -> effects.dustRate = v);
    addParam("effects.dust.moundMax", "dust.mound", "Polvo", "montana maxima",
        0, 40, 1, false, () -> effects.moundMax, v -> effects.moundMax = v);
    addParam("effects.lantern.ambient", "dark.ambient", "Linterna", "luz ambiente",
        0, 1, .02f, false, () -> darkAmbient, v -> darkAmbient = v);
    addParam("effects.lantern.spriteLight", "dark.sprite", "Linterna", "luz sprites",
        0, 10000, 100, true, () -> spriteLightIntensity, v -> spriteLightIntensity = v);
    addParam("effects.lantern.itemLight", "dark.item", "Linterna", "luz items",
        0, 10000, 100, true, () -> itemLightIntensity, v -> itemLightIntensity = v);
  }

  /** the preset name prompt while naming, and a 3s flash after save/load/cycle. */
  private void renderPresets() {
    String line;
    if (namingPreset)
      line = "guardar ambiente como: " + presetTyping + "_   (Enter ok, Esc cancela)";
    else if (System.currentTimeMillis() - presetFlashAt < 3000 && !presetFlash.isEmpty())
      line = presetFlash;
    else
      return;
    com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
        new com.badlogic.gdx.graphics.g2d.GlyphLayout(uiFont, line);
    float pad = 12, w = layout.width + pad * 2, h = layout.height + pad * 2;
    float x = (Gdx.graphics.getWidth() - w) / 2, y = 24;
    uiBatch.begin();
    uiBatch.setColor(0, 0, .18f, .85f);
    uiBatch.draw(whitePix, x, y, w, h);
    uiBatch.setColor(1, 1, .7f, 1);
    uiFont.draw(uiBatch, line, x + pad, y + h - pad);
    uiBatch.end();
  }

  private List<Param> groupParams() {
    return params.stream()
        .filter(p -> p.group().equals(paramGroups.get(tuneGroup))).toList();
  }

  private void adjustParam(int dir) {
    Param p = groupParams().get(tuneParam);
    float v = Math.max(p.min(), Math.min(p.max(), p.get().get() + dir * p.step()));
    p.set().accept(v);
    tuneShownAt = lastAdjustAt = System.currentTimeMillis();
    configDirty = true;
    configDirtyAt = tuneShownAt;
  }

  private void renderTuning() {
    if (tuneGroup < 0)
      return;
    StringBuilder sb = new StringBuilder("CONFIG  ").append(paramGroups.get(tuneGroup))
        .append("\nTAB efecto / arriba-abajo parametro / izq-der ajustar / ESC cerrar\n\n");
    List<Param> g = groupParams();
    for (int i = 0; i < g.size(); i++) {
      Param p = g.get(i);
      float v = p.get().get();
      sb.append(i == tuneParam ? "> " : "   ").append(p.name()).append(" = ")
          .append(p.integer() ? String.valueOf(Math.round(v))
              : String.format(java.util.Locale.US, "%.2f", v)).append('\n');
    }
    com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
        new com.badlogic.gdx.graphics.g2d.GlyphLayout(uiFont, sb.toString());
    float pad = 14, w = layout.width + pad * 2, h = layout.height + pad * 2;
    float x = Gdx.graphics.getWidth() - w - 20, y = Gdx.graphics.getHeight() - 20 - h;
    uiBatch.begin();
    uiBatch.setColor(0, 0, .12f, .82f);
    uiBatch.draw(whitePix, x, y, w, h);
    uiBatch.setColor(1, 1, 1, 1);
    uiFont.draw(uiBatch, sb.toString(), x + pad, y + h - pad);
    uiBatch.end();
  }

  private java.nio.file.Path configPath() {
    // per-game by default (~/.jsw3d-config-dd.json), so calibrating one game never
    // disturbs another; -Dconfig.file pins an explicit path across all games
    return java.nio.file.Path.of(System.getProperty("config.file",
        System.getProperty("user.home") + "/.jsw3d-config-" + activeGame + ".json"));
  }

  /** walk a dotted path down a parsed JSON tree; null when any hop is missing. */
  private static com.badlogic.gdx.utils.JsonValue at(com.badlogic.gdx.utils.JsonValue v,
                                                     String path) {
    for (String part : path.split("\\."))
      if ((v = v == null ? null : v.get(part)) == null)
        return null;
    return v;
  }

  private static float num(com.badlogic.gdx.utils.JsonValue root, String path,
                           String legacyKey, float def) {
    com.badlogic.gdx.utils.JsonValue x = at(root, path);
    return x != null ? x.asFloat() : root.getFloat(legacyKey, def);
  }

  private static boolean bool(com.badlogic.gdx.utils.JsonValue root, String path,
                              String legacyKey, boolean def) {
    com.badlogic.gdx.utils.JsonValue x = at(root, path);
    return x != null ? x.asBoolean() : root.getBoolean(legacyKey, def);
  }

  /**
   * Only the keys PRESENT in the file override; -D flags keep working as the defaults.
   * Reads the nested schema first and falls back to the old flat keys / {@code params}
   * object, so a pre-nesting file loads once and is rewritten in the new shape.
   */
  private void loadConfig() {
    loadConfigFrom(configPath(), configEnabled);
  }

  private void loadConfigFrom(java.nio.file.Path p, boolean gated) {
    if (gated && !configEnabled)
      return;
    try {
      if (!java.nio.file.Files.exists(p))
        return;
      applyConfig(new com.badlogic.gdx.utils.JsonReader().parse(java.nio.file.Files.readString(p)));
    } catch (Exception e) {
      if (TaintReplay.LOG)
        System.out.println("config no cargada: " + e);
    }
  }

  /** apply a parsed config tree (nested schema, legacy flat keys as fallback) live. */
  private void applyConfig(com.badlogic.gdx.utils.JsonValue v) {
    if (v == null)
      return;
      smooth = bool(v, "general.smooth", "smooth", smooth);
      smoothLevel = (int) num(v, "general.smoothLevel", "smoothLevel", smoothLevel);
      depthScale = num(v, "general.depthScale", "depthScale", depthScale);
      tileDepth = num(v, "general.tileDepth", "tileDepth", tileDepth);
      shadowsOn = bool(v, "general.shadows", "shadows", shadowsOn);
      ghostAlpha = num(v, "general.ghostAlpha", "ghostAlpha", ghostAlpha);
      cfgSpeed = num(v, "general.speed", "speed", cfgSpeed);
      junkCount = (int) num(v, "effects.junk.count", "junkCount", junkCount);
      for (Toggle t : toggles)
        t.set().accept(bool(v, t.path(), t.legacy(), t.get().get()));
      com.badlogic.gdx.utils.JsonValue legacyParams = v.get("params");
      for (Param par : params) {
        com.badlogic.gdx.utils.JsonValue x = at(v, par.id());
        if (x == null && legacyParams != null)
          x = legacyParams.get(par.legacy());
        if (x != null)
          par.set().accept(Math.max(par.min(), Math.min(par.max(), x.asFloat())));
      }
      junkSpawnPending = junkOn || lampsOn || balloonsOn;
      com.badlogic.gdx.utils.JsonValue pos = at(v, "camera.pos");
      if (pos == null)
        pos = v.get("camPos");
      if (pos != null && System.getProperty("cam.pos") == null) {
        float[] cp = pos.asFloatArray();
        com.badlogic.gdx.utils.JsonValue dir = at(v, "camera.dir");
        com.badlogic.gdx.utils.JsonValue up = at(v, "camera.up");
        float[] cd = (dir != null ? dir : v.get("camDir")).asFloatArray();
        float[] cu = (up != null ? up : v.get("camUp")).asFloatArray();
        cam.position.set(cp[0], cp[1], cp[2]);
        cam.direction.set(cd[0], cd[1], cd[2]);
        cam.up.set(cu[0], cu[1], cu[2]);
        cam.update();
      }
  }

  /** drop {@code value} into the nested tree at its dotted path, creating objects on the way. */
  @SuppressWarnings("unchecked")
  private static void put(Map<String, Object> root, String path, Object value) {
    String[] parts = path.split("\\.");
    Map<String, Object> m = root;
    for (int i = 0; i < parts.length - 1; i++)
      m = (Map<String, Object>) m.computeIfAbsent(parts[i],
          k -> new java.util.LinkedHashMap<String, Object>());
    m.put(parts[parts.length - 1], value);
  }

  @SuppressWarnings("unchecked")
  private static void writeJson(StringBuilder sb, Map<String, Object> m, String indent) {
    sb.append("{\n");
    int i = 0;
    for (Map.Entry<String, Object> e : m.entrySet()) {
      sb.append(indent).append("  \"").append(e.getKey()).append("\": ");
      Object v = e.getValue();
      if (v instanceof Map<?, ?> mm)
        writeJson(sb, (Map<String, Object>) mm, indent + "  ");
      else if (v instanceof float[] a)
        sb.append(String.format(java.util.Locale.US, "[%.4f, %.4f, %.4f]", a[0], a[1], a[2]));
      else if (v instanceof Float f)
        sb.append(String.format(java.util.Locale.US, "%.3f", f));
      else
        sb.append(v);
      sb.append(++i < m.size() ? ",\n" : "\n");
    }
    sb.append(indent).append("}");
  }

  private void saveConfig() {
    if (!configEnabled)
      return;
    saveConfigTo(configPath());
  }

  /** the whole live state as a nested tree — written to the config file and to presets. */
  private Map<String, Object> buildConfigTree() {
    Map<String, Object> root = new java.util.LinkedHashMap<>();
    put(root, "general.smooth", smooth);
    put(root, "general.smoothLevel", smoothLevel);
    put(root, "general.depthScale", depthScale);
    put(root, "general.tileDepth", tileDepth);
    put(root, "general.shadows", shadowsOn);
    put(root, "general.ghostAlpha", ghostAlpha);
    put(root, "general.speed", replay == null ? cfgSpeed : replay.getSpeed());
    put(root, "camera.pos", new float[]{cam.position.x, cam.position.y, cam.position.z});
    put(root, "camera.dir", new float[]{cam.direction.x, cam.direction.y, cam.direction.z});
    put(root, "camera.up", new float[]{cam.up.x, cam.up.y, cam.up.z});
    for (Toggle t : toggles)
      put(root, t.path(), t.get().get());
    for (Param p : params) {
      float pv = p.get().get();
      put(root, p.id(), p.integer() ? (Object) Math.round(pv) : (Object) pv);
    }
    return root;
  }

  private void saveConfigTo(java.nio.file.Path dest) {
    try {
      StringBuilder sb = new StringBuilder();
      writeJson(sb, buildConfigTree(), "");
      sb.append('\n');
      java.nio.file.Files.writeString(dest, sb.toString());
    } catch (Exception e) {
      if (TaintReplay.LOG)
        System.out.println("config no guardada: " + e);
    }
  }

  // ---- named ambience presets: ALL in ONE global file, shared across every game ----

  private final java.util.List<String> presets = new ArrayList<>();
  private int presetIdx = -1;
  /** while naming a new preset, keystrokes build this name instead of driving the game. */
  private boolean namingPreset;
  private final StringBuilder presetTyping = new StringBuilder();
  private String presetFlash = "";
  private long presetFlashAt;

  /** one global file holding {@code {"presets": {name: <config tree>}}}, any game reads it. */
  private java.nio.file.Path presetsFile() {
    return java.nio.file.Path.of(System.getProperty("presets.file",
        System.getProperty("user.home") + "/.jsw3d-config.json"));
  }

  private com.badlogic.gdx.utils.JsonValue readPresetsRoot() {
    try {
      java.nio.file.Path p = presetsFile();
      if (java.nio.file.Files.exists(p))
        return new com.badlogic.gdx.utils.JsonReader().parse(java.nio.file.Files.readString(p));
    } catch (Exception e) {
      if (TaintReplay.LOG)
        System.out.println("presets no leidos: " + e);
    }
    return null;
  }

  private void loadPresetList() {
    presets.clear();
    com.badlogic.gdx.utils.JsonValue root = readPresetsRoot();
    com.badlogic.gdx.utils.JsonValue ps = root == null ? null : root.get("presets");
    if (ps != null)
      for (com.badlogic.gdx.utils.JsonValue c = ps.child; c != null; c = c.next)
        presets.add(c.name);
    presets.sort(String.CASE_INSENSITIVE_ORDER);
  }

  private void flashPreset(String msg) {
    presetFlash = msg;
    presetFlashAt = System.currentTimeMillis();
  }

  /** convert a parsed JSON node back into the Map/float[]/Number/Boolean tree writeJson wants. */
  private static Object jsonToObj(com.badlogic.gdx.utils.JsonValue v) {
    if (v.isObject()) {
      Map<String, Object> m = new java.util.LinkedHashMap<>();
      for (com.badlogic.gdx.utils.JsonValue c = v.child; c != null; c = c.next)
        m.put(c.name, jsonToObj(c));
      return m;
    }
    if (v.isArray())
      return v.asFloatArray();
    if (v.isBoolean())
      return v.asBoolean();
    if (v.isNumber()) {
      double d = v.asDouble();
      return d == Math.floor(d) && !Double.isInfinite(d) ? (Object) (int) d : (Object) (float) d;
    }
    return v.asString();
  }

  /**
   * Save the current state as a named preset INSIDE the shared file, preserving every other
   * preset already there. Presets are game-agnostic — the same "dark-storm" loads in JSW,
   * Manic Miner or Dynamite Dan alike.
   */
  @SuppressWarnings("unchecked")
  private void savePreset(String name) {
    name = name.trim().replaceAll("[^A-Za-z0-9 _-]", "").replace(' ', '-');
    if (name.isEmpty())
      return;
    try {
      // start from whatever is on disk so other presets survive the rewrite
      Map<String, Object> root;
      com.badlogic.gdx.utils.JsonValue existing = readPresetsRoot();
      root = existing != null ? (Map<String, Object>) jsonToObj(existing)
          : new java.util.LinkedHashMap<>();
      Map<String, Object> ps = (Map<String, Object>)
          root.computeIfAbsent("presets", k -> new java.util.LinkedHashMap<String, Object>());
      ps.put(name, buildConfigTree());
      StringBuilder sb = new StringBuilder();
      writeJson(sb, root, "");
      sb.append('\n');
      java.nio.file.Files.writeString(presetsFile(), sb.toString());
      loadPresetList();
      presetIdx = presets.indexOf(name);
      flashPreset("preset guardado: " + name);
    } catch (Exception e) {
      flashPreset("no pude guardar: " + e);
    }
  }

  /** load a preset by list index, applying every setting live (models rebuilt). */
  private void loadPreset(int idx) {
    if (idx < 0 || idx >= presets.size())
      return;
    com.badlogic.gdx.utils.JsonValue root = readPresetsRoot();
    com.badlogic.gdx.utils.JsonValue ps = root == null ? null : root.get("presets");
    if (ps == null)
      return;
    presetIdx = idx;
    applyConfig(ps.get(presets.get(idx)));
    modelCache.values().forEach(Model::dispose); // smooth/depth may have changed
    modelCache.clear();
    pixModelCache.values().forEach(Model::dispose);
    pixModelCache.clear();
    junkSpawnPending = junkOn || lampsOn || balloonsOn;
    if (replay != null)
      replay.setSpeed(cfgSpeed);
    flashPreset("preset: " + presets.get(idx) + "  (" + (idx + 1) + "/" + presets.size() + ")");
  }

  private void cyclePreset(int dir) {
    if (presets.isEmpty()) {
      flashPreset("no hay presets — F3 para guardar el actual");
      return;
    }
    loadPreset((presetIdx + dir + presets.size()) % presets.size());
  }

  @SuppressWarnings("unchecked")
  private void deleteCurrentPreset() {
    if (presetIdx < 0 || presetIdx >= presets.size())
      return;
    String name = presets.get(presetIdx);
    try {
      com.badlogic.gdx.utils.JsonValue existing = readPresetsRoot();
      if (existing == null)
        return;
      Map<String, Object> root = (Map<String, Object>) jsonToObj(existing);
      Map<String, Object> ps = (Map<String, Object>) root.get("presets");
      if (ps != null)
        ps.remove(name);
      StringBuilder sb = new StringBuilder();
      writeJson(sb, root, "");
      sb.append('\n');
      java.nio.file.Files.writeString(presetsFile(), sb.toString());
      loadPresetList();
      presetIdx = Math.min(presetIdx, presets.size() - 1);
      flashPreset("preset borrado: " + name);
    } catch (Exception e) {
      flashPreset("no pude borrar: " + e);
    }
  }

  private static final String HELP_TEXT = """
      TECLAS
      0        mostrar / ocultar esta guia
      M        sprites suaves / voxel
      S / X    suavizado + / -
      D / C    profundidad + / -
      T / G    grosor de plataformas + / -
      L        modo linterna
      N        niebla
      F        fuego en los items
      R        lluvia (charcos y todo mojado)
      B        nieve
      E        tormenta electrica
      V        viento, rafagas y remolinos
      J        basura con fisica
      K / H    cantidad de basura + / -
      U        globos y burbujas
      P        lamparas colgantes
      Y        hojas y papeles
      Z        polvo y suciedad de los sprites
      O        sombras proyectadas
      Q        opacidad de los tiles fantasma
      , / .    velocidad mitad / doble
      Enter    velocidad normal
      TAB      menu de parametros (flechas ajustan, ESC cierra)
      F1       cortar el replay y JUGAR (una sola via)
      F2       esta guia (jugando, las teclas van al juego y
               los efectos se togglean con Ctrl+tecla)
      F3       guardar ambiente actual como preset (con nombre)
      F4 / F5  siguiente / anterior preset (sirven en todo juego)
      F6       borrar el preset actual
      Mouse    arrastrar rota - rueda zoom
      Config viva en ~/.jsw3d-config-<juego>.json;
      presets (compartidos) en ~/.jsw3d-config.json""";

  private void renderHelp() {
    if (!helpOn)
      return;
    com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
        new com.badlogic.gdx.graphics.g2d.GlyphLayout(uiFont, HELP_TEXT);
    float pad = 16, x = 20, h = layout.height + pad * 2;
    float y = Gdx.graphics.getHeight() - 20 - h;
    uiBatch.begin();
    uiBatch.setColor(0, 0, 0, .78f);
    uiBatch.draw(whitePix, x, y, layout.width + pad * 2, h);
    uiBatch.setColor(1, 1, 1, 1);
    uiFont.draw(uiBatch, HELP_TEXT, x + pad, y + h - pad);
    uiBatch.end();
  }

  private void printStatus() {
    if (!TaintReplay.LOG)
      return;
    System.out.printf("modo=%s smooth=%d (S/X) profundidad=%.2f (D/C) tiles=%.2fx (T/G) "
            + "luz=%s (L) niebla=%s (N) fuego=%s (F) lluvia=%s (R) nieve=%s (B) "
            + "basura=%s x%d (J, K/H) lamparas=%s (P) tormenta=%s (E) sombras=%s (O) "
            + "viento=%s (V) globos=%s (U) hojas=%s (Y) polvo=%s (Z) fantasma=%.2f (Q) "
            + "velocidad=%sx (,/., Enter) ayuda=0%n",
        smooth ? "suave" : "voxel", smoothLevel, depthScale, tileDepth,
        darkMode ? "linterna" : "normal", mistOn ? "si" : "no", fireOn ? "si" : "no",
        rainOn ? "si" : "no", snowOn ? "si" : "no", junkOn ? "si" : "no", junkCount,
        lampsOn ? "si" : "no", stormOn ? "si" : "no", shadowsOn ? "si" : "no",
        windOn ? "si" : "no", balloonsOn ? "si" : "no", leavesOn ? "si" : "no",
        dustOn ? "si" : "no", ghostAlpha,
        replay == null ? "?" : String.valueOf(replay.getSpeed()));
    if (playMode && TaintReplay.LOG)
      System.out.println("JUGANDO en vivo (F1): teclas al juego, efectos con Ctrl+tecla");
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
    if (TaintReplay.LOG)
      System.out.println("screenshot -> " + path + " (frame " + shownFrame + ")");
  }

  private boolean shotTaken;

  /**
   * pooled voxels for the foreign pixels over air (rope, arrows): one shared tiny box,
   * repositioned and re-tinted per pixel per frame; only the first {@link #ropeCount}
   * of the pool render.
   */
  private Model ropePixelModel;
  private final List<ModelInstance> ropePool = new ArrayList<>();
  private int ropeCount;

  private void ropePixel(int x, int y, Color ink) {
    if (ropePixelModel == null)
      ropePixelModel = new ModelBuilder().createBox(1.4f, 1.4f, 4f,
          new Material(ColorAttribute.createDiffuse(Color.WHITE)),
          Usage.Position | Usage.Normal);
    while (ropePool.size() <= ropeCount)
      ropePool.add(new ModelInstance(ropePixelModel));
    ModelInstance inst = ropePool.get(ropeCount++);
    inst.materials.first().set(ColorAttribute.createDiffuse(ink));
    inst.transform.setToTranslation(x + .5f, H - 1 - y + .5f, midZ());
  }

  /** the 2D room: every screen byte decoded normally, sprite-owned bytes erased to paper. */
  private void updateBackdrop(TaintReplay.FrameSnapshot snap) {
    ropeCount = 0;
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
        // tile-tainted bytes are the slabs' business — EXCEPT the pixels the game drew on
        // top that aren't in the tile's own bitmap. JSW's ropes (and arrows) are OR-ed
        // pixel by pixel onto air cells, which carry the empty leaf's taint from the room
        // reveal: masking the template byte out leaves exactly the foreign pixels. On an
        // AIR byte those become voxels at the characters' mid-depth — the rope swings on
        // the same plane Willy hangs from, not painted on the far backdrop.
        int bits;
        if (snap.owner()[i] != 0)
          // only the sprite's OWN ink leaves the backdrop. Under a masked compositing
          // engine the rest of the byte is background that must stay painted; with the
          // per-bit pass off the mask covers the whole byte, so this erases it entirely
          // exactly as it always did
          bits = snap.pixels()[i] & ~snap.spriteBits()[i] & 0xff;
        else if (tilesOn && snap.tile()[i] != 0 && y < playfieldRows * 8) {
          int tpl = replay.memByte(snap.tile()[i] - 1);
          int foreign = snap.pixels()[i] & ~tpl & 0xff;
          bits = 0;
          if (tpl == 0) {
            for (int bit = 0; foreign != 0 && bit < 8; bit++)
              if ((foreign & (0x80 >> bit)) != 0)
                ropePixel(col * 8 + bit, y, ink);
          } else
            bits = foreign; // over real tile content it stays 2D, hidden behind the slab
        } else
          bits = snap.pixels()[i] & 0xff; // status-area bytes render as-is, tainted or not
        for (int bit = 0; bit < 8; bit++)
          pixmap.drawPixel(col * 8 + bit, y, Color.rgba8888(
              (bits & (0x80 >> bit)) != 0 ? ink : paper));
      }
    }
    screenTex.draw(pixmap, 0, 0);
  }

  /** a libGDX mesh indexes its vertices with shorts, so one model tops out here. */
  private static final int MAX_VERTICES = Short.MAX_VALUE;

  /**
   * Adjacent mode only: revoke ownership of any blob whose model would blow past
   * {@link #MAX_VERTICES}, BEFORE {@link #updateBackdrop} reads the flags — that ordering is
   * the point. Those bytes then stay in the flat 2D backdrop; skipping them later instead
   * would punch a black hole, because updateBackdrop erases every sprite-owned byte.
   *
   * <p>Grouping by adjacency alone (which is what lets a composite object come out whole)
   * means one over-claimed background byte can chain a whole screen band into a single blob:
   * Exolon's terrain is one connected run of hundreds of cells, and inflating it threw
   * "Too many vertices". A blob that big is scenery the catalog over-claimed, not an entity,
   * so dropping it to 2D is also the better picture — until the classifier stops handing
   * out terrain as sprites (doc DETECCION-SPRITES-3D §5.1).
   */
  private void demoteOversizedBlobs(TaintReplay.FrameSnapshot snap) {
    int[] owner = snap.owner();
    boolean[] seen = new boolean[TaintReplay.PIXEL_BYTES];
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    List<Integer> cells = new ArrayList<>();
    for (int y0 = 0; y0 < H; y0++)
      for (int c0 = 0; c0 < 32; c0++) {
        int i0 = idx(y0, c0);
        if (owner[i0] == 0 || seen[i0])
          continue;
        cells.clear();
        seen[i0] = true;
        queue.add((y0 << 5) | c0);
        int minC = c0, maxC = c0, minR = y0, maxR = y0, lit = 0;
        while (!queue.isEmpty()) {
          int p = queue.poll(), y = p >> 5, c = p & 31;
          cells.add(p);
          minC = Math.min(minC, c);
          maxC = Math.max(maxC, c);
          minR = Math.min(minR, y);
          maxR = Math.max(maxR, y);
          lit += Integer.bitCount(snap.pixels()[idx(y, c)] & snap.spriteBits()[idx(y, c)] & 0xff);
          for (int dy = -1; dy <= 1; dy++)
            for (int dc = -1; dc <= 1; dc++) {
              int ny = y + dy, nc = c + dc;
              if (ny < 0 || ny >= H || nc < 0 || nc >= 32)
                continue;
              int i = idx(ny, nc);
              if (owner[i] != 0 && !seen[i]) {
                seen[i] = true;
                queue.add((ny << 5) | nc);
              }
            }
        }
        int verts = smooth
            ? SmoothSpriteBuilder.vertexCount(maxC - minC + 1, maxR - minR + 1)
            : VoxelSpriteBuilder.vertexCount(lit);
        if (verts > MAX_VERTICES)
          for (int p : cells)
            owner[idx(p >> 5, p & 31)] = 0;
      }
  }

  /** screen-byte index of a (row, column) cell: the interleaved Spectrum layout. */
  private static int idx(int y, int col) {
    return ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2) | col;
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
    List<byte[]> bitmaps = new ArrayList<>(); // adjacent mode: the blob's on-screen pixels
    java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
    List<int[]> cells = new ArrayList<>();
    int[] inkVotes = new int[16];
    for (int y0 = 0; y0 < H; y0++)
      for (int c0 = 0; c0 < 32; c0++) {
        int base = grid[y0][c0];
        if (base == 0)
          continue;
        int[] b = {base, c0, y0, c0, y0, 7};
        int bytes = 0;
        java.util.Arrays.fill(inkVotes, 0);
        cells.clear();
        queue.add(new int[]{c0, y0});
        grid[y0][c0] = 0;
        while (!queue.isEmpty()) {
          int[] p = queue.poll();
          bytes++;
          cells.add(p);
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
              // adjacent mode groups ANY owned neighbour: a composite object's pieces
              // have different bases but the game already put them side by side
              if (c >= 0 && c < 32 && y >= 0 && y < H
                  && (blobsAdjacent ? grid[y][c] != 0 : grid[y][c] == base)) {
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
          byte[] bmp = null;
          if (blobsAdjacent) {
            // the model inflates the pixels ALREADY composited on screen — a multi-piece
            // object has no single memory bitmap to read (doc §5, atajo de render)
            int w = b[3] - b[1] + 1;
            bmp = new byte[w * (b[4] - b[2] + 1)];
            for (int[] q : cells) {
              int i = ((q[1] & 0xC0) << 5) | ((q[1] & 7) << 8) | ((q[1] & 0x38) << 2) | q[0];
              // the sprite's own ink only: inflating the background bits of a masked byte
              // would grow the model out into the scenery it was composited over
              bmp[(q[1] - b[2]) * w + (q[0] - b[1])] =
                  (byte) (snap.pixels()[i] & snap.spriteBits()[i]);
            }
          }
          bitmaps.add(bmp);
        }
      }
    spriteInstances.clear();
    spriteBoxes.clear();
    // pixel-model cache eviction happens BEFORE any instance references this frame's
    // models: disposing mid-loop would pull a model out from under its own instance
    if (pixModelCache.size() > 512) {
      pixModelCache.values().forEach(Model::dispose);
      pixModelCache.clear();
    }
    for (int bi = 0; bi < blobs.size(); bi++) {
      int[] blob = blobs.get(bi);
      int base = blob[0] - 1;
      int[] b = {blob[1], blob[2], blob[3], blob[4]};
      int bytes = catalog.sizeOf.getOrDefault(base, 32);
      // bytes per row: 2 (16px) unless the catalog knows better (DD sprites are 1..3 wide)
      int stride = catalog.strideOf.getOrDefault(base, 2);
      Model model = blobsAdjacent
          ? pixModel(bitmaps.get(bi), b[2] - b[0] + 1)
          : modelCache.computeIfAbsent(base, k -> smooth
          ? SmoothSpriteBuilder.build(k, bytes, stride, replay::memByte, smoothLevel, depthScale)
          : VoxelSpriteBuilder.build(k, bytes, stride, replay::memByte, smoothLevel, depthScale));
      float cx = (b[0] + b[2] + 1) * 8 / 2f;          // byte cols -> pixels
      float cy = H - (b[1] + b[3] + 1) / 2f;          // screen y down -> world y up
      // playfield blobs only: the lives-row Willys must not kick junk around
      if (cy > 66)
        spriteBoxes.add(new float[]{cx, cy, (b[2] - b[0] + 1) * 4f, (b[3] - b[1] + 1) / 2f});
      Color c = PALETTE[blob[5]];
      // a blob much wider than the sprite itself, at the sprite's own height, is a ROW of
      // copies drawn shoulder to shoulder — JSW's lives row — that connectivity merged
      // into one: an instance per 16px slot puts a Willy on EVERY life instead of a
      // single one floating in the middle of the row
      // the shoulder-to-shoulder split exists for ONE thing: the lives row in the status
      // area. Inside the playfield a wide blob is guardians overlapping or a decorated
      // wall, and splitting it stamps rows of phantom copies (it wrecked Dynamite Dan)
      int colspan = b[2] - b[0] + 1;
      int copies = !blobsAdjacent && cy < 64 && colspan >= stride * 2
          && (b[3] - b[1] + 1) <= bytes / stride + 2
          ? (colspan + stride - 1) / stride : 1;
      for (int k = 0; k < copies; k++) {
        Color cc = c;
        if (copies > 1) {
          // each copy votes ITS OWN slot's cells: JSW tints every life differently
          // (a color wave runs along the row), and the blob-wide vote flattened that
          java.util.Arrays.fill(inkVotes, 0);
          int col0 = b[0] + k * stride;
          for (int y = b[1]; y <= b[3]; y += 8)
            for (int dc = 0; dc < stride; dc++) {
              int attr = snap.attrs()[(y >> 3) * 32 + Math.min(31, col0 + dc)] & 0xff;
              int ink = (attr & 7) | ((attr >> 3) & 8);
              if ((ink & 7) != ((attr >> 3) & 7))
                inkVotes[ink]++;
            }
          int best = 0;
          for (int v = 1; v < 16; v++)
            if (inkVotes[v] > inkVotes[best])
              best = v;
          if (inkVotes[best] > 0)
            cc = PALETTE[best];
        }
        ModelInstance inst = new ModelInstance(model);
        inst.transform.setToTranslation(
            copies == 1 ? cx : (b[0] + k * stride) * 8 + stride * 4f, cy, midZ());
        inst.materials.first().set(ColorAttribute.createDiffuse(cc));
        wetten(inst.materials.first());
        if (darkMode)
          // the sprite IS a light source: it glows a little itself and casts a small pool
          // of its own color around it — enough to make out its surroundings, no more
          inst.materials.first().set(ColorAttribute.createEmissive(
              cc.r * .4f, cc.g * .4f, cc.b * .4f, 1));
        spriteInstances.add(inst);
      }
      if (darkMode)
        frameLights.add(new com.badlogic.gdx.graphics.g3d.environment.PointLight().set(
            .5f + c.r * .5f, .5f + c.g * .5f, .5f + c.b * .5f,
            cx, cy, midZ() + 14, spriteLightIntensity));
    }
  }

  /**
   * Model built from a blob's on-screen pixels (adjacent mode): the game already composed
   * the object — pieces, masks and all — so the screen bitmap IS the sprite's shape.
   * Content-hashed cache: an animation cycle settles into a handful of entries.
   */
  private Model pixModel(byte[] bmp, int wBytes) {
    long key = wBytes * 1099511628211L;
    for (byte x : bmp)
      key = (key ^ (x & 0xff)) * 1099511628211L; // FNV-1a
    return pixModelCache.computeIfAbsent(key, k -> smooth
        ? SmoothSpriteBuilder.build(0, bmp.length, wBytes, a -> bmp[a] & 0xff, smoothLevel, depthScale)
        : VoxelSpriteBuilder.build(0, bmp.length, wBytes, a -> bmp[a] & 0xff, smoothLevel, depthScale));
  }

  /** rain-wet sheen: bright broad specular so lights glint off the surface. */
  private void wetten(Material m) {
    if (wetness > .01f) {
      float w = wetness;
      m.set(ColorAttribute.createSpecular(.8f * w, .8f * w, .9f * w, 1));
      m.set(FloatAttribute.createShininess(10));
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
   * the cached tile of a sprite-crossed cell, rendered see-through: same models as the
   * real tiles, but every material gets a blending attribute at ghost opacity. ModelBatch
   * sorts blended renderables after the opaque world, so the character inside shows.
   */
  private void ghostTile(int col, int y0, int leaf, int attr) {
    int tStride = catalog.tileStride(leaf);
    boolean item = itemLeaves.contains(leaf) && tStride == 1;
    Model model = modelCache.computeIfAbsent(item ? -leaf - 0x10000 : -leaf, k -> item
        ? (smooth
           ? SmoothSpriteBuilder.build(leaf, 8, 1, replay::memByte, smoothLevel, depthScale)
           : VoxelSpriteBuilder.build(leaf, 8, 1, replay::memByte, smoothLevel, depthScale))
        : TileSlabBuilder.build(leaf, replay::memByte, slabDepth(), tStride));
    if (model == null)
      return;
    ModelInstance inst = new ModelInstance(model);
    inst.transform.setToTranslation(col * 8 + 4, H - (y0 + 4), midZ());
    Color ink = PALETTE[(attr & 7) | ((attr >> 3) & 8)];
    if (item) {
      inst.materials.first().set(ColorAttribute.createDiffuse(ink));
      inst.materials.first().set(new com.badlogic.gdx.graphics.g3d.attributes
          .BlendingAttribute(true, ghostAlpha));
    } else {
      Material mi = inst.getMaterial(TileSlabBuilder.INK);
      mi.set(ColorAttribute.createDiffuse(ink));
      mi.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(true, ghostAlpha));
      Material paper = inst.getMaterial(TileSlabBuilder.PAPER);
      if (paper != null) {
        paper.set(ColorAttribute.createDiffuse(
            PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)]));
        paper.set(new com.badlogic.gdx.graphics.g3d.attributes
            .BlendingAttribute(true, ghostAlpha));
      }
    }
    tileInstances.add(inst);
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
    for (int cellY = 0; cellY < playfieldRows; cellY++)
      for (int col = 0; col < 32; col++) {
        int y0 = cellY * 8;
        int i0 = ((y0 & 0xC0) << 5) | ((y0 & 7) << 8) | ((y0 & 0x38) << 2) | col;
        int t = snap.tile()[i0];
        int cell = cellY * 32 + col;
        boolean spriteHere = false;
        for (int r = 0; r < 8 && !spriteHere; r++)
          spriteHere = snap.owner()[i0 | (r << 8)] != 0;
        if (t == 0 || spriteHere) {
          // a sprite crossing the cell claims its bytes, so the SCREEN can't say "tile"
          // this frame — but the platform is still there. Rebuild it from the per-cell
          // cache as a HALF-TRANSPARENT ghost: the character shows through the stairs
          // instead of punching a hole in them, and it stays solid for the weather/junk.
          if (spriteHere && cellLeaf[cell] != 0) {
            ghostTile(col, y0, cellLeaf[cell] - 1, cellAttr[cell]);
            solidCells[cell] = true;
          } else if (!spriteHere)
            cellLeaf[cell] = 0; // truly gone: collected item, room redraw
          continue;
        }
        int leaf = t - 1;
        // a cell's 8 rows sit tStride bytes apart (1 = plain cells; >1 inside DD's
        // multi-column UDG stamps)
        int tStride = catalog.tileStride(leaf);
        // AIR (all-zero bitmap) is background, full stop: no slab, and crucially no item
        // tracking — guardians crossing air cells eventually latch the air leaf as "item",
        // and in lantern mode hundreds of invisible air-items would flood the room with
        // light until the darkness is gone
        if (emptyLeaves.computeIfAbsent(leaf, k -> {
          for (int r = 0; r < 8; r++)
            if (replay.memByte(k + r * tStride) != 0)
              return false;
          return true;
        })) {
          cellLeaf[cell] = 0;
          continue;
        }
        int attr = snap.attrs()[cell] & 0xff;
        cellLeaf[cell] = leaf + 1;
        cellAttr[cell] = attr;
        // the cell is fully sprite-free here (ghosts returned above), so the attr and
        // ink-change tracking below always see the tile's own colors
        {
          int prev = prevLeafAttr[cell];
          if (prev != 0 && (prev >> 8) == t) {
            if (((prev ^ attr) & 7) != 0) {
              // an item's cell CYCLES through 3+ inks in a steady burst; everything else
              // is quieter AND poorer: the room reveal changes each cell once, and the
              // one-frame attr lag a passing guardian leaves behind only ever toggles
              // between two inks (the room's and the guardian's)
              int bits = (1 << (attr & 7)) | (1 << (prev & 7));
              leafLastFlash.put(leaf, shownFrame);
              if (shownFrame - cellLastChange[cell] <= 20) {
                cellInkChanges[cell]++;
                cellInkMask[cell] |= bits;
              } else {
                cellInkChanges[cell] = 1;
                cellInkMask[cell] = bits;
              }
              cellLastChange[cell] = shownFrame;
              // a real item cycles EVERY frame, so a dense burst is easy for it and
              // hard to fake with the attr lag passing sprites leave behind
              // -Ditems=false: games whose scenery flashes on its own (DD's lamps) fake
              // the burst — turn the detector off entirely there
              if (cellInkChanges[cell] >= 6 && Integer.bitCount(cellInkMask[cell]) >= 3
                  && !"false".equals(System.getProperty("items"))
                  && itemLeaves.add(leaf) && TaintReplay.LOG)
                System.out.println("item detectado: leaf $" + Integer.toHexString(leaf));
            }
          } else
            cellInkChanges[cell] = 0;
          prevLeafAttr[cell] = (t << 8) | attr;
        }
        if (itemLeaves.contains(leaf)
            && shownFrame - leafLastFlash.getOrDefault(leaf, 0) > 50) {
          // on screen, sprite-free, and not flashing: that is a PLATFORM, not an item —
          // drop the latch and let it be a solid slab again
          itemLeaves.remove(leaf);
          if (TaintReplay.LOG)
            System.out.println("item des-latcheado (no flashea): leaf $"
                + Integer.toHexString(leaf));
        }
        boolean item = itemLeaves.contains(leaf) && tStride == 1;
        // an air cell (empty bitmap) builds no model; computeIfAbsent leaves null uncached,
        // so the cheap 8-byte mask check re-runs — fine
        Model model = modelCache.computeIfAbsent(item ? -leaf - 0x10000 : -leaf, k -> item
            ? (smooth
               ? SmoothSpriteBuilder.build(leaf, 8, 1, replay::memByte, smoothLevel, depthScale)
               : VoxelSpriteBuilder.build(leaf, 8, 1, replay::memByte, smoothLevel, depthScale))
            : TileSlabBuilder.build(leaf, replay::memByte, slabDepth(), tStride));
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
          wetten(inst.getMaterial(TileSlabBuilder.INK));
          Material paper = inst.getMaterial(TileSlabBuilder.PAPER);
          if (paper != null) { // an all-ink bitmap has no paper part
            paper.set(ColorAttribute.createDiffuse(
                PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)]));
            wetten(paper);
          }
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
    if ((junkOn || lampsOn || balloonsOn) && junkSpawnPending) {
      junk.roomChanged(solidCells, java.util.Arrays.hashCode(solidCells),
          junkOn ? junkCount : 0, lampsOn ? lampCount : 0,
          balloonsOn ? balloonCount : 0, balloonsOn ? bubbleCount : 0);
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
        case BALL, BUBBLE -> mb.createSphere(kind.w, kind.h, kind.w, 12, 8, m, attrs);
        case APPLE -> mb.createSphere(kind.w, kind.h, kind.w, 10, 8, m, attrs);
        case BOTTLE, CAN -> mb.createCylinder(kind.w, kind.h, kind.w, 10, m, attrs);
        case BOX -> mb.createBox(kind.w, kind.h, kind.w * .8f, m, attrs);
        // taller than wide: the classic pear shape read at voxel scale
        case BALLOON -> mb.createSphere(kind.w, kind.h, kind.w, 12, 10, m, attrs);
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
      ModelInstance inst = junkInstances.get(i);
      inst.transform.setToTranslation(p.x(), p.y(), junkZ(p))
          .rotate(0, 0, 1, p.angleDeg());
      wetten(inst.materials.first());
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
    pixModelCache.values().forEach(Model::dispose);
    junkModels.values().forEach(Model::dispose);
    if (lampModel != null)
      lampModel.dispose();
    if (ropePixelModel != null)
      ropePixelModel.dispose();
    shadowLight.dispose();
    shadowBatch.dispose();
    uiBatch.dispose();
    uiFont.dispose();
    whitePix.dispose();
    junk.dispose();
    effects.dispose();
  }

  public static void main(String[] args) {
//    System.setProperty("sprites3d", "voxel");
    // the profile picks RZX + catalog + the game's tweaks (-Dgame=jsw|mm|dd|...). The
    // catalog must be THIS game's: replaying a game against another's catalog shows
    // almost no sprites (JSW with DD's catalog once did exactly that).
//    args= new String[]{
//            "-Dgame=exolon"
//    };
    GameProfile profile = GameProfile.resolve(args);
    activeGame = profile.id;
    if (TaintReplay.LOG)
      System.out.println("juego: " + profile.title + " [" + profile.id + "]  rzx="
          + profile.rzx + "  db=" + profile.db);
    Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
    cfg.setTitle(profile.title + " 3D — sprites por taint de origenes");
    cfg.setWindowedMode(1024, 768);
    // 4x MSAA: 1px-wide voxel columns 28 units deep shimmer badly at oblique angles without it
    cfg.setBackBufferConfig(8, 8, 8, 8, 16, 0, Integer.getInteger("msaa", 4));
    if (!Boolean.parseBoolean(System.getProperty("vsync", "true")))
      cfg.useVsync(false); // -Dvsync=false uncaps the framerate (perf measurements)
    new Lwjgl3Application(new JSW3D(profile.rzx, profile.db), cfg);
  }
}
