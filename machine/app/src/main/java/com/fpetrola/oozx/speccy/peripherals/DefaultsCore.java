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

import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a machine starts with when nobody has configured it, answering as if it were a machine so
 * that the same controls can be pointed at it.
 * <p>
 * Nothing here is running, so what is set is written where the next machine will read it: the
 * screen's defaults and the configuration file. What this cannot answer is inherited from the
 * stand-in core and does nothing, which is the same thing that happens to it on a real machine
 * today - and why those controls are shown disabled.
 */
public class DefaultsCore extends MockEmulatorCore {
  private final OOZxConfiguration config;

  public DefaultsCore(OOZxConfiguration config) {
    super(null);
    this.config = config;
  }

  /** Nothing is running, so the devices answer with what the file says they will start with. */
  @Override
  public java.util.List<com.fpetrola.oozx.config.Settings.Configurable> deviceSettings() {
    return com.fpetrola.oozx.config.Settings.defaults();
  }

  @Override
  public void setVideoOption(String option, Object value) {
    Map<String, String> screen = new LinkedHashMap<>(config.getScreenDefaults());
    screen.put(option, String.valueOf(value));
    ScreenSettings.setDefaults(screen);
    config.setScreenDefaults(screen);
  }

  @Override
  public void setGeneralOption(String option, Object value) {
    if (option.equals("turbo")) {
      config.setTurboByDefault((Boolean) value);
    } else {
      super.setGeneralOption(option, value);
    }
  }

  @Override
  public boolean isTurboMode() {
    return config.isTurboByDefault();
  }
}
