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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.*;

import static com.fpetrola.z80.registers.RegisterName.F;

public class SpyRegisterBankFactory<T extends WordNumber> extends DefaultRegisterBankFactory<T> {
  private final InstructionSpy spy;

  private SpyRegisterBankFactory(InstructionSpy spy) {
    this.spy = spy;
  }

  public static <T extends WordNumber> SpyRegisterBankFactory<T> createSpyRegisterBankFactory(InstructionSpy spy) {
    return new SpyRegisterBankFactory<T>(spy);
  }

  protected Register<T> createRRegister() {
    return spy.wrapRegister(super.createRRegister());
  }

  protected Register<T> createAlwaysIntegerPlain8BitRegister(RegisterName registerName) {
    return spy.wrapRegister(super.createAlwaysIntegerPlain8BitRegister(registerName));
  }

  protected Register<T> create8BitRegister(RegisterName registerName) {
    return spy.wrapRegister(super.create8BitRegister(registerName));
  }

  protected RegisterPair<T> createComposed16BitRegister(RegisterName registerName, Register<T> h, Register<T> l) {
    return (RegisterPair<T>) spy.wrapRegister(super.createComposed16BitRegister(registerName, h, l));
  }

  protected Register createAlwaysIntegerPlain16BitRegister(RegisterName registerName) {
    return spy.wrapRegister(super.createAlwaysIntegerPlain16BitRegister(registerName));
  }

  protected Register<T> createPlain16BitRegister(RegisterName registerName) {
    return spy.wrapRegister(super.createPlain16BitRegister(registerName));
  }

  protected RegisterPair createComposed16BitRegister(RegisterName registerName, RegisterName h, RegisterName l) {
    return (RegisterPair<T>) spy.wrapRegister(super.createComposed16BitRegister(registerName, h, l));
  }

  protected Register createFlagRegister() {
    return spy.wrapRegister(new Plain8BitRegister(F.name()));
  }
}
