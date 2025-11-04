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
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;

public class Out extends TargetSourceInstruction<ImmutableOpcodeReference> {
  public Out(ImmutableOpcodeReference source, OutPortOpcodeReference outPortOpcodeReference, Register flag) {
    super(outPortOpcodeReference, source, flag);
  }

  public int execute() {
    target.write(source.read());
    return cyclesCost;
  }

  public static class OutPortOpcodeReference implements OpcodeReference {
    private final IO io;
    public final ImmutableOpcodeReference target;
    private final Register a;

    public OutPortOpcodeReference(IO io, ImmutableOpcodeReference target, Register a) {
      this.io = io;
      this.target = target;
      this.a = a;
    }

    public void write(int value) {
      io.out(getRead(), value);
    }

    private int getRead() {
      Integer read = null;

      if (read == null) {
        read = target.read();
        if (!(target instanceof Register)) {
          Integer wordNumber = a.read();
          int i = ((wordNumber << 8) & 0xFFFF) & 0xFFFF;
          read = (read | i) & 0xFFFF;
        }
      }
      return read;
    }

    public int read() {
      return getRead();
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
