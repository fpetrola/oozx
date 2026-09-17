/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
