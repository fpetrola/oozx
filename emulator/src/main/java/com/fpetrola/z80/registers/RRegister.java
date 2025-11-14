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

package com.fpetrola.z80.registers;

public class RRegister extends Plain8BitRegister {
  private int regRBit7;

  public RRegister() {
    super(RegisterName.R.name());
  }

  public void write(int value) {
    data = (value & 0x7f) | (regRBit7 = value & 0x80);
  }

  public void increment() { //TODO: revisar regRBit7
    data = (data + 1 & 0x7f) | regRBit7;
  }
}
