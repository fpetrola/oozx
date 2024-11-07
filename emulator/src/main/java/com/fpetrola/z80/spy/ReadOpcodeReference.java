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

import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class ReadOpcodeReference<T extends WordNumber> extends AbstractSpyReference<T> implements Undoable {

  public String opcodeReference;
  public T value;
  public ReadOpcodeReference() {
  }

  public ReadOpcodeReference(String opcodeReference, T value, boolean indirectReference) {
    super();
    this.opcodeReference = opcodeReference;
    this.value = value;
    this.indirectReference = indirectReference;
  }

  public String toString() {
    return Helper.convertToHex(this.value) + "= " + this.opcodeReference.toString()+ (indirectReference ? " (I)" : "");
  }

  public void undo() {
  }
}
