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


import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.z80.cpu.Z80Clock;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.MachineCapability;
import com.google.inject.Inject;

/**
 * The same sound chip, wired as a +2A and a +3 wire it.
 * <p>
 * It differs in one wire: the data port answers when read, where on a 128K it does not.
 */
@com.google.inject.Singleton
public class AyPlus3Peripheral extends AyPeripheral {

  @Inject
  public AyPlus3Peripheral(Sound sound, Z80Clock clock) {
    super(sound, clock, true);
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return machine.has(MachineCapability.AY) && machine.has(MachineCapability.PLUS3_MEMORY);
  }
}
