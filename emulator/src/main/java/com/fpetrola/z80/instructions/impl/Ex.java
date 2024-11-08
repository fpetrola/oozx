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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class Ex<T extends WordNumber> extends AbstractInstruction<T> {
  private  OpcodeReference<T> target;

  private  OpcodeReference<T> source;
  public Ex(OpcodeReference<T> target, OpcodeReference<T> source) {
    this.target = target;
    this.source = source;
  }

  public int execute() {
    final T v1 = target.read();
    final T v2 = source.read();

    target.write(v2);
    source.write(v1);
    return cyclesCost;
  }

  public OpcodeReference<T> getTarget() {
    return target;
  }

  public void setTarget(OpcodeReference<T> target) {
    this.target = target;
  }

  public void setSource(OpcodeReference<T> source) {
    this.source = source;
  }

  public OpcodeReference<T> getSource() {
    return source;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitEx(this);
  }
}
