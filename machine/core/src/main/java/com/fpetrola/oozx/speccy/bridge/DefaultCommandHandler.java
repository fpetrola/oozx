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

package com.fpetrola.oozx.speccy.bridge;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.EmulatorCommandResult;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.LibretroCore;
import com.fpetrola.oozx.speccy.LocalLibretroCore;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DefaultCommandHandler implements CommandHandler  {
  public EmulatorCommand lastCommand;

  private List<EmulatorCommand> commandQueue = Collections.synchronizedList(new LinkedList<>());
  private List<EmulatorCommandResult> resultQueue = Collections.synchronizedList(new LinkedList<>());

  private DefaultCommandHandler() {
  }

  public static CommandHandler createCommandHandler(Speccy speccy) {
    LibretroCore core = LibretroCore.INSTANCE;
    core = new LocalLibretroCore(speccy.eventManager, speccy.display, speccy.machine, speccy.z80, speccy.zxClock, speccy.periph, speccy);
    return createCommandHandler(core);
  }

  public static CommandHandler createCommandHandler(LibretroCore core1) {
    DefaultCommandHandler commandHandler = new DefaultCommandHandler();
    OOSpectrumConnector OOSpectrumConnector = new OOSpectrumConnector();
    OOSpectrumConnector.init(commandHandler, core1);
    return commandHandler;
  }

  @Override
  public void addNoResultCommand(EmulatorCommand emulatorCommand) {
    commandQueue.add(emulatorCommand);
  }

  @Override
  public Object executeCommand(EmulatorCommand emulatorCommand) {
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

  @Override
  public void addResultFor(EmulatorCommand lastCommand, Object i) {
    resultQueue.add(new EmulatorCommandResult(lastCommand, i));
  }

  @Override
  public boolean noCommands() {
    return commandQueue.isEmpty();
  }

  @Override
  public EmulatorCommand pollCommand() {
    EmulatorCommand poll = commandQueue.remove(0);
    if (poll == null)
      return null;
//    System.out.println("processing command: " + poll);
    return poll;
  }

  @Override
  public void reset() {
    commandQueue.clear();
    resultQueue.clear();
  }

}
