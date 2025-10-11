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

import com.fpetrola.oozx.fuse.peripherals.PortHandler;

public abstract class DefaultPortHandler implements PortHandler {
  protected int mask;
  protected int value;
  protected boolean isReader;
  protected boolean isWriter;

  public DefaultPortHandler(int mask1, int value1, boolean isReader, boolean isWriter) {
    mask = mask1;
    value = value1;
    this.isReader = isReader;
    this.isWriter = isWriter;
  }

  public int getMask() {
    return mask;
  }

  public int getValue() {
    return value;
  }

  public boolean isReader() {
    return isReader;
  }

  public boolean isWriter() {
    return isWriter;
  }

  public byte read(int port, byte[] attached) {
    return -1;
  }
  public void write(int port, byte value) {
  }
}
