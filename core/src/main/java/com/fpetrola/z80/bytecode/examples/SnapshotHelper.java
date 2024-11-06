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

package com.fpetrola.z80.bytecode.examples;

import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.Base64Utils;

import java.util.Arrays;

public class SnapshotHelper {
  public static <T extends WordNumber> String getBase64Memory(State<T> state1) {
    WordNumber[] data1 = state1.getMemory().getData();
    int ramEnd = 0x10000;
    byte[] data = new byte[ramEnd];
    Arrays.fill(data, (byte) 0);

    for (int i = 0; i < ramEnd; i++) {
      WordNumber wordNumber = data1[i];
      int i1 = wordNumber == null ? 0 : wordNumber.intValue();
      data[i] = (byte) i1;
    }
    return Base64Utils.gzipArrayCompressToBase64(data);
  }
}
