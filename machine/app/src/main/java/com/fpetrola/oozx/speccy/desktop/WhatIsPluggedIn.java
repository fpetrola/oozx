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

package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.emulation.helpers.snapshots.SnapshotFile;
import com.fpetrola.oozx.speccy.devices.DeskEquipment;
import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.screen.ScreenEffect;
import com.google.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Lo que hay para enchufar, recibido y no buscado.
 * <p>
 * Cada uno de estos conjuntos es una vista viva: lo que llega aparece ahi sin que nadie vuelva a
 * preguntar, y lo que se saca deja de estar. Antes el escritorio le pedia cada lista a quien
 * carga los plugins y se guardaba una copia, asi que tenia que acordarse de volver a pedirla
 * cada vez que la carpeta cambiaba - y cuando se olvidaba, el menu mentia.
 */
public class WhatIsPluggedIn {

  private final Set<Equipment> equipment;
  private final Set<DeskEquipment> deskWindows;
  private final Set<ScreenEffect> effects;
  private final Set<SnapshotFile> formats;

  @Inject
  public WhatIsPluggedIn(Set<Equipment> equipment, Set<DeskEquipment> deskWindows,
      Set<ScreenEffect> effects, Set<SnapshotFile> formats) {
    this.equipment = equipment;
    this.deskWindows = deskWindows;
    this.effects = effects;
    this.formats = formats;
  }

  /** En el orden en que el menu las muestra, que es el unico orden que alguien espera. */
  public List<Equipment> equipment() {
    return equipment.stream().sorted(Comparator.comparing(Equipment::name)).toList();
  }

  public List<DeskEquipment> deskWindows() {
    return List.copyOf(deskWindows);
  }

  public List<ScreenEffect> effects() {
    return List.copyOf(effects);
  }

  public List<SnapshotFile> formats() {
    return List.copyOf(formats);
  }
}
