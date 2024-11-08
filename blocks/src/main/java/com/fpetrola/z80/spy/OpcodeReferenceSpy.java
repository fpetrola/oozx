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

import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;

public class OpcodeReferenceSpy<T> implements OpcodeReference<T> {
  private ImmutableOpcodeReference immutableOpcodeReference;

  public OpcodeReferenceSpy(ImmutableOpcodeReference immutableOpcodeReference) {
    this.immutableOpcodeReference = immutableOpcodeReference;
  }

  public void write(T value) {
    throw new RuntimeException("not implemented");
//    spy.addWriteReference(opcodeReference, value, false);
//    opcodeReference.write(value);
  }

  public T read() {
    throw new RuntimeException("not implemented");

//    int value = opcodeReference.read();
//    spy.addReadReference(opcodeReference, value);
//    return value;
  }

  public String toString() {
    return immutableOpcodeReference.toString();
  }

  public int getLength() {
    return immutableOpcodeReference.getLength();
  }

  public Object clone() throws CloneNotSupportedException {
    return new OpcodeReferenceSpy((ImmutableOpcodeReference) immutableOpcodeReference.clone());
  }
}