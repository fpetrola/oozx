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

package com.fpetrola.oozx.fuse.peripherals;

import com.fpetrola.oozx.fuse.machine.SpecPlus3;
import com.fpetrola.oozx.fuse.ports.Spec128PortHandler;
import com.fpetrola.oozx.fuse.ports.SpecPlus3PortHandler;

import java.util.List;

public class SpecPlus3MemoryPeripheral extends AbstractZxPeripheral {
  public SpecPlus3MemoryPeripheral(SpecPlus3 specPlus3) {
    super(Periph.Type.PLUS3_MEMORY, List.of(
        new Spec128PortHandler(0xc002, 0x4000, specPlus3),
        new SpecPlus3PortHandler(0xf002, 0x1000, specPlus3)
    ));
  }
}
