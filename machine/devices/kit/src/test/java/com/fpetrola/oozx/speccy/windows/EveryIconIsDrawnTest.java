package com.fpetrola.oozx.speccy.windows;

import dev.plugdesk.desk.Widgets;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every SVG the windows of the emulator and of the devices ship is drawn by the desk's renderer, and draws something. */
class EveryIconIsDrawnTest {

  @Test
  void everySvgOfTheWindowsDrawsSomething() throws Exception {
    assertEquals(List.of(), blank(Path.of("../../ui/src/main/resources/icons")));
  }

  @Test
  void everySvgOfTheKitDrawsSomething() throws Exception {
    assertEquals(List.of(), blank(Path.of("src/main/resources/icons")));
  }

  /** The icons of that folder that come out with nothing painted. */
  private static List<String> blank(Path folder) throws Exception {
    try (Stream<Path> files = Files.list(folder)) {
      return files.map(file -> file.getFileName().toString()).filter(name -> name.endsWith(".svg")).sorted()
          .filter(name -> painted((BufferedImage) Widgets.loadIcon(name).getImage()) == 0).toList();
    }
  }

  private static int painted(BufferedImage image) {
    int count = 0;
    for (int y = 0; y < image.getHeight(); y++)
      for (int x = 0; x < image.getWidth(); x++)
        if ((image.getRGB(x, y) >>> 24) > 0) count++;
    return count;
  }
}
