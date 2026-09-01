/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.bridge;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.bridge.EmulatorCommandResult;
import com.fpetrola.oozx.speccy.bridge.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.bridge.LibretroCore;
import com.fpetrola.oozx.speccy.bridge.LocalLibretroCore;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DefaultCommandHandler implements CommandHandler  {
  public EmulatorCommand lastCommand;

  private List<EmulatorCommand> commandQueue = Collections.synchronizedList(new LinkedList<>());
  private List<EmulatorCommandResult> resultQueue = Collections.synchronizedList(new LinkedList<>());

  private DefaultCommandHandler() {
  }

  /** The local core over that machine, which is how the bridge holds one of its own. */
  public static LibretroCore over(Speccy speccy) {
    return new LocalLibretroCore(speccy.scheduler, speccy.display, speccy.machine, speccy.cpu, speccy.zxClock, speccy.ports, speccy);
  }

  public static CommandHandler createCommandHandler(Speccy speccy) {
    return createCommandHandler(over(speccy));
  }

  public static CommandHandler createCommandHandler(LibretroCore core1) {
    DefaultCommandHandler commandHandler = new DefaultCommandHandler();
    OOSpectrumConnector OOSpectrumConnector = new OOSpectrumConnector();
    OOSpectrumConnector.drivenFrom(commandHandler, core1);
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
