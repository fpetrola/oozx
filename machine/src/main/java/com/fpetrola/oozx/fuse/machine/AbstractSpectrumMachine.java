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

package com.fpetrola.oozx.fuse.machine;

import com.fpetrola.oozx.Libspectrum;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.fuse.modules.Display;

public abstract class AbstractSpectrumMachine implements SpectrumMachine {
  protected final MachineTimings timings = new MachineTimings(); // How long do things take to happen?
  protected final long[] lineTimes; // Redraw line y this many tstates after interrupt
  protected final Settings settings;
  protected RamInfo ramInfo;

  public AbstractSpectrumMachine(Display display, Settings settings, RamInfo ramInfo) {
    lineTimes = new long[display.SCREEN_HEIGHT + 1];
    this.settings = settings;
    this.ramInfo = ramInfo;
  }


  public RamInfo getRamInfo() {
    return ramInfo;
  }

  public int getCapabilities() {
    return Libspectrum.machineCapabilities(this);
  }

  public MachineTimings getTimings() {
    return timings;
  }

  public long[] getLineTimes() {
    return lineTimes;
  }

  protected RamInfo getCurrentRamInfo() {
    return getRamInfo();
  }
}
