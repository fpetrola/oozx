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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.modules.Display;

public class FuseMachineInfo {
  RamInfo ramInfo = new RamInfo();

  public Libspectrum.Machine machine = Libspectrum.Machine._48K; // libspectrum_machine
  String id; // Used to select from command line
  public int capabilities; // Capabilities of this machine

  Runnable reset; // Reset function

  public boolean timex; // Timex machine (keyboard emulation/loading sounds etc.)

  public MachineTimings timings = new MachineTimings(); // How long do things take to happen?
  public long[] lineTimes; // Redraw line y this many tstates after interrupt

  public UnattachedPortFn unattachedPort; // What to return if we read from a port which isn't attached to anything

//    Ayinfo ay = new Ayinfo(); // The AY-3-8912 chip
//
//    SpecdrumInfo specdrum = new SpecdrumInfo(); // SpecDrum settings
//
//    CovoxInfo covox = new CovoxInfo(); // Covox settings

  Runnable shutdown; // Shutdown function

  public Runnable memoryMap; // Memory map function

  public FuseMachineInfo(Display display) {
    lineTimes = new long[display.SCREEN_HEIGHT + 1];
  }
}
