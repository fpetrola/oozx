import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * Films a clip of the real desk by driving it as a person would: a pointer that curves, speeds up and slows down,
 * pauses where somebody would look. Run inside the display being recorded, with the application on its classpath.
 * <p>
 * It brings its own folder window, because a clip that starts with a file has to show where the file was: the folder
 * is a real drag source, so what is filmed is the application's own drop and not a call made behind its back.
 */
public class Film {
  private static final Random hand = new Random(7);
  private static Robot robot;

  /**
   * The desk is started here, in this same program, and the hand waits for the camera: a drag between two programs
   * found no taker in a display with no window manager, and within one it does.
   */
  public static void main(String[] args) throws Exception {
    com.fpetrola.oozx.speccy.desktop.OOSpectrumLauncher.main(new String[0]);
    File rolling = new File(args[args.length - 1]);
    // What a clip starts from and nobody needs to see being done, done before the camera rolls.
    if (args[0].equals("speech")) {
      plugged(new File(args[1]), "Currah µSpeech:RIGHT");
      // A machine with nothing loaded says so; and the board is opened whole, not just its meter.
      SwingUtilities.invokeAndWait(() -> {
        for (Window window : Window.getWindows())
          if (inside(window, JButton.class, "OK") instanceof JButton ok) ok.doClick();
        ((AbstractButton) component(AbstractButton.class, "Show what it is saying, its volume and ROMs, or just the meter")).doClick();
      });
    }
    if (args[0].startsWith("art")) {
      plugged(new File(args[1]), "Kempston Mouse:BOTTOM", "ZX Printer:RIGHT");
      SwingUtilities.invokeAndWait(() -> ((AbstractButton) component(AbstractButton.class, "Fit")).doClick());
    }
    while (!rolling.exists()) Thread.sleep(200);
    robot = new Robot();
    robot.mouseMove(at.x, at.y);
    switch (args[0]) {
      case "any-name" -> anyName(new File(args[1]), args[2]);
      case "type-a-name" -> typeAName();
      case "art-probe" -> artProbe();
      case "art-scale" -> artScale();
      case "art" -> art();
      case "speech" -> speech();
      case "art-colour" -> { art(); artColoured(); }
      case "art-drift" -> { art(); mark("d0"); stroke(4, 158, 250, 158); mark("d1"); ink(56); mark("d2"); fill(32, 30, 60); mark("d3"); }
      case "art-menu" -> artMenu();
      case "an-hour" -> anHour(new File(args[1]), args[2]);
      case "take-over" -> takeOver(new File(args[1]), args[2]);
      case "look" -> look("Game Browser...");
      default -> throw new IllegalArgumentException("no clip called " + args[0]);
    }
    System.exit(0);
  }

  /** A file whose name says nothing is taken from a folder and let go of over the desk. */
  private static void anyName(File file, String marking) throws Exception {
    Point[] at = new Point[1];
    SwingUtilities.invokeAndWait(() -> at[0] = folder("Downloads", file, 70, 470));
    new ProcessBuilder(marking).start().waitFor();
    look(1800);
    move(at[0].x, at[0].y);
    look(500);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    look(250);
    move(at[0].x + 14, at[0].y - 10);
    move(620, 300);
    look(350);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    // The tape loads while the card fills; the hand goes to the card and waits for the game to be there.
    look(6000);
    move(980, 380);
    look(6500);
  }

