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

package com.fpetrola.z80.instructions;

import com.fpetrola.z80.instructions.base.AbstractInstruction;
import com.fpetrola.z80.instructions.base.FlagInstruction;
import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class SCF<T extends WordNumber> extends AbstractInstruction<T> implements FlagInstruction<T> {
  public static final AluOperation scfTableAluOperation = new AluOperation() {
    public int execute(int a, int carry) {
      setC();
      resetH();
      resetN();
      return data;
    }
  };

  public Register<T> getFlag() {
    return flag;
  }

  public void setFlag(Register<T> flag) {
    this.flag = flag;
  }

  private Register<T> flag;

  public SCF(Register<T> flag) {
    this.flag = flag;
  }

  public int execute() {
    scfTableAluOperation.executeWithCarry(WordNumber.createValue(0), flag);
    return 4;
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingScf(this);
  }

}
