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

package com.fpetrola.oozx.speccy.devices.joystick;


import com.fpetrola.oozx.speccy.peripherals.AbstractZxPeripheral;

import com.fpetrola.oozx.speccy.modules.Joystick;

import java.util.List;
import java.util.function.BooleanSupplier;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;

/**
 * The Kempston as most things decode it: bit 5 low.
 * <p>
 * Not built into any machine: a joystick interface is something somebody plugged in, so whether it
 * is there is theirs to say. It is handed the question rather than the settings - the answer has
 * to be read when a machine is selected and not frozen when this is built, because somebody can
 * turn it on while the emulator runs, but that needs a question and not everything the emulator
 * has ever been configured with.
 */
public class KempstonStrictPeripheral extends AbstractZxPeripheral {

  private final BooleanSupplier wanted;

  public KempstonStrictPeripheral(Joystick joystick, BooleanSupplier wanted) {
    super(List.of(new JoystickPortHandler(0x00e0, 0x0000, joystick)));
    this.wanted = wanted;
  }

  @Override
  public boolean isWanted() {
    return wanted.getAsBoolean();
  }

  /** Its port is decoded loosely, so it only goes where the machine leaves those bits alone. */
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.fullyDecodesPorts();
  }
}
