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

package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.emulation.helpers.snapshots.SnapshotException;
import dev.crystal.plugins.api.RoleInterface;

/**
 * Una ventana de maquina con lo que tiene alrededor, como un archivo, y de vuelta: lo que deja
 * cada maquina como estaba al cerrar el escritorio y volver a abrirlo.
 * <p>
 * El escritorio no sabe que hay en el archivo ni como se escribe: lo guarda donde guarda lo suyo,
 * y al volver lo abre como abre cualquier archivo. La maquina sale de ahi, y esto le devuelve a su
 * ventana el resto.
 */
@RoleInterface
public interface Session {

  /** Como terminan los archivos que escribe. */
  String extension();

  /** La maquina de esa ventana y lo que tiene alrededor. */
  byte[] write(EmulatorWindow window) throws SnapshotException;

  /** Sobre la maquina ya leida de ese archivo, lo que su ventana tenia alrededor. */
  void bringBack(byte[] file, EmulatorWindow window);
}
