/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package model.tests.plugins;

import com.fpetrola.oozx.plugins.Plugins;
import com.fpetrola.oozx.speccy.desktop.WhatIsPluggedIn;
import com.google.inject.Guice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que el escritorio tiene para ofrecer sale de un injector, no de una busqueda.
 * <p>
 * Es el primer test de como arranca el escritorio. No necesita pantalla porque lo que se prueba
 * no es la ventana: es que lo que hay enchufado llegue solo a quien lo va a mostrar.
 */
class WhatTheDeskIsGivenTest {

  private String previousHome;
  private String previousPlugins;

  @BeforeEach
  void aHomeOfItsOwn(@TempDir Path home) throws IOException {
    previousHome = System.getProperty("user.home");
    previousPlugins = System.getProperty("oozx.plugins");
    System.setProperty("user.home", home.toString());
    System.clearProperty("oozx.plugins");
    Files.createDirectories(Plugins.folder());
  }

  @AfterEach
  void putItBack() {
    System.setProperty("user.home", previousHome);
    if (previousPlugins == null) {
      System.clearProperty("oozx.plugins");
    } else {
      System.setProperty("oozx.plugins", previousPlugins);
    }
  }

  @Test
  void withNothingPluggedInThereIsNothingToOffer() {
    WhatIsPluggedIn has = Guice.createInjector(Plugins.asModule()).getInstance(WhatIsPluggedIn.class);

    assertEquals(List.of(), has.equipment(), "no hay placas sin un jar que las traiga");
    assertEquals(List.of(), has.deskWindows());
    assertTrue(has.formats().isEmpty(), "el lector que trae el build no es un plugin");
  }

  /** Una vista viva: lo que llega aparece sin que nadie vuelva a preguntar. */
  @Test
  void whatArrivesIsThereWithoutAskingAgain() throws IOException {
    WhatIsPluggedIn has = Guice.createInjector(Plugins.asModule()).getInstance(WhatIsPluggedIn.class);
    assertEquals(List.of(), has.deskWindows());

    Path jar = aRealPlugin();
    Files.copy(jar, Plugins.folder().resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    Plugins.readWhatArrived();

    assertEquals(List.of("Game Browser"),
        has.deskWindows().stream().map(one -> one.name()).toList(),
        "el mismo objeto de antes, sin volver a pedir nada");
  }

  /**
   * Un plugin de verdad, construido. No hay ninguno en este arbol -los plugins son otro
   * repositorio- asi que se usa el de al lado si esta construido, y si no el test se saltea
   * diciendo por que: probar esto contra un jar inventado no probaria nada.
   */
  private static Path aRealPlugin() throws IOException {
    Path built = null;
    for (Path up = Path.of(System.getProperty("user.dir")); up != null && built == null;
         up = up.getParent()) {
      Path beside = up.resolveSibling("oozx-plugins/tools/games/target");
      if (Files.isDirectory(beside)) {
        built = newest(beside);
      }
    }
    org.junit.jupiter.api.Assumptions.assumeTrue(built != null,
        "hace falta tool-games construido en el repositorio de plugins de al lado");
    return built;
  }

  private static Path newest(Path target) throws IOException {
    try (java.util.stream.Stream<Path> jars = Files.list(target)) {
      return jars.filter(one -> one.getFileName().toString().startsWith("tool-games-")
              && one.getFileName().toString().endsWith(".jar")).findFirst().orElse(null);
    }
  }
}
