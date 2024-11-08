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
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Sbc16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation sbc16TableAluOperation = new AluOperation() {
    public int execute(int HL, int DE, int carry) {
      data = carry;
      int a = HL;
      int b = DE;
      int c = getC() ? 1 : 0;
      int lans = (a - b) - c;
      int ans = lans & 0xffff;
      setS((ans & (FLAG_S << 8)) != 0);
      setZ(ans == 0);
      setC(lans < 0);
      // setPV( ((a ^ b) & (a ^ value) & 0x8000)!=0 );
      setOverflowFlagSub16(a, b, c);
      if ((((a & 0x0fff) - (b & 0x0fff) - c) & 0x1000) != 0)
        setH();
      else
        resetH();
      setN();

      return ans;
    }
  };

  public Sbc16(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, DE, HL) -> sbc16TableAluOperation.executeWithCarry(DE, HL, tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingSbc16(this);
  }
}
