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

import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import dev.crystal.plugins.api.RoleInterface;

/**
 * Algo que se usa sobre la maquina de una ventana: un boton en su barra.
 * <p>
 * La ventana de maquina no sabe que herramientas hay: pone la pantalla, y cada plugin que trae
 * una herramienta pone su boton. Sin plugins queda la pantalla sola.
 * <p>
 * Una herramienta que deja algo hecho sobre la maquina -unos pokes aplicados- lo guarda con la
 * ventana y lo vuelve a hacer cuando la ventana se reabre.
 */
@RoleInterface
public interface MachineTool {

  javax.swing.Icon icon();

  String tooltip();

  void use(EmulatorWindow window);

  /** Donde va en la barra: las de la maquina primero, las de lo que se le hace despues. */
  default int place() {
    return 100;
  }

  /**
   * Su boton en la barra de esa ventana. Por defecto uno que muestra su icono y la usa; una
   * herramienta con estado -la pausa, el turbo- arma el suyo y lo mantiene al dia.
   */
  default javax.swing.AbstractButton button(EmulatorWindow window) {
    javax.swing.JButton button = new javax.swing.JButton(icon());
    button.setToolTipText(tooltip());
    button.addActionListener(pressed -> use(window));
    return button;
  }

  /** Lo que agrega al menu del clic derecho sobre la pantalla, o nada. */
  default javax.swing.JMenuItem menuItem(EmulatorWindow window) {
    return null;
  }

  /** Lo que muestra en la barra de estado de esa ventana, o nada. */
  default javax.swing.JComponent status(EmulatorWindow window) {
    return null;
  }

  /** Lo que esta herramienta dejo hecho en esa ventana, escrito en lo que se guarda de ella. */
  default void remember(EmulatorWindow window, OOZxConfiguration.WindowState into) {
  }

  /** Lo mismo al reves, cuando la ventana se vuelve a abrir. */
  default void restore(EmulatorWindow window, OOZxConfiguration.WindowState from) {
  }
}
