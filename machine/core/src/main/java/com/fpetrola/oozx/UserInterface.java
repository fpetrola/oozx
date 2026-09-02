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

package com.fpetrola.oozx;

import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.peripherals.EmulatorControl;

/**
 * What the machine asks of whoever is showing it.
 * <p>
 * This used to be a class of statics, which meant the emulator core reached out to the desktop
 * by name and a headless run needed Swing on the classpath to say nothing at all. Asking for
 * one of these instead lets a test take {@link NullUserInterface} and a window take
 * {@link SwingUserInterface}.
 */
public interface UserInterface {

  /**
   * Reports a problem, printf style. This replaces five overloads that differed only in how
   * many arguments they took, and it keeps {@code level} as an Object because the code still
   * has two error enums; unifying those is a separate cleanup.
   */
  void error(Object level, String format, Object... args);


  void menuActivate(MenuItem item, boolean active);

  ConfirmJoystick confirmJoystick(Joystick.JoystickType joystick, int inputs);

  // The pointer. Grabbing used to be a static field the callers assigned from the result of a
  // static method; now the answer and the state live in the same object.
  boolean isMousePresent();

  boolean isMouseGrabbed();

  void grabMouse();

  void releaseMouse();

  void suspendMouse();

  int widgetLevel();

  void widgetKeyhandler(int value);

  void popupMenu(int value);

  void event();

  void errorFrame();

  void end();

  enum ConfirmJoystick {
    NONE, KEYBOARD, JOYSTICK_1, JOYSTICK_2
  }

  enum MenuItem {
    MEDIA_CARTRIDGE_DOCK, MEDIA_CARTRIDGE_IF2, MEDIA_IDE, MEDIA_IDE_SIMPLE8BIT, MEDIA_IDE_ZXATASP,
    MEDIA_IDE_ZXCF, MEDIA_IDE_DIVIDE, MEDIA_IDE_DIVMMC, MEDIA_IDE_ZXMMC, MEDIA_IF1,
    MEDIA_CARTRIDGE
  }
}
