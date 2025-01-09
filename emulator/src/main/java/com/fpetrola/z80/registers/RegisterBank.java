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

import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
public class RegisterBank<T extends WordNumber> {
  protected RegisterPair<T> af;
  protected RegisterPair<T> bc;
  protected RegisterPair<T> de;
  protected RegisterPair<T> hl;

  protected RegisterPair<T> _af;
  protected RegisterPair<T> _bc;
  protected RegisterPair<T> _de;
  protected RegisterPair<T> _hl;

  protected RegisterPair<T> ix;
  protected RegisterPair<T> iy;

  protected RegisterPair<T> ir;

  protected Register<T> pc;
  protected Register<T> sp;

  protected Register<T> memptr;
  protected Register<T> virtual;

  protected RegisterBank() {
  }

  public Register<T> get(String name) {
    return get(RegisterName.valueOf(name));
  }

  public Register<T> get(RegisterName name) {
    switch (name) {
      case A:
        return this.af.getHigh();
      case F:
        return this.af.getLow();
      case B:
        return this.bc.getHigh();
      case C:
        return this.bc.getLow();
      case D:
        return this.de.getHigh();
      case E:
        return this.de.getLow();
      case H:
        return this.hl.getHigh();
      case L:
        return this.hl.getLow();
      case IXH:
        return this.ix.getHigh();
      case IXL:
        return this.ix.getLow();
      case IYH:
        return this.iy.getHigh();
      case IYL:
        return this.iy.getLow();
      case AF:
        return this.af;
      case BC:
        return this.bc;
      case DE:
        return this.de;
      case HL:
        return this.hl;
      case PC:
        return this.pc;
      case SP:
        return this.sp;
      case IX:
        return this.ix;
      case IY:
        return this.iy;
      case I:
        return this.ir.getHigh();
      case R:
        return this.ir.getLow();
      case IR:
        return this.ir;
      case MEMPTR:
        return this.memptr;
      case VIRTUAL:
        return this.virtual;
      case Ax:
        return this._af.getHigh();
      case Fx:
        return this._af.getLow();
      case Bx:
        return this._bc.getHigh();
      case Cx:
        return this._bc.getLow();
      case Dx:
        return this._de.getHigh();
      case Ex:
        return this._de.getLow();
      case Hx:
        return this._hl.getHigh();
      case Lx:
        return this._hl.getLow();
      case AFx:
        return this._af;
      case BCx:
        return this._bc;
      case DEx:
        return this._de;
      case HLx:
        return this._hl;
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

  public List<Register<T>> getAll() {
    List<RegisterName> a = new ArrayList<>(getRegisters());
    List<RegisterName> b = new ArrayList<>(getAlternateRegisters());

    a.addAll(b);

    List<Register<T>> collect = a.stream().map(r -> get(r)).collect(Collectors.toList());
    return collect;
  }

  public void copyValuesFrom(RegisterBank<T> registerBank) {
    registerBank.getAll().stream().forEach(r -> get(r.getName()).write(r.read()));
  }
}
