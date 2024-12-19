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

package com.fpetrola.z80.bytecode.decompile;

import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleBytecodeProvider implements IBytecodeProvider {
  private Map<String, byte[]> bytecodes = new HashMap<>();

  public SimpleBytecodeProvider() {
  }

  public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
    return bytecodes.get(externalPath);
  }

  public void addBytecode(String key, byte[] bytecode) {
    bytecodes.put(key, bytecode);
  }
}
