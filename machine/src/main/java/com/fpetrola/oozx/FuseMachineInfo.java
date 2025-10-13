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

public class FuseMachineInfo implements IFuseMachineInfo {
  private RamInfo ramInfo;
  private Libspectrum.Machine machine = Libspectrum.Machine._48K; // libspectrum_machine
  private String id; // Used to select from command line
  private int capabilities; // Capabilities of this machine
  private Runnable reset; // Reset function
  private boolean timex; // Timex machine (keyboard emulation/loading sounds etc.)
  private MachineTimings timings = new MachineTimings(); // How long do things take to happen?
  private long[] lineTimes; // Redraw line y this many tstates after interrupt
  private Runnable shutdown; // Shutdown function
  private Runnable memoryMap; // Memory map function

  public FuseMachineInfo(Display display) {
    lineTimes = new long[display.SCREEN_HEIGHT + 1];
  }

  private FuseMachineInfo  fuseMachineInfo= this;

  @Override
  public RamInfo getRamInfo() {
    return ramInfo;
  }

  @Override
  public void setRamInfo(RamInfo ramInfo) {
    this.ramInfo = ramInfo;
  }

  @Override
  public Libspectrum.Machine getMachine() {
    return machine;
  }

  @Override
  public void setMachine(Libspectrum.Machine machine) {
    this.machine = machine;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int getCapabilities() {
    return capabilities;
  }

  @Override
  public void setCapabilities(int capabilities) {
    this.capabilities = capabilities;
  }

  @Override
  public boolean isTimex() {
    return timex;
  }

  @Override
  public void setTimex(boolean timex) {
    this.timex = timex;
  }

  @Override
  public MachineTimings getTimings() {
    return timings;
  }

  @Override
  public long[] getLineTimes() {
    return lineTimes;
  }
}
