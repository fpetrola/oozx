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

package com.fpetrola.z80.registers.flag;

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;

public class TableAluOperation extends AluOperation {
  protected int table[];

  protected void init(BiFunction<Integer, Integer, Integer> biFunction) {
    table = new int[256 * 2];
    for (int a = 0; a < 256; a++) {
      for (int c = 0; c < 2; c++) {
        Integer aluResult = biFunction.apply(a, c);
        table[((a & 0xff)) | (c << 8)] = ((aluResult & 0xff) << 16) + F;
      }
    }
  }

  public void init(TriFunction<Integer, Integer, Integer, Integer> triFunction) {
    table = new int[256 * 256 * 2];
    for (int a = 0; a < 256; a++) {
      for (int value = 0; value < 256; value++) {
        for (int c = 0; c < 2; c++) {
          Integer aluResult = triFunction.apply(a, value, c);
          table[((value & 0xff)) | (a << 8) | (c << 16)] = ((aluResult & 0xff) << 16) + F;
        }
      }
    }
  }

  public <T extends WordNumber> T executeWithoutCarry(T value, T regA, Register<T> flag) {
    int data1 = table[(regA.left(8)).or(value).intValue()];
    flag.write(WordNumber.createValue(data1 & 0xFF));
    return regA.createInstance(data1 >> 16);
  }

  public <T extends WordNumber> T executeWithCarry(T regA, Register<T> flag) {
    int data1 = table[((flag.read().intValue() & 0x01) << 8) | (regA.intValue() & 0xff)];
    flag.write(WordNumber.createValue(data1 & 0xFF));
    return regA.createInstance(data1 >> 16);
  }

  public <T extends WordNumber> T executeWithCarry2(T value, T regA, int carry, Register<T> flag) {
    int data1 = table[(regA.left(8)).or(value).intValue() | (carry << 16)];
    flag.write(WordNumber.createValue(data1 & 0xFF));
    return regA.createInstance(data1 >> 16);
  }
}
