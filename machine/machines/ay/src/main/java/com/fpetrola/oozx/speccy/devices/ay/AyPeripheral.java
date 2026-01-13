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
