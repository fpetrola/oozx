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

import java.util.List;

public class StartupModuleAdapter implements StartupModule {
  private final RegisteredModule registeredModule;

  public StartupModuleAdapter(RegisteredModule registeredModule) {
    this.registeredModule = registeredModule;
  }

  @Override
  public List<?> getDependencies() {
    return registeredModule.dependencies;
  }

  @Override
  public Object getInitContext() {
    return registeredModule.initContext;
  }

  @Override
  public int initFn(Object initContext) {
    if (registeredModule.initFn != null)
      return registeredModule.initFn.apply(initContext);
    else
      return 0;
  }

  @Override
  public void endFn() {
    registeredModule.endFn.apply();
  }

  @Override
  public Object getId() {
    return registeredModule.module;
  }

  @Override
  public StartupManagerModule getStartupManagerModule() {
    return registeredModule.module;
  }
}
