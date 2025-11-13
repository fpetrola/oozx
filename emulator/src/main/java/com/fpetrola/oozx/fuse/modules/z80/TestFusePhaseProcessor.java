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

package com.fpetrola.oozx.fuse.modules.z80;

import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.registers.Register;
import fuse.tstates.PhaseProcessor;

import java.util.function.Supplier;

public class TestFusePhaseProcessor  extends PhaseProcessor {

  public TestFusePhaseProcessor(InstructionFetcher instructionFetcher1, State state1) {
    super(instructionFetcher1, state1);
  }

  public void addMw(int address, int value) {
    getAddEvent(new Event(0, "MW", address, value));
  }

  public void addSingleMc(int time1, int delta, int baseAddress, String description) {
    getAddEvent(new Event(time1, "MC", baseAddress + delta, null, description));
  }

  public void addMr(int address, int value) {
    getAddEvent(new Event(0, "MR", address, value));
  }

  protected void getAddEvent(Event event) {
    event.description = getDescription(event);
    state.addEvent(event);
  }

  protected String getDescription(Event event) {
    if (event.description != null)
      return event.description;
    else
      return switch (event.getType()) {
        case "MR" -> "contend_read";
        case "MW" -> "contend_write";
        case "MC" -> "contend_read_no_mreq";
        default -> "unknown";
      };
  }

  protected Supplier<String> getAddMultipleMcStringSupplier(String description) {
    return () -> "ula " + (description != null ? description : "contend_read_no_mreq");
  }

  public void addMultipleMc(int x, int time1, int delta, int baseAddress, String description) {
    for (int i = 0; i < x; i++) {
      addSingleMc(time1, delta, baseAddress, description);
    }
  }

  public void addMultipleMc(int x, int time1, int delta, Register register, String description) {
    addMultipleMc(x, time1, delta, register.read(), description);
  }

  @Override
  protected void addMultipleMcRegister() {
    addMultipleMc(1, 1, 1, currentRegister, null);
  }

  @Override
  protected void addMultipleMCPC3() {
    addMultipleMc(1, 3, 1, registerPC, "readbyte");
  }

  @Override
  protected void addMultipleMCRegister(int x, int delta1) {
    addMultipleMc(x, 1, delta1, currentRegister, "contend_write_no_mreq");
  }

  @Override
  protected void addMultipleMcAddress() {
    addMultipleMc(1, 1, 0, address, null);
  }

  @Override
  protected void addMultipleMCPc2(int x, int delta) {
    addMultipleMc(x, 1, delta, registerPC, null);
  }

  @Override
  protected void addMultipleMCHL2(int x) {
    addMultipleMc(x, 1, 0, registerHL, null);
  }

  @Override
  protected void addMultipleMCIR(int time) {
    addMultipleMc(time, 1, 0, this.registerIR, null);
  }
}
