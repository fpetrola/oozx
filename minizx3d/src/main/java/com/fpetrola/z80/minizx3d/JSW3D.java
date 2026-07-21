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
  /** the resolved profile, kept so its per-sprite section can seed the override store. */
  private static GameProfile activeProfile;
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
  private boolean blobsAdjacent = "adjacent".equals(sprop("render.blobs", "blobs", "base"));
  /**
   * -Dtiles=false: leave background zones in the flat 2D backdrop instead of building slabs
   * for them. JSW/MM want the slabs — their backgrounds ARE tidy 8x8 tiles and the room
   * reads as architecture. Exolon's "tiles" are a dithered rock texture spread over the
   * whole screen: slabbing them turns the picture into noise, and terrain is scenery the
   * player never interacts with in 3D anyway.
   *
   * <p>Mutable so it is tunable live from the TAB menu; {@link #tilesOn()} derives the
   * on/off from it. Values: {@code slab} (default), {@code screen}, {@code false}.
   */
  private String tilesMode = sprop("render.tiles", "tiles", "slab");
  private boolean tilesOn() {
    return !"false".equals(tilesMode);
  }
  /** screen-relief slab depth as a fraction of {@link #slabDepth()} — 1 matches the tiles. */
  private float reliefDepth = fprop("render.relief.depth", "relief.depth", 1);

  /**
   * Reads a setting from its canonical nested path (set by {@link GameProfile} from
   * games.json's structured {@code properties}) OR its historical flat {@code -D} name, the
   * flat one winning so an explicit command-line flag still overrides the file. The default
   * applies when neither is present. Live edits and the per-game config file override both
   * later, through the param get/set closures.
   */
  static String sprop(String path, String legacy, String def) {
    String v = System.getProperty(legacy);
    if (v == null)
      v = System.getProperty(path);
    return v == null ? def : v;
  }

  static float fprop(String path, String legacy, float def) {
    try {
      return Float.parseFloat(sprop(path, legacy, Float.toString(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }

  static int iprop(String path, String legacy, int def) {
    try {
      return Integer.parseInt(sprop(path, legacy, Integer.toString(def)).trim());
    } catch (NumberFormatException e) {
      return def;
    }
  }

  static boolean bprop(String path, String legacy, boolean def) {
    return !"false".equals(sprop(path, legacy, Boolean.toString(def)));
  }
  /** screen-pixel sprite models, keyed by content hash (animation frames each get one). */
  private final Map<Long, Model> pixModelCache = new HashMap<>();
  /**
   * Sprite -> mesh: technique selection (override > auto > viewer default), baking and the
   * LRU mesh cache. Created in {@link #create()} because it keys its overrides by game.
   */
  private Sprite3DPipeline sprite3d;
  /**
   * Live sprite-shaping knobs. They are registered as {@link Param}/{@link Toggle} like every
   * other effect, which is what puts them in the tuning menu, in the presets and in the
   * per-game {@code properties} of games.json (through each one's legacy key) all at once.
   *
   * <p>Division of labour with the rules: a RULE picks the shape (technique + primitive),
   * these knobs control the FINISH. If the rules also set the finish, turning a knob here
   * would do nothing — which is exactly how the smoothing dial appeared dead before.
   */
  private float spriteMaxDepth = Float.parseFloat(System.getProperty("sprite3d.maxdepth", "8"));
  private float spriteSmoothing = Float.parseFloat(System.getProperty("sprite3d.smoothing", "0"));
  private float spriteRoundness = Float.parseFloat(System.getProperty("sprite3d.roundness", "1"));
  private float spriteVoxelFill = Float.parseFloat(System.getProperty("sprite3d.voxelfill", "1"));
  private float spriteEpx = Integer.getInteger("sprite3d.epx", 1);
  private boolean spriteVoxelLook = !"false".equals(System.getProperty("sprite3d.voxels"));
  /** which of {@link #frameBases} the F7..F10 tuning keys are pointing at. */
  private int tunedSprite;
  /** catalog bases drawn in the last frame, in a stable order, for the tuning keys. */
  private final List<Integer> frameBases = new ArrayList<>();
  private final Map<Integer, SpriteBitmap> lastBitmap = new HashMap<>();
  /**
   * Models dropped from a cache but possibly still referenced by an instance built last
   * frame. They are released in {@link #releaseRetired()}, once the instance lists have been
   * cleared — disposing them where they are dropped is a use-after-free: the keys that
   * rebuild the models fire between snapshots, and until the next snapshot arrives
   * {@code render()} keeps drawing the OLD instances. That surfaces far from the cause, as
   * "No buffer allocated!" inside ModelBatch.end().
   */
  private final List<Model> pendingDispose = new ArrayList<>();
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
  /** screen mode: each cell's last CLEAN pixels, so a ghost can rebuild the slab a
   *  passing sprite just overwrote — without this, characters punched holes in walls. */
  private final byte[][] cellBmp = new byte[24 * 32][];
  /** cells the screen relief claimed LAST frame, so the 2D backdrop leaves them out —
   *  without this a floating star renders twice: flat behind its own inflated model. */
  private final boolean[] cellClaimed = new boolean[24 * 32];
  /**
   * tiles (slabs, ghosts, item detection) exist only in the PLAYFIELD — the top 16 cell
   * rows in JSW and Manic Miner alike (-Dplayfield.rows overrides). Below lives the
   * status area: its text and bars are drawn from data that happens to be inside the
   * catalogued zones (MM keeps the cavern NAME in the same record as the tiles), and
   * slab-ifying those bytes shreds them — the status belongs to the 2D backdrop.
   */
  private int playfieldRows = iprop("render.playfield.rows", "playfield.rows", 16);
  /**
   * first cell row of the playfield (-Dplayfield.top). JSW and Manic Miner keep their
   * status at the BOTTOM, so the playfield starts at row 0 and this stays 0; Monty on the
   * Run puts SCORE/HISCORE at the TOP, and without an offset that text gets extruded into
   * relief along with the room. The playfield is the rows {@code [top, top+rows)}.
   */
  private int playfieldTop = iprop("render.playfield.top", "playfield.top", 0);
  /**
   * item detection (flashing-ink treasures). Off for games whose scenery flashes on its
   * own (Dynamite Dan's lamps), where the detector fires false positives.
   */
  private boolean itemsOn = bprop("render.items", "items", true);
  /**
   * catalog bases within one aligned window of this many bytes are the same CHARACTER, so
   * the 3D technique is voted over the whole animation strip instead of per frame.
   */
  private int spriteGroup = iprop("sprite3d.group", "sprite3d.group", 256);

  /**
   * The playfield spans cell rows {@code [playfieldTop, playEnd())}, clamped to the 24-row
   * screen: top and rows are tuned independently in the menu, so their sum can run off the
   * bottom, and every loop over the playfield indexes {@code idx(y0+r, col)} which overflows
   * the 6144-byte screen once {@code y0 >= 192}. Clamp here, once, and all callers are safe.
   */
  private int playEnd() {
    return Math.min(24, playfieldTop + playfieldRows);
  }

  /** is this PIXEL row inside the playfield? (everything else belongs to the 2D backdrop) */
  private boolean inPlayfield(int y) {
    int cellY = y >> 3;
    return cellY >= playfieldTop && cellY < playEnd();
  }
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
                       java.util.function.Consumer<Float> set, String[] labels, String tag) {
    /**
     * A value backed by a fixed set of named options (an enum flag like {@code tiles} =
     * slab/screen/off, or a yes/no). It rides the same float get/set as a numeric param —
     * the float IS the index into {@link #labels} — so the whole menu / save / load path
     * treats it like any other, and only the DISPLAY and the JSON serialization branch on
     * {@code labels != null} to show and store the word instead of the index.
     */
    boolean choice() {
      return labels != null;
    }

    /** the option word for the current value (choice/flag only). */
    String label() {
      int i = Math.max(0, Math.min(labels.length - 1, Math.round(get.get())));
      return labels[i];
    }
  }

  /** an effect's on/off switch, addressed by its nested JSON path (+ legacy flat key). */
  private record Toggle(String path, String legacy, java.util.function.Supplier<Boolean> get,
                        java.util.function.Consumer<Boolean> set) {
  }

  private final List<Param> params = new ArrayList<>();
  private final List<Toggle> toggles = new ArrayList<>();
  private final List<String> paramGroups = new ArrayList<>();
  /**
   * The offline taint-discovery knobs ({@code discover.*}). They do NOT change the current
   * view — the catalog is already baked — so they are not bound to any live field; they ride
   * this map only to be shown, edited and SAVED (to the per-game config file), from where the
   * next {@link TaintDiscover} run reads them. That is the whole point of surfacing them here:
   * calibrate in the menu, then re-catalog with {@code -Dgame=<this>} and the values apply.
   */
  private final java.util.Map<String, Float> discoverVals = new java.util.LinkedHashMap<>();
  private int tuneGroup = Integer.getInteger("tune", 0) - 1, tuneParam;
  /**
   * Menu depth: 0 = sections, 1 = groups, 2 = the parameter LIST, 3 = editing one value.
   *
   * <p>Levels 2 and 3 are separate on purpose. With adjusting bound to left/right at the
   * list level there was no way back UP: reaching a value trapped you there, because left
   * only ever decremented. Now left is "back" while you are picking a parameter, and only
   * turns into "decrease" once you have entered the value with right.
   * The breadcrumb in {@link #renderTuning} shows where you are and what is beside you, so
   * navigating does not mean remembering an invisible flat list.
   */
  private int tuneLevel;
  private int tuneSection, tuneGroupInSection;

  /** the section a group belongs to; everything unlisted falls under "Efectos". */
  private String sectionOf(String group) {
    return switch (group) {
      case "Sprites 3D" -> "Sprites 3D";
      case "Render", "Catalogo (offline)" -> "Render";
      case "Linterna", "Niebla", "Tormenta" -> "Escena";
      default -> "Efectos";
    };
  }

  private List<String> sections() {
    List<String> out = new ArrayList<>();
    for (String g : paramGroups)
      if (!out.contains(sectionOf(g)))
        out.add(sectionOf(g));
    return out;
  }

  private List<String> groupsInSection() {
    List<String> secs = sections();
    if (secs.isEmpty())
      return List.of();
    String sec = secs.get(Math.floorMod(tuneSection, secs.size()));
    return paramGroups.stream().filter(g -> sectionOf(g).equals(sec)).toList();
  }

  /** keeps the flat {@link #tuneGroup} in step with the hierarchical position. */
  private void syncTuneGroup() {
    List<String> gs = groupsInSection();
    if (gs.isEmpty())
      return;
    tuneGroup = paramGroups.indexOf(gs.get(Math.floorMod(tuneGroupInSection, gs.size())));
    tuneParam = 0;
  }
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
    loadFlatLeaves();
    loadGroups(); // the editor's objects, so the list is there whether or not you open it
    sprite3d = new Sprite3DPipeline(activeGame, 1024);
    if (activeProfile != null)
      sprite3d.store().seedDefaults(activeProfile.sprites);
    // every frame of an animation strip, so the technique is voted per CHARACTER and the
    // shape cannot change as it walks
    sprite3d.setGroupFrames(group -> {
      List<SpriteBitmap> out = new ArrayList<>();
      if (catalog == null)
        return out;
      int mask = ~(spriteGroup - 1);
      for (Map.Entry<Integer, Integer> e : catalog.sizeOf.entrySet())
        if ((e.getKey() & mask) == group)
          out.add(SpriteBitmap.ofMemory(e.getKey(), e.getValue(),
              catalog.strideOf.getOrDefault(e.getKey(), 2), replay::memByte));
      return out;
    });
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
            // Ctrl+E opens and closes the sprite editor, and while it is open IT owns the
            // arrows, space and its own letters — before the switch, so they mean what the
            // editor needs and not what they mean in the viewer
            if (ctrl && keycode == com.badlogic.gdx.Input.Keys.E) {
              editorOn = !editorOn;
              if (editorOn && edGroups.isEmpty())
                loadGroups();
              if (replay != null)
                replay.paused = editorOn;
              if (editorOn && lastSnap == null)
                lastSnap = latest;
              flashPreset(editorOn
                  ? "editor: flechas mueven, SHIFT+flechas redimensionan (Ctrl=x8), TAB cambia "
                    + "de rectángulo, RePág/AvPág de objeto, A/D agrega o borra rectángulo, "
                    + "C/X objeto, ENTER lo busca en pantalla, G graba"
                  : "editor: cerrado");
              return true;
            }
            if (editorOn && editorKey(keycode, ctrl))
              return true;
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
              // Sprite3D live tuning: F7 picks which sprite you are editing, F8/F9 change
              // its technique and primitive, F10 saves it as an override for this game.
              // No rebuild flag needed — the mesh cache keys on (bitmap, config), so the
              // next frame bakes the new variant on its own.
              case com.badlogic.gdx.Input.Keys.F7 -> {
                tunedSprite = tunedSprite + 1;
                rebuild = false;
                printSpriteTuning();
              }
              case com.badlogic.gdx.Input.Keys.F8 -> {
                cycleTuned(1, false);
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F9 -> {
                cycleTuned(0, true);
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F10 -> { // commit the live edit to disk
                int base = tunedBase();
                if (base >= 0) {
                  sprite3d.store().put(base, tunedConfig());
                  System.out.println("sprite3d: override guardado para $"
                      + Integer.toHexString(base));
                }
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F11 -> { // back to auto/default
                int base = tunedBase();
                if (base >= 0) {
                  sprite3d.store().remove(base);
                  printSpriteTuning();
                }
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.I -> {
                pickReliefLeaf(Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_RIGHT) ? -1 : 1);
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.W -> {
                toggleFlatLeaf();
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
                int d = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_RIGHT) ? -1 : 1;
                if (tuneLevel == 0)
                  tuneSection = Math.floorMod(tuneSection + d, Math.max(1, sections().size()));
                else
                  tuneGroupInSection = Math.floorMod(tuneGroupInSection + d,
                      Math.max(1, groupsInSection().size()));
                syncTuneGroup();
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
                int d = keycode == com.badlogic.gdx.Input.Keys.UP ? -1 : 1;
                if (tuneLevel == 0)
                  tuneSection = Math.floorMod(tuneSection + d, Math.max(1, sections().size()));
                else if (tuneLevel == 1)
                  tuneGroupInSection = Math.floorMod(tuneGroupInSection + d,
                      Math.max(1, groupsInSection().size()));
                else { // works while editing too: hop to the next value without backing out
                  int n = Math.max(1, groupParams().size());
                  tuneParam = Math.floorMod(tuneParam + d, n);
                }
                if (tuneLevel <= 1)
                  syncTuneGroup();
                tuneShownAt = System.currentTimeMillis();
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.RIGHT -> {
                if (tuneGroup < 0)
                  return false;
                if (tuneLevel >= 3)
                  adjustParam(1); // editing: right raises the value
                else {
                  tuneLevel++;    // otherwise right goes deeper, into the value at level 3
                  if (tuneLevel <= 1)
                    syncTuneGroup();
                }
                tuneShownAt = System.currentTimeMillis();
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.LEFT -> {
                if (tuneGroup < 0)
                  return false;
                if (tuneLevel >= 3)
                  adjustParam(-1); // editing: left lowers the value
                else
                  tuneLevel = Math.max(0, tuneLevel - 1); // picking: left goes back up
                tuneShownAt = System.currentTimeMillis();
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.BACKSPACE -> {
                if (tuneGroup < 0)
                  return false;
                // always a way out, including out of a value you are editing
                tuneLevel = Math.max(0, tuneLevel - 1);
                tuneShownAt = System.currentTimeMillis();
                rebuild = false;
              }
              case com.badlogic.gdx.Input.Keys.F12 -> {
                // save over the preset already selected, no prompt: the whole point is
                // tweak-and-store without retyping its name every time
                if (presetIdx >= 0) {
                  savePreset(presets.get(presetIdx));
                  flashPreset("preset guardado: " + presets.get(presetIdx));
                } else
                  flashPreset("no hay preset activo (F3 para crear uno)");
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
              retire(modelCache.values());
              modelCache.clear();
              retire(pixModelCache.values());
              pixModelCache.clear();
              // sin esto el pipeline devuelve la config ya resuelta por personaje y M/D/C
              // no cambian nada en pantalla
              sprite3d.clear();
              pendingDispose.addAll(sprite3d.drainRetired());
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
    // -Dtune=N opens the menu straight at group N's PARAMETER LIST (level 2) with its
    // section/sibling position synced, instead of the top sections list — so a screenshot
    // run lands on the values to check, and a launch pre-positions the menu where you left off.
    if (System.getProperty("tune") != null && tuneGroup >= 0 && tuneGroup < paramGroups.size()) {
      String grp = paramGroups.get(tuneGroup);
      List<String> secs = sections();
      tuneSection = Math.max(0, secs.indexOf(sectionOf(grp)));
      tuneGroupInSection = Math.max(0, groupsInSection().indexOf(grp));
      tuneLevel = 2;
    }
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
    // THE EDITOR REPAINTS ON ITS OWN. Everything below only runs when a NEW frame arrives,
    // and the editor's whole point is that the game is frozen — so nothing was ever
    // repainted while editing and no highlight ever appeared: the cyan composite, the white
    // graphic, the green selection were all being drawn into a texture nobody refreshed.
    if (editorOn && lastSnap != null)
      updateBackdrop(lastSnap);
    if (snap != null && snap.frame() != shownFrame) {
      shownFrame = snap.frame();
      frameLights.clear();
      effects.clearFireSpots();
      // the one point where no instance references a model: drop last frame's instances,
      // THEN free whatever a rebuild key or a cache eviction retired since
      spriteInstances.clear();
      tileInstances.clear();
      releaseRetired();
      long t0 = perf ? System.nanoTime() : 0;
      // Screen-pixel models are content-hashed and shared by the sprite blobs and the
      // background relief, so the cache is emptied HERE — once a frame, before anything
      // rebuilds this frame's instances. Doing it inside updateSprites meant the relief's
      // ~640 cells/frame kept pushing it over the limit and the whole set was disposed and
      // rebuilt every single frame; the viewer slowed to a crawl. The bound is generous
      // because a dithered texture only has so many distinct 8x8 patterns.
      if (pixModelCache.size() > 4096) {
        retire(pixModelCache.values());
        pixModelCache.clear();
      }
      if (blobsAdjacent)
        demoteOversizedBlobs(snap); // must precede updateBackdrop: it reads owner()
      lastSnap = snap;
      updateBackdrop(snap);
      long t1 = perf ? System.nanoTime() : 0;
      updateObjects(snap);
      updateSprites(snap);
      if (junkOn || lampsOn || balloonsOn)
        junk.syncSprites(spriteBoxes, snapDt);
      if (dustOn)
        effects.spriteDust(spriteBoxes, snapDt);
      snapDt = 0;
      long t2 = perf ? System.nanoTime() : 0;
      if (editorOn) {
        tileInstances.clear();
        spriteInstances.clear();
      } else if ("screen".equals(tilesMode))
        updateScreenRelief(snap);
      else if (tilesOn())
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
    renderEditorList();
    // held arrows keep adjusting; 6 quiet seconds close the tuning panel on their own.
    // ONLY while editing a value (level 3): at the list levels left/right navigate, and
    // repeating them there would both fight the navigation and change values unseen.
    if (tuneGroup >= 0) {
      long now = System.currentTimeMillis();
      boolean l = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT);
      boolean r = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT);
      if (tuneLevel >= 3 && (l || r) && now - lastAdjustAt > 70)
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
    addParam(id, legacy, group, name, min, max, step, integer, get, set, null, "");
  }

  private void addParam(String id, String legacy, String group, String name, float min,
                        float max, float step, boolean integer,
                        java.util.function.Supplier<Float> get,
                        java.util.function.Consumer<Float> set, String[] labels, String tag) {
    params.add(new Param(id, legacy, group, name, min, max, step, integer, get, set, labels, tag));
    if (!paramGroups.contains(group))
      paramGroups.add(group);
  }

  /**
   * A menu item that cycles a fixed set of option words ({@code tiles}=slab/screen/off). The
   * word — not an index — is what the field holds and what the JSON stores; {@code sGet}/
   * {@code sSet} read and write it, and the menu maps it to/from the index into {@code opts}.
   */
  private void addChoice(String id, String legacy, String group, String name, String[] opts,
                         java.util.function.Supplier<String> sGet,
                         java.util.function.Consumer<String> sSet, String tag) {
    addParam(id, legacy, group, name, 0, opts.length - 1, 1, true,
        () -> (float) Math.max(0, java.util.Arrays.asList(opts).indexOf(sGet.get())),
        v -> sSet.accept(opts[Math.max(0, Math.min(opts.length - 1, Math.round(v)))]), opts, tag);
  }

  /** a yes/no menu item over a boolean field, stored in JSON as its word (default "no"/"si"). */
  private void addFlag(String id, String legacy, String group, String name,
                       java.util.function.Supplier<Boolean> get,
                       java.util.function.Consumer<Boolean> set, String tag) {
    addParam(id, legacy, group, name, 0, 1, 1, true,
        () -> get.get() ? 1f : 0f, v -> set.accept(v >= .5f),
        new String[]{"no", "si"}, tag);
  }

  /**
   * An offline {@code discover.*} knob: seeded from games.json/-D, edited into
   * {@link #discoverVals}, saved to the config file, read back by {@link TaintDiscover}.
   * Tagged {@code recatalogar} because it has no effect until the catalog is rebuilt.
   */
  private void addDiscover(String id, String name, float min, float max, float step,
                           boolean integer, float def) {
    discoverVals.put(id, integer ? (float) iprop(id, id, Math.round(def)) : fprop(id, id, def));
    addParam(id, id, "Catalogo (offline)", name, min, max, step, integer,
        () -> discoverVals.getOrDefault(id, def), v -> discoverVals.put(id, v), null, "recatalogar");
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

    // Sprites 3D: mismo registro que cualquier efecto, asi entran solos al menu, a los
    // presets y a las properties por-juego de games.json (por su clave legacy).
    addParam("effects.sprite3d.maxDepth", "sprite3d.maxdepth", "Sprites 3D",
        "profundidad max (voxels)", 1, 32, 1, true,
        () -> spriteMaxDepth, v -> spriteMaxDepth = v);
    addParam("effects.sprite3d.smoothing", "sprite3d.smoothing", "Sprites 3D",
        "redondeo (0=cubos)", 0, 1.5f, .05f, false,
        () -> spriteSmoothing, v -> spriteSmoothing = v);
    addParam("effects.sprite3d.roundness", "sprite3d.roundness", "Sprites 3D",
        "forma geometrica vs sprite", 0, 1, .05f, false,
        () -> spriteRoundness, v -> spriteRoundness = v);
    addParam("effects.sprite3d.voxelFill", "sprite3d.voxelfill", "Sprites 3D",
        "tamano del voxel", .4f, 1, .02f, false,
        () -> spriteVoxelFill, v -> spriteVoxelFill = v);
    addParam("effects.sprite3d.epx", "sprite3d.epx", "Sprites 3D",
        "contorno EPX (1/2/4)", 1, 4, 1, true,
        () -> spriteEpx, v -> spriteEpx = v);
    addToggle("effects.sprite3d.voxels", "sprite3d.voxels",
        () -> spriteVoxelLook, v -> spriteVoxelLook = v);
    addFlag("sprite3d.auto", "sprite3d.auto", "Sprites 3D", "seleccion automatica",
        () -> sprite3d != null && sprite3d.autoEnabled(),
        v -> { if (sprite3d != null) sprite3d.setAuto(v); }, "");
    addParam("sprite3d.group", "sprite3d.group", "Sprites 3D", "grupo animacion (bytes)",
        1, 1024, 1, true, () -> (float) spriteGroup, v -> {
          spriteGroup = Math.round(v);
          if (sprite3d != null)
            sprite3d.setGroupSize(spriteGroup);
        });

    // Render: los flags que antes solo entraban por -D, ahora tunables en vivo (§DETECCION).
    // Todos se leen por frame desde el snapshot, asi que el cambio se ve al toque; los dos
    // del hilo de taint van marcados [re-seek] porque su efecto es hacia adelante.
    addChoice("render.tiles", "tiles", "Render", "tiles (fondo)",
        new String[]{"slab", "screen", "off"},
        () -> "false".equals(tilesMode) ? "off" : tilesMode,
        s -> tilesMode = "off".equals(s) ? "false" : s, "");
    addChoice("render.blobs", "blobs", "Render", "blobs (agrupado)",
        new String[]{"base", "adjacent"},
        () -> blobsAdjacent ? "adjacent" : "base",
        s -> blobsAdjacent = "adjacent".equals(s), "");
    addParam("render.playfield.top", "playfield.top", "Render", "playfield fila inicial",
        0, 23, 1, true, () -> (float) playfieldTop, v -> playfieldTop = Math.round(v));
    addParam("render.playfield.rows", "playfield.rows", "Render", "playfield alto (filas)",
        1, 24, 1, true, () -> (float) playfieldRows, v -> playfieldRows = Math.round(v));
    addParam("render.relief.depth", "relief.depth", "Render", "relieve profundidad",
        .1f, 8, .1f, false, () -> reliefDepth, v -> reliefDepth = v);
    addParam("render.relief.dyn", "relief.dyn", "Render", "relieve frames movil",
        0, 30, 1, true, () -> (float) DYN_FRAMES, v -> DYN_FRAMES = Math.round(v));
    addParam("render.relief.decor", "relief.decor", "Render", "relieve celdas decor",
        0, 60, 1, true, () -> (float) decorCells, v -> decorCells = Math.round(v));
    addParam("render.relief.dot", "relief.dot", "Render", "relieve motas del cielo (celdas)",
        0, 12, 1, true, () -> (float) dotCells, v -> dotCells = Math.round(v));
    addParam("render.relief.dotpx", "relief.dotpx", "Render", "relieve motas (px encendidos)",
        0, 32, 1, true, () -> (float) dotPixels, v -> dotPixels = Math.round(v));
    addChoice("render.relief.bar", "relief.bar", "Render", "relieve barras del cielo",
        new String[]{"slab", "flat", "float"}, () -> barMode, v -> barMode = v, "");
    addParam("render.relief.island", "relief.island", "Render", "relieve isla (celdas bbox)",
        0, 24, 1, true, () -> (float) islandCells, v -> islandCells = Math.round(v));
    addParam("render.relief.hold", "relief.hold", "Render", "relieve celdas quieto",
        0, 60, 1, true, () -> (float) holdCells, v -> holdCells = Math.round(v));
    addParam("render.relief.paper", "relief.paper", "Render", "relieve tinte del relleno",
        0, 1, .05f, false, () -> paperTint, v -> paperTint = v);
    addFlag("render.objects", "objects", "Render", "objetos definidos a mano",
        () -> objectsOn, v -> objectsOn = v, "");
    addParam("render.objects.match", "objects.match", "Render", "objetos: fraccion minima",
        .1f, 1, .05f, false, () -> objectsMatch, v -> objectsMatch = v);
    addFlag("render.relief.fill", "relief.fill", "Render", "relleno de siluetas huecas",
        () -> reliefFill, v -> reliefFill = v, "");
    addFlag("render.items", "items", "Render", "deteccion de items",
        () -> itemsOn, v -> itemsOn = v, "");
    addParam("general.ghostAlpha", "ghost.alpha", "Render", "fantasmas opacidad",
        .1f, 1, .05f, false, () -> ghostAlpha, v -> ghostAlpha = v);
    addFlag("render.spriteBits", "sprite.bits", "Render", "sprite bits (mascara)",
        () -> replay != null && replay.spriteBitsOn,
        v -> { if (replay != null) replay.spriteBitsOn = v; }, "re-seek");
    addParam("render.freshFrames", "fresh.frames", "Render", "fresh frames (0=off)",
        0, 30, 1, true, () -> replay != null ? (float) replay.freshFrames : 0,
        v -> { if (replay != null) replay.freshFrames = Math.round(v); }, null, "re-seek");
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

    // Catalogo (offline): las perillas de TaintDiscover. No cambian la vista actual (el
    // catalogo ya esta horneado): se editan, se guardan al config del juego, y la PROXIMA
    // corrida de TaintDiscover -Dgame=<este> las lee. Van tageadas [recatalogar].
    addDiscover("discover.gate", "gate (dirty-region)", 0, 64, 1, true, 8);
    addDiscover("discover.drift", "drift minimo", 0, 1, .05f, false, 0);
    addDiscover("discover.gap", "corte entre piezas", 1, 64, 1, true, 16);
    addDiscover("discover.rows", "filas de playfield", 1, 24, 1, true, 16);
    addDiscover("discover.sample", "muestreo (cada N)", 1, 60, 1, true, 5);
    addDiscover("discover.from", "desde frame", 0, 20000, 100, true, 500);
    addDiscover("discover.mobility", "mobility minima", 0, 20, .25f, false, 2);
    addDiscover("discover.reuse", "reuse maximo", 0, 100, .5f, false, 2);
    addDiscover("discover.freshfrac", "fresh fraccion", 0, 1, .05f, false, .35f);
    addDiscover("discover.freshwin", "fresh ventana", 1, 60, 1, true, 12);
    addDiscover("discover.stamps", "stamps maximo", 1, 64, 1, true, 16);
    addDiscover("discover.bg", "bytes por stamp (fondo)", 1, 256, 1, true, 64);
    addDiscover("discover.maxwrites", "escrituras max (buffer)", 1, 256, 1, true, 16);
    addDiscover("discover.min", "veces minimo", 1, 64, 1, true, 3);
    addDiscover("discover.minsize", "tamano minimo (bytes)", 1, 64, 1, true, 4);
    addDiscover("discover.cap", "hojas por byte (tope)", 1, 256, 1, true, 32);
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
    float v;
    if (p.choice()) // options cycle: past the last wraps to the first, so no dead end
      v = Math.floorMod(Math.round(p.get().get()) + dir, p.labels().length);
    else
      v = Math.max(p.min(), Math.min(p.max(), p.get().get() + dir * p.step()));
    p.set().accept(v);
    tuneShownAt = lastAdjustAt = System.currentTimeMillis();
    configDirty = true;
    configDirtyAt = tuneShownAt;
  }

  private void renderTuning() {
    if (tuneGroup < 0)
      return;
    // TV-style menu: a breadcrumb of where you are, then the list at THIS level with the
    // siblings visible, so what is available is on screen instead of being remembered.
    List<String> secs = sections();
    String sec = secs.isEmpty() ? "?" : secs.get(Math.floorMod(tuneSection, secs.size()));
    List<String> gs = groupsInSection();
    String grp = gs.isEmpty() ? "?" : gs.get(Math.floorMod(tuneGroupInSection, gs.size()));
    StringBuilder sb = new StringBuilder("CONFIG");
    List<Param> gp = groupParams();
    sb.append(tuneLevel >= 1 ? "  >  " + sec : "").append(tuneLevel >= 2 ? "  >  " + grp : "");
    if (tuneLevel >= 3 && !gp.isEmpty())
      sb.append("  >  ").append(gp.get(Math.min(tuneParam, gp.size() - 1)).name());
    sb.append(presetIdx >= 0 ? "        [preset: " + presets.get(presetIdx) + "]" : "");
    sb.append(tuneLevel >= 3
        ? "\nEDITANDO: izq-der cambian el valor / arriba-abajo otro valor\n"
          + "BACKSPACE vuelve a la lista"
        : "\narriba-abajo mover / derecha entrar / izquierda volver\n"
          + "TAB y SHIFT+TAB hermanos");
    sb.append(" / F12 guardar preset / ESC cerrar\n\n");
    if (tuneLevel == 0) {
      for (int i = 0; i < secs.size(); i++)
        sb.append(i == Math.floorMod(tuneSection, secs.size()) ? "> " : "   ")
            .append(secs.get(i)).append('\n');
    } else if (tuneLevel == 1) {
      for (int i = 0; i < gs.size(); i++)
        sb.append(i == Math.floorMod(tuneGroupInSection, gs.size()) ? "> " : "   ")
            .append(gs.get(i)).append('\n');
    } else {
      List<Param> g = gp;
      for (int i = 0; i < g.size(); i++) {
        Param p = g.get(i);
        float v = p.get().get();
        String shown = p.choice() ? p.label()
            : p.integer() ? String.valueOf(Math.round(v))
                : String.format(java.util.Locale.US, "%.2f", v);
        sb.append(i == tuneParam ? (tuneLevel >= 3 ? ">>" : "> ") : "   ")
            .append(p.name()).append(" = ").append(shown)
            .append(p.tag().isEmpty() ? "" : "  [" + p.tag() + "]").append('\n');
      }
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

  /** where the I/W picks live: one file per game, like every other per-game store here. */
  private java.nio.file.Path flatPath() {
    return java.nio.file.Path.of(System.getProperty("user.home"),
        ".jsw3d-relief-" + activeGame + ".json");
  }

  private void loadFlatLeaves() {
    for (String tok : sprop("render.relief.flat", "relief.flat", "").split(","))
      addFlatLeaf(tok);
    try {
      java.nio.file.Path p = flatPath();
      if (!java.nio.file.Files.exists(p))
        return;
      com.badlogic.gdx.utils.JsonValue v =
          new com.badlogic.gdx.utils.JsonReader().parse(java.nio.file.Files.readString(p));
      for (com.badlogic.gdx.utils.JsonValue e = v.get("flat") == null ? null : v.get("flat").child;
           e != null; e = e.next)
        addFlatLeaf(e.asString());
    } catch (Exception e) {
      System.out.println("relieve: no se pudo leer " + flatPath() + ": " + e);
    }
  }

  private void addFlatLeaf(String tok) {
    String t = tok.trim().replace("$", "").replace("0x", "");
    if (!t.isEmpty())
      try {
        flatLeaves.add(Integer.parseInt(t, 16));
      } catch (NumberFormatException ignored) {
        System.out.println("relieve: direccion invalida en relief.flat: " + tok);
      }
  }

  private void saveFlatLeaves() {
    StringBuilder sb = new StringBuilder("{\n  \"flat\": [");
    int i = 0;
    for (int leaf : flatLeaves)
      sb.append(i++ > 0 ? ", " : "").append('"').append('$')
          .append(Integer.toHexString(leaf)).append('"');
    sb.append("]\n}\n");
    try {
      java.nio.file.Files.writeString(flatPath(), sb.toString());
      System.out.println("relieve: " + flatLeaves.size() + " graficos planos -> " + flatPath());
    } catch (Exception e) {
      System.out.println("relieve: no se pudo guardar " + flatPath() + ": " + e);
    }
  }

  /** I: point at the next graphic the relief is modelling (Shift = previous). */
  private void pickReliefLeaf(int dir) {
    List<Integer> leaves = new ArrayList<>(frameLeaves.keySet());
    leaves.sort((a, b) -> frameLeaves.get(b) - frameLeaves.get(a));
    if (leaves.isEmpty()) {
      System.out.println("relieve: no hay graficos modelados en pantalla");
      return;
    }
    int at = leaves.indexOf(pickedLeaf);
    pickedLeaf = leaves.get(((at + dir) % leaves.size() + leaves.size()) % leaves.size());
    System.out.println("relieve: grafico $" + Integer.toHexString(pickedLeaf) + " — "
        + frameLeaves.get(pickedLeaf) + " celdas (se pintan BLANCAS) — "
        + (flatLeaves.contains(pickedLeaf) ? "plano" : "modelado") + " (W lo cambia)");
  }

  /** W: this graphic goes flat (or back to modelled), and it persists for the game. */
  private void toggleFlatLeaf() {
    if (pickedLeaf < 0) {
      System.out.println("relieve: elegi un grafico primero con I");
      return;
    }
    if (!flatLeaves.remove(pickedLeaf))
      flatLeaves.add(pickedLeaf);
    System.out.println("relieve: $" + Integer.toHexString(pickedLeaf) + " ahora es "
        + (flatLeaves.contains(pickedLeaf) ? "PLANO (fondo 2D)" : "MODELADO"));
    saveFlatLeaves();
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
      cfgSpeed = num(v, "general.speed", "speed", cfgSpeed);
      junkCount = (int) num(v, "effects.junk.count", "junkCount", junkCount);
      for (Toggle t : toggles)
        t.set().accept(bool(v, t.path(), t.legacy(), t.get().get()));
      com.badlogic.gdx.utils.JsonValue legacyParams = v.get("params");
      for (Param par : params) {
        com.badlogic.gdx.utils.JsonValue x = at(v, par.id());
        if (x == null && legacyParams != null)
          x = legacyParams.get(par.legacy());
        if (x == null)
          continue;
        if (par.choice()) {
          // stored as its option word; an index survives too for older files
          int idx = x.isString() ? java.util.Arrays.asList(par.labels()).indexOf(x.asString())
              : Math.round(x.asFloat());
          if (idx >= 0)
            par.set().accept((float) idx);
        } else
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
      else if (v instanceof String s)
        sb.append('"').append(s).append('"');
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
    put(root, "general.speed", replay == null ? cfgSpeed : replay.getSpeed());
    put(root, "camera.pos", new float[]{cam.position.x, cam.position.y, cam.position.z});
    put(root, "camera.dir", new float[]{cam.direction.x, cam.direction.y, cam.direction.z});
    put(root, "camera.up", new float[]{cam.up.x, cam.up.y, cam.up.z});
    for (Toggle t : toggles)
      put(root, t.path(), t.get().get());
    for (Param p : params) {
      float pv = p.get().get();
      put(root, p.id(), p.choice() ? (Object) p.label()
          : p.integer() ? (Object) Math.round(pv) : (Object) pv);
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
    retire(modelCache.values()); // smooth/depth may have changed
    modelCache.clear();
    retire(pixModelCache.values());
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

  /**
   * The on-screen guide (key 0). Grouped by what you are trying to DO, not by key order:
   * flat, it ran forty lines deep and the editor — the part with the most keys and the least
   * guessable — was buried at the bottom where nobody read it.
   */
  private static final String HELP_TEXT = """
      RENDER          M suave/voxel · S/X suavizado · D/C profundidad
                      T/G grosor de plataformas · Q opacidad fantasmas
                      I elegir grafico del relieve · W pasarlo a fondo plano

      AMBIENTE        L linterna · N niebla · F fuego · R lluvia · B nieve
                      E tormenta · V viento · U globos · P lamparas
                      Y hojas · Z polvo · O sombras
                      J basura · K/H cantidad de basura

      REPLAY          , / . mitad / doble velocidad · Enter normal
                      F1 cortar el replay y JUGAR (una sola via)

      AJUSTES         TAB menu de parametros (flechas ajustan, ESC cierra)
                      F3 guardar ambiente como preset · F4/F5 cambiar
                      F6 borrar el preset actual · 0 esta guia

      EDITOR DE SPRITES   Ctrl+E abre y cierra (congela el juego y
                      muestra la pantalla plana; la lista de objetos, con
                      la imagen de cada uno, va en la franja derecha)
        flechas       mover la caja seleccionada
        SHIFT+flechas redimensionarla     (Ctrl = de a 8 px)
        TAB           siguiente caja del objeto (SHIFT+TAB anterior)
        RePag/AvPag   objeto anterior / siguiente (lo busca en pantalla)
        A / D         agregar / borrar una caja
        C / X         crear / borrar un objeto
        ENTER         buscar el objeto en la pantalla actual
        R / P / O     tecnica / primitiva / redondez de ESTE objeto
        G             grabar (doc/objetos-<juego>.json y .png)

      Mouse arrastrar rota, rueda zoom. Config viva en
      ~/.jsw3d-config-<juego>.json; presets en ~/.jsw3d-config.json""";

  private void renderHelp() {
    if (!helpOn)
      return;
    com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
        new com.badlogic.gdx.graphics.g2d.GlyphLayout(uiFont, HELP_TEXT);
    // the widest LINE, not what the multi-line layout reports: with the guide grouped in
    // sections it under-measured and half the text sat outside its own panel, unreadable
    // over the game
    float wide = 0;
    for (String line : HELP_TEXT.split("\n"))
      wide = Math.max(wide, new com.badlogic.gdx.graphics.g2d.GlyphLayout(uiFont, line).width);
    float pad = 16, x = 20, h = layout.height + pad * 2;
    float y = Gdx.graphics.getHeight() - 20 - h;
    uiBatch.begin();
    uiBatch.setColor(0, 0, 0, .92f);
    uiBatch.draw(whitePix, x, y, wide + pad * 2, h);
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
    if (path == null || shotTaken || (spriteInstances.isEmpty() && !editorOn)
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
        if (objClaimed[i] && !editorOn)
          bits = 0; // an object instance is modelling this byte
        else if (editorOn)
          // the editor works on the 2D identification, so it shows the 2D screen: with the
          // relief on, its slabs cover the backdrop and the highlights are painted where
          // nobody can see them
          bits = snap.pixels()[i] & 0xff;
        else if ("screen".equals(tilesMode) && inPlayfield(y))
          // whatever the relief claimed (slab, ghost, floating decor or a moving blob) must
          // not ALSO stay painted flat behind its own model; everything else falls back to
          // 2D, minus the moving sprites' own ink, which updateSprites already models.
          //
          // Erasing every owner/tile-tainted byte outright was the bug behind "the platform
          // tiles vanish when a character comes near": a byte the relief could not claim
          // (its scenery cache empty, the sprite's mask over it, a model that came out null)
          // lost its 2D fallback as well and the room went black exactly where the player
          // was standing. Nothing may leave the screen without something drawing it.
          bits = cellClaimed[(y >> 3) * 32 + col] ? 0
              : snap.pixels()[i] & ~snap.spriteBits()[i] & 0xff;
        else if (snap.owner()[i] != 0)
          // only the sprite's OWN ink leaves the backdrop. Under a masked compositing
          // engine the rest of the byte is background that must stay painted; with the
          // per-bit pass off the mask covers the whole byte, so this erases it entirely
          // exactly as it always did
          bits = snap.pixels()[i] & ~snap.spriteBits()[i] & 0xff;
        else if (tilesOn() && snap.tile()[i] != 0 && inPlayfield(y)) {
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
        // in role-paint mode the flat 2D backdrop goes grey, so that ANY colour on screen is
        // a model and the ambiguity disappears: a red pixel of the game's own art used to be
        // indistinguishable from a red-painted slab
        Color pi = paintRoles ? Color.GRAY : ink, pp = paintRoles ? Color.BLACK : paper;
        for (int bit = 0; bit < 8; bit++)
          pixmap.drawPixel(col * 8 + bit, y, Color.rgba8888(
              (bits & (0x80 >> bit)) != 0 ? pi : pp));
      }
    }
    if (editorOn)
      paintEditor(snap, pixmap);
    screenTex.draw(pixmap, 0, 0);
  }

  /** a libGDX mesh indexes its vertices with shorts, so one model tops out here. */
  private static final int MAX_VERTICES = Short.MAX_VALUE;

  /**
   * Adjacent mode only: revoke ownership of any blob {@link #updateSprites} will NOT draw —
   * too big to mesh ({@link #MAX_VERTICES}) or too small to be a sprite at all — BEFORE
   * {@link #updateBackdrop} reads the flags; that ordering is the point. Those bytes then
   * stay in the flat 2D backdrop; skipping them later instead would punch a black hole,
   * because updateBackdrop erases every sprite-owned byte.
   *
   * <p>The small end is the same hole seen from the other side: updateSprites ignores blobs
   * under 4 bytes (a speck is noise, not a character), and measured on Monty that quietly
   * ate 1-3 byte scraps hundreds of times per run — each one a handful of lit pixels erased
   * from the room with nothing drawn in their place.
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
        if (verts > MAX_VERTICES || cells.size() < 4)
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
  /**
   * The objects identified by hand, matched on screen and rendered their own way.
   *
   * <p>The definition is a set of GRAPHICS, so finding the object is looking for those
   * graphics: no shape, no position, no template matching on pixels — the taint already
   * knows, for every byte, which graphic it came from, and that survives the object moving,
   * animating, or being drawn in another room.
   *
   * <p>Fuzzy on purpose: an object is routinely half-covered by another, or the game draws
   * only part of it this frame, so an instance is accepted with a FRACTION of its graphics
   * present ({@code objects.match}, half by default). Demanding all of them means the
   * definition works right up to the moment something walks in front of it.
   */
  private void updateObjects(TaintReplay.FrameSnapshot snap) {
    java.util.Arrays.fill(objClaimed, false);
    if (edGroups.isEmpty() || !objectsOn)
      return;
    // graphic -> the object that claims it; first definition wins a shared piece
    if (gfxToObj.isEmpty())
      for (int n = 0; n < edGroups.size(); n++)
        for (int gfx : edGroups.get(n).graphics)
          gfxToObj.putIfAbsent(gfx, n);
    int[] owner = new int[TaintReplay.PIXEL_BYTES];
    java.util.Arrays.fill(owner, -1);
    boolean any = false;
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      if ((snap.pixels()[i] & 0xff) == 0)
        continue;
      int gfx = originOf(i);
      if (gfx < 0)
        continue;
      Integer obj = gfxToObj.get(gfx);
      if (obj == null)
        continue;
      owner[i] = obj;
      any = true;
    }
    if (!any)
      return;
    // instances: bytes of the same object, flooded by adjacency. Two capsules on screen are
    // two instances of one definition, and each gets its own model where it stands
    boolean[] seen = new boolean[TaintReplay.PIXEL_BYTES];
    java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
    List<Integer> cells = new ArrayList<>();
    for (int i0 = 0; i0 < TaintReplay.PIXEL_BYTES; i0++) {
      if (owner[i0] < 0 || seen[i0])
        continue;
      int obj = owner[i0];
      cells.clear();
      seen[i0] = true;
      queue.add(new int[]{i0 & 31, (((i0 >> 11) & 3) << 6) | (((i0 >> 5) & 7) << 3)
          | ((i0 >> 8) & 7)});
      int minC = 31, maxC = 0, minR = H - 1, maxR = 0;
      java.util.Set<Integer> found = new java.util.HashSet<>();
      while (!queue.isEmpty()) {
        int[] p = queue.poll();
        int i = idx(p[1], p[0]);
        cells.add(i);
        found.add(originOf(i));
        minC = Math.min(minC, p[0]);
        maxC = Math.max(maxC, p[0]);
        minR = Math.min(minR, p[1]);
        maxR = Math.max(maxR, p[1]);
        for (int dy = -2; dy <= 2; dy++) // a couple of rows of slack: a masked draw leaves gaps
          for (int dc = -1; dc <= 1; dc++) {
            int c = p[0] + dc, y = p[1] + dy;
            if (c < 0 || c > 31 || y < 0 || y >= H)
              continue;
            int q = idx(y, c);
            if (owner[q] == obj && !seen[q]) {
              seen[q] = true;
              queue.add(new int[]{c, y});
            }
          }
      }
      EdGroup g = edGroups.get(obj);
      if (found.size() < Math.max(1, Math.round(g.graphics.size() * objectsMatch)))
        continue; // too little of it here to call it the object
      // AND IT HAS TO BE THE RIGHT SIZE. Graphics are shared: Exolon's rock texture belongs
      // to a planet AND to the whole terrain band, so a definition that names it matches a
      // chain of hundreds of cells and swallows the room. The boxes drawn by hand say how
      // big the thing is; twice that is already generous.
      int[] want = definedSize(g);
      if (want != null && (maxC - minC + 1 > want[0] * 2 + 1 || maxR - minR + 1 > want[1] * 2))
        continue;
      drawObject(snap, g, cells, minC, minR, maxC, maxR);
    }
  }

  /** how big the object was when it was marked: {bytes wide, pixel rows}, or null. */
  private int[] definedSize(EdGroup g) {
    if (g.rects.isEmpty())
      return null;
    int minX = 256, minY = H, maxX = 0, maxY = 0;
    for (int[] r : g.rects) {
      minX = Math.min(minX, r[0]);
      minY = Math.min(minY, r[1]);
      maxX = Math.max(maxX, r[0] + r[2]);
      maxY = Math.max(maxY, r[1] + r[3]);
    }
    return new int[]{Math.max(1, (maxX - minX + 7) / 8), Math.max(1, maxY - minY)};
  }

  /** one instance: its composed pixels, inflated the way the object asks to be rendered. */
  private void drawObject(TaintReplay.FrameSnapshot snap, EdGroup g, List<Integer> cells,
      int minC, int minR, int maxC, int maxR) {
    int w = maxC - minC + 1, rows = maxR - minR + 1;
    byte[] bits = new byte[w * rows];
    int[] inkVotes = new int[16];
    for (int i : cells) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      bits[(y - minR) * w + (c - minC)] = snap.pixels()[i];
      objClaimed[i] = true;
      int attr = snap.attrs()[(y >> 3) * 32 + c] & 0xff;
      if ((attr & 7) != ((attr >> 3) & 7))
        inkVotes[ink(attr)]++;
    }
    Model m = sprite3d.model(SpriteBitmap.ofScreen(bits, w, g.graphics.isEmpty() ? -1
        : g.graphics.iterator().next()), g.render == null ? viewerDefaults() : g.render);
    if (m == null) {
      for (int i : cells)
        objClaimed[i] = false; // could not mesh it: leave it to whoever drew it before
      return;
    }
    int best = 7;
    for (int i = 0; i < 16; i++)
      if (inkVotes[i] > inkVotes[best] || (inkVotes[best] == 0 && inkVotes[i] > 0))
        best = i;
    ModelInstance inst = new ModelInstance(m);
    inst.transform.setToTranslation((minC + maxC + 1) * 4f, H - (minR + maxR + 1) / 2f, midZ());
    inst.materials.first().set(ColorAttribute.createDiffuse(PALETTE[best]));
    wetten(inst.materials.first());
    spriteInstances.add(inst);
    spriteBoxes.add(new float[]{(minC + maxC + 1) * 4f, H - (minR + maxR + 1) / 2f,
        w * 4f, rows / 2f});
  }

  private void updateSprites(TaintReplay.FrameSnapshot snap) {
    int[][] grid = new int[H][32];
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7);
      grid[y][i & 31] = objClaimed[i] ? 0 : snap.owner()[i];
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
        if (bytes < 4 && holeWatch) {
          int lit = 0;
          for (int[] q : cells) {
            int i = ((q[1] & 0xC0) << 5) | ((q[1] & 7) << 8) | ((q[1] & 0x38) << 2) | q[0];
            lit += Integer.bitCount(snap.pixels()[i] & 0xff);
          }
          if (lit > 0)
            System.out.println("hueco (blob chico) frame " + snap.frame() + ": base $"
                + Integer.toHexString(base - 1) + " " + bytes + " bytes, " + lit
                + " px encendidos, en r" + b[2] + "c" + b[1]);
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
    frameBases.clear();
    for (int bi = 0; bi < blobs.size(); bi++) {
      int[] blob = blobs.get(bi);
      int base = blob[0] - 1;
      int[] b = {blob[1], blob[2], blob[3], blob[4]};
      int bytes = catalog.sizeOf.getOrDefault(base, 32);
      // bytes per row: 2 (16px) unless the catalog knows better (DD sprites are 1..3 wide)
      int stride = catalog.strideOf.getOrDefault(base, 2);
      // A catalog piece can cover a WHOLE ANIMATION STRIP: taint-discovery cuts pieces at
      // address GAPS, so frames stored back to back have nothing to cut on (Monty on the
      // Run: 85..113-byte pieces for a 16-row character). Reading all of it stacks several
      // frames into one model — the character comes out double height with the next frame
      // mixed into its shape, and every frame of the walk cycle a different mess. The blob
      // on screen knows how tall the graphic really is, so cap by it.
      //
      // Only when the excess is a WHOLE extra frame: a blob that is merely clipped by the
      // screen edge or overlapped by another sprite still reads its full bitmap, and a
      // catalog that already sizes its sprites right (JSW's 32 bytes over a 16-row blob)
      // is left exactly as it was.
      int blobBytes = (blob[4] - blob[2] + 1) * stride;
      if (blobBytes > 0 && bytes >= blobBytes * 2)
        bytes = blobBytes;
      // Sprite3D: the bitmap is the same one the old code fed the builders (memory bitmap,
      // or the on-screen pixels in adjacent mode), so with no override and auto off this
      // resolves to the very same builder call it always made
      SpriteBitmap sb = blobsAdjacent
          ? SpriteBitmap.ofScreen(bitmaps.get(bi), b[2] - b[0] + 1, base)
          : SpriteBitmap.ofMemory(base, bytes, stride, replay::memByte);
      if (base >= 0 && !frameBases.contains(base)) {
        frameBases.add(base);
        lastBitmap.put(base, sb); // the tuning keys need a bitmap to analyze
      }
      Model model = sprite3d.model(sb, viewerDefaults());
      if (model == null) {
        if (holeWatch)
          System.out.println("hueco (modelo null) frame " + snap.frame() + ": base $"
              + Integer.toHexString(base) + " en r" + b[1] + "c" + b[0]
              + " " + (b[2] - b[0] + 1) + "x" + (b[3] - b[1] + 1) + " celdas");
        continue; // too big to mesh under any technique: stays in the 2D backdrop
      }
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
        inst.materials.first().set(ColorAttribute.createDiffuse(paintRoles ? Color.WHITE : cc));
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
  private Model pixModel(byte[] bmp, int wBytes, float depth) {
    long key = (wBytes * 31L + Float.floatToIntBits(depth)) * 1099511628211L;
    for (byte x : bmp)
      key = (key ^ (x & 0xff)) * 1099511628211L; // FNV-1a
    return pixModelCache.computeIfAbsent(key, k -> smooth
        ? SmoothSpriteBuilder.build(0, bmp.length, wBytes, a -> bmp[a] & 0xff, smoothLevel, depth)
        : VoxelSpriteBuilder.build(0, bmp.length, wBytes, a -> bmp[a] & 0xff, smoothLevel, depth));
  }

  /**
   * The config a sprite gets when nothing overrides it and the automatic layer is off: the
   * viewer's own live sliders, i.e. exactly what the builders were called with before the
   * Sprite3D subsystem existed. Everything else is layered ON TOP of this.
   */
  /** hand a batch of models over to {@link #releaseRetired()} instead of disposing now. */
  private void retire(java.util.Collection<Model> models) {
    pendingDispose.addAll(models);
  }

  /**
   * Frees everything retired since the last frame. Only safe once the instance lists are
   * empty, which is why it is called at the top of the per-frame update, right after they
   * are cleared and before the passes rebuild them.
   */
  private void releaseRetired() {
    if (sprite3d != null)
      pendingDispose.addAll(sprite3d.drainRetired());
    if (pendingDispose.isEmpty())
      return;
    pendingDispose.forEach(Model::dispose);
    pendingDispose.clear();
  }

  /** the sprite the tuning keys act on: one of the bases drawn last frame, or -1. */
  private int tunedBase() {
    if (frameBases.isEmpty())
      return -1;
    return frameBases.get(Math.floorMod(tunedSprite, frameBases.size()));
  }

  /** its config today — the stored override if any, otherwise what it would resolve to. */
  private Sprite3DConfig tunedConfig() {
    int base = tunedBase();
    SpriteBitmap b = base < 0 ? null : lastBitmap.get(base);
    if (b == null)
      return viewerDefaults();
    return sprite3d.configFor(b, viewerDefaults()).copy();
  }

  /**
   * Steps the tuned sprite's technique or primitive and stores it WITHOUT writing to disk,
   * so the change is on screen next frame and F10 is what commits it.
   */
  private void cycleTuned(int techStep, boolean primitiveStep) {
    int base = tunedBase();
    if (base < 0)
      return;
    Sprite3DConfig c = tunedConfig();
    if (primitiveStep) {
      Sprite3DConfig.Primitive[] ps = Sprite3DConfig.Primitive.values();
      c.primitive = ps[(c.primitive.ordinal() + 1) % ps.length];
      c.technique = Sprite3DConfig.Technique.PRIMITIVE; // the primitive only shows there
    } else if (techStep != 0) {
      Sprite3DConfig.Technique[] ts = Sprite3DConfig.Technique.values();
      c.technique = ts[Math.floorMod(c.technique.ordinal() + techStep, ts.length)];
    }
    sprite3d.store().putTransient(base, c);
    printSpriteTuning();
  }

  private void printSpriteTuning() {
    int base = tunedBase();
    if (base < 0) {
      System.out.println("sprite3d: no hay sprites en pantalla para ajustar");
      return;
    }
    Sprite3DConfig c = tunedConfig();
    SpriteBitmap b = lastBitmap.get(base);
    SpriteFeatures f = b == null ? null : sprite3d.features(b);
    System.out.printf("sprite3d [%d/%d] $%04x  %s%s  %s  regla=%s  %s%n",
        Math.floorMod(tunedSprite, frameBases.size()) + 1, frameBases.size(), base,
        c.technique, c.technique == Sprite3DConfig.Technique.PRIMITIVE ? "/" + c.primitive : "",
        sprite3d.store().has(base) ? "[OVERRIDE]" : "(auto/default)",
        f == null ? "?" : sprite3d.selector().explain(f), f == null ? "" : f);
  }

  private Sprite3DConfig viewerDefaults() {
    Sprite3DConfig c = new Sprite3DConfig();
    c.technique = smooth ? Sprite3DConfig.Technique.INFLATE : Sprite3DConfig.Technique.VOXELS;
    c.depth = depthScale;
    c.smoothLevel = smoothLevel;
    c.voxelLook = spriteVoxelLook;
    c.smoothing = spriteSmoothing;
    c.roundness = spriteRoundness;
    c.voxelFill = spriteVoxelFill;
    c.epx = Math.max(1, Math.round(spriteEpx));
    SpriteFx.MAX_DEPTH = spriteMaxDepth; // the builders read it as a global
    return c;
  }

  /**
   * A tile SLAB built from a screen bitmap instead of a memory template, content-hashed.
   * Slab and not {@link #pixModel}: the sprite builders inflate a balloon around the
   * silhouette, which reads as a thin puffy sheet — a platform has to be a solid block
   * extruded back toward the backdrop, and it is that block's thickness that gives the
   * characters at {@link #midZ()} something to stand in the middle of.
   */
  private Model pixSlab(byte[] bmp, float depth) {
    long key = (Float.floatToIntBits(depth) * 31L + 0x5AB) * 1099511628211L;
    for (byte x : bmp)
      key = (key ^ (x & 0xff)) * 1099511628211L; // FNV-1a
    return pixModelCache.computeIfAbsent(key,
        k -> TileSlabBuilder.build(0, a -> bmp[a] & 0xff, depth, 1));
  }

  /**
   * Background relief built from the SCREEN, one model per 8x8 cell (-Dtiles=screen).
   *
   * <p>{@link #updateTiles} extrudes each cell from its tile TEMPLATE in memory, which needs
   * the catalog to have found real 8-row tile bitmaps. Exolon has none: its scenery is a
   * dithered texture, so reading 8 bytes at the discovered leaf yields an arbitrary bitmap
   * and the room comes out as noise. The screen, though, already holds the composed picture
   * — the same shortcut the composite sprites use (doc §5, atajo de render). Content-hashed,
   * so a dithered rock band collapses to a handful of distinct 8x8 models.
   *
   * <p>Shallower than the sprites ({@code relief.depth}) so entities still read as the
   * things standing in front of the scenery.
   */
  /** window for "this cell is being animated right now": missiles, explosions, walkers. */
  /**
   * How many frames back still counts as "moving" (-Drelief.dyn). Too long and a fast
   * character's TRAIL keeps qualifying, so the cells it left behind float as leftover
   * models beside it; too short and a sprite that pauses sinks back into the scenery.
   */
  private int DYN_FRAMES = iprop("render.relief.dyn", "relief.dyn", 4);
  /** static islands of at most this many cells are decor (float), not architecture. */
  private int decorCells = iprop("render.relief.decor", "relief.decor", 12);
  /**
   * Max bounding-box side (in cells) for a free-floating compact mass to count as a prop —
   * a planet, a moon — however many cells it has (-Drelief.island). See {@link #island}.
   */
  private int islandCells = iprop("render.relief.island", "relief.island", 8);
  /**
   * What to do with a SMALL THIN BAR hanging in the sky — a streak of stars, a trail, a
   * laser: the shape that is small enough to be a prop but too elongated to be a body, so
   * {@link #island} will not take it (-Drelief.bar).
   *
   * <p>Not decidable from the shape alone, which is why it is a knob: in Exolon those
   * streaks are background sparkle and extruding them puts a row of little combs across the
   * sky ("dots that go from the front to the back and look like a platform"); in Monty on
   * the Run the very same shape is the ledge Monty walks on. Default {@code slab} keeps
   * every game as it was; Exolon's profile sets {@code flat}.
   */
  private String barMode = sprop("render.relief.bar", "relief.bar", "slab");
  /**
   * A static island of at most this many cells hanging in the sky is a SPECK — a star, a
   * spark — and stays flat in the 2D backdrop instead of being modelled (-Drelief.dot,
   * 0 = off). Every other rule here decides slab vs prop; this one decides "not 3D at all",
   * which is the right answer for a single lit pixel: modelled as a prop it becomes a little
   * ball floating in front of the backdrop, and a skyful of them reads as dirt on the glass.
   */
  private int dotCells = iprop("render.relief.dot", "relief.dot", 0);
  /**
   * A cell that NO graphic claims (no catalog leaf) and that has at most this many lit
   * pixels is background and stays flat (-Drelief.dotpx, 0 = off). This is the rule for art
   * that is not a graphic at all — Exolon's stars are single pixels lit by a routine, so
   * there is no address to catalogue, none to name in {@link #flatLeaves}, and no island to
   * measure once connectivity has glued the scattering together.
   */
  private int dotPixels = iprop("render.relief.dotpx", "relief.dotpx", 0);
  /**
   * Graphics that must NEVER be modelled: their cells stay in the flat 2D backdrop, where
   * they belong. Keyed by the catalog leaf — the address the taint says those pixels came
   * from — which is what "this particular graphic" means here, the same idea as the
   * per-sprite overrides being keyed by catalog base.
   *
   * <p>This is the escape hatch for what no shape rule gets right: Exolon's star specks look
   * exactly like a piece of scenery to any geometric test (they are 8x8, they sit still,
   * they are lit), and extruded they read as little platforms hanging in the sky. Naming the
   * graphic is both simpler and more honest than a rule that pretends to infer it.
   *
   * <p>Seeded from {@code render.relief.flat} (a comma-separated list in games.json or -D)
   * and from what the I/W keys saved in {@code ~/.jsw3d-relief-<game>.json}.
   */
  private final java.util.Set<Integer> flatLeaves = new java.util.LinkedHashSet<>();
  /** leaf -> cells it covered this frame, for the I key to cycle through. */
  private final Map<Integer, Integer> frameLeaves = new java.util.LinkedHashMap<>();
  /** the graphic the I/W keys point at: its cells render white so you can see which it is. */
  private int pickedLeaf = -1;

  /**
   * THE SPRITE EDITOR (Ctrl+E). Automatic grouping cannot tell "the capsule" from "the
   * capsule with the player standing in front of it": on screen they are one connected,
   * one-drawing, same-frame thing, and every rule tried here split or merged the wrong pair.
   * A person looking at it knows instantly. So: the game freezes, a cursor walks the screen,
   * whatever GRAPHIC is under the cursor lights up, and space adds it to (or takes it out of)
   * the object being defined. What comes out is the identification by hand, saved per game.
   *
   * <p>The pieces are catalogue addresses — a sprite base or a tile leaf — because that is
   * what the taint can point at in a later run: the definition has to survive the object
   * moving, animating and being drawn somewhere else entirely.
   */
  private boolean editorOn = Boolean.getBoolean("editor");
  /**
   * Objects marked BY HAND, each a set of rectangles over the screen. Automatic grouping got
   * some right and many wrong, and no amount of tuning fixes the ones it merges (a capsule
   * with the player in front of it) or splits (a ship in two colours): on the screen there is
   * no signal that says where one object ends. Drawing the boxes takes seconds and handles
   * what nothing automatic can — parts that are disconnected, or a hole between two parts
   * that must NOT belong to the object.
   *
   * <p>Two levels, because there are two questions: which object, and which box of it.
   */
  private final List<EdGroup> edGroups = new ArrayList<>();
  private int edGroup, edRect;
  /** graphic -> the defined object that claims it, built once from {@link #edGroups}. */
  private final Map<Integer, Integer> gfxToObj = new HashMap<>();
  /** screen bytes an object instance is modelling this frame: nobody else may draw them. */
  private final boolean[] objClaimed = new boolean[TaintReplay.PIXEL_BYTES];
  /** what fraction of an object's graphics has to be on screen to call it that object. */
  private float objectsMatch = fprop("render.objects.match", "objects.match", .5f);
  private boolean objectsOn = bprop("render.objects", "objects", true);
  /** the last snapshot, so the editor can ask what is under the cursor while frozen. */
  private TaintReplay.FrameSnapshot lastSnap;
  /**
   * Biggest island (in cells) that may be held as "a character standing still" after the
   * sprite taint lets go of it (-Drelief.hold). A 16x16 character is 4 cells; anything much
   * larger is scenery the catalog over-claimed, and it settles into slabs as usual.
   */
  private int holdCells = iprop("render.relief.hold", "relief.hold", 12);
  /**
   * Fill the enclosed background of a floating blob before inflating it
   * (-Drelief.fill=false to keep the literal silhouette). The games draw items as hollow
   * outlines; without this they inflate into rings instead of bodies.
   */
  private boolean reliefFill = bprop("render.relief.fill", "relief.fill", true);
  /** how far a slab's paper volume is pulled towards its ink colour. See slabPaper(). */
  private float paperTint = fprop("render.relief.paper", "relief.paper", 0.35f);

  /**
   * -Drelief.audit=N dumps, at frame N, what {@link #updateScreenRelief} decided for every
   * playfield cell, as a 32-column map: '.' air, 'G' sprite cell rebuilt from the scenery
   * cache, 'f' sprite cell whose leftover ink floats, 'X' sprite cell that drew NOTHING (the
   * hole), 'D' cell floating because it is being rewritten, 'd' small island floating as
   * decor, 'T' slab, 'F' left flat to the 2D backdrop, 'g' prop ghost (floating, not a slab),
   * 'I' item, '?' the model came out null.
   * Deterministic, unlike the render.
   */
  private final int auditFrame = Integer.getInteger("relief.audit", -1);
  private boolean auditing;
  private boolean auditDone;
  /**
   * -Drelief.holes=true reports, EVERY frame, the playfield cells that are lit on the real
   * screen and end up drawn by nobody: the relief claimed them (so {@link #updateBackdrop}
   * left them out of the 2D fallback) but no model came out. That is precisely a black hole
   * in the room, the shape the "the platform vanishes when a character comes near" bug takes,
   * and it is deterministic — unlike judging it from a screenshot.
   */
  private final boolean holeWatch = Boolean.getBoolean("relief.holes");
  /**
   * -Drelief.paint=true paints every model by WHO DREW IT, ignoring the game's colours, so a
   * thing you cannot place in the pipeline can be identified by looking at it: red = slab
   * (architecture), green = floating prop (decor/moving), yellow = the ink left around a
   * character, blue = ghost slab under a sprite, cyan = prop ghost, magenta = detected item,
   * white = sprite model. Anything that keeps its own colour is NOT the relief: it is the
   * flat 2D backdrop or an ambient effect (junk, snow, balloons).
   */
  private final boolean paintRoles = Boolean.getBoolean("relief.paint");
  /**
   * -Drelief.flips=true reports every cell that CHANGES ROLE from one frame to the next while
   * its pixels stay the same. A cell whose content did not change but that swaps slab for
   * floating lump (or back) is a flicker the viewer sees as "it disappears and comes back",
   * and it is the shape both "the character stops being volumetric on some frames" and "the
   * tips of the platform blink" take.
   */
  private final boolean flipWatch = Boolean.getBoolean("relief.flips");
  private final char[] lastRole = new char[24 * 32];
  private final byte[][] lastBmp = new byte[24 * 32][];
  /** what the relief decided for each cell THIS frame, in the audit's own letters. */
  private final char[] role = new char[24 * 32];
  /** the last role the cell had while NO sprite was on it: what its ghost should look like. */
  private final char[] sceneryRole = new char[24 * 32];
  /** per cell, the last frame the sprite taint owned it. */
  private final int[] entityFrame = new int[24 * 32];
  /**
   * The pixels a character left in the cell when the taint stopped owning him: while the
   * cell still shows EXACTLY that, he is standing there and the cell is not scenery.
   */
  private final byte[][] holdBmp = new byte[24 * 32][];
  private final boolean[] pendingHold = new boolean[24 * 32];

  /** -Drelief.holes: cells the repaint rule kept as scenery this frame (they would have
   *  turned into floating lumps while a character walked past). */
  private int repainted;
  /** -Drelief.holes: cells above the floor rendered as slabs / left flat, this frame. */
  private int skySlabs, skyFlats;
  /** -Drelief.holes: ghosts drawn this frame, by the role the cell had before: slab/decor/flat */
  private final int[] ghostRoles = new int[3];

  private void updateScreenRelief(TaintReplay.FrameSnapshot snap) {
    repainted = skySlabs = skyFlats = 0;
    frameLeaves.clear();
    java.util.Arrays.fill(ghostRoles, 0);
    auditing = auditFrame >= 0 && !auditDone && shownFrame >= auditFrame;
    java.util.Arrays.fill(role, ' ');
    tileInstances.clear();
    System.arraycopy(solidCells, 0, prevSolidCells, 0, solidCells.length);
    java.util.Arrays.fill(solidCells, false);
    // indexed by ABSOLUTE cell (row 0..23), like every other per-cell array here, so a
    // playfield that does not start at row 0 needs no index juggling
    int top = Math.min(24, playfieldTop), end = playEnd(), n = 24 * 32;
    byte[][] bmps = new byte[n][];
    int[] tOf = new int[n];
    boolean[] spr = new boolean[n], dyn = new boolean[n];
    /** being rewritten with something OTHER than what the cell already had: it moves */
    boolean[] rew = new boolean[n];
    // what each floating cell contributes to its object, resolved into blobs at the end
    byte[][] floatBmp = new byte[n][];
    int[] floatAttr = new int[n];
    /** a detected item: in lantern mode it glows on its own, as it always did */
    boolean[] floatGlow = new boolean[n];
    for (int cellY = top; cellY < end; cellY++)
      for (int col = 0; col < 32; col++) {
        int y0 = cellY * 8, cell = cellY * 32 + col;
        byte[] bmp = new byte[8];
        int bits = 0, t = 0;
        for (int r = 0; r < 8; r++) {
          int i = idx(y0 + r, col);
          int v = objClaimed[i] ? 0 : snap.pixels()[i] & ~snap.spriteBits()[i] & 0xff;
          spr[cell] |= snap.owner()[i] != 0 || objClaimed[i];
          dyn[cell] |= snap.frame() - replay.lastWrite[i] <= DYN_FRAMES;
          if (t == 0)
            t = snap.tile()[i];
          bmp[r] = (byte) v;
          bits |= v;
        }
        bmps[cell] = bits == 0 ? null : bmp;
        tOf[cell] = t;
        if (spr[cell]) {
          entityFrame[cell] = snap.frame();
          holdBmp[cell] = null;
          pendingHold[cell] = true;
        } else if (pendingHold[cell]) {
          holdBmp[cell] = bmps[cell]; // the pose he was left in, the frame ownership lapsed
          pendingHold[cell] = false;
        }
        // air with nobody standing on it: the scenery cache is stale, drop it here so the
        // component graph below never bridges through a cell that is empty now
        if (bmps[cell] == null && !spr[cell])
          cellBmp[cell] = null;
        rew[cell] = dyn[cell] && !sameAsCache(cell, bmps[cell]);
      }
    // "Walkable vs decorative vs moving" is NOT the same cut as tile/sprite/none, and the
    // classification alone was rendering Exolon's floor flat and its stars as slabs. Two
    // extra signals decide: WRITE FRESHNESS (a cell being animated right now is a moving
    // thing, whatever the catalog says) and HORIZONTAL RUN LENGTH of the static
    // unclassified cells (a floor band spans many columns, a star or planet only a few).
    // Decor vs architecture by CONNECTED-COMPONENT SIZE over the static cells. A floor or a
    // wall is one big connected mass; a planet or a star is a small island floating in the
    // sky. Horizontal run length was tried first and could not tell a planet from a narrow
    // pillar — the pillar is narrow too, but it is connected all the way down to the floor.
    // Crucially this ignores the tile classification: Exolon's planets ARE tile-classified,
    // so gating on "unclassified" left them extruded as scenery.
    // A ROOM CHANGE IS NOT MOTION. Write freshness only means "this moves" when it is LOCAL:
    // when the game repaints the whole screen at once, every cell is fresh and the room spent
    // the whole dyn window as floating lumps before settling into slabs — the depth visibly
    // arrived a moment late. If most of what is lit was just rewritten, nothing here is a
    // moving thing and the frame is classified as if it were all static.
    int fresh = 0, litCells = 0;
    for (int c0 = top * 32; c0 < end * 32; c0++) {
      if (bmps[c0] == null)
        continue;
      litCells++;
      if (rew[c0])
        fresh++;
    }
    if (fresh > .4f * litCells) {
      java.util.Arrays.fill(rew, false);
      java.util.Arrays.fill(pendingHold, false);
      java.util.Arrays.fill(holdBmp, null);
    }
    // The component graph is "WHERE THERE IS SCENERY", not "what is standing still": a cell
    // counts if it has ink now or has clean pixels cached from before a sprite covered it.
    // Leaving the moving cells out made membership depend on whatever walked past — a sprite
    // crossing a platform split it in two, the leftover piece fell under the decor threshold,
    // and its cells swapped slab for floating lump and back with their pixels untouched
    // (measured: 853 T<->d flips on Exolon). Connectivity has to be a property of the room.
    int[] comp = new int[n];
    java.util.Arrays.fill(comp, -1);
    java.util.List<Integer> compSize = new ArrayList<>();
    // per component: minC, minR, maxC, maxR — the shape that tells a planet from a wall
    java.util.List<int[]> compBox = new ArrayList<>();
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    for (int c0 = 0; c0 < n; c0++) {
      if (comp[c0] >= 0 || !scenery(bmps, spr, rew, c0))
        continue;
      int id = compSize.size(), size = 0;
      int[] bx = {31, 23, 0, 0};
      comp[c0] = id;
      queue.add(c0);
      while (!queue.isEmpty()) {
        int p = queue.poll();
        size++;
        int py = p >> 5, px = p & 31;
        bx[0] = Math.min(bx[0], px);
        bx[1] = Math.min(bx[1], py);
        bx[2] = Math.max(bx[2], px);
        bx[3] = Math.max(bx[3], py);
        for (int dy = -1; dy <= 1; dy++)
          for (int dx = -1; dx <= 1; dx++) {
            int ny = py + dy, nx = px + dx;
            if (ny < top || ny >= end || nx < 0 || nx > 31)
              continue;
            int q = ny * 32 + nx;
            if (comp[q] < 0 && scenery(bmps, spr, rew, q)) {
              comp[q] = id;
              queue.add(q);
            }
          }
      }
      compSize.add(size);
      compBox.add(bx);
    }
    // A CHARACTER WHO STOPPED. Cells still showing the exact pose the taint last owned, and
    // showing something other than the room's own scenery, are him standing still — the
    // freshness gate dropped him but he never left. A time window was not enough (measured:
    // he flattens 40-110 frames after ownership lapses, and a window long enough to cover
    // that also freezes anything the catalog over-claims), so what bounds it is SIZE: a held
    // island bigger than a character is scenery the catalog claimed, and it settles.
    boolean[] held = new boolean[n];
    for (int c0 = top * 32; c0 < end * 32; c0++)
      held[c0] = holdBmp[c0] != null && java.util.Arrays.equals(holdBmp[c0], bmps[c0])
          && !sameAsCache(c0, bmps[c0]);
    boolean[] seenHold = new boolean[n];
    for (int c0 = top * 32; c0 < end * 32; c0++) {
      if (!held[c0] || seenHold[c0])
        continue;
      List<Integer> group = new ArrayList<>();
      queue.add(c0);
      seenHold[c0] = true;
      while (!queue.isEmpty()) {
        int p = queue.poll();
        group.add(p);
        int py = p >> 5, px = p & 31;
        for (int dy = -1; dy <= 1; dy++)
          for (int dx = -1; dx <= 1; dx++) {
            int ny = py + dy, nx = px + dx;
            if (ny < top || ny >= end || nx < 0 || nx > 31)
              continue;
            int q = ny * 32 + nx;
            if (held[q] && !seenHold[q]) {
              seenHold[q] = true;
              queue.add(q);
            }
          }
      }
      if (group.size() > holdCells)
        for (int p : group)
          held[p] = false;
    }
    for (int cellY = top; cellY < end; cellY++)
      for (int col = 0; col < 32; col++) {
        int y0 = cellY * 8, cell = cellY * 32 + col;
        byte[] bmp = bmps[cell];
        cellClaimed[cell] = false;
        int attr = snap.attrs()[cell] & 0xff;
        int cellLeaf = tOf[cell] - 1;
        if (tOf[cell] != 0 && flatLeaves.contains(cellLeaf)) {
          // named by hand as background art (W key / games.json): never a model, not even a
          // ghost — the 2D backdrop paints it, which is exactly where a star belongs
          role[cell] = 'F';
          sceneryRole[cell] = 'F';
          continue;
        }
        if (bmp != null && tOf[cell] != 0)
          frameLeaves.merge(cellLeaf, 1, Integer::sum);
        if (spr[cell]) {
          role[cell] = cellBmp[cell] != null ? 'G' : bmp != null ? 'f' : 'X';
          if (holeWatch && role[cell] == 'G')
            ghostRoles[sceneryRole[cell] == 'd' ? 1 : sceneryRole[cell] == 'F' ? 2 : 0]++;
          // A GHOST IS ONLY A SLAB IF THE CELL WAS ONE. The ghost rebuilds the scenery a
          // sprite is standing in from the cell's cache, and it always rebuilt it as a slab
          // — so every time anything crossed a star or a cloud, an 8x8 block appeared out of
          // nowhere and ran the slab's full depth into the backdrop ("dots that show up
          // suddenly and go from the front to the back"). It has to come back as whatever it
          // was: a prop floats, and a cell left flat stays flat.
          // A GHOST OF ALMOST NOTHING IS NOT A WALL. Ghosts exist so a character does not
          // punch a hole in a platform, but a cache with one or two lit pixels is a spark or
          // a star, and rebuilding it as a slab dropped an 8x8 block running the full depth
          // into the backdrop — the "dots that look like platforms", which show up NEXT TO
          // OTHER SPRITES precisely because that is when the taint claims the cell and the
          // ghost path takes over. Measured on Exolon: 54 of 317 ghosts were 1-2 pixel cells.
          int cachePx = 0;
          if (cellBmp[cell] != null)
            for (int r = 0; r < 8; r++)
              cachePx += Integer.bitCount(cellBmp[cell][r] & 0xff);
          boolean cacheSpeck = dotPixels > 0 && cachePx <= dotPixels;
          if (cellBmp[cell] != null && (sceneryRole[cell] != 'T' || cacheSpeck)) {
            if (cacheSpeck) {
              role[cell] = 'F'; // leave those pixels to the 2D backdrop, where they were
              continue;
            }
            if (sceneryRole[cell] == 'd') {
              byte[] both = cellBmp[cell].clone();
              if (bmp != null)
                for (int r = 0; r < 8; r++)
                  both[r] |= bmp[r];
              floatBmp[cell] = both;
              floatAttr[cell] = cellAttr[cell];
              cellClaimed[cell] = true;
              role[cell] = 'g';
            }
            continue; // 'F': nothing to model, the 2D backdrop still paints those pixels
          }
          // scenery the sprite is standing in, rebuilt see-through from the cell's cache.
          // Claim it only if the ghost really got drawn: a claim with no model is a hole,
          // because the backdrop then skips the cell too
          if (cellBmp[cell] != null && ghostScreenCell(col, y0, cell)) {
            solidCells[cell] = true;
            cellClaimed[cell] = true;
            if (holeWatch)
              System.out.println("fantasma-losa px=" + cachePx + " hoja="
                  + (tOf[cell] == 0 ? "NINGUNA" : "$" + Integer.toHexString(tOf[cell] - 1)));
          }
          // ink that is neither the sprite's own nor cached scenery is the REST of a
          // fragmented character: inflate it too, instead of dropping it into holes
          if (bmp != null) {
            byte[] extra = bmp.clone();
            int any = 0;
            for (int r = 0; r < 8; r++) {
              if (cellBmp[cell] != null)
                extra[r] &= ~cellBmp[cell][r];
              any |= extra[r] & 0xff;
            }
            if (any != 0) {
              // per cell, NOT merged into a blob: this ink is whatever the character's mask
              // did not claim inside his own cells — mostly his own pixels the taint missed.
              // Merging them built a smooth body around him that hid the real sprite model.
              boolean drawn = floatCell(col, y0, extra, attr);
              cellClaimed[cell] |= drawn;
              if (role[cell] != 'G')
                role[cell] = drawn ? 'f' : '?';
            }
          }
          continue;
        }
        if (bmp == null) { // air: nothing to build, and the scenery cache is stale
          cellBmp[cell] = null;
          role[cell] = '.';
          continue;
        }
        // REESCRITA CON LO MISMO QUE YA HABÍA = decorado, no cosa que se mueve. Un motor de
        // sprites borra y vuelve a pintar la plataforma que el personaje acaba de tapar, así
        // que sus celdas quedan "frescas" mientras él pasa: con la ventana de movimiento
        // larga la plataforma iba perdiendo celdas a su paso — la losa profunda se cambiaba
        // por un bulto fino a media profundidad y se leía como un mordisco en la plataforma.
        // Si los píxeles son EXACTAMENTE los que la celda ya tenía limpia, no se movió nada.
        boolean rewritten = rew[cell];
        if (holeWatch && dyn[cell] && !rewritten)
          repainted++;
        // AN ENTITY THAT STOPPED IS STILL AN ENTITY. A dirty-region engine does not repaint
        // what is not moving, so the player standing still loses his sprite taint to the
        // freshness gate, joins the terrain he stands on as one static component, and gets
        // extruded back into the wall — "some frames he stops being volumetric". A cell the
        // character OWNED moments ago (-Drelief.hold) keeps floating...
        //
        // Keyed on sprite ownership, NOT on write freshness: stamping every freshly written
        // cell meant a whole screen change held the entire room floating for the whole
        // window, and the room visibly took a moment to take its depth.
        // ...and what it shows is NOT the room's own scenery: when he walks AWAY the engine
        // repaints the background, that matches the clean cache, and the cell goes back to
        // being a slab at once instead of trailing lumps behind him.
        boolean holding = held[cell];
        // Schmitt trigger on the decor threshold: a cell that already floats needs the
        // component to grow to TWICE the limit before it is promoted to architecture. A
        // single threshold makes every component sitting near the limit — a star, a small
        // rock — swap roles with any one-cell change, and the eye reads that as blinking.
        int litPx = 0;
        for (int r = 0; r < 8; r++)
          litPx += Integer.bitCount(bmp[r] & 0xff);
        // A CELL NOBODY CLAIMS, WITH ALMOST NOTHING LIT, IS BACKGROUND. Exolon's stars are
        // not a graphic at all: a routine lights single pixels, so the taint has no address
        // to point at (`tile` = 0) and there is nothing to name with relief.flat either.
        // Measured: 10.446 sky cells came out as slabs with no leaf, and 8.365 of them had
        // exactly ONE lit pixel — while the unclassified TERRAIN cells carry 30-46. One
        // pixel and no origin is a speck of sky; that is the whole rule.
        //
        // It cannot be an island test: 8-connectivity glues a scattering of stars into
        // components of 47-112 cells, so by size and bbox they look like architecture.
        boolean speck = dotPixels > 0 && litPx <= dotPixels && tOf[cell] == 0
            || comp[cell] >= 0 && compBox.get(comp[cell])[3] < end - 1
                && compSize.get(comp[cell]) <= dotCells;
        boolean bar = !speck && !"slab".equals(barMode) && comp[cell] >= 0
            && skyBar(comp[cell], compBox, end);
        boolean decor = !speck && comp[cell] >= 0
            && (compSize.get(comp[cell]) <= (lastRole[cell] == 'd' ? 2 * decorCells : decorCells)
                || island(comp[cell], compBox, end)
                || (bar && "float".equals(barMode)));
        boolean flat = speck || (bar && !decor); // leave it to the 2D backdrop, no model
        role[cell] = rewritten || holding ? 'D' : decor ? 'd' : flat ? 'F' : 'T';
        if (!rewritten && !holding)
          sceneryRole[cell] = role[cell];
        if (holeWatch && comp[cell] >= 0 && compBox.get(comp[cell])[3] < end - 1) {
          int[] bx = compBox.get(comp[cell]);
          System.out.println("cielo rol=" + role[cell] + " px=" + litPx
              + " hoja=" + (tOf[cell] == 0 ? "NINGUNA" : "$" + Integer.toHexString(cellLeaf))
              + " comp=" + compSize.get(comp[cell])
              + " bbox=" + (bx[2] - bx[0] + 1) + "x" + (bx[3] - bx[1] + 1));
        }
        if (holeWatch && comp[cell] >= 0 && compBox.get(comp[cell])[3] < end - 1) {
          if (role[cell] == 'T')
            skySlabs++;
          else if (role[cell] == 'F')
            skyFlats++;
        }
        if (flat)
          continue; // unclaimed: updateBackdrop paints these pixels in 2D, as they were
        if (flipWatch && role[cell] == 'T' && lastRole[cell] == 'D'
            && snap.frame() - entityFrame[cell] <= 80)
          System.out.println("aplanado r" + cellY + "c" + col + ": edad="
              + (snap.frame() - entityFrame[cell]) + " sameCache=" + sameAsCache(cell, bmp)
              + " comp=" + (comp[cell] < 0 ? -1 : compSize.get(comp[cell])));
        if (rewritten || holding) {
          // BEING REWRITTEN RIGHT NOW: a moving thing — the player, a missile, an
          // explosion, an enemy — floating at mid depth like any sprite.
          //
          // This wins over the tile classification on purpose. Measured on Exolon, 17.4%
          // of lit cells are dynamic AND tile-classified: the catalog knows WHICH graphic
          // is on the cell but not that it is currently in motion, so the player was being
          // extruded backwards as architecture. Only 18% of cells are dynamic at all, so
          // the scenery is not at risk of floating away with them.
          floatBmp[cell] = bmp;
          floatAttr[cell] = attr;
          cellClaimed[cell] = true;
          continue;
        }
        if (decor) {
          // a small isolated island: stars, planets, floating decor — rendered like a
          // mobile sprite (inflated, mid depth), never as a walkable slab.
          // Cached like the slabs: it is scenery too, so a character walking over the
          // ledge gets its ghost instead of punching the ledge out of the room.
          cacheScenery(cell, bmp, attr, snap);
          floatBmp[cell] = bmp;
          floatAttr[cell] = attr;
          cellClaimed[cell] = true;
          continue;
        }
        // scenery: tile-classified, or a wide unclassified band (Exolon's floor)
        int t = tOf[cell], leaf = t - 1;
        if (t != 0) {
          // same item detector as updateTiles: snap.tile() carries the graphic identity
          // even when its memory template is junk
          int prev = prevLeafAttr[cell];
          if (prev != 0 && (prev >> 8) == t) {
            if (((prev ^ attr) & 7) != 0) {
              int fl = (1 << (attr & 7)) | (1 << (prev & 7));
              leafLastFlash.put(leaf, shownFrame);
              if (shownFrame - cellLastChange[cell] <= 20) {
                cellInkChanges[cell]++;
                cellInkMask[cell] |= fl;
              } else {
                cellInkChanges[cell] = 1;
                cellInkMask[cell] = fl;
              }
              cellLastChange[cell] = shownFrame;
              if (cellInkChanges[cell] >= 6 && Integer.bitCount(cellInkMask[cell]) >= 3
                  && itemsOn
                  && itemLeaves.add(leaf) && TaintReplay.LOG)
                System.out.println("item detectado (screen): leaf $" + Integer.toHexString(leaf));
            }
          } else
            cellInkChanges[cell] = 0;
          prevLeafAttr[cell] = (t << 8) | attr;
          if (itemLeaves.contains(leaf)
              && shownFrame - leafLastFlash.getOrDefault(leaf, 0) > 50)
            itemLeaves.remove(leaf); // not flashing anymore: platform, not item
        }
        Color inkColor = PALETTE[ink(attr)];
        if (t != 0 && itemLeaves.contains(leaf)) {
          // A DETECTED ITEM IS NOT A TILE: it goes down the same road as everything else
          // with volume — one model for the whole object, built by floatBlobs from all its
          // cells at once. Modelling it cell by cell here made a 16x16 treasure four
          // separate 8x8 lumps, which is exactly what reads as "still a tile".
          cacheScenery(cell, bmp, attr, snap);
          floatBmp[cell] = bmp;
          floatAttr[cell] = attr;
          floatGlow[cell] = true;
          cellClaimed[cell] = true;
          role[cell] = 'I';
          if (fireOn)
            effects.addFireSpot(col * 8 + 4, H - (y0 + 4), midZ() + 6);
          if (darkMode) {
            float flick = fireOn ? effects.flicker(cell) : 1;
            frameLights.add(new com.badlogic.gdx.graphics.g3d.environment.PointLight().set(
                fireOn ? 1 : inkColor.r, fireOn ? .55f : inkColor.g, fireOn ? .2f : inkColor.b,
                col * 8 + 4, H - (y0 + 4), midZ() + 8, itemLightIntensity * flick));
          }
          continue;
        }
        Model model = pixSlab(bmp, slabDepth() * reliefDepth);
        role[cell] = model == null ? '?' : 'T';
        if (model == null)
          continue;
        cacheScenery(cell, bmp, attr, snap);
        solidCells[cell] = true;
        cellClaimed[cell] = true;
        ModelInstance inst = new ModelInstance(model);
        inst.transform.setToTranslation(col * 8 + 4, H - (y0 + 4), midZ());
        Material ink = inst.getMaterial(TileSlabBuilder.INK);
        ink.set(ColorAttribute.createDiffuse(cellLeaf == pickedLeaf ? Color.WHITE
            : paintRoles ? Color.RED : inkColor));
        wetten(ink);
        Material side = inst.getMaterial(TileSlabBuilder.PAPER_SIDE);
        if (side != null) {
          side.set(ColorAttribute.createDiffuse(paintRoles ? Color.MAROON : slabPaper(attr)));
          wetten(side);
        }
        Material paper = inst.getMaterial(TileSlabBuilder.PAPER);
        if (paper != null) { // an all-ink cell has no paper part
          // NOTE: painting this with the INK colour to hide the black extruded edge was
          // tried and reverted — in Exolon most cells are mostly paper with dithered ink
          // on top, so the whole room turned into solid single-colour bricks and every
          // bit of texture was lost. The black edge needs a narrower fix than this.
          paper.set(ColorAttribute.createDiffuse(paintRoles ? Color.MAROON
              : PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)]));
          wetten(paper);
        }
        tileInstances.add(inst);
      }
    if (holeWatch && (ghostRoles[1] > 0 || ghostRoles[2] > 0))
      System.out.println("fantasmas frame " + snap.frame() + ": losa=" + ghostRoles[0]
          + " sobre-decor=" + ghostRoles[1] + " sobre-plano=" + ghostRoles[2]);
    if (holeWatch && (skySlabs > 0 || skyFlats > 0))
      System.out.println("cielo frame " + snap.frame() + ": losa=" + skySlabs
          + " plano=" + skyFlats);
    if (holeWatch && repainted > 0)
      System.out.println("repintadas frame " + snap.frame() + ": " + repainted
          + " celdas de decorado se salvaron de flotar");
    floatBlobs(floatBmp, floatAttr, floatGlow, tOf, top, end);
    if (auditing) {
      auditDone = true;
      dumpAudit(snap, top, end, tOf, spr, dyn, comp, compSize);
    }
    reportFlips(snap, bmps, top, end);
  }

  /**
   * Everything that FLOATS — an item, a moving thing, a piece of decor — is inflated as the
   * OBJECT it belongs to: adjacent floating cells are one connected blob and get ONE model,
   * exactly how {@link #updateSprites} handles a composite sprite in adjacent mode.
   *
   * <p>Inflating cell by cell was the bug behind "the items must have volume, they are being
   * treated as tiles": each 8x8 chunk got its own rounded lump, so a 16x16 item came out as
   * four beads in a ring instead of one body, and a ledge came out as a row of separate
   * pillows. The silhouette a shape needs to read as a volume simply is not there inside one
   * cell — the distance transform can only see 8 pixels in any direction.
   *
   * <p>Blobs stop at a COLOR change, because on this hardware color is the only thing that
   * says where one object ends: a magenta guardian standing on a cyan ledge is 8-connected
   * to it, and merging the two gave one model tinted by the majority vote — the ledge turned
   * magenta and grew a bulge. Same ink, same object.
   *
   * <p>Falls back to the per-cell inflate when the blob is too big to mesh in one piece
   * (the builders return null past the index limit): better lumpy than missing.
   */
  private void floatBlobs(byte[][] floatBmp, int[] floatAttr, boolean[] floatGlow,
      int[] tOf, int top, int end) {
    boolean[] seen = new boolean[24 * 32];
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    List<Integer> cells = new ArrayList<>();
    for (int c0 = top * 32; c0 < end * 32; c0++) {
      if (seen[c0] || floatBmp[c0] == null)
        continue;
      cells.clear();
      seen[c0] = true;
      queue.add(c0);
      int ink0 = ink(floatAttr[c0]);
      int minC = 31, maxC = 0, minR = 23, maxR = 0;
      boolean glow = false;
      while (!queue.isEmpty()) {
        int p = queue.poll();
        cells.add(p);
        glow |= floatGlow[p];
        int py = p >> 5, px = p & 31;
        minC = Math.min(minC, px);
        maxC = Math.max(maxC, px);
        minR = Math.min(minR, py);
        maxR = Math.max(maxR, py);
        for (int dy = -1; dy <= 1; dy++)
          for (int dx = -1; dx <= 1; dx++) {
            int ny = py + dy, nx = px + dx;
            if (ny < top || ny >= end || nx < 0 || nx > 31)
              continue;
            int q = ny * 32 + nx;
            if (!seen[q] && floatBmp[q] != null && ink(floatAttr[q]) == ink0) {
              seen[q] = true;
              queue.add(q);
            }
          }
      }
      int wB = maxC - minC + 1, rows = (maxR - minR + 1) * 8;
      byte[] bmp = new byte[wB * rows];
      for (int p : cells) {
        int py = p >> 5, px = p & 31;
        for (int r = 0; r < 8; r++)
          bmp[((py - minR) * 8 + r) * wB + (px - minC)] = floatBmp[p][r];
      }
      // the leaf of the blob's first cell keys per-graphic overrides (F7..F10): the same
      // item lands on the same catalog address every time it is drawn
      int base = tOf[cells.get(0)] - 1;
      Model m = sprite3d.model(
          SpriteBitmap.ofScreen(reliefFill ? SpriteFx.fillHoles(bmp, wB) : bmp, wB, base),
          viewerDefaults());
      if (m == null) {
        for (int p : cells) {
          boolean drawn = floatCell(p & 31, (p >> 5) * 8, floatBmp[p], floatAttr[p]);
          cellClaimed[p] = drawn;
          if (!drawn)
            role[p] = '?';
        }
        continue;
      }
      ModelInstance inst = new ModelInstance(m);
      inst.transform.setToTranslation(minC * 8 + wB * 4f,
          H - (minR * 8 + (maxR - minR + 1) * 4f), midZ());
      Color c = base >= 0 && base == pickedLeaf ? Color.WHITE
          : paintRoles ? (glow ? Color.MAGENTA : Color.GREEN)
          : PALETTE[ink0]; // one ink per blob: that is what bounded it in the first place
      inst.materials.first().set(ColorAttribute.createDiffuse(c));
      if (glow && darkMode) // an item blazes with light of its own in the gloom
        inst.materials.first().set(
            ColorAttribute.createEmissive(c.r * .75f, c.g * .75f, c.b * .75f, 1));
      wetten(inst.materials.first());
      tileInstances.add(inst);
    }
  }

  /**
   * -Drelief.flips: the cells that changed role with IDENTICAL pixels, which is the only
   * kind of change the eye reads as flicker — the world did not move, the rendering did.
   */
  private void reportFlips(TaintReplay.FrameSnapshot snap, byte[][] bmps, int top, int end) {
    if (flipWatch) {
      StringBuilder sb = new StringBuilder();
      int n = 0, entityFlips = 0;
      for (int cell = top * 32; cell < end * 32; cell++) {
        char was = lastRole[cell], now = role[cell];
        if (was == 0 || was == now || !java.util.Arrays.equals(lastBmp[cell], bmps[cell])
            || (!draws(was) && !draws(now)))
          continue; // air <-> sprite-only cell draws nothing either way: not a flicker
        n++;
        // '*' marks a cell the sprite taint owned recently: that one is the character
        boolean mine = snap.frame() - entityFrame[cell] <= 80;
        if (mine)
          entityFlips++;
        if (n <= 8)
          sb.append(' ').append(was).append("->").append(now).append(mine ? "*" : "")
              .append("@r").append(cell >> 5).append('c').append(cell & 31);
      }
      if (n > 0)
        System.out.println("flip frame " + snap.frame() + ": " + n + " celdas ("
            + entityFlips + " de personaje)" + sb);
    }
    for (int cell = top * 32; cell < end * 32; cell++) {
      lastRole[cell] = role[cell];
      lastBmp[cell] = bmps[cell];
    }
  }

  /** the -Drelief.audit dump: the decision map plus what the two open questions need. */
  private void dumpAudit(TaintReplay.FrameSnapshot snap, int top, int end, int[] tOf,
      boolean[] spr, boolean[] dyn, int[] comp, java.util.List<Integer> compSize) {
    StringBuilder sb = new StringBuilder("relief.audit frame " + shownFrame + "\n");
    Map<Integer, Integer> leafCells = new HashMap<>();
    for (int cellY = top; cellY < end; cellY++) {
      for (int col = 0; col < 32; col++) {
        int cell = cellY * 32 + col;
        sb.append(role[cell]);
        if (role[cell] != '.' && role[cell] != ' ' && tOf[cell] != 0)
          leafCells.merge(tOf[cell] - 1, 1, Integer::sum);
      }
      sb.append("  row ").append(cellY).append('\n');
    }
    // sprite-owned cells: how STALE their pixels are. A real character is rewritten every
    // frame; scenery the taint mislabelled as sprite has not been touched in ages.
    for (int cellY = top; cellY < end; cellY++)
      for (int col = 0; col < 32; col++) {
        int cell = cellY * 32 + col;
        if (!spr[cell] || role[cell] == '.' || role[cell] == ' ')
          continue;
        int age = Integer.MAX_VALUE, y0 = cellY * 8;
        for (int r = 0; r < 8; r++) {
          int i = idx(y0 + r, col);
          if ((snap.pixels()[i] & 0xff) != 0)
            age = Math.min(age, snap.frame() - replay.lastWrite[i]);
        }
        sb.append("  spr cell r").append(cellY).append("c").append(col)
            .append(" role=").append(role[cell])
            .append(" leaf=$").append(Integer.toHexString(Math.max(0, tOf[cell] - 1)))
            .append(" age=").append(age == Integer.MAX_VALUE ? -1 : age)
            .append(" comp=").append(comp[cell] < 0 ? -1 : compSize.get(comp[cell]))
            .append('\n');
      }
    // every static component with its shape: size, bbox and how full that bbox is. This is
    // what a rule for "isolated island in the sky" (a planet) vs "architecture" has to work
    // with, since size alone calls a big planet a wall.
    Map<Integer, int[]> box = new HashMap<>(); // id -> minC,minR,maxC,maxR,cells
    for (int cellY = top; cellY < end; cellY++)
      for (int col = 0; col < 32; col++) {
        int id = comp[cellY * 32 + col];
        if (id < 0)
          continue;
        int[] b = box.computeIfAbsent(id, k -> new int[]{31, 23, 0, 0, 0});
        b[0] = Math.min(b[0], col);
        b[1] = Math.min(b[1], cellY);
        b[2] = Math.max(b[2], col);
        b[3] = Math.max(b[3], cellY);
        b[4]++;
      }
    sb.append("  componentes estaticas (celdas, bbox, llenado, toca borde):\n");
    box.entrySet().stream()
        .sorted((a, b) -> b.getValue()[4] - a.getValue()[4])
        .limit(14)
        .forEach(e -> {
          int[] b = e.getValue();
          int w = b[2] - b[0] + 1, h = b[3] - b[1] + 1;
          boolean edge = b[0] == 0 || b[2] == 31 || b[1] == top || b[3] == end - 1;
          sb.append(String.format("    #%d %d celdas  %dx%d en r%dc%d  llenado=%.2f  borde=%s%n",
              e.getKey(), b[4], w, h, b[1], b[0], b[4] / (float) (w * h), edge ? "si" : "no"));
        });
    // leaf -> how many cells show it. A platform tile repeats; an item's graphic does not.
    sb.append("  leaf histogram (cells per leaf):\n");
    leafCells.entrySet().stream()
        .sorted((a, b) -> b.getValue() - a.getValue())
        .forEach(e -> sb.append("    $").append(Integer.toHexString(e.getKey()))
            .append(" -> ").append(e.getValue()).append('\n'));
    System.out.println(sb);
  }

  /**
   * A static component that is a THING IN THE SKY, not architecture: it touches no edge of
   * the playfield, its bounding box is small and roughly square, and it fills that box.
   *
   * <p>Size alone could not decide it (Exolon's biggest planets run 16+ cells, well past any
   * decor threshold that does not also swallow a wall) and the planets came out extruded to
   * the backdrop like a piece of terrain. What separates them is that architecture REACHES
   * THE FLOOR — the terrain band, a pillar, a wall — while a planet hangs in the sky, and it
   * is compact where a ledge is a thin bar. Screen-edge contact is NOT anchoring: Exolon's
   * planets are routinely clipped by the left edge, and testing for it left half a planet as
   * a sphere and the clipped half extruded into a beam.
   */
  private boolean island(int id, java.util.List<int[]> box, int end) {
    int[] b = box.get(id);
    int w = b[2] - b[0] + 1, h = b[3] - b[1] + 1;
    if (b[3] == end - 1)
      return false; // standing on the floor of the room: architecture
    // small and not a bar: a planet, a moon, a cloud, a scattering of stars. NOT gated on
    // how FULL the box is — that was measured throwing sparse sky away (a 5-cell cloud in a
    // 4x4 box fills 0.31), and those came out extruded to the backdrop like little combs.
    return Math.max(w, h) <= islandCells && Math.max(w, h) <= 2.5f * Math.min(w, h);
  }

  /**
   * Is there SCENERY in this cell? Clean pixels cached from before a sprite covered it, or
   * ink that is not a moving thing. Both halves matter: without the cache a character
   * crossing a platform splits it in two and the pieces fall under the decor threshold
   * (the tips of the platform blink); without excluding what moves, a missile flying past a
   * planet joins its component and the planet stops being a planet for those frames.
   */
  /** does this role put anything on screen? '.'/'X' are cells the relief leaves to others. */
  private static boolean draws(char role) {
    return role != '.' && role != 'X' && role != ' ';
  }

  private boolean scenery(byte[][] bmps, boolean[] spr, boolean[] rew, int cell) {
    return cellBmp[cell] != null || (bmps[cell] != null && !spr[cell] && !rew[cell]);
  }

  /**
   * Remember the cell's clean pixels — but NOT while a character is (or just was) standing
   * there. Caching him as scenery closed a loop that no amount of holding could open: the
   * one frame he got extruded into the wall, his own pixels became the cell's "clean"
   * bitmap, and from then on {@link #sameAsCache} answered yes and the cell kept treating
   * him as part of the room.
   */
  private void cacheScenery(int cell, byte[] bmp, int attr, TaintReplay.FrameSnapshot snap) {
    if (holdBmp[cell] != null && java.util.Arrays.equals(holdBmp[cell], bmp))
      return; // that is a character parked here, not the room
    cellBmp[cell] = bmp.clone();
    cellAttr[cell] = attr;
  }

  /**
   * A small thin strip hanging in the sky: prop-sized but bar-shaped, so {@link #island}
   * rejects it as "a ledge, not a body". Which of the two it really is depends on the game,
   * so what happens to it is {@link #barMode}.
   */
  private boolean skyBar(int id, java.util.List<int[]> box, int end) {
    int[] b = box.get(id);
    int w = b[2] - b[0] + 1, h = b[3] - b[1] + 1;
    return b[3] < end - 1 && Math.max(w, h) <= islandCells;
  }

  /** are these the very pixels the cell already had while nothing was standing on it? */
  private boolean sameAsCache(int cell, byte[] bmp) {
    return cellBmp[cell] != null && java.util.Arrays.equals(cellBmp[cell], bmp);
  }

  /**
   * The colour for a slab's PAPER volume: the attribute's paper pulled towards its ink by
   * {@code relief.paper} (0 = the paper colour itself, 1 = the ink colour).
   *
   * <p>Only the DEPTH of the paper columns gets this ({@link TileSlabBuilder#PAPER_SIDE});
   * their front skin keeps the true paper colour. Exolon's scenery is a dither of ink over
   * black paper, so the extruded block is mostly paper and a platform read as a black mass
   * from any angle. Tinting the whole paper part instead (tried, reverted twice now) turns
   * the room into solid single-colour bricks: with white ink, a 35% tint made a pillar one
   * flat grey block. The front face is where the dither lives; the mass behind it is where
   * the platform's colour belongs.
   */
  private Color slabPaper(int attr) {
    Color paper = PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)];
    Color inkc = PALETTE[ink(attr)];
    return new Color(paper).lerp(inkc, paperTint);
  }

  /**
   * The GRAPHIC a screen byte came from, rolled up to the catalogue entry that contains it.
   * The taint answers with the exact ADDRESS, so two rows of one sprite report different
   * origins: rolled up, a piece is the graphic, which is what a person sees and names.
   */
  private int originOf(int i) {
    int addr = lastSnap.owner()[i] != 0 ? lastSnap.owner()[i] - 1
        : lastSnap.tile()[i] != 0 ? lastSnap.tile()[i] - 1 : -1;
    if (addr < 0)
      return -1;
    int entry = catalog.entryOf[addr & 0xffff];
    return entry != 0 ? entry - 1 : addr;
  }

  /** one composed object marked by hand: its boxes, what is under them, and its picture. */
  private static final class EdGroup {
    String name;
    final List<int[]> rects = new ArrayList<>(); // {x, y, w, h} in screen pixels
    final java.util.Set<Integer> graphics = new java.util.LinkedHashSet<>();
    com.badlogic.gdx.graphics.Texture thumb;
    int tw, th;
    /** the captured selection: ARGB per pixel, and the graphic behind each one (-1 none). */
    int[] px, owner;
    /** where this object sits in the sheet PNG: {x, y, w, h}, so it can be read back. */
    int[] sheetBox;
    /** how THIS object wants to be rendered, or null for the viewer's defaults. */
    Sprite3DConfig render;
  }

  private EdGroup group() {
    if (edGroups.isEmpty()) {
      EdGroup g = new EdGroup();
      g.name = "objeto" + (edGroups.size() + 1);
      g.rects.add(new int[]{112, 88, 16, 16});
      edGroups.add(g);
      edGroup = 0;
      edRect = 0;
    }
    edGroup = Math.min(edGroup, edGroups.size() - 1);
    return edGroups.get(edGroup);
  }

  /** the editor's own keys while it is open; false lets the viewer have the key. */
  private boolean editorKey(int keycode, boolean ctrl) {
    boolean shift = Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_LEFT)
        || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_RIGHT);
    int step = ctrl ? 8 : 1;
    EdGroup g = group();
    edRect = Math.min(edRect, g.rects.size() - 1);
    int[] r = g.rects.isEmpty() ? null : g.rects.get(edRect);
    switch (keycode) {
      // arrows MOVE the box, with shift they RESIZE it: the two things you do to a
      // selection, on the same keys, without moving your hand
      case com.badlogic.gdx.Input.Keys.LEFT -> {
        if (r != null) {
          if (shift)
            r[2] = Math.max(1, r[2] - step);
          else
            r[0] = Math.max(0, r[0] - step);
        }
      }
      case com.badlogic.gdx.Input.Keys.RIGHT -> {
        if (r != null) {
          if (shift)
            r[2] = Math.min(256 - r[0], r[2] + step);
          else
            r[0] = Math.min(256 - r[2], r[0] + step);
        }
      }
      case com.badlogic.gdx.Input.Keys.UP -> {
        if (r != null) {
          if (shift)
            r[3] = Math.max(1, r[3] - step);
          else
            r[1] = Math.max(0, r[1] - step);
        }
      }
      case com.badlogic.gdx.Input.Keys.DOWN -> {
        if (r != null) {
          if (shift)
            r[3] = Math.min(H - r[1], r[3] + step);
          else
            r[1] = Math.min(H - r[3], r[1] + step);
        }
      }
      // TAB walks the boxes of THIS object, PgUp/PgDn walk the objects: two levels, two
      // keys, the same shape as the report's "objects, and the graphics inside each"
      case com.badlogic.gdx.Input.Keys.TAB -> {
        if (!g.rects.isEmpty())
          edRect = (edRect + (shift ? g.rects.size() - 1 : 1)) % g.rects.size();
      }
      case com.badlogic.gdx.Input.Keys.PAGE_DOWN -> cycleGroup(1);
      case com.badlogic.gdx.Input.Keys.PAGE_UP -> cycleGroup(-1);
      case com.badlogic.gdx.Input.Keys.A -> { // one more box for this object
        int[] base = r == null ? new int[]{112, 88, 16, 16} : r;
        g.rects.add(new int[]{Math.min(240, base[0] + 8), Math.min(H - 8, base[1] + 8),
            base[2], base[3]});
        edRect = g.rects.size() - 1;
      }
      case com.badlogic.gdx.Input.Keys.D -> {
        if (!g.rects.isEmpty()) {
          g.rects.remove(edRect);
          edRect = Math.max(0, Math.min(edRect, g.rects.size() - 1));
        }
      }
      case com.badlogic.gdx.Input.Keys.C -> { // a new object, empty
        EdGroup n = new EdGroup();
        n.name = "objeto" + (edGroups.size() + 1);
        n.rects.add(new int[]{112, 88, 16, 16});
        edGroups.add(n);
        edGroup = edGroups.size() - 1;
        edRect = 0;
      }
      case com.badlogic.gdx.Input.Keys.X -> { // drop the whole object
        if (!edGroups.isEmpty()) {
          disposeThumb(edGroups.remove(edGroup));
          edGroup = Math.max(0, edGroup - 1);
          edRect = 0;
        }
      }
      // how THIS object renders: the whole point of naming it by hand is being able to say
      // "this one is a sphere" instead of hoping a rule guesses it
      case com.badlogic.gdx.Input.Keys.R -> {
        if (g.render == null)
          g.render = viewerDefaults().copy();
        Sprite3DConfig.Technique[] ts = Sprite3DConfig.Technique.values();
        g.render.technique = ts[(g.render.technique.ordinal() + 1) % ts.length];
      }
      case com.badlogic.gdx.Input.Keys.P -> {
        if (g.render == null)
          g.render = viewerDefaults().copy();
        Sprite3DConfig.Primitive[] ps = Sprite3DConfig.Primitive.values();
        g.render.primitive = ps[(g.render.primitive.ordinal() + 1) % ps.length];
        g.render.technique = Sprite3DConfig.Technique.PRIMITIVE;
      }
      case com.badlogic.gdx.Input.Keys.O -> {
        if (g.render != null)
          g.render.roundness = g.render.roundness >= .95f ? .2f : g.render.roundness + .15f;
      }
      case com.badlogic.gdx.Input.Keys.ENTER -> findGroupOnScreen();
      case com.badlogic.gdx.Input.Keys.G -> {
        saveGroups();
        gfxToObj.clear(); // the definitions changed: the runtime map is rebuilt from them
      }
      default -> {
        return false;
      }
    }
    flashEditor();
    return true;
  }

  private void cycleGroup(int d) {
    if (edGroups.isEmpty())
      return;
    edGroup = ((edGroup + d) % edGroups.size() + edGroups.size()) % edGroups.size();
    edRect = 0;
    // stepping onto an object also LOOKS for it: its boxes were drawn on another screen and
    // mean nothing here until they are put back where the thing actually is now
    findGroupOnScreen();
  }

  private void flashEditor() {
    EdGroup g = group();
    int[] r = g.rects.isEmpty() ? new int[]{0, 0, 0, 0} : g.rects.get(edRect);
    flashPreset(g.name + " (" + (edGroup + 1) + "/" + edGroups.size() + ")  ·  rect "
        + (edRect + 1) + "/" + g.rects.size() + " x=" + r[0] + " y=" + r[1] + " " + r[2] + "x"
        + r[3] + "  ·  " + graphicsInRects(g).size() + " gráficos  ·  render: "
        + (g.render == null ? "default" : g.render.technique + "/" + g.render.primitive + " "
            + String.format("%.2f", g.render.roundness)));
  }

  /**
   * The catalogue entries whose LIT pixels fall inside the object's boxes. This is what a
   * definition is made of: the boxes are how you point at the thing, but a rectangle on the
   * screen is worth nothing later — the object moves, animates, and the next room draws
   * something else there. What survives is which graphics it is made of.
   */
  private java.util.Set<Integer> graphicsInRects(EdGroup g) {
    java.util.Set<Integer> out = new java.util.LinkedHashSet<>();
    if (lastSnap == null)
      return out;
    for (int[] r : g.rects)
      for (int y = r[1]; y < Math.min(H, r[1] + r[3]); y++)
        for (int x = r[0]; x < Math.min(256, r[0] + r[2]); x++) {
          int i = idx(y, x >> 3);
          if ((lastSnap.pixels()[i] & (0x80 >> (x & 7))) == 0)
            continue; // only what is LIT inside the box: the paper around it is not the object
          int gfx = originOf(i);
          if (gfx >= 0)
            out.add(gfx);
        }
    return out;
  }

  /**
   * Put the object's boxes back on top of the object, wherever it is NOW. A definition is a
   * set of graphics, so finding it is looking for those graphics on the screen: the boxes
   * are then translated by the difference between where they were drawn and where the thing
   * turned out to be. Partial matches count — an object is routinely half-covered by another
   * — so it reports the fraction found instead of demanding all of it.
   */
  private void findGroupOnScreen() {
    EdGroup g = group();
    if (lastSnap == null || g.graphics.isEmpty())
      return;
    int minX = 256, minY = H, maxX = -1, maxY = -1;
    java.util.Set<Integer> seen = new java.util.LinkedHashSet<>();
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      int gfx = originOf(i);
      if (gfx < 0 || !g.graphics.contains(gfx))
        continue;
      seen.add(gfx);
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), col = i & 31;
      minX = Math.min(minX, col * 8);
      maxX = Math.max(maxX, col * 8 + 7);
      minY = Math.min(minY, y);
      maxY = Math.max(maxY, y);
    }
    if (maxX < 0 || seen.size() * 2 < g.graphics.size()) {
      flashPreset(g.name + ": no está en esta pantalla (" + seen.size() + "/"
          + g.graphics.size() + " gráficos)");
      return;
    }
    int oldX = 256, oldY = H;
    for (int[] r : g.rects) {
      oldX = Math.min(oldX, r[0]);
      oldY = Math.min(oldY, r[1]);
    }
    int dx = minX - oldX, dy = minY - oldY;
    for (int[] r : g.rects) {
      r[0] = Math.max(0, Math.min(256 - r[2], r[0] + dx));
      r[1] = Math.max(0, Math.min(H - r[3], r[1] + dy));
    }
    flashPreset(g.name + ": encontrado (" + seen.size() + "/" + g.graphics.size()
        + " gráficos), cajas reubicadas");
  }

  /**
   * Captures what is under the boxes: the graphics (the definition) and a picture of the
   * selection (for the list on the right). Both are taken NOW, from this screen, because
   * that is the moment the person is looking at the object and says "this is it".
   */
  private void captureGroup(EdGroup g) {
    g.graphics.clear();
    g.graphics.addAll(graphicsInRects(g));
    int minX = 256, minY = H, maxX = 0, maxY = 0;
    for (int[] r : g.rects) {
      minX = Math.min(minX, r[0]);
      minY = Math.min(minY, r[1]);
      maxX = Math.max(maxX, Math.min(256, r[0] + r[2]));
      maxY = Math.max(maxY, Math.min(H, r[1] + r[3]));
    }
    if (maxX <= minX || maxY <= minY || lastSnap == null)
      return;
    int gw = maxX - minX, gh = maxY - minY;
    Pixmap pm = new Pixmap(gw, gh, Pixmap.Format.RGBA8888);
    pm.setColor(0, 0, 0, 0);
    pm.fill();
    g.px = new int[gw * gh];
    g.owner = new int[gw * gh];
    java.util.Arrays.fill(g.owner, -1);
    for (int[] r : g.rects)
      for (int y = r[1]; y < Math.min(H, r[1] + r[3]); y++)
        for (int x = r[0]; x < Math.min(256, r[0] + r[2]); x++) {
          int i = idx(y, x >> 3);
          if ((lastSnap.pixels()[i] & (0x80 >> (x & 7))) == 0)
            continue;
          int attr = lastSnap.attrs()[(y >> 3) * 32 + (x >> 3)] & 0xff;
          Color c = PALETTE[ink(attr)];
          pm.drawPixel(x - minX, y - minY, Color.rgba8888(c.r, c.g, c.b, 1));
          int at = (y - minY) * gw + (x - minX);
          g.px[at] = 0xff000000 | ((int) (c.r * 255) << 16) | ((int) (c.g * 255) << 8)
              | (int) (c.b * 255);
          g.owner[at] = originOf(i);
        }
    disposeThumb(g);
    g.thumb = new com.badlogic.gdx.graphics.Texture(pm);
    g.tw = gw;
    g.th = gh;
    pm.dispose();
  }

  private void disposeThumb(EdGroup g) {
    if (g.thumb != null) {
      g.thumb.dispose();
      g.thumb = null;
    }
  }

  /** every object marked by hand, per game, in {@code ~/.jsw3d-objetos-<juego>.json}. */
  private void saveGroups() {
    captureGroup(group());
    try {
      writeSheet(); // first: it assigns each object its box in the sheet, which the json cites
    } catch (Exception e) {
      flashPreset("no pude escribir la hoja: " + e);
    }
    StringBuilder sb = new StringBuilder("{\n  \"objetos\": [\n");
    for (int n = 0; n < edGroups.size(); n++) {
      EdGroup g = edGroups.get(n);
      StringBuilder piezas = new StringBuilder(), rects = new StringBuilder();
      for (int gfx : g.graphics)
        piezas.append(piezas.length() > 0 ? "," : "").append('$')
            .append(Integer.toHexString(gfx));
      for (int[] r : g.rects)
        rects.append(rects.length() > 0 ? "," : "").append(r[0]).append(' ').append(r[1])
            .append(' ').append(r[2]).append(' ').append(r[3]);
      String hoja = g.sheetBox == null ? "" : g.sheetBox[0] + " " + g.sheetBox[1] + " "
          + g.sheetBox[2] + " " + g.sheetBox[3];
      String render = g.render == null ? ""
          : g.render.technique + " " + g.render.primitive + " " + g.render.roundness;
      sb.append("    {\"nombre\": \"").append(g.name).append("\", \"piezas\": \"")
          .append(piezas).append("\", \"rects\": \"").append(rects)
          .append("\", \"hoja\": \"").append(hoja)
          .append("\", \"render\": \"").append(render).append("\"}")
          .append(n < edGroups.size() - 1 ? "," : "").append('\n');
    }
    sb.append("  ]\n}\n");
    try {
      java.nio.file.Files.writeString(objectsPath(), sb.toString());
      flashPreset("grabado: " + edGroups.size() + " objetos en " + objectsPath()
          + " y en " + sheetPath());
    } catch (Exception e) {
      flashPreset("no pude grabar: " + e);
    }
  }

  /**
   * The objects as a PNG that is also their definition: every pixel carries, invisibly, the
   * address of the graphic it came from ({@link ObjectSheet}). A picture plus a table beside
   * it drift apart the first time somebody edits one; here they cannot.
   */
  private void writeSheet() throws Exception {
    List<String> names = new ArrayList<>();
    List<int[]> cells = new ArrayList<>(), pixels = new ArrayList<>(), owners = new ArrayList<>();
    int x = 0, y = 1, shelf = 0; // row 0 belongs to the legend
    for (EdGroup g : edGroups) {
      if (g.px == null)
        continue;
      int gw = g.tw, gh = g.th;
      if (x + gw > 512 && x > 0) {
        x = 0;
        y += shelf + 2;
        shelf = 0;
      }
      names.add(g.name);
      g.sheetBox = new int[]{x, y, gw, gh};
      cells.add(new int[]{x, y, gw, gh});
      pixels.add(g.px);
      owners.add(g.owner);
      x += gw + 2;
      shelf = Math.max(shelf, gh);
    }
    if (!cells.isEmpty())
      ObjectSheet.write(sheetPath().toString(), names, cells, pixels, owners);
  }

  private java.nio.file.Path sheetPath() {
    return java.nio.file.Path.of("doc", "objetos-" + activeGame + ".png");
  }

  /**
   * Beside the catalogue and not in the home directory: what was identified by hand is the
   * project's knowledge, the kind of thing that belongs in the repo and in a diff, not live
   * state of one machine. The old location still loads, so nothing marked before is lost.
   */
  private java.nio.file.Path objectsPath() {
    java.nio.file.Path doc = java.nio.file.Path.of("doc", "objetos-" + activeGame + ".json");
    java.nio.file.Path home = java.nio.file.Path.of(System.getProperty("user.home"),
        ".jsw3d-objetos-" + activeGame + ".json");
    return java.nio.file.Files.exists(doc) || !java.nio.file.Files.exists(home) ? doc : home;
  }

  /** what was marked in earlier sessions, so the list starts where you left it. */
  private void loadGroups() {
    try {
      if (!java.nio.file.Files.exists(objectsPath()))
        return;
      com.badlogic.gdx.utils.JsonValue v = new com.badlogic.gdx.utils.JsonReader()
          .parse(java.nio.file.Files.readString(objectsPath()));
      for (com.badlogic.gdx.utils.JsonValue o = v.get("objetos") == null ? null
          : v.get("objetos").child; o != null; o = o.next) {
        EdGroup g = new EdGroup();
        g.name = o.getString("nombre", "objeto" + (edGroups.size() + 1));
        for (String p : o.getString("piezas", "").split(","))
          if (!p.isBlank())
            g.graphics.add(Integer.parseInt(p.trim().replace("$", ""), 16));
        for (String rc : o.getString("rects", "").split(","))
          if (!rc.isBlank()) {
            String[] f = rc.trim().split("\\s+");
            if (f.length == 4)
              g.rects.add(new int[]{Integer.parseInt(f[0]), Integer.parseInt(f[1]),
                  Integer.parseInt(f[2]), Integer.parseInt(f[3])});
          }
        if (g.rects.isEmpty())
          g.rects.add(new int[]{112, 88, 16, 16});
        String[] rend = o.getString("render", "").trim().split("\\s+");
        if (rend.length == 3)
          try {
            g.render = viewerDefaults().copy();
            g.render.technique = Sprite3DConfig.Technique.valueOf(rend[0]);
            g.render.primitive = Sprite3DConfig.Primitive.valueOf(rend[1]);
            g.render.roundness = Float.parseFloat(rend[2]);
          } catch (Exception ignored) {
            g.render = null; // an unknown technique must not take the object down with it
          }
        String[] hoja = o.getString("hoja", "").trim().split("\\s+");
        if (hoja.length == 4)
          g.sheetBox = new int[]{Integer.parseInt(hoja[0]), Integer.parseInt(hoja[1]),
              Integer.parseInt(hoja[2]), Integer.parseInt(hoja[3])};
        edGroups.add(g);
      }
      loadSheet();
      if (!edGroups.isEmpty())
        System.out.println("editor: " + edGroups.size() + " objetos cargados de "
            + objectsPath());
    } catch (Exception e) {
      System.out.println("editor: no pude leer " + objectsPath() + ": " + e);
    }
  }

  /**
   * Reads the sheet back into the objects: their picture (so the list shows what you marked,
   * not just names) and their per-pixel graphic. Continuing where you left off is the whole
   * point of writing the file — a session that starts blank makes you re-mark everything.
   *
   * <p>The pixels come from the PNG itself, addresses included ({@link ObjectSheet#read}),
   * so a re-save keeps every object intact even if you only touched one of them.
   */
  private void loadSheet() {
    try {
      if (!java.nio.file.Files.exists(sheetPath()))
        return;
      java.awt.image.BufferedImage img =
          javax.imageio.ImageIO.read(sheetPath().toFile());
      int[][] addr = ObjectSheet.read(sheetPath().toString());
      for (EdGroup g : edGroups) {
        int[] b = g.sheetBox;
        if (b == null || b[0] + b[2] > img.getWidth() || b[1] + b[3] > img.getHeight())
          continue;
        g.px = new int[b[2] * b[3]];
        g.owner = new int[b[2] * b[3]];
        Pixmap pm = new Pixmap(b[2], b[3], Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        for (int y = 0; y < b[3]; y++)
          for (int x = 0; x < b[2]; x++) {
            int rgb = img.getRGB(b[0] + x, b[1] + y) & 0xffffff;
            int a = addr[b[1] + y][b[0] + x];
            g.owner[y * b[2] + x] = a;
            // black with no graphic behind it is the gap between objects, not the object
            boolean solid = a >= 0 || rgb > 0x0c0c0c;
            g.px[y * b[2] + x] = solid ? 0xff000000 | rgb : 0;
            if (solid)
              pm.drawPixel(x, y, (rgb << 8) | 0xff);
          }
        disposeThumb(g);
        g.thumb = new com.badlogic.gdx.graphics.Texture(pm);
        g.tw = b[2];
        g.th = b[3];
        pm.dispose();
      }
    } catch (Exception e) {
      System.out.println("editor: no pude leer " + sheetPath() + ": " + e);
    }
  }

  /**
   * The editor's overlay, painted INTO the 2D screen image so it lands exactly on the game's
   * own pixels: every box of the object being edited, the selected one pulsing gold and the
   * rest in amber, with the lit pixels inside them tinted so you see exactly what you
   * enclosed. Colours the Spectrum CANNOT produce — white and cyan are the game's own ink
   * and the highlight vanished into it.
   */
  private void paintEditor(TaintReplay.FrameSnapshot snap, Pixmap pixmap) {
    if (edGroups.isEmpty())
      return;
    float t = (System.currentTimeMillis() % 700) / 700f;
    float beat = .55f + .45f * (float) Math.sin(t * (float) Math.PI * 2);
    int gold = Color.rgba8888(1, .2f + .78f * beat, 0, 1);
    int goldEdge = Color.rgba8888(1, .75f, .1f, 1);
    int amber = Color.rgba8888(.95f, .45f, .05f, 1);
    int amberEdge = Color.rgba8888(.6f, .3f, 0, 1);
    List<int[]> rects = edGroups.get(edGroup).rects;
    for (int n = 0; n < rects.size(); n++) {
      int[] r = rects.get(n);
      boolean sel = n == edRect;
      int fill = sel ? gold : amber, edge = sel ? goldEdge : amberEdge;
      int x1 = Math.min(255, r[0] + r[2] - 1), y1 = Math.min(H - 1, r[1] + r[3] - 1);
      for (int y = r[1]; y <= y1; y++)
        for (int x = r[0]; x <= x1; x++) {
          int i = idx(y, x >> 3);
          if ((snap.pixels()[i] & (0x80 >> (x & 7))) != 0)
            pixmap.drawPixel(x, y, fill);
        }
      // the frame goes just outside the box, so it never eats a pixel of the object
      for (int x = Math.max(0, r[0] - 1); x <= Math.min(255, x1 + 1); x++) {
        if (r[1] > 0)
          pixmap.drawPixel(x, r[1] - 1, edge);
        if (y1 < H - 1)
          pixmap.drawPixel(x, y1 + 1, edge);
      }
      for (int y = Math.max(0, r[1] - 1); y <= Math.min(H - 1, y1 + 1); y++) {
        if (r[0] > 0)
          pixmap.drawPixel(r[0] - 1, y, edge);
        if (x1 < 255)
          pixmap.drawPixel(x1 + 1, y, edge);
      }
    }
  }

  /**
   * The objects marked so far, drawn down the right edge as the pictures the person
   * selected. A list of names would be useless: what you need to know at a glance is whether
   * object 3 is the capsule or the capsule plus half a rocket, and that is a picture.
   *
   * <p>A panel and not a second OS window on purpose: libGDX would need a second window with
   * its own GL context and its own texture uploads, and the thing being asked for is "see
   * them while I work", which a strip on the side does with none of that.
   */
  private void renderEditorList() {
    if (!editorOn || edGroups.isEmpty())
      return;
    float pad = 8, w = 150, x = Gdx.graphics.getWidth() - w - pad, y = Gdx.graphics.getHeight() - pad;
    uiBatch.begin();
    uiBatch.setColor(0, 0, 0, .72f);
    uiBatch.draw(whitePix, x - pad, 0, w + pad * 2, Gdx.graphics.getHeight());
    for (int n = 0; n < edGroups.size(); n++) {
      EdGroup g = edGroups.get(n);
      float hh = g.thumb == null ? 14 : Math.min(64, g.th * Math.min(3f, w / Math.max(1, g.tw)));
      float ww = g.thumb == null ? 0 : g.tw * (hh / Math.max(1, g.th));
      y -= hh + 16;
      if (y < 0)
        break;
      boolean sel = n == edGroup;
      uiBatch.setColor(sel ? 1 : .35f, sel ? .7f : .25f, 0, sel ? .9f : .5f);
      uiBatch.draw(whitePix, x - 2, y - 2, w + 4, hh + 16);
      uiBatch.setColor(1, 1, 1, 1);
      if (g.thumb != null)
        uiBatch.draw(g.thumb, x, y + 12, ww, hh);
      uiFont.draw(uiBatch, (n + 1) + ". " + g.name + " (" + g.graphics.size() + ")", x, y + 12);
    }
    uiBatch.end();
  }

  /** the attribute's ink index into {@link #PALETTE} (bright included). */
  private static int ink(int attr) {
    return (attr & 7) | ((attr >> 3) & 8);
  }

  /** a cell rendered like a small mobile sprite: inflated from its pixels, mid-depth. */
  private boolean floatCell(int col, int y0, byte[] bmp, int attr) {
    Model m = sprite3d.model(SpriteBitmap.ofScreen(bmp.clone(), 1, -1), viewerDefaults());
    if (m == null)
      return false;
    ModelInstance inst = new ModelInstance(m);
    inst.transform.setToTranslation(col * 8 + 4, H - (y0 + 4), midZ());
    inst.materials.first().set(ColorAttribute.createDiffuse(
        paintRoles ? Color.YELLOW : PALETTE[(attr & 7) | ((attr >> 3) & 8)]));
    wetten(inst.materials.first());
    tileInstances.add(inst);
    return true;
  }

  /** the screen-mode ghost: the cell's cached clean pixels, rebuilt see-through. */
  private boolean ghostScreenCell(int col, int y0, int cell) {
    Model model = pixSlab(cellBmp[cell], slabDepth() * reliefDepth);
    if (model == null)
      return false;
    ModelInstance inst = new ModelInstance(model);
    inst.transform.setToTranslation(col * 8 + 4, H - (y0 + 4), midZ());
    int attr = cellAttr[cell];
    Material mi = inst.getMaterial(TileSlabBuilder.INK);
    mi.set(ColorAttribute.createDiffuse(paintRoles ? Color.BLUE
        : PALETTE[(attr & 7) | ((attr >> 3) & 8)]));
    mi.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(true, ghostAlpha));
    Material paper = inst.getMaterial(TileSlabBuilder.PAPER);
    if (paper != null) {
      paper.set(ColorAttribute.createDiffuse(paintRoles ? Color.NAVY
          : PALETTE[((attr >> 3) & 7) | ((attr >> 3) & 8)]));
      paper.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(true, ghostAlpha));
    }
    Material side = inst.getMaterial(TileSlabBuilder.PAPER_SIDE);
    if (side != null) {
      side.set(ColorAttribute.createDiffuse(paintRoles ? Color.NAVY : slabPaper(attr)));
      side.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(true, ghostAlpha));
    }
    tileInstances.add(inst);
    return true;
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
    Model model = item
        ? sprite3d.model(SpriteBitmap.ofMemory(leaf, 8, 1, replay::memByte), viewerDefaults())
        : modelCache.computeIfAbsent(-leaf,
            k -> TileSlabBuilder.build(leaf, replay::memByte, slabDepth(), tStride));
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
    for (int cellY = Math.min(24, playfieldTop); cellY < playEnd(); cellY++)
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
                  && itemsOn
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
        // an ITEM is a graphic, not architecture, so it goes through Sprite3D like any
        // sprite. It used to call the builders straight from here, which is why one object
        // in a room could stay flat while every real sprite around it got the oval.
        Model model = item
            ? sprite3d.model(SpriteBitmap.ofMemory(leaf, 8, 1, replay::memByte), viewerDefaults())
            : modelCache.computeIfAbsent(-leaf,
                k -> TileSlabBuilder.build(leaf, replay::memByte, slabDepth(), tStride));
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
    sprite3d.dispose();
    junkModels.values().forEach(Model::dispose);
    if (lampModel != null)
      lampModel.dispose();
    if (ropePixelModel != null)
      ropePixelModel.dispose();
    shadowLight.dispose();
    if (replay != null)
      replay.paused = false; // never leave the replay thread parked on the way out
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
    activeProfile = profile;
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
