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

package com.fpetrola.oozx.fuse.ports;

import com.fpetrola.oozx.fuse.machine.Spec128;

public class Spec128PortHandler extends DefaultPortHandler {
  private Spec128 spec128;

  public Spec128PortHandler(int mask, int value, Spec128 spec128) {
    super(mask, value, false, true);
    this.spec128 = spec128;
  }

  public void write(int port, byte value) {
    spec128.memoryPortWrite(port, value);
  }
}
