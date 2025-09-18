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

public class CommandHandler {
  FuseLibretroExample fuseLibretroExample = new FuseLibretroExample();

  public CommandHandler() {
    fuseLibretroExample.init();
  }

  public void addNoResultCommand(EmulatorCommand emulatorCommand) {
    fuseLibretroExample.commandQueue.add(emulatorCommand);
  }

  public int executeCommand(EmulatorCommand emulatorCommand) {
    fuseLibretroExample.commandQueue.add(emulatorCommand);
    while (true) {
      if (!fuseLibretroExample.resultQueue.empty()) {
        EmulatorCommandResult item = fuseLibretroExample.resultQueue.poll();
        if (item.getCommand() != emulatorCommand)
          throw new IllegalStateException("Unexpected command result");

        return item.getValue();
      }
    }
  }
}
