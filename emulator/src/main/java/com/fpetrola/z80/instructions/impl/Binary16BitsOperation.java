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

  public int[] compressParameters(int v1, int v2, int temp, int f) {
    int packed = 0;

    // Bits de Value1 (Posiciones 11 y 15) -> ocupan bits 0 y 1 del packed
    packed |= ((v1 & 0x0800) >> 11);
    packed |= ((v1 & 0x8000) >> 14);

    // Bits de Value2 (Posiciones 11 y 15) -> ocupan bits 2 y 3 del packed
    packed |= ((v2 & 0x0800) >> 9);
    packed |= ((v2 & 0x8000) >> 12);

    // Bits de Temp para Lookup y Flags (11, 13, 15, 16) -> ocupan bits 4, 5, 6, 7
    packed |= ((temp & 0x0800) >> 7);  // Bit 11 -> p4
    packed |= ((temp & 0x2000) >> 8);  // Bit 13 -> p5
    packed |= ((temp & 0x8000) >> 9);  // Bit 15 -> p6
    packed |= ((temp & 0x10000) >> 9); // Bit 16 -> p7

    // Flag Zero (¿Es el resultado diferente de cero?) -> bit 8
    packed |= (((temp & 0x1FFFF) != 0 ? 1 : 0) << 8);

    // Bits de F (3 bits de entrada) -> bits 9, 10, 11
    packed |= (f & 0x04) << 7;
    packed |= (f & 0x40) << 4;
    packed |= (f & 0x80) << 4;

    return new int[]{packed >> 8 & 0xFF, packed & 0xFF};
  }

  public static int[] decompress(int v1, int v2) {
    int packed = (v1 << 8) | v2;
    int[] d = new int[5];
    // Reconstruir Value1 (solo bits 11 y 15)
    d[0] = ((packed & 0x01) << 11) | ((packed & 0x02) << 14);

    // Reconstruir Value2 (solo bits 11 y 15)
    d[1] = ((packed & 0x04) << 9) | ((packed & 0x08) << 12);

    // Reconstruir F (bits 0-2 originales)
    d[2] = ((packed >> 7) & 0x04) + ((packed >> 4) & 0x40) + ((packed >> 4) & 0x80);

    // Reconstruir Temp (bits 11, 13, 15, 16)
    d[3] = ((packed & 0x10) << 7) |
           ((packed & 0x20) << 8) |
           ((packed & 0x40) << 9) |
           ((packed & 0x80) << 9);

    // Reconstruir indicador Zero (1 si no es cero)
    d[4] = (packed >> 8) & 0x01;

    return d;
  }

  protected int doExecute(int sourceValue, int targetValue) {
    int f = flag.read() & 0xff;
    int result = operation(targetValue, sourceValue, f);
    int[] compressedParameters = compressParameters(targetValue, sourceValue, result, f);
    aluOperation.execute2Values1Boolean(compressedParameters[0], compressedParameters[1], f, flag);
    return result & 0xffff;
  }

  protected int operation(int v1, int v2, int f) {
    return 0;
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visiting16BitsOperation(this))
      super.accept(visitor);
  }
}
