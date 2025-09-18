/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.windows;

/**
 * A window showing a machine, which is what an {@link AttachedFrame} clips onto.
 * <p>
 * The frame used to look for the application's own window class, so the thing underneath knew the
 * name of the thing on top. It only ever needed to know which windows are machines, and a machine
 * window is in a position to say so.
 */
public interface MachineWindow {
  /**
   * What this window shows the machine on, for a peripheral that has to work on top of the picture
   * - a mouse, a light gun, anything that points at the screen. The frame underneath cannot know
   * what an emulator core is, and does not have to: it needs the component and nothing else.
   */
  javax.swing.JComponent picture();
}
