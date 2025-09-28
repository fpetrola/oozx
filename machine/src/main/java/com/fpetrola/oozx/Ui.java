/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.Joystick;

public class Ui {
  public static int widgetLevel;

  public static void widgetKeyhandler(int value) {

  }

  public static void popupMenu(int value) {

  }

  // Error levels
  public enum UIErrorLevel {
    UI_ERROR_INFO,
    UI_ERROR_WARNING,
    UI_ERROR_ERROR
  }

  public enum UIConfirmJoystick {
    UI_CONFIRM_JOYSTICK_NONE,
    UI_CONFIRM_JOYSTICK_KEYBOARD,
    UI_CONFIRM_JOYSTICK_JOYSTICK_1,
    UI_CONFIRM_JOYSTICK_JOYSTICK_2
  }

  // Confirm joystick (from Joystick.java)
  public static UIConfirmJoystick confirmJoystick(Joystick.JoystickType joystick, int inputs) {
    // Placeholder: Show joystick configuration dialog
    return UIConfirmJoystick.UI_CONFIRM_JOYSTICK_NONE;
  }

  public static boolean mousePresent;
  public static boolean mouseGrabbed;

  public static int init(int argc, String[] argv) {
    return 0;
  }

  public static void error(Object error, String s, int size) {

  }

  public static void event() {

  }

  public static void errorFrame() {

  }

  public static void error(Object error, String s) {

  }

  public static void error(String error, String s, String id) {

  }

  public static void error(String error, String s, String filename, int length, int expectedLength) {

  }

  public static void end() {
  }

  public static boolean mouseGrab(boolean i) {
    return i;
  }

  public static void error(Object error, String s, Object version, String libspectrumMinVersion) {

  }

  public static void menuActivate(MenuItem menuItem, boolean cartridge) {

  }

  public static boolean mouseRelease(boolean b) {
    return false;
  }

  public static void mouseSuspend() {

  }

  public enum MenuItem {MEDIA_CARTRIDGE_DOCK, MEDIA_CARTRIDGE_IF2, MEDIA_IDE, MEDIA_IDE_SIMPLE8BIT, MEDIA_IDE_ZXATASP, MEDIA_IDE_ZXCF, MEDIA_IDE_DIVIDE, MEDIA_IDE_DIVMMC, MEDIA_IDE_ZXMMC, MEDIA_IF1, MEDIA_CARTRIDGE}
}
