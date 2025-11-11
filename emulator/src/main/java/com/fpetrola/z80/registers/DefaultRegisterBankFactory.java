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

    registerBank.registerAf = createComposed16BitRegister(AF, A, F);
    registerBank.registerBc = createComposed16BitRegister(BC, B, C);
    registerBank.registerDe = createComposed16BitRegister(DE, D, E);
    registerBank.registerHl = createComposed16BitRegister(HL, H, L);

    registerBank.register_af = createInvertedComposed16BitRegister(AFx, Ax, Fx);
    registerBank.register_bc = createInvertedComposed16BitRegister(BCx, Bx, Cx);
    registerBank.register_de = createInvertedComposed16BitRegister(DEx, Dx, Ex);
    registerBank.register_hl = createInvertedComposed16BitRegister(HLx, Hx, Lx);

    registerBank.registerIx = createInvertedComposed16BitRegister(IX, IXH, IXL);
    registerBank.registerIy = createInvertedComposed16BitRegister(IY, IYH, IYL);
    registerBank.registerIr = createComposed16BitRegister(IR, create8BitRegister(I), createRRegister());

    registerBank.registerPc = createPlain16BitRegister(PC);
    registerBank.registerSp = createPlain16BitRegister(SP);

    registerBank.registerMemptr = createPlain16BitRegister(MEMPTR);
    registerBank.registerVirtual = createPlain16BitRegister(VIRTUAL);

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
