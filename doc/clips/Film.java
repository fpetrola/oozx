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
    while (!rolling.exists()) Thread.sleep(200);
    robot = new Robot();
    switch (args[0]) {
      case "any-name" -> anyName(new File(args[1]), args[2]);
      case "type-a-name" -> typeAName();
      case "an-hour" -> anHour(new File(args[1]), args[2]);
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
    for (Window window : Window.getWindows()) {
      Component found = inside(window, kind, saying);
      if (found != null) return found;
    }
    throw new IllegalStateException("nothing on screen is a " + kind.getSimpleName() + " saying " + saying);
  }

  private static Component inside(Component component, Class<? extends Component> kind, String saying) {
    if (!component.isShowing()) return null;
    String says = component instanceof AbstractButton button && button.getText() != null && !button.getText().isEmpty() ? button.getText()
        : component instanceof JComponent j ? j.getToolTipText() : null;
    if (kind.isInstance(component) && (saying == null || saying.equals(says))) return component;
    if (component instanceof JMenu menu && menu.isPopupMenuVisible()) {
      Component found = inside(menu.getPopupMenu(), kind, saying);
      if (found != null) return found;
    }
    if (component instanceof Container container) for (Component each : container.getComponents()) {
      Component found = inside(each, kind, saying);
      if (found != null) return found;
    }
    return null;
  }

  private static Point at(Component component) {
    Point on = component.getLocationOnScreen();
    return new Point(on.x + Math.min(component.getWidth() / 2, 60), on.y + component.getHeight() / 2);
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
  private static void move(int x, int y) {
    Point from = MouseInfo.getPointerInfo().getLocation();
    double far = from.distance(x, y);
    int steps = (int) Math.max(18, far / 9);
    double bend = (hand.nextDouble() - 0.5) * far * 0.22;
    double midX = (from.x + x) / 2.0 - (y - from.y) / Math.max(1, far) * bend, midY = (from.y + y) / 2.0 + (x - from.x) / Math.max(1, far) * bend;
    for (int step = 1; step <= steps; step++) {
      double part = (double) step / steps, eased = part * part * (3 - 2 * part), rest = 1 - eased;
      robot.mouseMove((int) (rest * rest * from.x + 2 * rest * eased * midX + eased * eased * x),
          (int) (rest * rest * from.y + 2 * rest * eased * midY + eased * eased * y));
      robot.delay(9 + hand.nextInt(6));
    }
  }

  /** The time somebody takes to look at what has just happened, never quite the same twice. */
  private static void look(int about) {
    robot.delay(about + hand.nextInt(Math.max(1, about / 5)));
  }
}
