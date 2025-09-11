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

// Assuming ported dependencies:
// - Libspectrum
// - Ui (UiError, error)

// Enum for the modules the startup manager knows about
public enum StartupManagerModule {
    AY,
    BETA,
    COVOX,
    CREATOR,
    DEBUGGER,
    DIDAKTIK,
    DISCIPLE,
    DISPLAY,
    DIVIDE,
    DIVMMC,
    EVENT,
    FDD,
    FULLER,
    IF1,
    IF2,
    JOYSTICK,
    KEMPMOUSE,
    KEYBOARD,
    LIBSPECTRUM,
    LIBXML2,
    MACHINE,
    MACHINES_PERIPH,
    MELODIK,
    MEMORY,
    MEMPOOL,
    MULTIFACE,
    OPUS,
    PHANTOM_TYPIST,
    PLUSD,
    PRINTER,
    PROFILE,
    PSG,
    RZX,
    SCLD,
    SCREENSHOT,
    SETTINGS_END,
    SETUID,
    SIMPLEIDE,
    SLT,
    SOUND,
    SPECCYBOOT,
    SPECDRUM,
    SPECTRANET,
    SPECTRUM,
    TAPE,
    TTX2000S,
    TIMER,
    ULA,
    USOURCE,
    Z80,
    ZXATASP,
    ZXCF,
    ZXMMC;
}

// Functional interfaces for init and end callbacks
@FunctionalInterface
interface StartupManagerInitFn {
    int apply(Object context);
}

@FunctionalInterface
interface StartupManagerEndFn {
    void apply();
}

// Class to represent a registered module
class RegisteredModule {
    StartupManagerModule module;
    List<StartupManagerModule> dependencies;
    StartupManagerInitFn initFn;
    Object initContext;
    StartupManagerEndFn endFn;

    RegisteredModule(StartupManagerModule module, StartupManagerModule[] dependencies,
                     StartupManagerInitFn initFn, Object initContext, StartupManagerEndFn endFn) {
        this.module = module;
        this.dependencies = dependencies != null ? new ArrayList<>(Arrays.asList(dependencies)) : new ArrayList<>();
        this.initFn = initFn;
        this.initContext = initContext;
        this.endFn = endFn;
    }
}

