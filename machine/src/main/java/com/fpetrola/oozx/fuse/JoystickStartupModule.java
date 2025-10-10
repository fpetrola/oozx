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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.oozx.fuse.peripherals.Peripheral;

public class JoystickStartupModule extends AbstractStartupModule {
  Peripheral kempstonStrictPeriph = new Peripheral(new boolean[]{Settings.current.joyKempston}, new JoystickPortHandler(0x00e0, 0x0000));
  Peripheral kempstonLoosePeriph = new Peripheral(new boolean[]{Settings.current.joyKempston}, new JoystickPortHandler(0x0020, 0x0000));

  public JoystickStartupModule() {
    super(LibspectrumStartupModule.class, SetUidStartupModule.class);
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    Joystick.joysticksSupported = UiJoystick.init();
    Joystick.kempstonValue = Joystick.timex1Value = Joystick.timex2Value = 0x00;
    Joystick.fullerValue = (byte) 0xff;

    Module.register(Joystick.joystickModuleInfo);
    Periph.register(Periph.Type.KEMPSTON, kempstonStrictPeriph);
    Periph.register(Periph.Type.KEMPSTON_LOOSE, kempstonLoosePeriph);

    return 0;
  }

  public void endFn() {
    Joystick.end();
  }

}
