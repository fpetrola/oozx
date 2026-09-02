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

/** The usual way of being {@link Pluggable}: a flag, which is what {@link #isWanted} answers. */
public abstract class PluggablePeripheral extends AbstractPeripheral implements Pluggable {
  private boolean pluggedIn;

  protected PluggablePeripheral(List<PortHandler> ports) {
    super(ports);
  }

  @Override
  public void plugIn(boolean connected) {
    pluggedIn = connected;
  }

  @Override
  public boolean isPluggedIn() {
    return pluggedIn;
  }

  @Override
  public boolean isWanted() {
    return pluggedIn;
  }
}
