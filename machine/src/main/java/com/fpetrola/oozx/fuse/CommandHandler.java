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

import com.fpetrola.oozx.Fuse;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CommandHandler {
  static FuseLibretroExample fuseLibretroExample = new FuseLibretroExample();
  static LibretroCore core = fuseLibretroExample.core;

  private List<EmulatorCommand> commandQueue = Collections.synchronizedList(new LinkedList<>());
  private List<EmulatorCommandResult> resultQueue = Collections.synchronizedList(new LinkedList<>());

  private CommandHandler() {
  }

  public static CommandHandler createCommandHandler() {
    CommandHandler commandHandler = new CommandHandler();
//    core= new LocalLibretroCore();
    fuseLibretroExample.init(commandHandler, core);
    return commandHandler;
  }

  public void addNoResultCommand(EmulatorCommand emulatorCommand) {
    commandQueue.add(emulatorCommand);
  }

  public int executeCommand(EmulatorCommand emulatorCommand) {
    if (!resultQueue.isEmpty()) {
      System.out.println("eh!!!!1111");
    }
    commandQueue.add(emulatorCommand);
    while (true) {
      if (!resultQueue.isEmpty()) {
        EmulatorCommandResult item = resultQueue.remove(0);
        if (item.getCommand() != emulatorCommand)
          throw new IllegalStateException("Unexpected command result");

        return item.getValue();
      }
    }
  }

  public void addResultFor(EmulatorCommand lastCommand, int i) {
    resultQueue.add(new EmulatorCommandResult(lastCommand, i));
  }

  public boolean noCommands() {
    return commandQueue.isEmpty();
  }

  public EmulatorCommand pollCommand() {
    EmulatorCommand poll = commandQueue.remove(0);
    if (poll == null)
      return null;
    System.out.println("processing command: " + poll);
    return poll;
  }

  public void reset() {
    commandQueue.clear();
    resultQueue.clear();
  }

}
