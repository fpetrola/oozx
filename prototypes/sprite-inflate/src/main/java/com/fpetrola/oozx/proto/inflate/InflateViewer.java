package com.fpetrola.oozx.proto.inflate;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.ScreenUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The sprites, inflated, with all of them down the side to pick from.
 * <p>
 * Given a sheet it cuts it up, shows every sprite on it in a column, and inflates whichever one is
 * clicked, there and then - which is the only way to find out that a method works on a person and
 * falls apart on a spinning coin. Given a directory it reads the OBJ files
 * {@link SpriteInflate} wrote, which is for comparing profiles on one figure.
 * <p>
 * Inflating as you go is affordable because none of it is expensive at a sprite's size: sixteen
 * pixels at four times over is a few thousand triangles and a few milliseconds, so a click can
 * rebuild the solid rather than fetch a prepared one.
 */
public class InflateViewer extends ApplicationAdapter {

  /** The Spectrum's own fifteen, which is what these sprites were drawn to be shown in. */
  private static final int[] PALETTE = {
      0x0000D7, 0xD70000, 0xD700D7, 0x00D700, 0x00D7D7, 0xD7D700, 0xD7D7D7,
      0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};

  private static final int PANEL = 250;
  private static final int THUMB = 44;
  private static final int GAP = 6;
  private static final int SWATCH = 22;

  private final String path;

  /** Sheet mode. */
  private List<BufferedImage> sprites;
  private Texture atlas;
  private final List<TextureRegion> thumbnails = new ArrayList<>();
  private int chosenSprite;
  private SpriteInflate.Profile profile = SpriteInflate.Profile.SPHERE_LOCAL;
  /** -1 means the sprite's own colours, anything else an index into {@link #PALETTE}. */
  private int chosenColour = -1;
  private float scroll;

  /** Directory mode. */
  private final List<FileHandle> files = new ArrayList<>();
  private final List<String> names = new ArrayList<>();
  private int chosenFile;

  private PerspectiveCamera camera;
  private CameraInputController controller;
  private ModelBatch batch;
  private Environment environment;
  private SpriteBatch overlay;
  private BitmapFont font;
  private Texture block;

  private Model model;
  private ModelInstance instance;
  private String caption = "";
  private boolean turning = true;
  private float angle;

  InflateViewer(String path) {
    this.path = path;
  }

