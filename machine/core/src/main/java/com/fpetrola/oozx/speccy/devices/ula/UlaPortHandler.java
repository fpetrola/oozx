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

import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import com.fpetrola.oozx.speccy.modules.Ula;

class UlaPortHandler extends DefaultPortHandler {
  private Ula ula;

  public UlaPortHandler(Ula ula) {
    super(0x0001, 0x0000, true, true);
    this.ula = ula;
  }

  public byte read(int port, byte[] attached) {
    return ula.read(port, attached);
  }

  public void write(int port, byte value) {
    ula.write(port, value);
  }
}
