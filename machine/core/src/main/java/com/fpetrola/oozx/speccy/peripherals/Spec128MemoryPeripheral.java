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

import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.ports.Spec128PortHandler;

import java.util.List;

public class Spec128MemoryPeripheral extends AbstractZxPeripheral {
  public Spec128MemoryPeripheral(Spec128 spec128) {
    super(Periph.Type._128_MEMORY,
        List.of(new Spec128PortHandler(0x8002, 0x0000, spec128)));
  }
}
