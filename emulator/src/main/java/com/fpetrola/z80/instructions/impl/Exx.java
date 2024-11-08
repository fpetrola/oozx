/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Exx<T extends WordNumber> extends AbstractInstruction<T> {
  public Register<T> getBc() {
    return bc;
  }

  public Register<T> getDe() {
    return de;
  }

  public Register<T> getHl() {
    return hl;
  }

  public Register<T> get_bc() {
    return _bc;
  }

  public Register<T> get_de() {
    return _de;
  }

  public Register<T> get_hl() {
    return _hl;
  }

  private Register<T> bc;
  private Register<T> de;
  private Register<T> hl;
  private Register<T> _bc;
  private Register<T> _de;
  private Register<T> _hl;

  public Exx(Register<T> bc, Register<T> de, Register<T> hl, Register<T> _bc, Register<T> _de, Register<T> _hl) {
    this.bc = bc;
    this.de = de;
    this.hl = hl;
    this._bc = _bc;
    this._de = _de;
    this._hl = _hl;
  }

  public int execute() {
    T v1 = bc.read();
    bc.write(_bc.read());
    _bc.write(v1);

    v1 = de.read();
    de.write(_de.read());
    _de.write(v1);

    v1 = hl.read();
    hl.write(_hl.read());
    _hl.write(v1);

    return 4;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    visitor.visitExx(this);
  }

  public void setBc(Register<T> bc) {
    this.bc = bc;
  }

  public void setDe(Register<T> de) {
    this.de = de;
  }

  public void setHl(Register<T> hl) {
    this.hl = hl;
  }

  public void set_bc(Register<T> _bc) {
    this._bc = _bc;
  }

  public void set_de(Register<T> _de) {
    this._de = _de;
  }

  public void set_hl(Register<T> _hl) {
    this._hl = _hl;
  }
}
