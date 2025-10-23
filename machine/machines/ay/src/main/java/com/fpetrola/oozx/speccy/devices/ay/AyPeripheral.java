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

package com.fpetrola.oozx.speccy.devices.ay;


import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;

import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.List;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.google.inject.Inject;

/**
 * The port-facing side of the AY chip (128K only). Holds the {@link Ay} instance itself, since
 * register writes pass through here; the chip is created on activation rather than construction
 * because the mixer isn't ready to build a synth before the machine declares its hardware.
 */
@com.google.inject.Singleton
public class AyPeripheral extends AbstractPeripheral {

  private final Sound sound;
  private final AyRegisters registers = new AyRegisters();
  private Ay chip;

  @Inject
  public AyPeripheral(Sound sound, Z80Clock clock) {
    this(sound, clock, false);
  }

  protected AyPeripheral(Sound sound, Z80Clock clock, boolean dataPortAnswers) {
    // 0xFFFD selects the register (bits 14-15 both high); 0xBFFD writes its value (bit 14
    // high, bit 15 low).
    this(sound, clock, 0xC002, 0xC000, 0xC002, 0x8000, dataPortAnswers);
  }

  /** Lets a clone board expose this chip through its own port addresses. */
  protected AyPeripheral(Sound sound, Z80Clock clock, int selectMask, int selectValue, int dataMask, int dataValue,
                         boolean dataPortAnswers) {
    super(List.of());
    this.sound = sound;
    ports(Wired.at(selectMask, selectValue, new AyPortHandler(true, registers, this, clock)),
        Wired.at(dataMask, dataValue, new AyPortHandler(false, registers, this, clock, dataPortAnswers)));
  }

  @Override
  public void activate(SpectrumMachine machine) {
    registers.reset();
    chip = sound.add(new Ay(sound));
  }

  @Override
  public void deactivate() {
    // Must remove the chip from the mixer, or it lingers mixing silence from an empty queue.
    if (chip != null) {
      sound.remove(chip);
      chip = null;
    }
  }

  /** Forwards a write to whichever register the select port last chose. */
  public void heard(int register, int value, long tstates) {
    if (chip != null) {
      chip.write(register, value, tstates);
    }
  }

  /** Exposes only the write count, not the chip itself, which stays package-private. */
  public long writes() {
    return chip == null ? 0 : chip.writes;
  }

}
