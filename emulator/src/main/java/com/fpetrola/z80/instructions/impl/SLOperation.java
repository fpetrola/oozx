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

public class SLOperation extends TableAluOperation {
  protected int doSL(int a, int carry) {
    F = carry;
    setS((a & 0x0080) != 0);
    if ((a & 0x00FF) == 0)
      setZ();
    else
      resetZ();
    resetH();
    if ((a & 0x0FF00) != 0)
      setC();
    else
      resetC();
    a = a & 0x00FF;
    setPV(parity[a]);
    resetN();
    // put value back
    setUnusedFlags(a);
    return a;
  }
}
