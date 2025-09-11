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

package com.fpetrola.oozx;import java.util.*;

// Assuming LibspectrumSnap is a ported class from libspectrum.h
// Functional interfaces replace function pointers

@FunctionalInterface
interface ModuleResetFn {
    void apply(int hardReset);
}

@FunctionalInterface
interface ModuleRomcsFn {
    void apply();
}

@FunctionalInterface
interface ModuleSnapshotEnabledFn {
    void apply(LibspectrumSnap snap);
}

@FunctionalInterface
interface ModuleSnapshotFromFn {
    void apply(LibspectrumSnap snap);
}

@FunctionalInterface
interface ModuleSnapshotToFn {
    void apply(LibspectrumSnap snap);
}

class ModuleInfo {
    ModuleResetFn reset;
    ModuleRomcsFn romcs;
    ModuleSnapshotEnabledFn snapshotEnabled;
    ModuleSnapshotFromFn snapshotFrom;
    ModuleSnapshotToFn snapshotTo;
}

public class Module {

    private static LinkedList<ModuleInfo> registeredModules = null;

    public static int moduleRegister(ModuleInfo module) {
        if (registeredModules == null) {
            registeredModules = new LinkedList<>();
        }
        registeredModules.add(module);
        return 0;
    }

    public static void moduleEnd() {
        if (registeredModules != null) {
            registeredModules.clear();
            registeredModules = null;
        }
    }

    public static void moduleReset(int hardReset) {
        if (registeredModules == null) return;
        for (ModuleInfo module : registeredModules) {
            if (module.reset != null) {
                module.reset.apply(hardReset);
            }
        }
    }

    public static void moduleRomcs() {
        if (registeredModules == null) return;
        for (ModuleInfo module : registeredModules) {
            if (module.romcs != null) {
                module.romcs.apply();
            }
        }
    }

    public static void moduleSnapshotEnabled(LibspectrumSnap snap) {
        if (registeredModules == null) return;
        for (ModuleInfo module : registeredModules) {
            if (module.snapshotEnabled != null) {
                module.snapshotEnabled.apply(snap);
            }
        }
    }

    public static void moduleSnapshotFrom(LibspectrumSnap snap) {
        if (registeredModules == null) return;
        for (ModuleInfo module : registeredModules) {
            if (module.snapshotFrom != null) {
                module.snapshotFrom.apply(snap);
            }
        }
    }

    public static void moduleSnapshotTo(LibspectrumSnap snap) {
        if (registeredModules == null) return;
        for (ModuleInfo module : registeredModules) {
            if (module.snapshotTo != null) {
                module.snapshotTo.apply(snap);
            }
        }
    }
}