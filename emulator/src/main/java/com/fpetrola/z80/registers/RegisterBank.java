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

package com.fpetrola.z80.registers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
@SuppressWarnings("ALL")
public class RegisterBank  {
  protected RegisterPair registerAf;
  protected RegisterPair registerBc;
  protected RegisterPair registerDe;
  protected RegisterPair registerHl;

  protected RegisterPair register_af;
  protected RegisterPair register_bc;
  protected RegisterPair register_de;
  protected RegisterPair register_hl;

  protected RegisterPair registerIx;
  protected RegisterPair registerIy;

  protected RegisterPair registerIr;

  protected Register registerPc;
  protected Register registerSp;

  protected Register registerMemptr;
  protected Register registerVirtual;

  protected RegisterBank() {
  }

  public Register get(RegisterName name) {
    switch (name) {
      case A:
        return this.registerAf.getHigh();
      case F:
        return this.registerAf.getLow();
      case B:
        return this.registerBc.getHigh();
      case C:
        return this.registerBc.getLow();
      case D:
        return this.registerDe.getHigh();
      case E:
        return this.registerDe.getLow();
      case H:
        return this.registerHl.getHigh();
      case L:
        return this.registerHl.getLow();
      case IXH:
        return this.registerIx.getHigh();
      case IXL:
        return this.registerIx.getLow();
      case IYH:
        return this.registerIy.getHigh();
      case IYL:
        return this.registerIy.getLow();
      case AF:
        return this.registerAf;
      case BC:
        return this.registerBc;
      case DE:
        return this.registerDe;
      case HL:
        return this.registerHl;
      case PC:
        return this.registerPc;
      case SP:
        return this.registerSp;
      case IX:
        return this.registerIx;
      case IY:
        return this.registerIy;
      case I:
        return this.registerIr.getHigh();
      case R:
        return this.registerIr.getLow();
      case IR:
        return this.registerIr;
      case MEMPTR:
        return this.registerMemptr;
      case VIRTUAL:
        return this.registerVirtual;
      case Ax:
        return this.register_af.getHigh();
      case Fx:
        return this.register_af.getLow();
      case Bx:
        return this.register_bc.getHigh();
      case Cx:
        return this.register_bc.getLow();
      case Dx:
        return this.register_de.getHigh();
      case Ex:
        return this.register_de.getLow();
      case Hx:
        return this.register_hl.getHigh();
      case Lx:
        return this.register_hl.getLow();
      case AFx:
        return this.register_af;
      case BCx:
        return this.register_bc;
      case DEx:
        return this.register_de;
      case HLx:
        return this.register_hl;
      default:
        return null;
    }
  }

//  @Override
//  public String toString() {
//    return /*"AF=" + String.format("%04X", af.read().intValue()) + //*/
//        " BC=" + String.format("%04X", bc.read().intValue()) + //
//            " DE=" + String.format("%04X", de.read().intValue()) + //
//            " HL=" + String.format("%04X", hl.read().intValue()) + //
//            " AF'=" + String.format("%04X", _af.read().intValue()) + //
//            " BC'=" + String.format("%04X", _bc.read().intValue()) + //
//            " DE'=" + String.format("%04X", _de.read().intValue()) + //
//            " HL'=" + String.format("%04X", _hl.read().intValue()) + //
//            " PC=" + String.format("%04X", pc.read().intValue()) + //
//            " SP=" + String.format("%04X", sp.read().intValue()) + //
//            " IX=" + String.format("%04X", ix.read().intValue()) + //
//            " IY=" + String.format("%04X", iy.read().intValue()) + //
//            " IR=" + String.format("%04X", ir.read().intValue()) + //
//            " MEMPTR=" + String.format("%04X", memptr.read().intValue());
//  }

  protected List<RegisterName> getAlternateRegisters() {
    return Arrays.asList(RegisterName.AFx, RegisterName.BCx, RegisterName.DEx, RegisterName.HLx);
  }

  protected List<RegisterName> getRegisters() {
    return Arrays.asList(RegisterName.AF, RegisterName.BC, RegisterName.DE, RegisterName.HL, RegisterName.IX, RegisterName.IY, RegisterName.PC, RegisterName.SP, RegisterName.IR);
  }

  public List<Register> getAll() {
    List<RegisterName> a = getRegisters();
    List<RegisterName> b = getAlternateRegisters();

    a.addAll(b);

    List<Register> collect = a.stream().map(r -> get(r)).collect(Collectors.toList());
    return collect;
  }

}
