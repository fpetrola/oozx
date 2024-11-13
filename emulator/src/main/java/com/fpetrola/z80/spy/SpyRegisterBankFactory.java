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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.registers.*;

import static com.fpetrola.z80.registers.RegisterName.F;

public class SpyRegisterBankFactory extends DefaultRegisterBankFactory {
  private final InstructionSpy spy;

  public SpyRegisterBankFactory(InstructionSpy spy) {
    this.spy = spy;
  }

  protected Register createRRegister() {
    return spy.wrapRegister(super.createRRegister());
  }

  protected Register create8BitRegister(RegisterName registerName) {
    return spy.wrapRegister(super.create8BitRegister(registerName));
  }

  protected RegisterPair createComposed16BitRegister(RegisterName registerName, Register h, Register l) {
    return (RegisterPair) spy.wrapRegister(super.createComposed16BitRegister(registerName, h, l));
  }

  protected Register createPlain16BitRegister(RegisterName registerName) {
    return spy.wrapRegister(super.createPlain16BitRegister(registerName));
  }

  protected RegisterPair createComposed16BitRegister(RegisterName registerName, RegisterName h, RegisterName l) {
    return (RegisterPair) spy.wrapRegister(super.createComposed16BitRegister(registerName, h, l));
  }

  protected Register createFlagRegister() {
    return spy.wrapRegister(new Plain8BitRegister(F.name()));
  }
}
