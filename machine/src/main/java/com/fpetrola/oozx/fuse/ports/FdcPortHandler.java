/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse.ports;

import com.fpetrola.oozx.fuse.machine.SpecPlus3;

public class FdcPortHandler extends DefaultPortHandler {
  private SpecPlus3 specPlus3;

  public FdcPortHandler(int mask, int value, SpecPlus3 specPlus3) {
    super(mask, value, true, true);
    this.specPlus3 = specPlus3;
  }

  public byte read(int port, byte[] attached) {
    return specPlus3.fdcRead(port, attached);
  }

  public void write(int port, byte value) {
    specPlus3.fdcWrite(port, value);
  }
}
