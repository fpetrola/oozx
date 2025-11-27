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

package com.fpetrola.oozx.fuse.startup;

import com.fpetrola.oozx.Ui;
import com.fpetrola.oozx.UiError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StartupManager {
  private List<StartupModule> registeredModules;
  private List<Runnable> endFunctions;

  // Initialize the startup manager itself
  public void init() {
    registeredModules = new ArrayList<>();
    endFunctions = new ArrayList<>();
  }

  // Clean up the startup manager
  private void end() {
    registeredModules.clear();
    endFunctions.clear();
  }

  public void register(StartupModule e) {
    registeredModules.add(e);
  }

  // Remove a dependency from all modules
  private void removeDependency(StartupModule module) {
    for (StartupModule registeredModule : registeredModules) {
      registeredModule.removeDependency(module);
    }
  }

  // Run all the registered init functions in the right order
  public int run() {
    boolean progressMade;
    int error;

    // Loop until we can't make any more progress
    do {
      progressMade = false;
      Iterator<StartupModule> iterator = registeredModules.iterator();

      while (iterator.hasNext()) {
        StartupModule registeredModule = iterator.next();

        if (registeredModule.getDependencies().isEmpty()) {
          error = registeredModule.initFn(registeredModule.getInitContext());
          if (error != 0)
            return error;

          endFunctions.add(registeredModule::endFn);

          removeDependency(registeredModule);

          iterator.remove();
          progressMade = true;
        }
      }
    } while (progressMade && !registeredModules.isEmpty());

    // If there are still any modules left to be called, that's an error
    if (!registeredModules.isEmpty()) {
      Ui.error(UiError.ERROR, "%d startup modules could not be called", registeredModules.size());
      return 1;
    }

    return 0;
  }

  // Run all the end functions in inverse order of the init functions
  public void runEnd() {
    for (int i = endFunctions.size() - 1; i >= 0; i--) {
      endFunctions.get(i).run();
    }
    end();
  }
}
