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

import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.ports.AyPortHandler;
import com.fpetrola.oozx.speccy.ports.AyRegisters;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A box with an AY in it, for a machine that has not got one.
 * <p>
 * The Melodik decodes the chip exactly as a 128 does, which is the point of it: music written for
 * a 128K plays on a 48K with one of these plugged in. Somebody sold these because a 48K makes no
 * music, and an emulator that will not let you plug one in is deciding for them.
 * <p>
 * Unlike a machine's own chip this announces itself when it is switched on, because whether it is
 * there is settled after the sound has been set up - the machine is chosen, the sound is prepared
 * for it, and only then is it worked out what is plugged in.
 */
public class MelodikPeripheral extends AbstractZxPeripheral {

  private final Sound sound;
  private final BooleanSupplier wanted;

  public MelodikPeripheral(Sound sound, Z80Clock clock, BooleanSupplier wanted) {
    this(sound, clock, wanted, new AyRegisters());
  }

  private MelodikPeripheral(Sound sound, Z80Clock clock, BooleanSupplier wanted, AyRegisters registers) {
    super(Periph.Type.MELODIK, List.of(
        new AyPortHandler(0xC002, 0xC000, true, registers, sound, clock),
        new AyPortHandler(0xC002, 0x8000, false, registers, sound, clock)));
    this.sound = sound;
    this.wanted = wanted;
  }

  @Override
  public boolean isWanted() {
    return wanted.getAsBoolean();
  }

  @Override
  public boolean canActivate() {
    return true;
  }

  @Override
  public void activate() {
    sound.attachSoundChip();
  }
}
