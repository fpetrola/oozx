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

package com.fpetrola.oozx.speccy.bridge;

import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.modules.memory.MemoryPart;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.display.BeamPosition;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.ports.MachinePortBus;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Clock;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.google.common.base.Supplier;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;

public class LocalLibretroCore implements LibretroCore {
  private Scheduler scheduler;

  public static retro_input_state_t retroInputStateT;
  private Display display;
  private Machine machine;
  private Cpu cpu;
  private Z80Clock z80Clock;
  private MachinePortBus ports;
  private Speccy speccy;

  public LocalLibretroCore(Scheduler scheduler, Display display, Machine machine, Cpu cpu, Z80Clock z80Clock, MachinePortBus ports, Speccy speccy) {
    this.scheduler = scheduler;
    this.display = display;
    this.machine = machine;
    this.cpu = cpu;
    this.z80Clock = z80Clock;
    this.ports = ports;
    this.speccy = speccy;
  }

  public int retro_get_beam_x() {
    return getBeam().x;
  }

  private BeamPosition getBeam() {
    return display.getBeamPosition();
  }

  public int retro_get_beam_y() {
    return getBeam().y;
  }

  public void retro_init() {
    speccy.init();
  }

  public void retro_deinit() {
    speccy.end();
  }

  public int retro_api_version() {
    return 0;
  }

  /** Told before every instruction, through the trap the machine offers anything that wants to watch an address: here, all of them. */
  public void retro_set_bridge_command(bridge_command bridgeCommand) {
    if (bridgeCommand != bridge_command.NONE)
      cpu.beforeFetch().watch(0x0000, 0xFFFF, pc -> bridgeCommand.invoke(0, null));
  }

  public void retro_set_environment(retro_environment_t cb) {

  }

  public void retro_set_video_refresh(retro_video_refresh_t cb) {

  }

  public void retro_set_audio_sample(retro_audio_sample_t cb) {

  }

  public void retro_set_audio_sample_batch(retro_audio_sample_batch_t cb) {
  }

  public void retro_set_input_poll(retro_input_poll_t cb) {

  }

  public void retro_set_input_state(retro_input_state_t retroInputStateT) {
    this.retroInputStateT = retroInputStateT;
  }

  public void retro_get_system_info(Pointer info) {

  }

  public void retro_get_system_av_info(Pointer avInfo) {

  }

  public boolean retro_load_game(retro_game_info game) {
    Snapshots.of(speccy).load(game.path);
    return true;
  }

  public void retro_unload_game() {

  }

  public void retro_run() {
    speccy.loop.doOpcodes();
    scheduler.runDue();
  }

  public void retro_reset() {
    cpu.reset(0);
  }

  private Memory getMemory() {
    return getState().getMemory();
  }

  private State getState() {
    return cpu.getOoz80().getState();
  }

  private int executePreservingTstates(Supplier<Integer> supplier) {
    List<TStateUpdate> tstatesUpdates = new ArrayList<>(GetTStatesHistory.tstatesUpdates);
    int tstates = z80Clock.getTStates();
    int result = supplier.get();
    z80Clock.setTStates(tstates);
    GetTStatesHistory.tstatesUpdates = tstatesUpdates;
    return result;
  }

  public int retro_get_memory_data(int id) {
    return executePreservingTstates(() -> {
      return getMemory().read(id, 0);
    });
  }

  public void retro_set_memory_data(int address, int id) {
    executePreservingTstates(() -> {
      getMemory().write(address, id);
      return 0;
    });
  }

  public int retro_get_memory_data_contended(int id) {
    return getMemory().read(id, 0);
  }

  public void retro_set_memory_data_contended(int address, int id) {
    getMemory().write(address, id);
  }

  public long retro_get_memory_size(int id) {
    return 0;
  }

  public void retro_set_register_data(String register, int value) {
    if (register.equals("tstates")) {
      z80Clock.setTStates(value);
      cpu.getOoz80().getState().clock.setTStates(value);
    } else
      getRegister(register).write(value);
  }

  public int retro_get_register_data(String register) {
    if (register.equals("tstates")) {
      return (int) z80Clock.getTStates();
    } else if (register.equals("R")) {
      return getRegister(register).read();
    } else {
      return getRegister(register).read();
    }
  }

  private Register getRegister(String register) {
    return getState().getRegister(RegisterName.valueOf(register));
  }

  public void fuse_set_show_frame(boolean v) {

  }

  public int fuse_get_show_frame() {
    return 0;
  }

  public void retro_write_port(int port, int value) {
    ports.writeInternal(port, (byte) value);

//    getState().getIo().out(WordNumber.createValue(port), WordNumber.createValue(value));
  }

  public void retro_select_machine(String name) {
    machine.forShortName(name).ifPresentOrElse(machine::select, machine::selectDefault);
  }

  public void retro_if1_page(boolean in) {

  }

  public int retro_read_lan_port() {
    return 0;
  }

  @Override
  public MemoryPageStructure retro_get_memory_map_read(int i) {
//    return speccy.memory.mapRead[i];
    return null;
  }

  @Override
  public int retro_get_memory_map_write(int i) {
//    return speccy.memory.mapWrite[i];
    return 0;
  }

  @Override
  public int retro_get_memory_map_read_source(int index) {
    return sourceOf(speccy.memory.reading(index << 11).memory());
  }

  @Override
  public int retro_get_memory_map_write_source(int index) {
    return sourceOf(speccy.memory.writing(index << 11).memory());
  }

  /** Fuse's memory sources, by the order it registers them: ROM, RAM, and anything else. */
  private static int sourceOf(MemoryPart bank) {
    return bank instanceof Rom ? 0 : bank instanceof Ram ? 1 : 2;
  }

  @Override
  public int retro_get_memory_map_read_page_num(int index) {
    return speccy.memory.reading(index << 11).memory().pageNum;
  }

  @Override
  public int retro_get_memory_map_write_page_num(int index) {
    return speccy.memory.writing(index << 11).memory().pageNum;
  }

  @Override
  public int retro_get_current_screen() {
    return speccy.banks.shown().pageNum;
  }

  @Override
  public int retro_ram_locked() {
    return speccy.machine.current.paging().locked() ? 1 : 0;
  }

  @Override
  public int retro_get_ula_contention(int i) {
    return speccy.ula.contention.delay[i];
  }

  @Override
  public void retro_set_late_timings(int b) {
    speccy.machine.unit.lateTimings = b != 0;
  }

  @Override
  public int retro_get_late_timings() {
    return speccy.machine.unit.lateTimings ? 1 : 0;
  }


  @Override
  public Pointer retro_tstates_history() {
    return null;
  }

  @Override
  public void retro_tstates_history_init() {
    GetTStatesHistory.tstatesUpdates.clear();
  }

  @Override
  public boolean retro_is_intruption_enabled() {
    return cpu.getOoz80().getState().isIff1();
  }
}
