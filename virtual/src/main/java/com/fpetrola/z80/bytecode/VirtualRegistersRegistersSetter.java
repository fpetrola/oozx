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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Composed16BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.transformations.VirtualRegister;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;

import static com.fpetrola.z80.registers.RegisterName.*;

public class VirtualRegistersRegistersSetter<T extends WordNumber> extends DefaultRegistersSetter<T> {

  private VirtualRegisterFactory virtualRegisterFactory;

  public VirtualRegistersRegistersSetter(State<T> state, VirtualRegisterFactory virtualRegisterFactory) {
    super(state);
    this.virtualRegisterFactory = virtualRegisterFactory;
  }

  @Override
  public Register<T> getRegister(RegisterName registerName) {
    Register<T> register = getState().getRegister(registerName);
    Register<T> result;

    VirtualRegisterFactory virtualRegisterFactory = getVirtualRegisterFactory();

    if (registerName != IY && registerName != IX && register instanceof RegisterPair<T>) {
      RegisterPair<WordNumber> wordNumberRegisterPair = (RegisterPair<WordNumber>) register;
      Register<T> high = (Register<T>) wordNumberRegisterPair.getHigh();
      Register<T> h = (Register) virtualRegisterFactory.lastVirtualRegisters.get(high);
      h = h != null ? h : high;
      Register<WordNumber> low = wordNumberRegisterPair.getLow();
      Register<WordNumber> l = (VirtualRegister) virtualRegisterFactory.lastVirtualRegisters.get(low);
      l = l != null ? l : low;
      result = new Composed16BitRegister<>(registerName, h, l);
    } else {
      VirtualRegister<T> l = (VirtualRegister) virtualRegisterFactory.lastVirtualRegisters.get(register);
      if (l != null) {
        result = l;
      } else {
        result = register;
      }
    }
    return result;
  }

  public VirtualRegisterFactory getVirtualRegisterFactory() {
    return virtualRegisterFactory;
  }

}
