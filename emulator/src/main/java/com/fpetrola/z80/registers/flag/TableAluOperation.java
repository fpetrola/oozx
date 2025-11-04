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

import com.fpetrola.z80.registers.Register;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.ToIntBiFunction;

public class TableAluOperation extends AluOperation {
  protected int[] table;

  protected void init(ToIntBiFunction<Integer, Integer> biFunction) {
    table = new int[256 * 2];
    for (int a = 0; a < 256; a++) {
      for (int c = 0; c < 2; c++) {
        int aluResult = biFunction.applyAsInt(a, c);
        table[((a & 0xff)) | (c << 8)] = ((aluResult & 0xff) << 16) + F;
      }
    }
  }

  public void init(ToIntTriFunction<java.lang.Integer, java.lang.Integer, java.lang.Integer> triFunction) {
    table = new int[256 * 256 * 2];
    for (int a = 0; a < 256; a++) {
      for (int value = 0; value < 256; value++) {
        for (int c = 0; c < 2; c++) {
          int aluResult = triFunction.applyAsInt(a, value, c);
          table[((value & 0xff)) | (a << 8) | (c << 16)] = ((aluResult & 0xff) << 16) + F;
        }
      }
    }
  }

  public int executeWithoutCarry(int value, int regA, Register flag) {
    int data1 = table[((regA << 8) & 0xFFFF | value & 0xFFFF) & 0xFFFF];
    flag.write(data1 & 0xFF);
    return data1 >> 16 & 0xFFFF;
  }

  public int executeWithCarry(int regA, Register flag) {
    int data1 = table[((flag.read() & 0x01) << 8) | (regA & 0xff)];
    flag.write(data1 & 0xFF);
    return data1 >> 16 & 0xFFFF;
  }

  public int executeWithCarry2(int value, int regA, int carry, Register flag) {
    int data1 = table[(((regA << 8) & 0xFFFF | value & 0xFFFF) & 0xFFFF) | ((carry & 1) << 16)];
    flag.write(data1 & 0xFF);
    return data1 >> 16 & 0xFFFF;
  }

  public int[] executeWithoutCarry2(int value, int regA) {
    int data1 = table[regA << 8 | (value & 0xff)];
    return new int[]{data1 >> 16, data1 & 0xFF};
  }

  public int[] executeWithCarry2(int regA, int i1) {
    int data1 = table[(i1 & 0x01) << 8 | (regA & 0xff)];
    return new int[]{data1 >> 16, data1 & 0xFF};
  }
}
