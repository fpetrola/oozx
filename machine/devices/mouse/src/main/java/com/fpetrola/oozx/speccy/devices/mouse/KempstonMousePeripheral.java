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
package com.fpetrola.oozx.speccy.devices.mouse;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;

import java.util.List;

/**
 * A Kempston Mouse hanging off the expansion port, which is a thing no Spectrum came with.
 * <p>
 * Three ports, all read, nothing written, no ROM of its own: this is about the smallest a
 * peripheral gets, and what it adds is out of all proportion to that - a machine whose software
 * was written for a keyboard suddenly being pointed at.
 * <p>
 * The port patterns are Fuse's, from peripherals/kempmouse.c. They are given as a mask and the
 * value the masked port must equal, so the buttons answer at 0xFADF, the horizontal count at
 * 0xFBDF and the vertical at 0xFFDF, along with everything else that decodes the same.
 */
@com.google.inject.Singleton
public class KempstonMousePeripheral extends PluggablePeripheral {

  private final KempstonMouse mouse;

  @com.google.inject.Inject
  public KempstonMousePeripheral(KempstonMouse mouse) {
    super(List.of(
        new KempstonMousePortHandler(0x0121, 0x0001, mouse::buttons),
        new KempstonMousePortHandler(0x0521, 0x0101, mouse::x),
        new KempstonMousePortHandler(0x0521, 0x0501, mouse::y)));
    this.mouse = mouse;
  }

  public KempstonMouse mouse() {
    return mouse;
  }

  /** Unplugging puts the buttons back up, so nothing is left held down. */
  @Override
  public void plugIn(boolean connected) {
    super.plugIn(connected);
    if (!connected) {
      mouse.rest();
    }
  }

  /**
   * Any of them. It plugs into the expansion port and asks nothing of the machine behind it,
   * which is why one could be bought for a Spectrum somebody already owned.
   */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return true;
  }
}