  @Override
  public void create() {
    camera = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.near = 0.1f;
    camera.far = 1000f;
    controller = new CameraInputController(camera);

    environment = new Environment();
    environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.35f, 0.35f, 0.4f, 1f));
    // Two lights and not one: a single lamp leaves the whole shaded side flat black, and the
    // shaded side is half of what there is to judge.
    environment.add(new DirectionalLight().set(0.9f, 0.88f, 0.82f, -0.6f, -0.7f, -0.5f));
    environment.add(new DirectionalLight().set(0.25f, 0.28f, 0.35f, 0.7f, 0.2f, 0.4f));

    batch = new ModelBatch();
    overlay = new SpriteBatch();
    font = new BitmapFont();
    block = white();

    File given = new File(path);
    if (given.isFile() && given.getName().toLowerCase().endsWith(".png")) {
      try {
        sprites = SpriteSheet.slice(given);
      } catch (Exception e) {
        throw new IllegalStateException("could not read the sheet " + given + ": " + e, e);
      }
      buildThumbnails();
      showSprite();
    } else {
      File[] found = given.listFiles((dir, name) -> name.endsWith(".obj"));
      if (found == null || found.length == 0) {
        // Said in full, because the last time this was wrong it printed a directory name and left
        // whoever ran it to guess which of the two things it wanted.
        throw new IllegalStateException("nothing to show. " + given.getAbsolutePath()
            + " is neither a sheet of sprites (a .png) nor a directory holding the .obj files "
            + "SpriteInflate writes. Try: -Dexec.args=\"sprites/img.png\", or run SpriteInflate "
            + "first and pass its output directory.");
      }
      java.util.Arrays.sort(found);
      for (File one : found) {
        files.add(new FileHandle(one));
        names.add(one.getName().replaceAll("^\\d+-|\\.obj$", ""));
      }
      showFile(0);
    }

    // Ours first, so a click on the side panel picks a sprite instead of spinning the camera.
    InputMultiplexer input = new InputMultiplexer();
    input.addProcessor(new Picking());
    input.addProcessor(controller);
    Gdx.input.setInputProcessor(input);
  }

  private static Texture white() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }

  // ------------------------------------------------------------------------------- the panel

  /**
   * All the sprites on one texture rather than one texture each.
   * <p>
   * A hundred and fifty small textures would work and would also mean a hundred and fifty
   * bindings every frame, which is the one thing a panel like this can do wrong.
   */
  private void buildThumbnails() {
    int side = sprites.get(0).getWidth(), tall = sprites.get(0).getHeight();
    int across = (int) Math.ceil(Math.sqrt(sprites.size()));
    int down = (sprites.size() + across - 1) / across;
    Pixmap sheet = new Pixmap(across * side, down * tall, Pixmap.Format.RGBA8888);
    for (int i = 0; i < sprites.size(); i++) {
      BufferedImage sprite = sprites.get(i);
      int x0 = (i % across) * side, y0 = (i / across) * tall;
      for (int y = 0; y < tall; y++) {
        for (int x = 0; x < side; x++) {
          int argb = sprite.getRGB(x, y);
          sheet.drawPixel(x0 + x, y0 + y, (argb << 8) | (argb >>> 24));
        }
      }
    }
    atlas = new Texture(sheet);
    sheet.dispose();
    for (int i = 0; i < sprites.size(); i++) {
      thumbnails.add(new TextureRegion(atlas, (i % across) * side, (i / across) * tall, side, tall));
    }
  }

  private int columns() {
    return Math.max(1, (PANEL - GAP) / (THUMB + GAP));
  }

  /** How tall the swatches and their label are, measured from the bottom of the window. */
  private int swatchBand() {
    int rows = (PALETTE.length + 1 + columns() * 2 - 1) / (columns() * 2);
    return 30 + rows * (SWATCH + GAP);
  }

  /** Where a thumbnail sits, in libGDX's coordinates - origin bottom left. */
  private float thumbTop(int index) {
    return Gdx.graphics.getHeight() - GAP - (index / columns()) * (THUMB + GAP) + scroll;
  }

  private float thumbLeft(int index) {
    return GAP + (index % columns()) * (THUMB + GAP);
  }

  private float scrollLimit() {
    int rows = (sprites.size() + columns() - 1) / columns();
    return Math.max(0, rows * (THUMB + GAP) + GAP
        - (Gdx.graphics.getHeight() - swatchBand()));
  }

  /** The swatches along the bottom: the sprite's own colours first, then the Spectrum's. */
  private float swatchLeft(int index) {
    return GAP + (index % (columns() * 2)) * (SWATCH + GAP);
  }

  private float swatchBottom(int index) {
    int rows = (PALETTE.length + 1 + columns() * 2 - 1) / (columns() * 2);
    return GAP + (rows - 1 - index / (columns() * 2)) * (SWATCH + GAP);
  }

  // ------------------------------------------------------------------------------- sheet mode

  private void showSprite() {
    BufferedImage sprite = sprites.get(chosenSprite);
    SpriteInflate.Fields fields = SpriteInflate.measure(sprite, new int[]{4});
    List<double[]> triangles = SpriteInflate.inflate(fields, profile);
    put(build(triangles, fields));
    caption = String.format("sprite %d of %d   %s   %d triangles",
        chosenSprite + 1, sprites.size(), profile.label, triangles.size());
  }

  /**
   * The triangles as something libGDX can draw, welded, with the averaged normal at each vertex
   * and a colour: the sprite's own where it has one, or the chosen one everywhere.
   */
  private Model build(List<double[]> triangles, SpriteInflate.Fields fields) {
    Map<String, double[]> normals = SpriteInflate.smoothNormals(triangles);
    ModelBuilder builder = new ModelBuilder();
    builder.begin();
    MeshPartBuilder part = builder.part("sprite", GL20.GL_TRIANGLES,
        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
            | VertexAttributes.Usage.ColorUnpacked, new Material());
    Map<String, Short> numbered = new HashMap<>();
    MeshPartBuilder.VertexInfo vertex = new MeshPartBuilder.VertexInfo();
    short[] corner = new short[3];
    for (double[] triangle : triangles) {
      for (int i = 0; i < 3; i++) {
        double x = triangle[i * 3], y = triangle[i * 3 + 1], z = triangle[i * 3 + 2];
        String at = SpriteInflate.key(x, y, z);
        Short already = numbered.get(at);
        if (already != null) {
          corner[i] = already;
          continue;
        }
        double[] normal = normals.get(at);
        // Even with a colour chosen, the detail keeps its own darker shade - a guardian in
        // solid yellow with no eye is a worse picture than a yellow one with an eye.
        int rgb = chosenColour < 0 ? SpriteInflate.colourFor(fields, x, y)
            : fields.detail()[Math.min(fields.height() - 1, Math.max(0, (int) Math.round(y)))
            * fields.width() + Math.min(fields.width() - 1, Math.max(0, (int) Math.round(x)))]
            ? shade(PALETTE[chosenColour]) : PALETTE[chosenColour];
        // Y flipped, because an image counts rows downwards and the world counts them up.
        vertex.setPos((float) x, (float) -y, (float) z)
            .setNor((float) normal[0], (float) -normal[1], (float) normal[2])
            .setCol(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f, 1f);
        corner[i] = part.vertex(vertex);
        numbered.put(at, corner[i]);
      }
      part.index(corner[0], corner[1], corner[2]);
    }
    return builder.end();
  }

  private static int shade(int rgb) {
    return ((int) (((rgb >> 16) & 0xFF) * 0.18) << 16)
        | ((int) (((rgb >> 8) & 0xFF) * 0.18) << 8) | (int) ((rgb & 0xFF) * 0.18);
  }

  // --------------------------------------------------------------------------- directory mode

  private void showFile(int which) {
    chosenFile = which;
    Model loaded = new ObjLoader().loadModel(files.get(which));
    for (Material material : loaded.materials) {
      material.set(ColorAttribute.createDiffuse(new Color(0.85f, 0.85f, 0.88f, 1f)));
    }
    put(loaded);
    caption = names.get(which);
  }

  // -------------------------------------------------------------------------------------------

  /** Puts a model in front of the camera at a size that does not change as models change. */
  private void put(Model built) {
    if (model != null) {
      model.dispose();
    }
    model = built;
    instance = new ModelInstance(model);
    BoundingBox box = new BoundingBox();
    instance.calculateBoundingBox(box);
    Vector3 centre = new Vector3();
    box.getCenter(centre);
    instance.transform.idt().translate(-centre.x, -centre.y, -centre.z);
    float span = Math.max(box.getWidth(), Math.max(box.getHeight(), box.getDepth()));
    camera.position.set(0, 0, span * 2.2f);
    camera.lookAt(0, 0, 0);
    camera.update();
    controller.target.set(0, 0, 0);
  }

  @Override
  public void render() {
    keys();

    if (turning) {
      angle += Gdx.graphics.getDeltaTime() * 45f;
      BoundingBox box = new BoundingBox();
      instance.calculateBoundingBox(box);
      Vector3 centre = new Vector3();
      box.getCenter(centre);
      instance.transform.idt().rotate(Vector3.Y, angle).translate(-centre.x, -centre.y, -centre.z);
    }

    ScreenUtils.clear(0.06f, 0.06f, 0.08f, 1f, true);
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

    // The figure lives to the right of the panel, so it is centred in what is left rather than
    // in the window - otherwise it sits half behind the sprites it is being chosen from.
    int left = sprites == null ? 0 : PANEL;
    int width = Gdx.graphics.getWidth() - left, height = Gdx.graphics.getHeight();
    Gdx.gl.glViewport(left, 0, width, height);
    camera.viewportWidth = width;
    camera.viewportHeight = height;
    camera.update();
    controller.update();
    batch.begin(camera);
    batch.render(instance, environment);
    batch.end();
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), height);

    overlay.begin();
    if (sprites != null) {
      panel();
    }
    font.setColor(Color.WHITE);
    font.draw(overlay, caption, left + 12, height - 12);
    font.draw(overlay, sprites != null
        ? "click a sprite or a colour   arrows = sprite   wheel = scroll   1-4 = profile   "
        + "space = turn   drag = orbit"
        : "1-" + files.size() + " = profile   space = turn   drag = orbit   esc = quit",
        left + 12, 22);
    overlay.end();
  }

  private void panel() {
    int height = Gdx.graphics.getHeight();
    int band = swatchBand();
    overlay.setColor(0.10f, 0.10f, 0.13f, 1f);
    overlay.draw(block, 0, 0, PANEL, height);

    for (int i = 0; i < thumbnails.size(); i++) {
      float top = thumbTop(i), bottom = top - THUMB;
      if (top < band || bottom > height) {
        continue;                       // scrolled out of sight, or behind the swatches
      }
      float x = thumbLeft(i);
      overlay.setColor(i == chosenSprite ? new Color(0.30f, 0.45f, 0.75f, 1f)
          : new Color(0.16f, 0.16f, 0.20f, 1f));
      overlay.draw(block, x - 2, bottom - 2, THUMB + 4, THUMB + 4);
      overlay.setColor(Color.WHITE);
      overlay.draw(thumbnails.get(i), x, bottom, THUMB, THUMB);
    }

    // The swatches sit on their own strip, drawn after and over the thumbnails, so scrolling the
    // list does not carry the colours off the bottom of the window with it.
    overlay.setColor(0.07f, 0.07f, 0.09f, 1f);
    overlay.draw(block, 0, 0, PANEL, band);
    for (int i = 0; i <= PALETTE.length; i++) {
      float x = swatchLeft(i), y = swatchBottom(i);
      boolean picked = chosenColour == i - 1;
      overlay.setColor(picked ? Color.WHITE : new Color(0.25f, 0.25f, 0.3f, 1f));
      overlay.draw(block, x - 2, y - 2, SWATCH + 4, SWATCH + 4);
      if (i == 0) {
        // The sprite's own colours: a chequer, since there is no one colour to show.
        overlay.setColor(new Color(0.55f, 0.55f, 0.6f, 1f));
        overlay.draw(block, x, y, SWATCH, SWATCH);
        overlay.setColor(new Color(0.85f, 0.85f, 0.9f, 1f));
        overlay.draw(block, x, y + SWATCH / 2f, SWATCH / 2f, SWATCH / 2f);
        overlay.draw(block, x + SWATCH / 2f, y, SWATCH / 2f, SWATCH / 2f);
      } else {
        int rgb = PALETTE[i - 1];
        overlay.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
            (rgb & 0xFF) / 255f, 1f);
        overlay.draw(block, x, y, SWATCH, SWATCH);
      }
    }
    overlay.setColor(Color.WHITE);
    font.draw(overlay, "colour", GAP, band - 8);
  }

  private void keys() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      turning = !turning;
    }
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit();
      return;
    }
    if (sprites != null) {
      int step = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
          || Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ? 1
          : Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
          || Gdx.input.isKeyJustPressed(Input.Keys.UP) ? -1
          : Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN) ? columns()
          : Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP) ? -columns() : 0;
      if (step != 0) {
        select(Math.floorMod(chosenSprite + step, sprites.size()));
      }
      SpriteInflate.Profile[] all = SpriteInflate.Profile.values();
      for (int i = 0; i < all.length; i++) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i) && profile != all[i]) {
          profile = all[i];
          showSprite();
        }
      }
    } else {
      for (int i = 0; i < files.size() && i < 9; i++) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i) && i != chosenFile) {
          showFile(i);
        }
      }
    }
  }

  /** Chooses a sprite and brings it into view, since the arrows can walk off the visible rows. */
  private void select(int which) {
    chosenSprite = which;
    float top = thumbTop(which), bottom = top - THUMB;
    if (bottom < swatchBand()) {
      scroll += swatchBand() - bottom;
    } else if (top > Gdx.graphics.getHeight() - GAP) {
      scroll -= top - (Gdx.graphics.getHeight() - GAP);
    }
    scroll = Math.max(0, Math.min(scroll, scrollLimit()));
    showSprite();
  }

  /** Clicks and the wheel over the panel; everything else falls through to the camera. */
  private class Picking extends InputAdapter {

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
      if (sprites == null || screenX > PANEL) {
        return false;
      }
      float y = Gdx.graphics.getHeight() - screenY;      // input counts down, drawing counts up
      if (y < swatchBand()) {
        for (int i = 0; i <= PALETTE.length; i++) {
          if (screenX >= swatchLeft(i) && screenX < swatchLeft(i) + SWATCH
              && y >= swatchBottom(i) && y < swatchBottom(i) + SWATCH) {
            chosenColour = i - 1;
            showSprite();
            return true;
          }
        }
        return true;
      }
      for (int i = 0; i < thumbnails.size(); i++) {
        if (screenX >= thumbLeft(i) && screenX < thumbLeft(i) + THUMB
            && y <= thumbTop(i) && y > thumbTop(i) - THUMB) {
          chosenSprite = i;
          showSprite();
          return true;
        }
      }
      return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
      if (sprites == null || Gdx.input.getX() > PANEL) {
        return false;
      }
      scroll = Math.max(0, Math.min(scroll + amountY * (THUMB + GAP), scrollLimit()));
      return true;
    }
  }

  @Override
  public void dispose() {
    batch.dispose();
    overlay.dispose();
    font.dispose();
    block.dispose();
    if (atlas != null) {
      atlas.dispose();
    }
    if (model != null) {
      model.dispose();
    }
  }

  /**
   * What to show when nobody said: the sheet that comes with this prototype.
   * <p>
   * Looked for beside the code as well as beside the working directory, because "beside the
   * working directory" is a guess about how it was started. Run from a shell in this module it is
   * right; run from the green arrow in an IDE the working directory is as likely to be the whole
   * repository, and then a perfectly good default resolves to nothing and the window opens on an
   * error about a directory the person never typed.
   */
  private static String defaultPath() {
    List<File> looked = new ArrayList<>();
    for (File base : new File[]{new File("."), moduleRoot()}) {
      if (base == null) {
        continue;
      }
      File sheet = new File(base, "sprites/img.png");
      if (sheet.isFile()) {
        return sheet.getPath();
      }
      looked.add(sheet);
      File made = new File(base, "target/out");
      File[] objs = made.listFiles((dir, name) -> name.endsWith(".obj"));
      if (objs != null && objs.length > 0) {
        return made.getPath();
      }
      looked.add(made);
    }
    throw new IllegalStateException("nothing to show and nothing said. Looked for " + looked
        + ". Pass a sheet of sprites (a .png) or a directory of .obj files.");
  }

  /** The directory this prototype lives in, found by walking up from the code to its pom. */
  private static File moduleRoot() {
    try {
      File at = new File(InflateViewer.class.getProtectionDomain().getCodeSource()
          .getLocation().toURI());
      while (at != null && !new File(at, "pom.xml").isFile()) {
        at = at.getParentFile();
      }
      return at;
    } catch (Exception cannotTell) {
      return null;
    }
  }

  public static void main(String[] args) {
    String path = args.length > 0 ? args[0] : defaultPath();
    System.out.println("showing " + new File(path).getAbsolutePath());
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("sprite inflated - " + path);
    config.setWindowedMode(1180, 760);
    config.useVsync(true);
    config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
    new Lwjgl3Application(new InflateViewer(path), config);
  }
}
