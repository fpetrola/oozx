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
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.transformations.Virtual8BitsRegister;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;

public class VirtualRegisterDataflowService<T extends WordNumber> implements DataflowService<T> {
  private final State<T> state;

  public VirtualRegisterDataflowService(State<T> state) {
    this.state = state;
  }

  @Override
  public int findValueOrigin(Register<T> register) {
    int pointerAddress = -1;
    if (register instanceof VirtualComposed16BitRegister<T>) {
      VirtualComposed16BitRegister<T> virtualComposed16BitRegister = (VirtualComposed16BitRegister<T>) register;
      VirtualComposed16BitRegister<T> first = (VirtualComposed16BitRegister<T>) virtualComposed16BitRegister.getPreviousVersions().get(0);
      Virtual8BitsRegister<T> low = (Virtual8BitsRegister<T>) first.getLow();
      Ld<T> instruction = (Ld<T>) low.instruction;
      if (instruction.getSource() instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
        ImmutableOpcodeReference<T> target1 = indirectMemory16BitReference.target;
        pointerAddress = target1.read().intValue();
        System.out.println("indirectMemory16BitReference: " + target1);
      }
    } else if (register.read() instanceof DirectAccessWordNumber directAccessWordNumber) {
      pointerAddress = directAccessWordNumber.getAddressesSupplier().iterator().next();
    }
    return pointerAddress;
  }

  public T findCurrentReturnAddress() {
    return Memory.read16Bits(state.getMemory(), (T) state.getRegisterSP().read());
  }

  public boolean isSyntheticReturnAddress() {
    T t = findCurrentReturnAddress();
    return !(t instanceof ReturnAddressWordNumber);
  }
}
