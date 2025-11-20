/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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
