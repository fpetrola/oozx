package com.fpetrola.oozx.proto.inflate;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
 * The sprites, inflated, turnable by hand.
 * <p>
 * Two ways in. Given a sheet of sprites it inflates whichever one is selected, there and then, so
 * a hundred and fifty guardians can be walked through with the arrow keys - which is the only way
 * to find out that a method works on a person and falls apart on a spinning coin. Given a
 * directory it reads the OBJ files {@link SpriteInflate} wrote.
 * <p>
 * Inflating as you go is affordable because none of it is expensive at a sprite's size: a
 * sixteen-pixel figure at four times over is a few thousand triangles and a few milliseconds.
 */
public class InflateViewer extends ApplicationAdapter {

  private final String path;

  /** Sheet mode: the sprites themselves, and the one being looked at. */
  private List<BufferedImage> sprites;
  private int chosenSprite;
  private SpriteInflate.Profile profile = SpriteInflate.Profile.SPHERE_LOCAL;
  private Texture thumbnail;

  /** Directory mode: the files written earlier. */
  private final List<FileHandle> files = new ArrayList<>();
  private final List<String> names = new ArrayList<>();
  private int chosenFile;

  private PerspectiveCamera camera;
  private CameraInputController controller;
  private ModelBatch batch;
  private Environment environment;
  private SpriteBatch overlay;
  private BitmapFont font;

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
    Gdx.input.setInputProcessor(controller);

    environment = new Environment();
    environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.35f, 0.35f, 0.4f, 1f));
    // Two lights and not one: a single lamp leaves the whole shaded side flat black, and the
    // shaded side is half of what there is to judge.
    environment.add(new DirectionalLight().set(0.9f, 0.88f, 0.82f, -0.6f, -0.7f, -0.5f));
    environment.add(new DirectionalLight().set(0.25f, 0.28f, 0.35f, 0.7f, 0.2f, 0.4f));

    batch = new ModelBatch();
    overlay = new SpriteBatch();
    font = new BitmapFont();

    File given = new File(path);
    if (given.isFile() && given.getName().toLowerCase().endsWith(".png")) {
      try {
        sprites = SpriteSheet.slice(given);
      } catch (Exception e) {
        throw new IllegalStateException("could not read the sheet " + given + ": " + e, e);
      }
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
  }

  // ------------------------------------------------------------------------------- sheet mode

  private void showSprite() {
    BufferedImage sprite = sprites.get(chosenSprite);
    SpriteInflate.Fields fields = SpriteInflate.measure(sprite, new int[]{4});
    List<double[]> triangles = SpriteInflate.inflate(fields, profile);
    put(build(triangles, fields));
    caption = String.format("sprite %d of %d   %s   %d triangles",
        chosenSprite + 1, sprites.size(), profile.label, triangles.size());

    if (thumbnail != null) {
      thumbnail.dispose();
    }
    Pixmap pixmap = new Pixmap(sprite.getWidth(), sprite.getHeight(), Pixmap.Format.RGBA8888);
    for (int y = 0; y < sprite.getHeight(); y++) {
      for (int x = 0; x < sprite.getWidth(); x++) {
        int argb = sprite.getRGB(x, y);
        pixmap.drawPixel(x, y, (argb << 8) | (argb >>> 24));
      }
    }
    thumbnail = new Texture(pixmap);
    pixmap.dispose();
  }

  /**
   * The triangles as something libGDX can draw, welded, with the averaged normal at each vertex
   * and the sprite's own colour where the vertex sits on it.
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
        int rgb = colourAt(fields, x, y);
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

  /**
   * The colour under a point of the surface. A vertex on the outline sits half in the background,
   * where there is no colour at all, so the nearest thing that has one is taken instead - without
   * it the whole rim of every figure comes out black.
   */
  private int colourAt(SpriteInflate.Fields fields, double x, double y) {
    for (int radius = 0; radius <= 3; radius++) {
      for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
          int sx = (int) Math.round(x) + dx, sy = (int) Math.round(y) + dy;
          if (sx < 0 || sy < 0 || sx >= fields.width() || sy >= fields.height()) continue;
          int argb = fields.scaled().getRGB(sx, sy);
          if ((argb >>> 24) >= 128) {
            return argb & 0xFFFFFF;
          }
        }
      }
    }
    return 0xD0D0D0;
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
    controller.update();
    batch.begin(camera);
    batch.render(instance, environment);
    batch.end();

    overlay.begin();
    if (thumbnail != null) {
      int side = 96;
      overlay.draw(thumbnail, 12, Gdx.graphics.getHeight() - side - 34, side, side);
    }
    font.draw(overlay, caption, 12, Gdx.graphics.getHeight() - 12);
    font.draw(overlay, sprites != null
        ? "left/right = sprite   page up/down = ten at a time   1-4 = profile   "
        + "space = turn   drag = orbit   esc = quit"
        : "1-" + files.size() + " = profile   space = turn   drag = orbit   esc = quit", 12, 22);
    overlay.end();
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
      int step = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) ? 1
          : Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ? -1
          : Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN) ? 10
          : Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP) ? -10 : 0;
      if (step != 0) {
        chosenSprite = Math.floorMod(chosenSprite + step, sprites.size());
        showSprite();
      }
      SpriteInflate.Profile[] all = SpriteInflate.Profile.values();
      for (int i = 0; i < all.length; i++) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
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

  @Override
  public void dispose() {
    batch.dispose();
    overlay.dispose();
    font.dispose();
    if (thumbnail != null) {
      thumbnail.dispose();
    }
    if (model != null) {
      model.dispose();
    }
  }

  public static void main(String[] args) {
    // Sensible without arguments, because it is usually started from a green arrow in an IDE.
    String path = args.length > 0 ? args[0]
        : new File("sprites/img.png").isFile() ? "sprites/img.png" : "target/out";
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("sprite inflated - " + path);
    config.setWindowedMode(900, 700);
    config.useVsync(true);
    config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
    new Lwjgl3Application(new InflateViewer(path), config);
  }
}
