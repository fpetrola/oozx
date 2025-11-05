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

package com.fpetrola.z80.se;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.registers.Register;

public class VirtualRegisterDataflowService implements DataflowService {
  private final State state;

  public VirtualRegisterDataflowService(State state) {
    this.state = state;
  }

  @Override
  public int findValueOrigin(Register register) {
    int pointerAddress = -1;
//    if (register instanceof VirtualComposed16BitRegister) {
//      VirtualComposed16BitRegister virtualComposed16BitRegister = (VirtualComposed16BitRegister) register;
//      VirtualComposed16BitRegister first = (VirtualComposed16BitRegister) virtualComposed16BitRegister.getPreviousVersions().get(0);
//      Virtual8BitsRegister low = (Virtual8BitsRegister) first.getLow();
//      Ld instruction = (Ld) low.instruction;
//      if (instruction.getSource() instanceof IndirectMemory16BitReference indirectMemory16BitReference) {
//        ImmutableOpcodeReference target1 = indirectMemory16BitReference.target;
//        pointerAddress = target1.read();
//        System.out.println("indirectMemory16BitReference: " + target1);
//      }
//    } else if (register.read() instanceof DirectAccessWordNumber directAccessWordNumber) {
//      pointerAddress = directAccessWordNumber.address;
//    }
    return pointerAddress;
  }

  public Integer findCurrentReturnAddress() {
    return state.getMemory().read16Bits(state.getRegisterSP().read());
  }

  public boolean isSyntheticReturnAddress() {
    int t = findCurrentReturnAddress();
    boolean b = true; //!(t instanceof ReturnAddressWordNumber);
    return b;
  }
}
