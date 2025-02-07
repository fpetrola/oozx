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

package com.fpetrola.z80.instructions.cache;

import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class CacheInvalidatorMemoryWriteListener<T extends WordNumber>  implements MemoryWriteListener<T> {
  public CacheInvalidatorMemoryWriteListener(Runnable[] cacheInvalidators) {
    this.cacheInvalidators = cacheInvalidators;
  }

  private final Runnable[] cacheInvalidators;

  @Override
  public T writtingMemoryAt(T address, T value) {
    Runnable cacheInvalidator = cacheInvalidators[address.intValue()];
    if (cacheInvalidator != null)
      cacheInvalidator.run();
    return value;
  }
}
