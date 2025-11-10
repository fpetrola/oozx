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

import static com.fpetrola.z80.registers.RegisterName.*;

public class DefaultRegisterBankFactory {

  public DefaultRegisterBankFactory() {
  }

  public RegisterBank createBank() {
    return initBasicBank();
  }

  public RegisterBank initBasicBank() {
    RegisterBank registerBank = new RegisterBank();

    registerBank.af = createComposed16BitRegister(AF, A, F);
    registerBank.bc = createComposed16BitRegister(BC, B, C);
    registerBank.de = createComposed16BitRegister(DE, D, E);
    registerBank.hl = createComposed16BitRegister(HL, H, L);

    registerBank._af = createInvertedComposed16BitRegister(AFx, Ax, Fx);
    registerBank._bc = createInvertedComposed16BitRegister(BCx, Bx, Cx);
    registerBank._de = createInvertedComposed16BitRegister(DEx, Dx, Ex);
    registerBank._hl = createInvertedComposed16BitRegister(HLx, Hx, Lx);

    registerBank.ix = createInvertedComposed16BitRegister(IX, IXH, IXL);
    registerBank.iy = createInvertedComposed16BitRegister(IY, IYH, IYL);
    registerBank.ir = createComposed16BitRegister(IR, create8BitRegister(I), createRRegister());

    registerBank.pc = createPlain16BitRegister(PC);
    registerBank.sp = createPlain16BitRegister(SP);

    registerBank.memptr = createPlain16BitRegister(MEMPTR);
    registerBank.virtual = createPlain16BitRegister(VIRTUAL);

    return registerBank;
  }

  protected Register createRRegister() {
    return new RRegister();
  }

  protected Register create8BitRegister(RegisterName registerName) {
    return new Plain8BitRegister(registerName.name());
  }

  protected RegisterPair createComposed16BitRegister(RegisterName registerName, Register h, Register l) {
    return new Composed16BitRegister<>(registerName.name(), h, l);
  }

  protected Register createPlain16BitRegister(RegisterName registerName) {
    return new Plain16BitRegister(registerName.name());
  }

  protected RegisterPair createComposed16BitRegister(RegisterName registerName, RegisterName h, RegisterName l) {
    return new Composed16BitRegister(registerName.name(), create8BitRegister(h), create8BitRegister(l));
  }

  protected RegisterPair createInvertedComposed16BitRegister(RegisterName registerName, RegisterName h, RegisterName l) {
    return new InvertedComposed16BitRegister(registerName.name(), h, l);
  }

}
