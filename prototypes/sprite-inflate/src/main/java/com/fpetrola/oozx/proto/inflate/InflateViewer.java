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
import com.badlogic.gdx.graphics.VertexAttributes;
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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The inflated sprites, turnable by hand.
 * <p>
 * The still pictures the pipeline writes settle the measurements; what they cannot settle is
 * whether the thing reads as a body when you move it, which is the only question that matters
 * for putting it in a viewer. Number keys change profile, space starts and stops the turn, and
 * the mouse drags it about.
 */
public class InflateViewer extends ApplicationAdapter {

  private final List<FileHandle> files = new ArrayList<>();
  private final List<String> names = new ArrayList<>();
  private final String directory;

  private PerspectiveCamera camera;
  private CameraInputController controller;
  private ModelBatch batch;
  private Environment environment;
  private SpriteBatch overlay;
  private com.badlogic.gdx.graphics.g2d.BitmapFont font;

  private Model model;
  private ModelInstance instance;
  private int chosen = 0;
  private boolean turning = true;
  private float angle;

  InflateViewer(String directory) {
    this.directory = directory;
  }

  @Override
  public void create() {
    File[] found = new File(directory).listFiles((dir, name) -> name.endsWith(".obj"));
    if (found == null || found.length == 0) {
      throw new IllegalStateException("no .obj in " + directory);
    }
    java.util.Arrays.sort(found);
    for (File one : found) {
      files.add(new FileHandle(one));
      names.add(one.getName().replaceAll("^\\d+-|\\.obj$", ""));
    }

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
    font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
    load(0);
  }

  private void load(int which) {
    if (model != null) {
      model.dispose();
    }
    chosen = which;
    // flipV false: these carry no texture, and the loader's flip would only move the normals.
    model = new ObjLoader().loadModel(files.get(which));
    for (Material material : model.materials) {
      material.set(ColorAttribute.createDiffuse(new Color(0.85f, 0.85f, 0.88f, 1f)));
    }
    instance = new ModelInstance(model);

    // Sat in front of the camera whatever size the sprite was, so switching profiles does not
    // also change how big the thing looks.
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
    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      turning = !turning;
    }
    for (int i = 0; i < files.size() && i < 9; i++) {
      if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
        load(i);
      }
    }
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit();
    }

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
    font.draw(overlay, names.get(chosen), 12, Gdx.graphics.getHeight() - 12);
    StringBuilder keys = new StringBuilder();
    for (int i = 0; i < names.size(); i++) {
      keys.append(i + 1).append('=').append(names.get(i)).append("   ");
    }
    font.draw(overlay, keys + "  space=turn  drag=orbit  esc=quit", 12, 22);
    overlay.end();
  }

  @Override
  public void dispose() {
    batch.dispose();
    overlay.dispose();
    font.dispose();
    if (model != null) {
      model.dispose();
    }
  }

  public static void main(String[] args) {
    String directory = args.length > 0 ? args[0] : "outw";
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("sprite inflated - " + directory);
    config.setWindowedMode(900, 700);
    config.useVsync(true);
    config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
    new Lwjgl3Application(new InflateViewer(directory), config);
  }
}
