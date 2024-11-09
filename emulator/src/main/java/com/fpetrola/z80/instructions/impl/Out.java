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

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Out<T extends WordNumber> extends TargetSourceInstruction<T, ImmutableOpcodeReference<T>> {
  public Out(ImmutableOpcodeReference source, OutPortOpcodeReference outPortOpcodeReference, Register<T> flag) {
    super(outPortOpcodeReference, source, flag);
  }

  public int execute() {
    target.write(source.read());
    return cyclesCost;
  }

  public static class OutPortOpcodeReference<T> implements OpcodeReference<T> {
    private final IO<T> io;
    private final ImmutableOpcodeReference target;

    public OutPortOpcodeReference(IO<T> io, ImmutableOpcodeReference target) {
      this.io = io;
      this.target = target;
    }

    public void write(T value) {
      io.out((T) target.read(), value);
    }

    public T read() {
      return (T) target.read();
    }

    public int getLength() {
      return target.getLength();
    }

    public Object clone() throws CloneNotSupportedException {
      return target.clone();
    }

    public String toString() {
      return target.toString();
    }
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitOut(this);
  }
}
