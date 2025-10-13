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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.machine.SpecPlus3;
import com.fpetrola.oozx.fuse.ports.DefaultPortHandler;

public class SpecPlus3PortHandler extends DefaultPortHandler {
  private SpecPlus3 specPlus3;

  public SpecPlus3PortHandler(int mask, int value, SpecPlus3 specPlus3) {
    super(mask, value, false, true);
    this.specPlus3 = specPlus3;
  }

  public void write(int port, byte value) {
    specPlus3.memoryPort2Write(port, value);
  }
}