  /** A game is let go of over the desk and the rocket is pressed: what takes an hour goes by in seconds, and comes back. */
  private static void anHour(File file, String marking) throws Exception {
    Point[] at = new Point[1];
    SwingUtilities.invokeAndWait(() -> at[0] = folder("Downloads", file, 70, 470));
    new ProcessBuilder(marking).start().waitFor();
    look(900);
    move(at[0].x, at[0].y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    look(200);
    move(at[0].x + 14, at[0].y - 10);
    move(300, 250);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    look(5000);
    press(at(component(AbstractButton.class, "Full speed - right-click for a speed")));
    look(9000);
    press(at(component(AbstractButton.class, "Full speed - right-click for a speed")));
    look(3000);
  }

  /** Somebody's recording is let go of over the desk and watched; then the machine is taken, and it is ours to play. */
  private static void takeOver(File file, String marking) throws Exception {
    Point[] at = new Point[1];
    SwingUtilities.invokeAndWait(() -> at[0] = folder("Downloads", file, 70, 520));
    new ProcessBuilder(marking).start().waitFor();
    look(900);
    move(at[0].x, at[0].y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    look(200);
    move(at[0].x + 14, at[0].y - 10);
    move(320, 240);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    look(9000);
    // The fourth button of the player is the joystick: take over.
    press(at(button("RZX", 3)));
    look(900);
    press(at(window("ZX Spectrum", "Spectrum #")));
    for (int key : new int[]{java.awt.event.KeyEvent.VK_P, java.awt.event.KeyEvent.VK_SPACE, java.awt.event.KeyEvent.VK_P, java.awt.event.KeyEvent.VK_O, java.awt.event.KeyEvent.VK_SPACE}) {
      robot.keyPress(key);
      robot.delay(500 + hand.nextInt(500));
      robot.keyRelease(key);
      robot.delay(150 + hand.nextInt(200));
    }
    look(2500);
  }

  /** The desk window whose title starts with one of those. */
  private static JInternalFrame window(String... starts) {
    for (Window window : Window.getWindows()) {
      Component found = inside(window, JInternalFrame.class, null, frame -> java.util.Arrays.stream(starts).anyMatch(((JInternalFrame) frame).getTitle()::startsWith));
      if (found != null) return (JInternalFrame) found;
    }
    throw new IllegalStateException("no window on the desk starts with " + String.join(" or ", starts));
  }

  /** One of the buttons of that window, counted from the first: for the ones that say nothing but their picture. */
  private static Component button(String title, int which) {
    java.util.List<Component> buttons = new java.util.ArrayList<>();
    gather(window(title), buttons);
    return buttons.get(which);
  }

  private static void gather(Component component, java.util.List<Component> buttons) {
    if (component instanceof JButton && component.isShowing() && !(component.getParent() instanceof javax.swing.plaf.basic.BasicInternalFrameTitlePane)) buttons.add(component);
    if (component instanceof Container container) for (Component each : container.getComponents()) gather(each, buttons);
  }

  /** A machine with that file, and those boards clipped onto it, left on the desk where the hand will find them. */
  private static void plugged(File file, String... boards) throws Exception {
    Thread.sleep(6000);
    com.fpetrola.oozx.speccy.devices.Desk desk = com.fpetrola.oozx.speccy.devices.Desk.theOne();
    JInternalFrame[] machine = new JInternalFrame[1];
    SwingUtilities.invokeAndWait(() -> {
      machine[0] = (JInternalFrame) desk.machineFrom(file);
      machine[0].setLocation(40, 60);
    });
    Thread.sleep(1500);
    // Twice the size: the picture made 512 by 384, the window around it growing by as much.
    SwingUtilities.invokeAndWait(() -> {
      Component picture = ((com.fpetrola.oozx.speccy.windows.MachineWindow) machine[0]).picture();
      machine[0].setSize(machine[0].getWidth() + 512 - picture.getWidth(), machine[0].getHeight() + 384 - picture.getHeight());
      machine[0].validate();
    });
    Thread.sleep(2000);
    for (String board : boards) SwingUtilities.invokeAndWait(() -> {
      String[] nameAndSide = (board + ":BOTTOM").split(":");
      JInternalFrame window = desk.window(nameAndSide[0]);
      if (window instanceof com.fpetrola.oozx.speccy.windows.AttachedFrame clipped)
        clipped.attachTo(machine[0], com.fpetrola.oozx.speccy.windows.AttachedFrame.Dock.valueOf(nameAndSide[1]));
      // Beside the machine, as tall as it and as wide as the desk leaves: big enough to watch the paper come out.
      if ("RIGHT".equals(nameAndSide[1])) window.setSize(420, machine[0].getHeight());
    });
  }

  /** Not a clip: the hand goes round a square and back, and the machine's picture is kept at every corner. */
  private static void artProbe() throws Exception {
    JInternalFrame machine = window("ZX Spectrum", "Spectrum #");
    Component picture = ((com.fpetrola.oozx.speccy.windows.MachineWindow) machine).picture();
    Point on = picture.getLocationOnScreen();
    System.out.println("picture " + on + " " + picture.getSize());
    // Pixels of the Spectrum, and one trip off the picture and back.
    int[][] corners = {{20, 40}, {200, 40}, {200, 150}, {20, 150}, {-80, 100}, {128, 96}, {240, 20}, {60, 170}};
    int i = 0;
    int left = (picture.getWidth() - 256) / 2;
    for (int[] corner : corners) {
      move(on.x + left + corner[0], on.y + corner[1]);
      look(900);
      java.awt.image.BufferedImage shot = robot.createScreenCapture(new Rectangle(on, picture.getSize()));
      Graphics2D pen = shot.createGraphics();
      pen.setColor(Color.RED);
      pen.drawOval(left + corner[0] - 4, corner[1] - 4, 8, 8);
      javax.imageio.ImageIO.write(shot, "png", new File("/tmp/claude-1000/art-" + i++ + ".png"));
    }
    javax.imageio.ImageIO.write(robot.createScreenCapture(new Rectangle(0, 0, 1280, 800)), "png", new File("/tmp/claude-1000/art-desk.png"));
  }

  /** Where the hand is over that pixel of the Spectrum's screen. */
  private static Point over(int across, int down) {
    Component picture = ((com.fpetrola.oozx.speccy.windows.MachineWindow) window("ZX Spectrum", "Spectrum #")).picture();
    Point on = picture.getLocationOnScreen();
    double scale = Math.min(picture.getWidth() / 256.0, picture.getHeight() / 192.0);
    if (scale - Math.floor(scale) < 0.3 && scale >= 1) scale = Math.floor(scale);
    return new Point((int) (on.x + (picture.getWidth() - 256 * scale) / 2 + across * scale), (int) (on.y + (picture.getHeight() - 192 * scale) / 2 + down * scale));
  }

  /** A line drawn with the button held, through those pixels of the Spectrum, at a hand's pace. */
  private static void stroke(int... pixels) {
    Point start = over(pixels[0], pixels[1]);
    move(start.x, start.y);
    look(250);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    for (int i = 2; i < pixels.length; i += 2) {
      Point from = new Point(at), to = over(pixels[i], pixels[i + 1]);
      // The pen puts a dot where the pointer is once a frame: a pixel or two a frame and the dots join.
      int steps = (int) Math.max(4, from.distance(to) / 3);
      for (int s = 1; s <= steps; s++) {
        handTo(from.x + (to.x - from.x) * s / steps, from.y + (to.y - from.y) * s / steps);
        robot.delay(18 + hand.nextInt(8));
      }
    }
    look(300);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    look(300);
  }

  /** Not a clip: the menus for filling and for colour, opened and kept. */
  /** The horizon under the house, and the picture printed on the ZX Printer. */
  private static void artColoured() throws Exception {
    // Short of the edges, where the program would hold its brush back while the hand went on.
    stroke(8, 159, 247, 159);
    look(1200);
    // The ZX Printer is the one the program calls ZX/Alphacom, the other printer it drove the same way.
    choose(over(16, 4));
    look(1200);
    choose(over(40, 172));
    look(2500);
    choose(over(16, 4));
    look(1200);
    choose(over(40, 26));
    look(4000);
    shoot("art-p1");
    look(14000);
    shoot("art-p2");
  }

  /** Not a clip: the hand still over one place, and the picture kept with that place marked. */
  private static void mark(String name) throws Exception {
    Point there = over(150, 110);
    move(there.x, there.y);
    look(2500);
    java.awt.image.BufferedImage shot = robot.createScreenCapture(new Rectangle(0, 0, 1280, 800));
    Graphics2D pen = shot.createGraphics();
    pen.setColor(Color.RED);
    pen.drawOval(there.x - 5, there.y - 5, 10, 10);
    javax.imageio.ImageIO.write(shot, "png", new File("/tmp/claude-1000/art-" + name + ".png"));
  }

  /**
   * The hand off the picture and back. The program stops reading the mouse while it fills, and
   * starts again from wherever its pointer is, so what the hand did meanwhile is lost; coming back in,
   * the mouse puts the pointer under the hand again.
   */
  private static void outAndBack() {
    Point outside = over(290, 120), back = over(200, 120);
    move(outside.x, outside.y);
    look(800);
    move(back.x, back.y);
    look(1600);
  }

  /** Fill with a texture: the one at that place of the program's grid of them, poured at that place. */
  private static void textured(int textureAcross, int textureDown, int across, int down) {
    choose(over(75, 16));
    look(900);
    choose(over(70, 44));
    look(1000);
    choose(over(textureAcross, textureDown));
    look(900);
    choose(over(across, down));
    look(3500);
    outAndBack();
  }

  /** Attributes, ink, and the colour at that place in the row of eight the program shows. */
  private static void ink(int colour) {
    choose(over(95, 4));
    look(900);
    choose(over(74, 26));
    look(1000);
    choose(over(colour, 48));
    look(800);
  }

  /** Fill, the kind on that row of its menu, and a press on the place to fill. */
  private static void fill(int kind, int across, int down) {
    choose(over(75, 16));
    look(900);
    choose(over(70, kind));
    look(900);
    choose(over(across, down));
    look(3500);
    outAndBack();
  }

  /** The µSpeech saying each key as it is typed, and then saying what is written beside it, in Spanish. */
  private static void speech() throws Exception {
    look(1200);
    press(over(128, 96));
    look(800);
    type("v\n");
    look(1500);
    type("p2\n");
    look(2500);
    press(at(component(JTextField.class, null)));
    look(600);
    JComboBox<?> language = (JComboBox<?>) component(JComboBox.class, null, box -> ((JComboBox<?>) box).getItemCount() == 3);
    press(at(language));
    look(700);
    robot.keyPress(java.awt.event.KeyEvent.VK_DOWN);
    robot.keyRelease(java.awt.event.KeyEvent.VK_DOWN);
    look(300);
    robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
    robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
    look(700);
    press(at(component(JTextField.class, null)));
    type("hola, soy un spectrum\n");
    look(9000);
  }

  /** Not a clip: a click on a menu of the program, and what it opens. */
  private static void artMenu() throws Exception {
    look(800);
    Point paint = over(128, 4);
    move(paint.x, paint.y);
    look(1500);
    shoot("art-0");
    press(paint);
    look(2500);
    shoot("art-1");
    Point first = over(128, 30);
    move(first.x, first.y);
    look(1500);
    shoot("art-2");
  }

  private static void shoot(String name) throws Exception {
    javax.imageio.ImageIO.write(robot.createScreenCapture(new Rectangle(0, 0, 1280, 800)), "png", new File("/tmp/claude-1000/" + name + ".png"));
  }

  /** A house and its sun drawn in the OCP Art Studio with the Kempston Mouse. */
  private static void art() throws Exception {
    look(1000);
    Point in = over(150, 90);
    move(in.x, in.y);
    look(2200);
    choose(over(128, 4));
    look(1200);
    choose(over(112, 45));
    look(1500);
    // The thickest brush there is, round: drawn with it a line looks sprayed on a wall.
    choose(over(70, 97));
    look(900);
    stroke(70, 150, 70, 100, 130, 100, 130, 150, 70, 150);
    stroke(62, 104, 100, 70, 138, 104);
    stroke(92, 150, 92, 125, 108, 125, 108, 150);
    int[] sun = new int[34];
    for (int i = 0; i <= 16; i++) {
      sun[2 * i] = (int) (200 + 18 * Math.cos(i * Math.PI / 8));
      sun[2 * i + 1] = (int) (60 + 18 * Math.sin(i * Math.PI / 8));
    }
    stroke(sun);
    look(800);
    look(2500);
  }

  /** Not a clip: slow and quick strokes of known length, the picture kept after each. */
  private static void artScale() throws Exception {
    JInternalFrame machine = window("ZX Spectrum", "Spectrum #");
    Component picture = ((com.fpetrola.oozx.speccy.windows.MachineWindow) machine).picture();
    Point on = picture.getLocationOnScreen();
    int left = on.x + (picture.getWidth() - 256) / 2;
    int[][] path = {{100, 80, 0}, {100, 80, 0}, {160, 80, 40}, {160, 130, 40}, {100, 130, 2}, {100, 80, 2}};
    int i = 0;
    for (int[] step : path) {
      Point from = new Point(at);
      int steps = Math.max(1, step[2]);
      for (int s = 1; s <= steps; s++) {
        handTo(from.x + (left + step[0] - from.x) * s / steps, from.y + (on.y + step[1] - from.y) * s / steps);
        robot.delay(50);
      }
      look(1500);
      java.awt.image.BufferedImage shot = robot.createScreenCapture(new Rectangle(on, picture.getSize()));
      Graphics2D pen = shot.createGraphics();
      pen.setColor(Color.RED);
      pen.drawOval(left - on.x + step[0] - 4, step[1] - 4, 8, 8);
      javax.imageio.ImageIO.write(shot, "png", new File("/tmp/claude-1000/art-" + i++ + ".png"));
    }
  }

  /** The catalogue is opened from the menu, a name is typed, and the first game found is played. */
  private static void typeAName() throws Exception {
    look(1200);
    press(at(component(JMenu.class, "Emulator")));
    look(500);
    press(at(component(JMenuItem.class, "Game Browser...")));
    look(2500);
    press(at(component(JTextField.class, null)));
    look(400);
    type("dizzy\n");
    look(3200);
    // The first game found, where the gallery starts: twice, as anything is opened.
    Component gallery = component(JSplitPane.class, null);
    Point first = gallery.getLocationOnScreen();
    press(new Point(first.x + 370, first.y + 110));
    robot.delay(110);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.delay(60);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    look(16000);
  }

  /** Not a clip: what a window is made of, written out, to find what a clip has to press. */
  private static void look(String menuItem) throws Exception {
    press(at(component(JMenu.class, "Emulator")));
    look(500);
    press(at(component(JMenuItem.class, menuItem)));
    look(2500);
    press(at(component(JTextField.class, null)));
    type("dizzy\n");
    look(9000);
    for (Window window : Window.getWindows()) written(window, "");
  }

  private static void written(Component component, String indent) {
    if (!component.isShowing()) return;
    String says = component instanceof AbstractButton button ? button.getText() : component instanceof JLabel label ? label.getText()
        : component instanceof JInternalFrame frame ? frame.getTitle() : "";
    Point on = component.getLocationOnScreen();
    System.out.println(indent + component.getClass().getSimpleName() + " [" + on.x + "," + on.y + " " + component.getWidth() + "x" + component.getHeight() + "] "
        + (says == null ? "" : says) + (component instanceof JComponent j && j.getToolTipText() != null ? " (" + j.getToolTipText() + ")" : ""));
    if (component instanceof Container container && indent.length() < 60) for (Component inside : container.getComponents()) written(inside, indent + "  ");
  }

  /** The first thing of that kind on screen that says that, or the first of that kind when nothing is asked of what it says. */
  private static Component component(Class<? extends Component> kind, String saying) {
    return component(kind, saying, any -> true);
  }

  private static Component component(Class<? extends Component> kind, String saying, java.util.function.Predicate<Component> which) {
    for (Window window : Window.getWindows()) {
      Component found = inside(window, kind, saying, which);
      if (found != null) return found;
    }
    throw new IllegalStateException("nothing on screen is a " + kind.getSimpleName() + " saying " + saying);
  }

  private static Component inside(Component component, Class<? extends Component> kind, String saying) {
    return inside(component, kind, saying, any -> true);
  }

  private static Component inside(Component component, Class<? extends Component> kind, String saying, java.util.function.Predicate<Component> which) {
    if (!component.isShowing()) return null;
    String says = component instanceof AbstractButton button && button.getText() != null && !button.getText().isEmpty() ? button.getText()
        : component instanceof JComponent j ? j.getToolTipText() : null;
    if (kind.isInstance(component) && (saying == null || saying.equals(says)) && which.test(component)) return component;
    if (component instanceof JMenu menu && menu.isPopupMenuVisible()) {
      Component found = inside(menu.getPopupMenu(), kind, saying, which);
      if (found != null) return found;
    }
    if (component instanceof Container container) for (Component each : container.getComponents()) {
      Component found = inside(each, kind, saying, which);
      if (found != null) return found;
    }
    return null;
  }

  private static Point at(Component component) {
    Point on = component.getLocationOnScreen();
    return new Point(on.x + Math.min(component.getWidth() / 2, 60), on.y + component.getHeight() / 2);
  }

  /** A press on something of a Spectrum program: it looks at the buttons once a frame, and its pointer comes behind the hand. */
  private static void choose(Point where) {
    move(where.x, where.y);
    look(1400);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.delay(160 + hand.nextInt(60));
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
  }

  private static void press(Point where) {
    move(where.x, where.y);
    look(180);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.delay(70 + hand.nextInt(50));
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
  }

  /** Letters, digits, spaces and Enter, a key at a time and never at a steady pace. */
  private static void type(String text) {
    for (char letter : text.toCharArray()) {
      int key = letter == '\n' ? java.awt.event.KeyEvent.VK_ENTER : java.awt.event.KeyEvent.getExtendedKeyCodeForChar(letter);
      robot.keyPress(key);
      robot.delay(40 + hand.nextInt(40));
      robot.keyRelease(key);
      robot.delay(90 + hand.nextInt(140));
    }
  }

  /** A folder as a file manager shows it, with that file among others to be taken by the hand; where the file is. */
  private static Point folder(String name, File file, int x, int y) {
    JFrame folder = new JFrame(name);
    JList<String> files = new JList<>(new String[]{"notas.txt", file.getName(), "foto-0412.jpg"});
    files.setFont(files.getFont().deriveFont(15f));
    files.setFixedCellHeight(30);
    files.setSelectedIndex(1);
    files.setDragEnabled(true);
    files.setTransferHandler(new TransferHandler() {
      @Override
      public int getSourceActions(JComponent from) {
        return COPY;
      }

      @Override
      protected Transferable createTransferable(JComponent from) {
        return new Transferable() {
          public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
          }

          public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
          }

          public Object getTransferData(DataFlavor flavor) {
            return List.of(file);
          }
        };
      }
    });
    JLabel where = new JLabel("  " + name);
    where.setFont(where.getFont().deriveFont(Font.BOLD, 14f));
    where.setPreferredSize(new Dimension(10, 32));
    folder.add(where, BorderLayout.NORTH);
    folder.add(files, BorderLayout.CENTER);
    folder.setBounds(x, y, 300, 170);
    folder.setAlwaysOnTop(true);
    folder.setVisible(true);
    Rectangle cell = files.getCellBounds(1, 1);
    Point on = files.getLocationOnScreen();
    return new Point(on.x + 70, on.y + cell.y + cell.height / 2);
  }

