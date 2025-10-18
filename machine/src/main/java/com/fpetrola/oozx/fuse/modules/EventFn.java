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

package com.fpetrola.oozx.fuse.modules;// Assuming libspectrum is ported to Java (Libspectrum class with dword as long, new/free as new/null)
// Assuming infrastructure/startup_manager ported (StartupManager, StartupManagerModule)
// Assuming fuse, ui, utils ported (Fuse, Ui, Utils)
// Use LinkedList for GSList, ArrayList for GArray
// GFunc as functional interface
// event_fn_t as functional interface

@FunctionalInterface
public interface EventFn {
    void apply(int tstates, int type, Object userData);
}

// The function to be called when an event occurs
// (Defined as EventFn above)

