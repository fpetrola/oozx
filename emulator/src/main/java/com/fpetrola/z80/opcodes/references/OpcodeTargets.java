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

package com.fpetrola.z80.opcodes.references;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import static com.fpetrola.z80.registers.RegisterName.PC;

public class OpcodeTargets<T> {

  private final State state;

  public OpcodeTargets(State state) {
    this.state = state;
  }

  public ImmutableOpcodeReference c(int value) {
    return new ConstantOpcodeReference<T>(WordNumber.createValue(value));
  }

  public Register r(RegisterName name) {
    return state.getRegister(name);
  }

  public OpcodeReference iRR(RegisterName name) {
    return iRR(r(name));
  }

  public OpcodeReference iRR(ImmutableOpcodeReference r) {
    return new IndirectMemory8BitReference(r, state.getMemory());
  }

  public OpcodeReference iRRn(RegisterName name, boolean rewindOnWrite, int valueDelta) {
    return iRRn(valueDelta, r(name));
  }

  public OpcodeReference iRRn(int valueDelta, ImmutableOpcodeReference r) {
    return new MemoryPlusRegister8BitReference(r, state.getMemory(), r(PC), valueDelta);
  }

  public OpcodeReference iiRR(RegisterName name) {
    Register r = r(name);
    return iiRR(r);
  }

  public OpcodeReference iiRR(ImmutableOpcodeReference r) {
    return new IndirectMemory16BitReference(r, state.getMemory());
  }

  public ImmutableOpcodeReference n(int delta) {
    return new Memory8BitReference(state.getMemory(), r(PC), delta);
  }

  public ImmutableOpcodeReference nn(int delta) {
    return new Memory16BitReference(state.getMemory(), r(PC), delta);
  }

  public OpcodeReference iinn(int delta) {
    return new IndirectMemory16BitReference(nn(delta), state.getMemory());
  }

  public OpcodeReference inn(int delta) {
    return new IndirectMemory8BitReference(nn(delta), state.getMemory());
  }
}
