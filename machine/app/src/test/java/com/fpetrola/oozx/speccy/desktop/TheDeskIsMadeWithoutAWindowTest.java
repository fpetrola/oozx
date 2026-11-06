package com.fpetrola.oozx.speccy.desktop;

import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The desk is a panel the base puts in a window, not the window: so it is built where there is no screen, and what it
 * offers can be asked of the real one rather than of a copy.
 */
class TheDeskIsMadeWithoutAWindowTest {

  @Test
  void theRealDeskIsBuiltAndOffersItsMenus() throws Exception {
    AtomicReference<ZXSpectrumDesktopApp> made = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> made.set(new ZXSpectrumDesktopApp((file, machine) -> null, session -> null)));
    ZXSpectrumDesktopApp desk = made.get();
    JMenuBar menus = desk.menus();

    assertEquals(null, SwingUtilities.getWindowAncestor(desk), "in no window: the base makes the window");
    assertEquals(List.of("File", "Emulator", "Window"), names(menus).subList(0, 3));
    assertTrue(items(menus.getMenu(0)).containsAll(List.of("Open...", "Save State...", "Load State...", "Quit")));
    assertTrue(items(menus.getMenu(1)).containsAll(List.of("New Emulator", "Plugins...")));
    assertEquals(List.of("TV"), items(menus.getMenu(2)),
        "the window menu, the looks and the help are plugins, and with plugins off they are not here");
    assertTrue(!names(menus).contains("Help") && !names(menus).contains("Discover"));
  }

  private static List<String> names(JMenuBar bar) {
    return Stream.of(bar.getComponents()).map(menu -> ((JMenu) menu).getText()).toList();
  }

  private static List<String> items(JMenu menu) {
    return Stream.of(menu.getMenuComponents()).filter(JMenuItem.class::isInstance).map(Component.class::cast)
        .map(item -> ((JMenuItem) item).getText()).toList();
  }
}
