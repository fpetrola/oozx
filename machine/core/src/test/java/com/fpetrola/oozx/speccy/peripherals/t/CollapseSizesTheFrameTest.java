package com.fpetrola.oozx.speccy.peripherals.t;

import org.junit.jupiter.api.Test;

import javax.swing.JDesktopPane;

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

  private void collapses(AttachedFrame window) {
    JDesktopPane desktop = desktop();
    desktop.add(window);
    window.setVisible(true);
    window.setSize(320, 460);

    window.setCompact(false);
    int expanded = window.getHeight();
    assertTrue(expanded > 200, window.getTitle() + " did not open to a usable size: " + expanded);

    window.setCompact(true);
    assertTrue(window.getHeight() < 140,
        window.getTitle() + " folded its contents away and stayed " + window.getHeight() + " tall");

    window.setCompact(false);
    assertEquals(expanded, window.getHeight(),
        window.getTitle() + " did not open back to the size it was given");
  }

  @Test
  void thePrinterFolds() {
    collapses(new PrinterInternalFrame(window -> null));
  }

  /** The same base class, so the same bug: worth saying out loud that it is fixed for all of them. */
  @Test
  void theRzxPlayerFolds() {
    collapses(new RzxPlayerInternalFrame(1, one -> null, (one, session) -> { }));
  }
}
