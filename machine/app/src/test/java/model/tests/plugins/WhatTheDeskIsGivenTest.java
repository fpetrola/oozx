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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  /**
   * El jar lleva los plugins adentro y el primer arranque los pone, asi que lo que el
   * escritorio ofrece es lo que quedo puesto. Dicho asi y no contra una lista fija porque un
   * build que no encontro ninguno para meter adentro tiene que dar lo mismo: nada puesto, nada
   * que ofrecer.
   */
  @Test
  void theDeskIsGivenWhatTheJarBrought() {
    WhatIsPluggedIn has = Guice.createInjector(Plugins.asModule()).getInstance(WhatIsPluggedIn.class);

    assertEquals(Plugins.pluggedIn().isEmpty(), has.equipment().isEmpty(),
        "lo que trajo el jar es lo que el escritorio ofrece");
  }

  /**
   * Una vista viva: lo que se saca y lo que vuelve aparece sin que nadie vuelva a preguntarle
   * al mismo objeto. Se hace sobre uno que el jar trae, asi que no hace falta red ni un jar
   * construido al lado.
   */
  @Test
  void whatComesAndGoesIsThereWithoutAskingAgain() {
    WhatIsPluggedIn has = Guice.createInjector(Plugins.asModule()).getInstance(WhatIsPluggedIn.class);
    org.junit.jupiter.api.Assumptions.assumeTrue(named(has).contains(GAMES),
        "hace falta que el jar traiga tool-games adentro");

    assertTrue(Plugins.takeOut("tool-games"), "nada lo esta usando todavia");
    assertFalse(named(has).contains(GAMES), "se fue del escritorio");

    Plugins.add("tool-games");
    assertTrue(named(has).contains(GAMES), "el mismo objeto de antes, sin volver a pedir nada");
  }

  private static final String GAMES = "Game Browser";

  private static List<String> named(WhatIsPluggedIn has) {
    return has.deskWindows().stream().map(one -> one.name()).toList();
  }

}