  /** The pointer to that place as a hand takes it: along a slight curve, slow to start and to arrive, a little off each time. */
  /** Where the hand is: asking the display gives a stale answer under Xephyr, so the hand keeps it. */
  private static final Point at = new Point(640, 400);

  private static void handTo(int x, int y) {
    at.setLocation(x, y);
    robot.mouseMove(x, y);
  }

  private static void move(int x, int y) {
    Point from = new Point(at);
    double far = from.distance(x, y);
    int steps = (int) Math.max(18, far / 9);
    double bend = (hand.nextDouble() - 0.5) * far * 0.22;
    double midX = (from.x + x) / 2.0 - (y - from.y) / Math.max(1, far) * bend, midY = (from.y + y) / 2.0 + (x - from.x) / Math.max(1, far) * bend;
    for (int step = 1; step <= steps; step++) {
      double part = (double) step / steps, eased = part * part * (3 - 2 * part), rest = 1 - eased;
      handTo((int) (rest * rest * from.x + 2 * rest * eased * midX + eased * eased * x),
          (int) (rest * rest * from.y + 2 * rest * eased * midY + eased * eased * y));
      robot.delay(9 + hand.nextInt(6));
    }
  }

  /** The time somebody takes to look at what has just happened, never quite the same twice. */
  private static void look(int about) {
    robot.delay(about + hand.nextInt(Math.max(1, about / 5)));
  }
}
