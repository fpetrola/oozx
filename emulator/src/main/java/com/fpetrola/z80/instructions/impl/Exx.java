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
