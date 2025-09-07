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

package com.fpetrola.oozx.speccy.ports;

public abstract class DefaultPortHandler implements PortHandler {
  protected boolean isReader;
  protected boolean isWriter;

  public DefaultPortHandler(boolean isReader, boolean isWriter) {
    this.isReader = isReader;
    this.isWriter = isWriter;
  }

  public boolean isReader() {
    return isReader;
  }

  public boolean isWriter() {
    return isWriter;
  }

  public BusAnswer read(int port) {
    return BusAnswer.NONE;
  }
  public void write(int port, byte value) {
  }
}
