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

import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.BlockInstruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.ConditionAlwaysTrue;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import static com.fpetrola.z80.registers.RegisterName.SP;

class AfterFetchAdder<T extends WordNumber> extends StatesAdder<T, Integer> {

  private Z80Cpu<T> cpu;

  public AfterFetchAdder(Z80Cpu<T> cpu) {
    this.cpu = cpu;
  }

  public static <T extends WordNumber> boolean isLdSP(Ld<T> ld, Z80Cpu<T> cpu) {
    return ld.getTarget().equals(cpu.getState().getRegister(SP)) && ld.getSource() instanceof Register<T>;
  }

  public void visitingRst(RST rst) {
    result = 1;
  }

  public boolean visitingRet(Ret ret) {
    if (!(ret.getCondition() instanceof ConditionAlwaysTrue))
      result = 1;
    return false;
  }

  public boolean visitingAdc16(Adc16<T> tAdc16) {
    result = 7;
    return false;
  }

  public void visitingAdd16(Add16 tAdd16) {
    result = 7;
  }

  public void visitingInc16(Inc16 tInc16) {
    result = 2;
  }

  public void visitingSbc16(Sbc16<T> sbc16) {
    result = 7;
  }

  public void visitingDec16(Dec16 tDec16) {
    result = 2;
  }

  public void visitPush(Push push) {
    result = 1;
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    if (blockInstruction instanceof Ini ||
        blockInstruction instanceof Outi
    )
      result = 1;
  }

  @Override
  public boolean visitRepeatingInstruction(RepeatingInstruction<T> repeatingInstruction) {
    if (repeatingInstruction instanceof Inir ||
        repeatingInstruction instanceof Indr ||
        repeatingInstruction instanceof Outir ||
        repeatingInstruction instanceof Outdr
    )
      result = 1;
    return super.visitRepeatingInstruction(repeatingInstruction);
  }

  public void visitingLd(Ld<T> ld) {
    if (isLdSP(ld, cpu))
      result = 2;
  }

  @Override
  public boolean visitingDjnz(DJNZ<T> djnz) {
    result = 1;
    return false;
  }
}
