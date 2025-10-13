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

package com.fpetrola.oozx.fuse.ports;

import com.fpetrola.oozx.fuse.peripherals.Port;

public class PortHandlerAdapter implements PortHandler {
  private final Port port;

  public PortHandlerAdapter(Port port) {
    this.port = port;
  }

  @Override
  public byte read(int portNumber, byte[] attached) {
    return port.read.apply(portNumber, attached);
  }

  @Override
  public int getMask() {
    return port.mask;
  }

  @Override
  public int getValue() {
    return port.value;
  }

  @Override
  public boolean isReader() {
    return port.read != null;
  }

  @Override
  public boolean isWriter() {
    return port.write != null;
  }

  @Override
  public void write(int portNumber, byte value) {
    port.write.apply(portNumber, value);
  }
}
