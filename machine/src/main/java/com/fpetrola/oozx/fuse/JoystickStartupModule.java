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

import com.fpetrola.oozx.LibspectrumStartupModule;
import com.fpetrola.oozx.SetUidStartupModule;
import com.fpetrola.oozx.StartupManagerModule;

public class JoystickStartupModule extends AbstractStartupModule {
  public JoystickStartupModule() {
    super(LibspectrumStartupModule.class, SetUidStartupModule.class);
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    return Joystick.joystickInit(initContext);
  }

  public void endFn() {
    Joystick.end();
  }

  public StartupManagerModule getStartupManagerModule() {
    return StartupManagerModule.JOYSTICK;
  }
}
