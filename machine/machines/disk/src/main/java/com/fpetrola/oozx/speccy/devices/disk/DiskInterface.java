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
package com.fpetrola.oozx.speccy.devices.disk;

/** Common surface a disk interface presents to the UI: drives, ROM availability, and a
 * board-specific button (NMI on the +D, boot on the Beta), regardless of which board it is. */
public interface DiskInterface {
  int drives();

  Fdd drive(int which);

  void insert(int which, Disk disk);

  /** Inserts a freshly blanked disk in this interface's native format. */
  void insertBlank(int which) throws DiskException;

  void eject(int which);

  boolean isAvailable();

  boolean isPaged();

  /** This board's button label, or null if it has none. */
  String buttonName();

  String buttonTip();

  void button();

  /** File extensions this interface recognises as disk images. */
  String[] imageExtensions();

  /** A placeholder DiskInterface describing a board's shape (drives, extensions, button)
   * before any real board exists, for UI drawn ahead of being clipped to a machine. */
  static DiskInterface shape(int drives, String buttonName, String buttonTip, String... extensions) {
    return new DiskInterface() {
      public int drives() {
        return drives;
      }

      public Fdd drive(int which) {
        throw new IllegalStateException("only a shape");
      }

      public void insert(int which, Disk disk) {
      }

      public void insertBlank(int which) {
      }

      public void eject(int which) {
      }

      public boolean isAvailable() {
        return false;
      }

      public boolean isPaged() {
        return false;
      }


      public String buttonName() {
        return buttonName;
      }

      public String buttonTip() {
        return buttonTip;
      }

      public void button() {
      }

      public String[] imageExtensions() {
        return extensions;
      }
    };
  }
}
