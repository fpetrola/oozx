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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;

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

  private static final String[] SCALES = {"2x", "3x", "4x", "5x", "6x", "8x (4 then 2)"};
  private static final int[][] SCALE_PASSES = {{2}, {3}, {4}, {5}, {6}, {4, 2}};

  private static final int PANEL = 250;
  private static final int CONTROLS = 300;
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
  private SpriteInflate.Options options = SpriteInflate.Options.standard();
  private SpriteInflate.Fields fields;
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

  private Stage stage;
  private Table controls;
  private Label.LabelStyle labelStyle;
  private Slider.SliderStyle sliderStyle;
  private SelectBox.SelectBoxStyle selectStyle;
  private CheckBox.CheckBoxStyle checkStyle;
  private TextButton.TextButtonStyle buttonStyle;

  private Model model;
  private ModelInstance instance;
  /** Where the model's own middle is, found once while it is unrotated. */
  private final Vector3 centre = new Vector3();
  /** Whether the camera has been put somewhere sensible; after that it is the person's. */
  private boolean framed;
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
      styles();
      buildControls();
      rebuild(true);
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

    // Ours first, so a click on the sprites picks one; then the controls; the camera last, so it
    // only turns the model when the click landed on neither panel.
    InputMultiplexer input = new InputMultiplexer();
    input.addProcessor(new Picking());
    if (stage != null) {
      input.addProcessor(stage);
    }
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

  /**
   * @param measureAgain whether what is KNOWN about the sprite changed, or only the shape built
   *                     from it. Measuring is the expensive half - the local thickness is
   *                     quadratic - so a slider that does not affect it does not pay for it.
   */
  private void rebuild(boolean measureAgain) {
    if (measureAgain || fields == null) {
      fields = SpriteInflate.measure(sprites.get(chosenSprite), options);
    }
    List<double[]> triangles = SpriteInflate.inflate(fields, profile, options);
    put(build(triangles, fields));
    caption = String.format("sprite %d of %d   %s   %dx   %d triangles",
        chosenSprite + 1, sprites.size(), profile.label, options.factor(), triangles.size());
  }

  // ------------------------------------------------------------------------------ the controls

  /**
   * A set of widget styles out of one white pixel and the built-in font.
   * <p>
   * Rather than a skin file, because a prototype that needs an atlas beside it in order to start
   * is a prototype nobody runs. Everything here is that pixel, tinted and stretched.
   */
  private void styles() {
    TextureRegionDrawable pixel = new TextureRegionDrawable(new TextureRegion(block));
    labelStyle = new Label.LabelStyle(font, new Color(0.85f, 0.86f, 0.9f, 1f));
    sliderStyle = new Slider.SliderStyle(tinted(pixel, 0.22f, 0.22f, 0.28f, 6, 6),
        tinted(pixel, 0.55f, 0.68f, 0.95f, 10, 18));

    Drawable field = tinted(pixel, 0.17f, 0.17f, 0.22f, 24, 24);
    Drawable chosen = tinted(pixel, 0.30f, 0.45f, 0.75f, 4, 4);
    com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle =
        new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(font, Color.WHITE,
            new Color(0.8f, 0.8f, 0.85f, 1f), chosen);
    listStyle.background = tinted(pixel, 0.12f, 0.12f, 0.16f, 4, 4);
    selectStyle = new SelectBox.SelectBoxStyle(font, new Color(0.9f, 0.9f, 0.95f, 1f), field,
        new ScrollPane.ScrollPaneStyle(), listStyle);

    checkStyle = new CheckBox.CheckBoxStyle(tinted(pixel, 0.22f, 0.22f, 0.28f, 16, 16),
        tinted(pixel, 0.55f, 0.68f, 0.95f, 16, 16), font, new Color(0.85f, 0.86f, 0.9f, 1f));

    buttonStyle = new TextButton.TextButtonStyle(field, chosen, field, font);
    buttonStyle.fontColor = new Color(0.9f, 0.9f, 0.95f, 1f);
  }

  private static Drawable tinted(TextureRegionDrawable pixel, float r, float g, float b,
                                 float minWidth, float minHeight) {
    Drawable drawable = pixel.tint(new Color(r, g, b, 1f));
    drawable.setMinWidth(minWidth);
    drawable.setMinHeight(minHeight);
    return drawable;
  }

  private void buildControls() {
    stage = new Stage(new ScreenViewport());
    controls = new Table();
    controls.top().left().pad(12);
    controls.defaults().left().padBottom(4).width(CONTROLS - 34);
    stage.addActor(controls);

    controls.add(new Label("shape", labelStyle)).padBottom(2).row();
    SelectBox<String> shape = new SelectBox<>(selectStyle);
    String[] labels = new String[SpriteInflate.Profile.values().length];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = SpriteInflate.Profile.values()[i].label;
    }
    shape.setItems(labels);
    shape.setSelected(profile.label);
    shape.addListener(new ChangeListener() {
      public void changed(ChangeEvent event, Actor actor) {
        profile = SpriteInflate.Profile.values()[shape.getSelectedIndex()];
        rebuild(false);
      }
    });
    controls.add(shape).height(28).padBottom(10).row();

    controls.add(new Label("scale", labelStyle)).padBottom(2).row();
    SelectBox<String> scale = new SelectBox<>(selectStyle);
    scale.setItems(SCALES);
    scale.setSelectedIndex(2);
    scale.addListener(new ChangeListener() {
      public void changed(ChangeEvent event, Actor actor) {
        set(with(SCALE_PASSES[scale.getSelectedIndex()], options.depth(), options.smoothing(),
            options.dentDepth(), options.dentReach(), options.holeAcross(), options.mirrored(), options.rimRoll()),
            true);
      }
    });
    controls.add(scale).height(28).padBottom(10).row();

    // Shaping: these reuse what was measured, so dragging one only rebuilds the mesh.
    slider("depth", 0.1f, 2.5f, 0.05f, (float) options.depth(), "%.2f x", value ->
        set(with(options.passes(), value, options.smoothing(), options.dentDepth(),
            options.dentReach(), options.holeAcross(), options.mirrored(), options.rimRoll()), false));
    slider("dent at the detail", 0f, 0.95f, 0.05f, (float) options.dentDepth(), "%.2f", value ->
        set(with(options.passes(), options.depth(), options.smoothing(), value,
            options.dentReach(), options.holeAcross(), options.mirrored(), options.rimRoll()), false));
    slider("how wide that dent is", 0.1f, 3f, 0.1f, (float) options.dentReach(), "%.1f px",
        value -> set(with(options.passes(), options.depth(), options.smoothing(),
            options.dentDepth(), value, options.holeAcross(), options.mirrored(), options.rimRoll()), false));

    // Measuring: these change what is known about the sprite, so the pipeline runs again.
    slider("smoothing", 0f, 10f, 1f, options.smoothing(), "%.0f passes", value ->
        set(with(options.passes(), options.depth(), (int) value, options.dentDepth(),
            options.dentReach(), options.holeAcross(), options.mirrored(), options.rimRoll()), true));
    slider("a hole is detail up to", 0f, 6f, 0.5f, (float) options.holeAcross(), "%.1f px",
        value -> set(with(options.passes(), options.depth(), options.smoothing(),
            options.dentDepth(), options.dentReach(), value, options.mirrored(), options.rimRoll()), true));

    slider("rounding at the seam", 0f, 3f, 0.1f, (float) options.rimRoll(), "%.1f px", value ->
        set(with(options.passes(), options.depth(), options.smoothing(), options.dentDepth(),
            options.dentReach(), options.holeAcross(), options.mirrored(), value), false));

    CheckBox mirrored = new CheckBox(" bulge on both sides", checkStyle);
    mirrored.setChecked(options.mirrored());
    mirrored.addListener(new ChangeListener() {
      public void changed(ChangeEvent event, Actor actor) {
        set(with(options.passes(), options.depth(), options.smoothing(), options.dentDepth(),
            options.dentReach(), options.holeAcross(), mirrored.isChecked(), options.rimRoll()), false);
      }
    });
    controls.add(mirrored).padTop(8).padBottom(10).row();

    TextButton view = new TextButton("put the view back", buttonStyle);
    view.addListener(new ChangeListener() {
      public void changed(ChangeEvent event, Actor actor) {
        BoundingBox box = new BoundingBox();
        new ModelInstance(model).calculateBoundingBox(box);
        angle = 0;
        pose();
        frame(box);
      }
    });
    controls.add(view).height(30).padTop(6).row();

    TextButton save = new TextButton("write the OBJ", buttonStyle);
    save.addListener(new ChangeListener() {
      public void changed(ChangeEvent event, Actor actor) {
        write();
      }
    });
    controls.add(save).height(30).padTop(6).row();

    place();
  }

  private static SpriteInflate.Options with(int[] passes, double depth, int smoothing,
                                            double dentDepth, double dentReach, double holeAcross,
                                            boolean mirrored, double rimRoll) {
    return new SpriteInflate.Options(passes, depth, smoothing, dentDepth, dentReach, holeAcross,
        mirrored, rimRoll);
  }

  private void set(SpriteInflate.Options changed, boolean measureAgain) {
    options = changed;
    rebuild(measureAgain);
  }

  /** A named slider that shows what it is set to, since a bare bar says nothing. */
  private void slider(String name, float low, float high, float step, float now, String format,
                      DoubleConsumer apply) {
    Label label = new Label(name + "   " + String.format(format, now), labelStyle);
    controls.add(label).padBottom(2).row();
    Slider bar = new Slider(low, high, step, false, sliderStyle);
    bar.setValue(now);
    bar.addListener(new ChangeListener() {
      public void changed(ChangeEvent event, Actor actor) {
        label.setText(name + "   " + String.format(format, bar.getValue()));
        apply.accept(bar.getValue());
      }
    });
    controls.add(bar).height(22).padBottom(10).row();
  }

  private void place() {
    controls.setBounds(Gdx.graphics.getWidth() - CONTROLS, 0, CONTROLS, Gdx.graphics.getHeight());
  }

  @Override
  public void resize(int width, int height) {
    if (stage != null) {
      stage.getViewport().update(width, height, true);
      place();
    }
  }

  /** Keeps what is on screen, since the point of turning the knobs is to arrive somewhere. */
  private void write() {
    try {
      File root = moduleRoot();
      File out = new File(root == null ? new File(".") : root, "target/from-viewer");
      out.mkdirs();
      File file = new File(out, String.format("sprite-%03d-%s.obj", chosenSprite, profile.label));
      SpriteInflate.writeObj(SpriteInflate.inflate(fields, profile, options), file.toPath());
      caption = "written to " + file.getPath();
    } catch (Exception e) {
      caption = "could not write it: " + e;
    }
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

  /**
   * Swaps in a new model without disturbing how it is being looked at.
   * <p>
   * The camera is placed once and then left alone, and the new model takes the pose the old one
   * had. Turning a knob rebuilds the mesh, and if that also reset the camera and the rotation the
   * figure would jump to a standard three-quarter view every time - which is exactly the thing
   * that makes it impossible to judge what the knob did. What is being compared is two shapes
   * from ONE viewpoint; moving the viewpoint at the same moment compares nothing.
   */
  private void put(Model built) {
    if (model != null) {
      model.dispose();
    }
    model = built;
    instance = new ModelInstance(model);
    BoundingBox box = new BoundingBox();
    // While the transform is still the identity, so this is the model's own middle. Asking an
    // instance for its box AFTER rotating it gives the box of the rotated thing, whose middle
    // moves as it turns - which had the figure drifting a little on its own.
    instance.calculateBoundingBox(box);
    box.getCenter(centre);
    pose();
    if (!framed) {
      frame(box);
    }
  }

  /** Puts the camera where the whole figure can be seen. Once, and on request after that. */
  private void frame(BoundingBox box) {
    float span = Math.max(box.getWidth(), Math.max(box.getHeight(), box.getDepth()));
    camera.position.set(0, 0, span * 2.2f);
    camera.up.set(0, 1, 0);
    camera.lookAt(0, 0, 0);
    camera.update();
    controller.target.set(0, 0, 0);
    framed = true;
  }

  private void pose() {
    instance.transform.idt().rotate(Vector3.Y, angle).translate(-centre.x, -centre.y, -centre.z);
  }

  @Override
  public void render() {
    keys();

    if (turning) {
      angle += Gdx.graphics.getDeltaTime() * 45f;
      pose();
    }

    ScreenUtils.clear(0.06f, 0.06f, 0.08f, 1f, true);
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

    // The figure lives to the right of the panel, so it is centred in what is left rather than
    // in the window - otherwise it sits half behind the sprites it is being chosen from.
    int left = sprites == null ? 0 : PANEL;
    int right = sprites == null ? 0 : CONTROLS;
    int width = Math.max(1, Gdx.graphics.getWidth() - left - right);
    int height = Gdx.graphics.getHeight();
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
      overlay.setColor(0.09f, 0.09f, 0.12f, 1f);
      overlay.draw(block, Gdx.graphics.getWidth() - CONTROLS, 0, CONTROLS, height);
    }
    font.setColor(Color.WHITE);
    font.draw(overlay, caption, left + 12, height - 12);
    font.draw(overlay, sprites != null
        ? "click a sprite or a colour   arrows = sprite   wheel = scroll   space = turn   "
        + "drag = orbit   the view stays put while you turn the knobs"
        : "1-" + files.size() + " = profile   space = turn   drag = orbit   esc = quit",
        left + 12, 22);
    overlay.end();

    if (stage != null) {
      stage.act(Gdx.graphics.getDeltaTime());
      stage.draw();
    }
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
    rebuild(true);
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
            rebuild(false);
            return true;
          }
        }
        return true;
      }
      for (int i = 0; i < thumbnails.size(); i++) {
        if (screenX >= thumbLeft(i) && screenX < thumbLeft(i) + THUMB
            && y <= thumbTop(i) && y > thumbTop(i) - THUMB) {
          chosenSprite = i;
          rebuild(true);
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
    if (stage != null) {
      stage.dispose();
    }
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
    config.setWindowedMode(1320, 780);
    config.useVsync(true);
    config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
    new Lwjgl3Application(new InflateViewer(path), config);
  }
}
