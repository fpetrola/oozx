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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.ports.DefaultPortHandler;

public class Tc2068AyDatarPortHandler extends DefaultPortHandler {
  public Tc2068AyDatarPortHandler() {
    super(0x00ff, 0x00f6, true, true);
  }

  public byte read(int port, byte[] attached) {
    return Tc2068.ayDataportRead(port, attached);
  }

  public void write(int port, byte value) {
    Ay.dataportWrite(port, value);
  }
}
