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
package com.fpetrola.oozx.speccy.devices.fuller;

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.PortHandler;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fuller Box: the 128's sound chip before there was a 128, on ports 0x3f (which register)
 * and 0x5f (its value), and a joystick read on 0x7f, active low. Sold for the 48K.
 */
@Singleton
public class FullerPeripheral extends AyPeripheral implements Pluggable {

  private boolean pluggedIn;

  @Inject
  public FullerPeripheral(Sound sound, Z80Clock clock, Joystick joystick) {
    super(sound, clock, 0x00ff, 0x003f, 0x00ff, 0x005f, false);
    List<PortHandler> all = new ArrayList<>(List.of(getPorts()));
    all.add(new DefaultPortHandler(0x00ff, 0x007f, true, false) {
      public byte read(int port, byte[] attached) {
        return joystick.fullerRead(port, attached);
      }
    });
    ports(all.toArray(new PortHandler[0]));
  }

  @Override
  public void plugIn(boolean connected) {
    pluggedIn = connected;
  }

  @Override
  public boolean isPluggedIn() {
    return pluggedIn;
  }

  @Override
  public boolean isWanted() {
    return pluggedIn;
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.MEMORY_128) && !machine.fullyDecodesPorts();
  }
}
