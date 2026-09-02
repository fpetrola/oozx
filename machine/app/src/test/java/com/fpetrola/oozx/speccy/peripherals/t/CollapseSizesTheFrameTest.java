package com.fpetrola.oozx.speccy.peripherals.t;

import com.fpetrola.oozx.speccy.devices.printer.PrinterInternalFrame;

import org.junit.jupiter.api.Test;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Collapsing folds the window, not just what is inside it.
 * <p>
 * A window that has never been clipped onto anything still says which side it would clip onto, and
 * taking that as being attached sent collapsing through the path that places a window against its
 * machine - which returns at once when there is no machine. So the content folded away and the
 * frame stayed exactly as big as it was, with the buttons floating in a window of empty space.
 * Every one of these windows had it, and it only shows while the window is loose.
 */
class CollapseSizesTheFrameTest {

  private JDesktopPane desktop() {
    JDesktopPane desktop = new JDesktopPane();
    desktop.setSize(1200, 800);
    return desktop;
  }

  /**
   * Lets the resize be delivered. A component's size change POSTS an event rather than calling the
   * listener there and then, and the frame remembers the size it was given from that listener - so
   * a test that never lets the queue run reads back the size the window had before.
   */
  private static void settle() {
    try {
      javax.swing.SwingUtilities.invokeAndWait(() -> { });
    } catch (Exception interrupted) {
      throw new IllegalStateException(interrupted);
    }
  }

  private void collapses(AttachedFrame window) {
    JDesktopPane desktop = desktop();
    desktop.add(window);
    window.setVisible(true);
    window.setSize(320, 460);
    settle();

    window.setCompact(false);
    settle();
    int expanded = window.getHeight();
    assertTrue(expanded > 200, window.getTitle() + " did not open to a usable size: " + expanded);

    window.setCompact(true);
    settle();
    assertTrue(window.getHeight() < 140,
        window.getTitle() + " folded its contents away and stayed " + window.getHeight() + " tall");

    window.setCompact(false);
    settle();
    assertEquals(expanded, window.getHeight(),
        window.getTitle() + " did not open back to the size it was given");
  }

  /**
   * The case the first fix missed, which is the one that happens: a window clipped onto the side
   * of a machine. Placing one against a side only moved it, on purpose - a toolbar stretched to a
   * machine's height is a column of empty space - and never resizing at all meant collapsing did
   * nothing to the frame either.
   */
  @Test
  void aWindowClippedToTheSideFoldsToo() throws Exception {
    JDesktopPane desktop = desktop();
    JInternalFrame machine = new JInternalFrame("machine");
    machine.setBounds(400, 100, 400, 300);
    desktop.add(machine);
    machine.setVisible(true);

    PrinterInternalFrame printer = new PrinterInternalFrame();
    desktop.add(printer);
    printer.setVisible(true);
    printer.setSize(320, 460);
    settle();
    printer.attachTo(machine);
    // A side is chosen by dragging the window there, which needs events and a settling delay; the
    // state that produces is this, and it is the state being tested.
    java.lang.reflect.Field side = AttachedFrame.class.getDeclaredField("dock");
    side.setAccessible(true);
    side.set(printer, AttachedFrame.Dock.RIGHT);

    printer.setCompact(false);
    assertTrue(printer.getHeight() > 200, "it did not open: " + printer.getHeight());
    int wide = printer.getWidth();

    printer.setCompact(true);
    assertTrue(printer.getHeight() < 140,
        "clipped to the side, it folded its contents away and stayed " + printer.getHeight() + " tall");
    assertEquals(wide, printer.getWidth(), "collapsing changed its width, which is not its business");
  }

  @Test
  void thePrinterFolds() {
    collapses(new PrinterInternalFrame());
  }

  /** The same base class, so the same bug: worth saying out loud that it is fixed for all of them. */
  @Test
  void theRzxPlayerFolds() {
    collapses(new RzxPlayerInternalFrame(1, one -> null, (one, session) -> { }));
  }
}
