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

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;

import static com.fpetrola.z80.registers.RegisterName.*;

public class UnprefixedTableOpCodeGenerator<T> extends TableOpCodeGenerator<T> {
  private final Instruction<T> cbOpcode;
  private final Instruction<T> ddOpcode;
  private final Instruction<T> edOpcode;
  private final Instruction<T> fdOpcode;
  private final int delta;

  public UnprefixedTableOpCodeGenerator(int delta, State state, Instruction<T> cbOpcode, Instruction<T> ddOpcode, Instruction<T> edOpcode, Instruction<T> fdOpcode, RegisterName main16BitRegister, RegisterName mainHigh8BitRegister, RegisterName mainLow8BitRegister, OpcodeReference main16BitRegisterReference, OpcodeConditions opc1, DefaultInstructionFactory instructionFactory) {
    super(state, main16BitRegister, mainHigh8BitRegister, mainLow8BitRegister, main16BitRegisterReference, opc1, instructionFactory);
    this.delta = delta;
    this.cbOpcode = cbOpcode;
    this.ddOpcode = ddOpcode;
    this.edOpcode = edOpcode;
    this.fdOpcode = fdOpcode;
  }

  protected Instruction getOpcode() {
    OpcodeReference hlOrIx = r(main16BitRegister);
    switch (x) {
    case 0:
      switch (z) {
      case 0:
        switch (y) {
        case 0:
          return i.Nop();
        case 1:
          return i.Ex(r(AF), r(AFx));
        case 2:
          return i.DJNZ(opc.bnz(), d());
        case 3:
          return i.JR(opc.t(), d());
        case 4, 5, 6, 7:
          return i.JR(cc[y - 4], d());
        }
      case 1:
        return select(i.Ld(rp[p], nn()), i.Add16(hlOrIx, rp[p]))[q];
        case 2:
        switch (q) {
        case 0:
          return select(i.Ld(iRR(BC), r(A)), i.Ld(iRR(DE), r(A)), i.Ld(iinn(), hlOrIx), i.Ld(inn(), r(A)))[p];
          case 1:
            return select(i.Ld(r(A), iRR(BC)), i.Ld(r(A), iRR(DE)), i.Ld(hlOrIx, iinn()), i.Ld(r(A), inn()))[p];
        }
      case 3:
        return select(i.Inc16(rp[p]), i.Dec16(rp[p]))[q];
        case 4:
        return i.Inc(r[y]);
      case 5:
        return i.Dec(r[y]);
      case 6:
        return createLd1();
      case 7:
        return select(i.RLCA(), i.RRCA(), i.RLA(), i.RRA(), i.DAA(), i.CPL(), i.SCF(), i.CCF())[y];
      }
      return null;
    case 1:
      if (z == 6 && y == 6)
        return i.Halt();
      else
        return createLd();
    case 2:
      return alu.get(y).apply(r[z]);
    case 3:
      switch (z) {
      case 0:
        return i.Ret(cc[y]);
      case 1:
        switch (q) {
        case 0:
          return i.Pop(rp2[p]);
        case 1:
          return select(i.Ret(opc.t()), i.Exx(), i.JP(hlOrIx, opc.t()), i.Ld(r(SP), hlOrIx))[p];
        }
      case 2:
        return i.JP(nn(), cc[y]);
      case 3:
        return select(i.JP(nn(), opc.t()), cbOpcode, i.Out(d(), r(A)), i.In(r(A), d()), i.Ex(iiRR(SP), hlOrIx), i.Ex(r(DE), r(RegisterName.HL)), i.DI(), i.EI())[y];
        case 4:
        return i.Call(cc[y], nn());
      case 5:
        switch (q) {
        case 0:
          return i.Push(rp2[p]);
        case 1:
          return select(i.Call(opc.t(), nn()), ddOpcode, edOpcode, fdOpcode)[p];
        }
      case 6:
        return alu.get(y).apply(d());
      case 7:
        return i.RST(y * 8);
      }
      return null;
    }
    return null;
  }

  private OpcodeReference<WordNumber> inn() {
    return inn(delta);
  }

  private ImmutableOpcodeReference nn() {
    return nn(delta);
  }

  private OpcodeReference<WordNumber> iinn() {
    return iinn(delta);
  }

  private ImmutableOpcodeReference d() {
    return n(delta);
  }

  protected Ld createLd1() {
    return i.Ld(r[y], d());
  }

  protected Ld createLd() {
    return i.Ld(r[y], r[z]);
  }
}
