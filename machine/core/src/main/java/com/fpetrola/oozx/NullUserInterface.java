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
import com.google.inject.Singleton;

/**
 * A machine with nobody watching. Everything is accepted and nothing is shown, which is what a
 * headless run wants — and it is the honest description of what most of the old static Ui did,
 * since almost every method there was an empty placeholder.
 */
@Singleton
public class NullUserInterface implements UserInterface {

  private boolean mouseGrabbed;

  public void error(Object level, String format, Object... args) {
  }


  public void menuActivate(MenuItem item, boolean active) {
  }

  public ConfirmJoystick confirmJoystick(Joystick.JoystickType joystick, int inputs) {
    return ConfirmJoystick.NONE;
  }

  public boolean isMousePresent() {
    return false;
  }

  public boolean isMouseGrabbed() {
    return mouseGrabbed;
  }

  public void grabMouse() {
    mouseGrabbed = true;
  }

  public void releaseMouse() {
    mouseGrabbed = false;
  }

  public void suspendMouse() {
  }

  public int widgetLevel() {
    return 0;
  }

  public void widgetKeyhandler(int value) {
  }

  public void popupMenu(int value) {
  }

  public void event() {
  }

  public void errorFrame() {
  }

  public void end() {
  }
}
