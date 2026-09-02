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

package com.fpetrola.oozx.speccy.devices.mouse;

import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.function.IntSupplier;

/**
 * One of the mouse's three ports, each of which only ever answers with one number.
 * <p>
 * One class for the three rather than three classes: what distinguishes them is which port they
 * answer on and which number they give back, and both are arguments.
 */
class KempstonMousePortHandler extends DefaultPortHandler {

  private final IntSupplier reading;

  KempstonMousePortHandler(int mask, int value, IntSupplier reading) {
    super(mask, value, true, false);
    this.reading = reading;
  }

  @Override
  public byte read(int port, byte[] attached) {
    attached[0] = (byte) 0xff;
    return (byte) reading.getAsInt();
  }
}
