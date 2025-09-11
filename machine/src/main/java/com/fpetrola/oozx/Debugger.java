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

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Debugger {
  public static DebuggerMode mode;

  public static void addTimeEvents() {

  }

  public static void breakpointReduceTstates(long frameLength) {

  }

  public static void systemVariableRegister(String debuggerTypeString, String frameCountName, Supplier<Long> getFrameCount, Consumer<Long> o) {

  }

  public static int getExitCode() {
    return 0;
  }

  public static void registerStartup() {
    StartupManager.registerNoDependencies(StartupManagerModule.DEBUGGER,
        null, null, null);
  }

  public static void commandEvaluate(String debuggerCommand) {
  }

  public static void check(DebuggerBreakpointType debuggerBreakpointType, int address) {

  }
}
