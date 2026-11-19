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
    File rolling = new File(args[3]);
    while (!rolling.exists()) Thread.sleep(200);
    robot = new Robot();
    switch (args[0]) {
      case "any-name" -> anyName(new File(args[1]), args[2]);
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
