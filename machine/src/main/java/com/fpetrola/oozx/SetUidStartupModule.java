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

import com.fpetrola.oozx.fuse.AbstractStartupModule;

import java.util.ArrayList;
import java.util.List;

public class SetUidStartupModule extends AbstractStartupModule {
  public SetUidStartupModule() {
    super(StartupManagerModule.DISPLAY, StartupManagerModule.LIBSPECTRUM);
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    return Fuse.setuidInit(initContext);
  }

  public void endFn() {
  }

  @Override
  public StartupManagerModule getStartupManagerModule() {
    return StartupManagerModule.SETUID;
  }
}
