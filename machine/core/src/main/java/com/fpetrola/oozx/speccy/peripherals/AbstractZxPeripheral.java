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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.speccy.ports.PortHandler;

import java.util.List;

public class AbstractZxPeripheral implements ZxPeripheral {
  protected final Periph.Type type;
  private PortHandler[] portHandlers;

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

  /**
   * For a peripheral whose ports talk back to it: they cannot be handed to super, because
   * there is no this to give them yet.
   */
  protected void ports(PortHandler... handlers) {
    this.portHandlers = handlers;
  }

  public PortHandler[] getPorts() {
    return portHandlers;
  }

  /** Built in, or not offered: either way nobody is asked. A peripheral that can be plugged in says so. */
  public boolean isWanted() {
    return false;
  }

  public boolean hasHardReset() {
    return false;
  }
}
