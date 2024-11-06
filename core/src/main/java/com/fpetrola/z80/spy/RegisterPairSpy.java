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
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;

public class RegisterPairSpy<T extends WordNumber> extends RegisterSpy<T> implements RegisterPair<T> {

  public RegisterPairSpy(Register register, InstructionSpy spy) {
    super(register, spy);
  }

  public Register getHigh() {
    return getPair().getHigh();
  }

  private RegisterPair<T> getPair() {
    return (RegisterPair<T>) register;
  }

  public Register getLow() {
    return getPair().getLow();
  }

  public Object clone() throws CloneNotSupportedException {
    return this;
  }

  public String getName() {
    return getPair().getName();
  }
}
