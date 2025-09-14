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
package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.z80.tstates.PhaseProcessor;

/**
 * The processor's view of the machine's memory: what the page costs, what it holds, and the
 * T-states the access takes.
 * <p>
 * It has a name rather than being an anonymous class inside the Z80's setup because the generator
 * reads it: this is the source it inlines into the generated core, so that a case reaches the page
 * table without a call. Whoever changes it changes that core.
 */
public final class ContendedMemory implements com.fpetrola.z80.memory.Memory {
    private final MemoryBus memory;
    private final SpectrumZ80Clock clock;
    /**
     * The OOP core has the contention as a listener outside; the generated one carries it inside.
     */
    private final boolean contentionOutside;
    private PhaseProcessor phaseProcessor;

    public ContendedMemory(MemoryBus memory, SpectrumZ80Clock clock, boolean contentionOutside) {
        this.memory = memory;
        this.clock = clock;
        this.contentionOutside = contentionOutside;
    }

    /**
     * The aspect is built after the memory it watches, so it arrives later.
     */
    public void watchedBy(PhaseProcessor phaseProcessor) {
        this.phaseProcessor = phaseProcessor;
    }

    public int read(int address, int fetching) {
        int value = memory.read(address);
        clock.addTStates(fetching == 1 ? 4 : 3);
        if (contentionOutside)
            phaseProcessor.afterRead(address);
        return value;
    }

    public void write(int address, int value) {
        if (contentionOutside)
            phaseProcessor.beforeWrite(address);
        memory.write(address, (byte) (value & 0xff));
        clock.addTStates(3);
    }

    public void reset() {
    }
}
