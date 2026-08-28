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

package com.fpetrola.oozx.speccy.bridge;

import com.fpetrola.oozx.speccy.LibretroCore;

public class ReadMemoryCommand implements EmulatorCommand<Integer> {
  public final int address;
  public final boolean contended;

  public ReadMemoryCommand(int address, boolean contended) {
    this.address = address;
    this.contended = contended;
  }

  public String toString() {
    return "ReadMemoryCommand{" +
        "address=" + String.format("%04X", address) +
        ", contended=" + contended +
        '}';
  }

  public Integer execute(LibretroCore core) {
    if (contended) {
      return core.retro_get_memory_data_contended(address);
    } else
      return core.retro_get_memory_data(address);
  }
}
