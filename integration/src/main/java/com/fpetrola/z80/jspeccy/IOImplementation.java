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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.cpu.IO;
import z80core.MemIoOps;

final class IOImplementation implements IO {
  private final MemIoOps memIoOps;
  private final int[] ports = new int[0x10000];

  public IOImplementation(MemIoOps memory) {
    this.memIoOps = memory;
  }

  public void out(int port, int value) {
    memIoOps.outPort(port, value);
  }

  public int in(int port) {
    int value = memIoOps.inPort(port);
    //if (value.intValue() != 255 && value.intValue() != 191)
    //if (port.intValue() == 49150)

    int port1 = ports[port];
    if (value != port1) {
      if (port == 31) {
        System.out.println(port + "= " + value);
      }
    }

    ports[port]= value;

    return value;
  }
}