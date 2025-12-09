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
import java.util.HashMap;
import java.util.Map;

public class CachedTableAluOperation extends AluOperation {
  private static final Map<String, int[]> TABLE_CACHE = new HashMap<>();
  
  private final AluOperation delegate;
  private int[] table;

  public CachedTableAluOperation(AluOperation delegate) {
    this.delegate = delegate;
    initializeTable(delegate);
  }

  private void initializeTable(AluOperation delegate) {
    ToPrimitiveIntTriFunction triFunction = null;
    int i = 2;
    if (delegate.calculate2Values1Boolean(0, 0, 0) != -1) {
      triFunction = delegate::calculate2Values1Boolean;
    } else if (delegate.calculate1Value(0) != -1) {
      triFunction = (value1, value2, carry) -> delegate.calculate1Value(value1);
    } else if (delegate.calculate3Values(0, 0, 0) != -1) {
      triFunction = delegate::calculate3Values;
      i = 256;
    }
    buildTable(triFunction, i);
  }

  public void buildTable(ToPrimitiveIntTriFunction triFunction, int i) {
    String cacheKey = generateCacheKey(triFunction, i);
    
    // Verificar si la tabla ya existe en el cache
    if (TABLE_CACHE.containsKey(cacheKey)) {
      table = TABLE_CACHE.get(cacheKey);
      return;
    }
    
    // Construir la tabla si no existe
    table = new int[256 * 256 * i];
    for (int a = 0; a < 256; a++) {
      for (int b = 0; b < 256; b++) {
        for (int c = 0; c < i; c++) {
          delegate.F = b;
          int aluResult = triFunction.applyAsInt(a, b, c);
          table[((a & 0xff)) | (b << 8) | (c << 16)] = ((aluResult & 0xff) << 8) + delegate.F;
        }
      }
    }
    
    // Guardar en el cache
    TABLE_CACHE.put(cacheKey, table);
  }
  
  private String generateCacheKey(ToPrimitiveIntTriFunction triFunction, int i) {
    return delegate.getClass().getName() + "_" + i;
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
