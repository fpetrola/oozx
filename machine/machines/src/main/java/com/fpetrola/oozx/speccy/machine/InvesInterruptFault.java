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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.z80.registers.RegisterName;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * The fault this clone has, which is a thing that happens on its board and is in nothing it reads:
 * every interrupt it accepts leaves a byte of ones where the address of the next one is read from.
 * <p>
 * A part of the machine with no port of its own, so that it goes in when that machine goes in and
 * comes out when it comes out - which a fault fastened to the processor for good would not do.
 * <p>
 * The machine it was read off writes the byte as the interrupt is taken and before the address is
 * read; here it lands a moment later, so a table is eaten one interrupt further along and the
 * program comes apart just the same.
 */
@Singleton
public class InvesInterruptFault extends AbstractPeripheral {
  private final Cpu cpu;
  private final MemoryBus memory;
  private final Runnable itHappens = this::aByteOfOnes;

  @Inject
  public InvesInterruptFault(Cpu cpu, MemoryBus memory) {
    super(List.of());
    this.cpu = cpu;
    this.memory = memory;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine instanceof Inves;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    cpu.stopTellingAboutInterrupts(itHappens);
    cpu.whenAnInterruptIsTaken(itHappens);
  }

  @Override
  public void deactivate() {
    cpu.stopTellingAboutInterrupts(itHappens);
  }

  private void aByteOfOnes() {
    var state = cpu.getOoz80().getState();
    int at = (state.getRegister(RegisterName.I).read() << 8) | (state.getRegister(RegisterName.R).read() & 0xff);
    memory.poke(at & 0xffff, (byte) 0xff);
  }
}
