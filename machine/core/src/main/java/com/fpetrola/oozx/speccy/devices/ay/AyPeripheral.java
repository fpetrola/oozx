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

import com.fpetrola.oozx.speccy.peripherals.Periph;

import com.fpetrola.oozx.speccy.peripherals.AbstractZxPeripheral;

import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.List;

/**
 * The sound chip a 128K machine has and a 48K one does not, and the chip itself.
 * <p>
 * It holds its own: the mixer knows how to make a synth and how to mix what comes out of one, and
 * nothing more about what a sound chip is. Writes used to go through it to reach the chip, which
 * is how it came to hold one.
 * <p>
 * The chip is made when this is switched on rather than when it is built, because the sound is set
 * up for a machine after that machine has said what it contains - so at construction there is
 * nothing yet to make a synth from.
 */
public class AyPeripheral extends AbstractZxPeripheral {

  private final Sound sound;
  private final AyRegisters registers = new AyRegisters();
  private Ay chip;

  public AyPeripheral(Sound sound, Z80Clock clock) {
    this(Periph.Type.AY, sound, clock, false);
  }

  protected AyPeripheral(Periph.Type type, Sound sound, Z80Clock clock, boolean dataPortAnswers) {
    super(type, List.of());
    this.sound = sound;
    // 0xFFFD: bits 15 and 14 high - which register is being spoken to.
    // 0xBFFD: bit 14 high, bit 15 low - the value for it.
    ports(new AyPortHandler(0xC002, 0xC000, true, registers, this, clock),
        new AyPortHandler(0xC002, 0x8000, false, registers, this, clock, dataPortAnswers));
  }

  @Override
  public boolean canActivate() {
    return true;
  }

  @Override
  public void activate() {
    registers.reset();
    chip = sound.add(new Ay(sound));
  }

  @Override
  public void deactivate() {
    // Pulled out. Without this the chip stayed in the mixer once its ports were gone, making
    // silence out of a queue nothing was filling - two thousand ticks a frame of nothing.
    if (chip != null) {
      sound.remove(chip);
      chip = null;
    }
  }

  /** A value arrived for the register that was last selected. */
  public void heard(int register, int value, long tstates) {
    if (chip != null) {
      chip.write(register, value, tstates);
    }
  }

  /**
   * How much has reached the chip, which is how you tell it is wired up at all.
   * <p>
   * The count and not the chip: the chip is this package's business, and handing it out is
   * what kept it public.
   */
  public long writes() {
    return chip == null ? 0 : chip.writes;
  }
}
