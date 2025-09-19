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

package com.fpetrola.oozx.fuse;

import com.sun.jna.Structure;

import java.util.List;

// WriteMemoryCommand
public class WriteMemoryCommand extends Structure implements EmulatorCommand {
  public int address;
  public int value;
  public boolean contended;

  public WriteMemoryCommand(int address, int value, boolean contended) {
    this.address = address;
    this.value = value;
    this.contended = contended;
  }

  @Override
  protected List<String> getFieldOrder() {
    return List.of("address", "value");
  }

  public String toString() {
    return "WriteMemoryCommand{" +
            "address=" + String.format("%04X", address) +
            ", value=" + String.format("%02X", value) +
            '}';
  }
}
