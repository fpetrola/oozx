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

package com.fpetrola.z80.opcodes.references;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;

public class CachedMemory8BitReference<T extends WordNumber> extends Memory8BitReference<T> {
  public CachedMemory8BitReference(T lastFetchedAddress, Memory<T> memory, Register<T> pc, int delta) {
    super(memory, pc, delta);
    this.fetchedAddress = lastFetchedAddress;
  }

  @Override
  protected T fetchAddress() {
    return fetchedAddress;
  }
}
