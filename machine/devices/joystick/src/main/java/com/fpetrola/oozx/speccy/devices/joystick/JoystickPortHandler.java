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

package com.fpetrola.oozx.speccy.devices.joystick;

import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import com.fpetrola.oozx.speccy.modules.joystick.Joystick;

class JoystickPortHandler extends DefaultPortHandler {
  private Joystick joystick;

  public JoystickPortHandler(Joystick joystick) {
    super(true, false);
    this.joystick = joystick;
  }

  @Override
  public BusAnswer read(int port) {
    return joystick.kempstonRead(port);
  }

  @Override
  public void write(int port, byte value) {
  }
}
