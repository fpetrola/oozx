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

package com.fpetrola.z80.opcodes.decoder.table;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeReference;

import static com.fpetrola.z80.registers.RegisterName.*;

public class EDPrefixTableOpCodeGenerator<T> extends TableOpCodeGenerator<T> {

  public EDPrefixTableOpCodeGenerator(State state, OpcodeReference a, OpcodeConditions opc1, DefaultInstructionFactory instructionFactory) {
    super(state, HL, H, L, a, opc1, instructionFactory);
  }

  protected Instruction<T> getOpcode() {
    switch (x) {
    case 1:
      switch (z) {
      case 0:
        return y == 6 ? i.In(r(A), r(BC)) : i.In(r[y], r(BC));
      case 1:
        return y == 6 ? i.Out(r(C), c(0)) : i.Out(r(C), r[y]);
      case 2:
        return q == 0 ? i.Sbc16(r(HL), rp[p]) : i.Adc16(r(HL), rp[p]);
      case 3:
        return q == 0 ? i.Ld(iinn(2), rp[p]) : i.Ld(rp[p], iinn(2));
      case 4:
        return i.Neg(r(A));
      case 5:
        return y != 1 ? i.RetN(opc.t()) : i.RetN(opc.t());
      case 6:
        return i.IM(im[y]);
      case 7:
        return select(i.Ld(r(I), r(A)), i.Ld(r(R), r(A)), i.Ld(r(A), r(I)), i.LdAR(r(A), r(R)), i.RRD(), i.RLD(), i.Nop(), i.Nop())[y];
      }
    case 2:
      if (z <= 3 && y >= 4)
        return bli[y][z];
    }
    return null;
  }
}
