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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.registers.flag.TableAluOperation;

public class RLOperation extends TableAluOperation {
  protected int getA(int a, boolean c1) {
    boolean c = (a & 0x0080) != 0;
    a = ((a << 1) & 0x00FF);
    if (c1)
      a = (a | 0x01);
    if (c) {
      setC();
    } else
      resetC();
    resetH();
    resetN();
    return a;
  }
}
