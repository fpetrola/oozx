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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.ports.AyPortHandler;
import com.fpetrola.oozx.speccy.ports.AyRegisters;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.List;

/**
 * The sound chip a 128K machine has and a 48K one does not.
 * <p>
 * {@link Periph.Type#AY} was declared, and declared present for every machine that has one, and
 * there was nothing behind it: the type carried no class, so marking it present marked nothing.
 * This is the class that was missing.
 */
public class AyPeripheral extends AbstractZxPeripheral {

  public AyPeripheral(Sound sound, Z80Clock clock) {
    this(Periph.Type.AY, sound, clock, false);
  }

  protected AyPeripheral(Periph.Type type, Sound sound, Z80Clock clock, boolean dataPortAnswers) {
    this(type, sound, clock, new AyRegisters(), dataPortAnswers);
  }

  private AyPeripheral(Periph.Type type, Sound sound, Z80Clock clock, AyRegisters registers,
                       boolean dataPortAnswers) {
    super(type, List.of(
        // 0xFFFD: bits 15 and 14 high - which register is being spoken to.
        new AyPortHandler(0xC002, 0xC000, true, registers, sound, clock),
        // 0xBFFD: bit 14 high, bit 15 low - the value for it.
        new AyPortHandler(0xC002, 0x8000, false, registers, sound, clock, dataPortAnswers)));
  }
}
