/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.registers.Register;

public class Exx extends AbstractInstruction {
  private final Register bc;
  private final Register de;
  private final Register hl;
  private final Register _bc;
  private final Register _de;
  private final Register _hl;

  public Exx(Register bc, Register de, Register hl, Register _bc, Register _de, Register _hl) {
    this.bc = bc;
    this.de = de;
    this.hl = hl;
    this._bc = _bc;
    this._de = _de;
    this._hl = _hl;
  }

  public void execute() {
    int v1 = bc.read();
    bc.write(_bc.read());
    _bc.write(v1);

    v1 = de.read();
    de.write(_de.read());
    _de.write(v1);

    v1 = hl.read();
    hl.write(_hl.read());
    _hl.write(v1);
  }

  public Register getBc() {
    return bc;
  }

  public Register getDe() {
    return de;
  }

  public Register getHl() {
    return hl;
  }

  public Register get_bc() {
    return _bc;
  }

  public Register get_de() {
    return _de;
  }

  public Register get_hl() {
    return _hl;
  }

  public void accept(InstructionVisitor<?> visitor) {
    visitor.visitExx(this);
  }
}
