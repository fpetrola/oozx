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

/**
 * A disk interface as the desk sees it: some drives, a ROM it may or may not have found, and a
 * button - the +D's NMI, the Beta's boot. What the bay window shows, whichever board is behind it.
 */
public interface DiskInterface {
  int drives();

  Fdd drive(int which);

  void insert(int which, Disk disk);

  /** A blank disk of the shape this interface formats. */
  void insertBlank(int which) throws DiskException;

  void eject(int which);

  boolean isAvailable();

  boolean isPaged();

  String romName();

  /** What the button is called, or null for a board without one. */
  String buttonName();

  String buttonTip();

  void button();

  /** The file extensions of the images this interface reads. */
  String[] imageExtensions();

  /**
   * The shape of a board before there is one - how many drives, which images, what its button
   * is called - for a window that has to be drawn before it is clipped onto a machine.
   */
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

      public String romName() {
        return null;
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
