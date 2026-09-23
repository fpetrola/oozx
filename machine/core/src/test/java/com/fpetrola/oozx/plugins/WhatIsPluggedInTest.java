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


package com.fpetrola.oozx.plugins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cual es el mismo plugin cuando el nombre del archivo cambia. Lo demas de esta pregunta - que
 * hay puesto, que hay en la carpeta, si lo publicado es mas nuevo - lo contesta quien los
 * maneja, y esta clase ya no lo repite.
 */
class WhatIsPluggedInTest {

  /** The same board at another version is the same board, which is what the file name hides. */
  @Test
  void aBoardIsTheSameBoardAtAnotherVersion() {
    assertEquals("tool-calls", PluginReleases.whichBoard("tool-calls-0.0.2-alu-SNAPSHOT.jar"));
    assertEquals("tool-calls", PluginReleases.whichBoard("tool-calls-0.0.3.jar"));
    assertEquals("device-mouse", PluginReleases.whichBoard("device-mouse.jar"));
  }
}
