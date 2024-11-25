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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class MemoryPlusRegister8BitReferenceSpy<T extends WordNumber> extends MemoryPlusRegister8BitReference<T> {

  private final MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference;

  public MemoryPlusRegister8BitReferenceSpy(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    super();
    this.memoryPlusRegister8BitReference = memoryPlusRegister8BitReference;
  }

  public T read() {
    return memoryPlusRegister8BitReference.read();
  }

  public void write(T value) {
    memoryPlusRegister8BitReference.write(value);
  }

  public String toString() {
    return memoryPlusRegister8BitReference.toString();
  }

  public int getLength() {
    return memoryPlusRegister8BitReference.getLength();
  }

  public Object clone() throws CloneNotSupportedException {
    return new MemoryPlusRegister8BitReferenceSpy((MemoryPlusRegister8BitReference) memoryPlusRegister8BitReference.clone());
  }
}
