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

package com.fpetrola.oozx.fuse.bridge;

import java.util.Arrays;
import java.util.List;

/**
 * Multiplexor que reenvía todas las llamadas a múltiples instancias de CommandHandler.
 */
public class CommandHandlerMultiplexor implements CommandHandler {

    private final List<CommandHandler> delegates;

    public CommandHandlerMultiplexor(List<CommandHandler> delegates) {
        this.delegates = delegates;
    }

    public CommandHandlerMultiplexor(CommandHandler... delegates) {
        this.delegates = Arrays.asList(delegates);
    }

    @Override
    public void addNoResultCommand(EmulatorCommand emulatorCommand) {
        delegates.forEach(d -> d.addNoResultCommand(emulatorCommand));
    }

    @Override
    public Object executeCommand(EmulatorCommand emulatorCommand) {
        Object result = null;
        for (CommandHandler d : delegates) {
            Object r = d.executeCommand(emulatorCommand);
            if (result == null) {
                result = r; // tomamos el resultado de la primera instancia
            }
        }
        return result;
    }

    @Override
    public void addResultFor(EmulatorCommand lastCommand, Object i) {
        delegates.forEach(d -> d.addResultFor(lastCommand, i));
    }

    @Override
    public boolean noCommands() {
        boolean result = true;
        for (CommandHandler d : delegates) {
            result &= d.noCommands();
        }
        return result;
    }

    @Override
    public EmulatorCommand pollCommand() {
        EmulatorCommand result = null;
        for (CommandHandler d : delegates) {
            EmulatorCommand cmd = d.pollCommand();
            if (result == null) {
                result = cmd; // devolvemos solo la primera instancia
            }
        }
        return result;
    }

    @Override
    public void reset() {
        delegates.forEach(CommandHandler::reset);
    }
}
