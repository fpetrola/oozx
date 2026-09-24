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

package com.fpetrola.oozx.speccy.modules.snapshot;

import com.fpetrola.emulation.helpers.snapshots.SpectrumState;

/**
 * Una parte de la maquina que se lleva su estado adentro de un snapshot y lo sabe poner de vuelta.
 * <p>
 * Quien abre un snapshot no sabe de que partes esta hecha la maquina: le pasa lo que el archivo
 * trae a cada una que diga que tiene algo que restaurar. Un chip que viene en un plugin restaura
 * lo suyo sin que el emulador sepa que existe.
 */
public interface RestoredFromASnapshot {

  void restore(SpectrumState snapshot);
}
