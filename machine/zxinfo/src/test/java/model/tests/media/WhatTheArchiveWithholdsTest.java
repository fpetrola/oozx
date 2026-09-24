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


package model.tests.media;

import com.fpetrola.oozx.api.ZxInfoApiHandler;
import com.fpetrola.oozx.speccy.media.DownloadAndUnzip;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Que el archivo no reparte todo lo que lista, y que quien baja los archivos se entera.
 * <p>
 * Quien los baja no sabe que quiere decir /denied/: se lo dice este modulo, que es de donde
 * salen esos enlaces. La prueba vive aca por lo mismo, y prueba las dos mitades: que lo dicho
 * llego, y que el que baja lo usa.
 */
class WhatTheArchiveWithholdsTest {

  private static final Function<String, String> WHOLE_URL = url -> url;

  /** Tocar la clase del catalogo es lo que se lo cuenta al que baja. */
  private static void theCatalogueIsAround() {
    ZxInfoApiHandler.denied("");
  }

  @Test
  void what_the_archive_may_not_hand_over_comes_last() {
    theCatalogueIsAround();
    String denied = "https://zxinfo.dk/media/denied/entries/0011243/Game.tzx.zip";
    String plain = "https://zxinfo.dk/media/pub/sinclair/games/g/Game.tap.zip";
    assertEquals(plain, DownloadAndUnzip.preferred(List.of(denied, plain), WHOLE_URL),
        "eligio el que el archivo no va a entregar");
    // Sin nada mas ofrecido sigue siendo la respuesta: mejor una negativa que se explica que
    // hacer de cuenta que la entrada no tiene nada.
    assertEquals(denied, DownloadAndUnzip.preferred(List.of(denied), WHOLE_URL));
  }

  @Test
  void what_the_archive_withholds_is_known_before_asking_for_it() {
    theCatalogueIsAround();
    assertFalse(DownloadAndUnzip.available(
        "https://zxinfo.dk/media/denied/entries/0011243/DizzyCollection.tzx.zip"));
    assertTrue(DownloadAndUnzip.available(
        "https://zxinfo.dk/media/pub/sinclair/games/d/DizzyCollection.tzx.zip"));
    assertFalse(DownloadAndUnzip.available(null), "nada no esta disponible tampoco");
  }
}
