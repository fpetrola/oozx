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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Binary16BitsOperation extends ParameterizedBinaryAluInstruction {
  public Binary16BitsOperation(OpcodeReference target, ImmutableOpcodeReference source, Register flag, AluOperation aluOperation) {
    super(target, source, flag, aluOperation);
  }

  protected int calculate(int a, int b) {
    int result = operation(a, b, flag.read());
    executeAction(compress(a, b, result), b, result);
    return result & 0xffff;
  }

  protected int doExecute(int sourceValue, int targetValue) {
    return calculate(targetValue, sourceValue);
  }

  /**
   * The eight bits the flags of a sixteen-bit add or subtract are made of, packed into a byte so
   * the operation can be a table: bits 15 and 11 of each operand, and bits 16, 15, 13 and 11 of
   * the result. Eight things, eight bits, and no room for a mistake.
   * <p>
   * There was one. Bit 13 of the result was arriving TWICE - once folded down to a bit of its
   * own, which is where the operation reads it from for the undocumented bit 5, and once through
   * the plain mask, which lands it on top of bit 11 of the second operand. So whenever a result
   * reached 0x2000 the second operand looked as though it had carried out of its eleventh bit
   * when it had not, and the half carry, which is read from exactly those three bits, came out
   * set: ADC HL,DE of 0000 and 20FF reported one. Taking bit 13 out of the plain mask and leaving
   * the fold gives eight distinct things in eight bits, which is what there is room for.
   */
  protected int compress(int v1, int v2, int result) {
    return ((v1 & 0x8800 | (v2 & 0x8800) >> 1)
        | ((result & 0x18800) | ((result & 0x2000) >> 1)) >> 3) >> 8;
  }

  protected void executeAction(int v1, int v2, int result) {
    // The sixteen bits that land in the register, not the seventeen the addition made: adding
    // 0000 and FFFF with a carry gives 10000, which is zero in HL and was not being called zero.
    aluOperation.execute2Values1Boolean((result & 0xFFFF) != 0 ? 1 : 0, v1, flag.read() & 1, flag);
  }

  protected int operation(int v1, int v2, int f) {
    return 0;
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visiting16BitsOperation(this))
      super.accept(visitor);
  }

  interface Binary16BitsAluOperation {
    int execute(Register flag, int value1, int value2, int result);
  }
}
