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
package net.emustudio.plugins.cpu.zilogZ80.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

@SuppressWarnings("unused")
@PluginContext
public interface ContextZ80 extends Context8080 {

    /**
     * Signals a non-maskable interrupt.
     * <p>
     * On the interrupt execution, CPU ignores the next instruction and instead performs a restart
     * at address 0066h. Routines should exit with RETN instruction.
     */
    void signalNonMaskableInterrupt();

    /**
     * Explicitly adds machine cycles (slows down CPU).
     * <p>
     * Used primarily in contention implementation.
     *
     * @param tStates number of t-states (machine cycles) to add
     */
    void addCycles(long tStates);
}
