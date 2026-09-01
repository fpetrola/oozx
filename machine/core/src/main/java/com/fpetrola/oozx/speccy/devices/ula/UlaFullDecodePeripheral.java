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

package com.fpetrola.oozx.speccy.devices.ula;


import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;

import com.fpetrola.oozx.speccy.modules.Ula;

import java.util.List;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;

public class UlaFullDecodePeripheral extends AbstractPeripheral {

  private final Ula ula;
  public UlaFullDecodePeripheral(Ula ula) {
    super(List.of(new UlaFullDecodePortHandler(ula)));
    this.ula = ula;
  }
  @Override
  public boolean canActivate() {
    return true;
  }

  /** Every machine has a speaker, and it is part of the ULA. */
  @Override
  public void activate() {
    ula.attachSpeaker();
  }

  @Override
  public void deactivate() {
    ula.detachSpeaker();
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return machine.fullyDecodesPorts();
  }
}
