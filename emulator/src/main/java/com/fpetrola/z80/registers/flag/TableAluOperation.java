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

package com.fpetrola.z80.registers.flag;

import com.fpetrola.z80.registers.Register;

public class TableAluOperation extends AluOperation {
  protected int[] table;

  protected void init(ToPrimitiveIntBiFunction biFunction) {
    table = new int[256 * 2];
    for (int a = 0; a < 256; a++) {
      for (int c = 0; c < 2; c++) {
        F = c;
        int aluResult = biFunction.applyAsInt(a, c);
        table[((a & 0xff)) | (c << 8)] = ((aluResult & 0xff) << 8) + F;
      }
    }
  }

  public void init(ToPrimitiveIntBiAndBooleanFunction biAndBooleanFunction) {
    table = new int[256 * 256 * 2];
    for (int a = 0; a < 256; a++) {
      for (int value = 0; value < 256; value++) {
        for (int c = 0; c < 2; c++) {
          int aluResult = biAndBooleanFunction.applyAsInt(a, value, c);
          table[((value & 0xff)) | (a << 8) | (c << 16)] = ((aluResult & 0xff) << 8) + F;
        }
      }
    }
  }

  public void init(ToPrimitiveIntTriFunction triFunction) {
    table = new int[256 * 256 * 256];
    for (int a = 0; a < 256; a++) {
      for (int value = 0; value < 256; value++) {
        for (int c = 0; c < 256; c++) {
          int aluResult = triFunction.applyAsInt(a, value, c);
          table[((value & 0xff)) | (a << 8) | (c << 16)] = ((aluResult & 0xff) << 8) + F;
        }
      }
    }
  }

  final public int execute2Values1Boolean(int value1, int value2, int booleanValue, Register flag) {
    return fetchValueAndWriteFlag(flag, (value2 << 8 | value1) & 0xFFFF | ((booleanValue & 1) << 16));
  }

  final public int execute2Values(int value1, int value2, Register flag) {
    return fetchValueAndWriteFlag(flag, (value2 << 8 | value1 & 0xff) & 0xFFFF);
  }

  final public void execute3Values(int value1, int value2, int value3, Register flag) {
    fetchValueAndWriteFlag(flag, (value2 << 8 | value1) & 0xFFFF | value3 << 16);
  }

  private int fetchValueAndWriteFlag(Register flag, int i) {
    int data1 = table[i];
    flag.write(data1 & 0xFF);
    return data1 >> 8;
  }
}
