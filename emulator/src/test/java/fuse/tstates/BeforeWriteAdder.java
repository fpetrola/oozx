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

package fuse.tstates;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;

import static com.fpetrola.z80.registers.RegisterName.*;

public class BeforeWriteAdder<T extends WordNumber> extends StatesAdder<T, StatesAddition> {
  private State<T> state;

  public BeforeWriteAdder(State<T> state) {
    this.state = state;
  }

  public boolean visitingCall(Call tCall) {
    if (getState().getTStatesSinceCpuStart() == 10)
      result = new StatesAddition(1, IR, 1);

    return super.visitingCall(tCall);
  }

  private State<T> getState() {
    return state;
  }

  public void visitEx(Ex<T> ex) {
    if (ex.getTarget() instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
      int i = ex.getSource().equals(getState().getRegister(HL)) && indirectMemory16BitReference.target.equals(getState().getRegister(SP)) ? 10 : 14;
      if (getState().getTStatesSinceCpuStart() == i)
        result = new StatesAddition(1, SP, 1);
    }

    super.visitEx(ex);
  }

  public boolean visitingInc(Inc tInc) {
    if (AfterExecutionAdder.isIndirectHL(tInc))
      result = new StatesAddition(1, HL, 0);
    return super.visitingInc(tInc);
  }

  @Override
  public boolean visitingDec(Dec dec) {
    if (AfterExecutionAdder.isIndirectHL(dec))
      result = new StatesAddition(1, HL, 0);
    return super.visitingDec(dec);
  }
}
