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

package com.fpetrola.oozx.fuse.peripherals;

import com.fpetrola.oozx.fuse.ports.PortHandler;

import java.util.List;

public class AbstractZxPeripheral implements ZxPeripheral {
  protected final Periph.Type type;
  private final PortHandler[] portHandlers;

  public AbstractZxPeripheral(Periph.Type type, List<PortHandler> portHandlers) {
    this.type = type;
    this.portHandlers = portHandlers.toArray(new PortHandler[0]);
  }

  public Periph.Type getType() {
    return type;
  }

  public boolean canActivate() {
    return false;
  }

  public void activate() {
  }

  public PortHandler[] getPorts() {
    return portHandlers;
  }

  public boolean hasOption() {
    return false;
  }

  public boolean[] getOption() {
    return new boolean[0];
  }

  public boolean hasHardReset() {
    return false;
  }
}
