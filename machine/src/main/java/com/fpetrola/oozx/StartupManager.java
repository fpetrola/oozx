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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StartupManager {

    private static List<RegisteredModule> registeredModules;
    private static List<StartupManagerEndFn> endFunctions;

    // Initialize the startup manager itself
    public static void init() {
        registeredModules = new ArrayList<>();
        endFunctions = new ArrayList<>();
    }

    // Clean up the startup manager
    private static void end() {
        registeredModules.clear();
        registeredModules = null;
        endFunctions.clear();
        endFunctions = null;
    }

    // Register a module with the startup manager
    public static void register(StartupManagerModule module, StartupManagerModule[] dependencies,
                               StartupManagerInitFn initFn, Object initContext, StartupManagerEndFn endFn) {
        RegisteredModule registeredModule = new RegisteredModule(module, dependencies, initFn, initContext, endFn);
        registeredModules.add(registeredModule);
    }

    // Register a module with no dependencies with the startup manager
    public static void registerNoDependencies(StartupManagerModule module,
                                             StartupManagerInitFn initFn,
                                             Object initContext,
                                             StartupManagerEndFn endFn) {
        register(module, null, initFn, initContext, endFn);
    }

    // Remove a dependency from all modules
    private static void removeDependency(StartupManagerModule module) {
        for (RegisteredModule registeredModule : registeredModules) {
            registeredModule.dependencies.remove(module);
        }
    }

    // Run all the registered init functions in the right order
    public static int run() {
        boolean progressMade;
        int error;

        // Loop until we can't make any more progress
        do {
            progressMade = false;
            Iterator<RegisteredModule> iterator = registeredModules.iterator();

            while (iterator.hasNext()) {
                RegisteredModule registeredModule = iterator.next();

                if (registeredModule.dependencies.isEmpty()) {
                    if (registeredModule.initFn != null) {
                        error = registeredModule.initFn.apply(registeredModule.initContext);
                        if (error != 0) return error;
                    }

                    if (registeredModule.endFn != null) {
                        endFunctions.add(registeredModule.endFn);
                    }

                    removeDependency(registeredModule.module);

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
    public static void runEnd() {
        for (int i = endFunctions.size() - 1; i >= 0; i--) {
            endFunctions.get(i).apply();
        }
        end();
    }
}
