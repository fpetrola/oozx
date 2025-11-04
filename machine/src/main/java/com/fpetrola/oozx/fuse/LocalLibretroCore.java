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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.Fuse;
import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.fuse.bridge.GetTStatesHistory;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.modules.BeanPosition;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Clock;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.google.common.base.Supplier;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.List;

public class LocalLibretroCore implements LibretroCore {
  private EventManager eventManager;

  public static retro_input_state_t retroInputStateT;
  private Display display;
  private Machine machine;
  private Z80 z80;
  private Z80Clock z80Clock;
  private IPeriph periph;
  private Fuse fuse;

  public LocalLibretroCore(EventManager eventManager, Display display, Machine machine, Z80 z80, Z80Clock z80Clock, IPeriph periph, Fuse fuse) {
    this.eventManager = eventManager;
    this.display = display;
    this.machine = machine;
    this.z80 = z80;
    this.z80Clock = z80Clock;
    this.periph = periph;
    this.fuse = fuse;
  }

  public int retro_get_beam_x() {
    return getBeam().x;
  }

  private BeanPosition getBeam() {
    return display.getBeamPosition();
  }

  public int retro_get_beam_y() {
    return getBeam().y;
  }

  public void retro_init() {
    fuse.init();
  }

  public void retro_deinit() {
    fuse.end();
  }

  public int retro_api_version() {
    return 0;
  }

  public void retro_set_bridge_command(bridge_command bridgeCommand) {
    z80.bridgeCommand = bridgeCommand;
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
    z80.loadSnap(game.path);
    return true;
  }

  public void retro_unload_game() {

  }

  public void retro_run() {
    z80.doOpcodes();
    eventManager.eventDoEvents();
  }

  public void retro_reset() {
    z80.reset(0);
  }

  private Memory<WordNumber> getMemory() {
    return getState().getMemory();
  }

  private State<WordNumber> getState() {
    return z80.ooz80.getState();
  }

  private int executePreservingTstates(Supplier<Integer> supplier) {
    List<TStateUpdate> tstatesUpdates = new ArrayList<>(GetTStatesHistory.tstatesUpdates);
    long tstates = z80Clock.getTStates();
    int result = supplier.get();
    z80Clock.setTStates(tstates);
    GetTStatesHistory.tstatesUpdates = tstatesUpdates;
    return result;
  }

  public int retro_get_memory_data(int id) {
    return executePreservingTstates(() -> {
      return getMemory().read((WordNumber) new WordNumber(id), 0).valueXYZ;
    });
  }

  public void retro_set_memory_data(int address, int id) {
    executePreservingTstates(() -> {
      getMemory().write((WordNumber) new WordNumber(address), (WordNumber) new WordNumber(id));
      return 0;
    });
  }

  public int retro_get_memory_data_contended(int id) {
    return getMemory().read((WordNumber) new WordNumber(id), 0).valueXYZ;
  }

  public void retro_set_memory_data_contended(int address, int id) {
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) z80.ooz80.getInstructionFetcher();
    instructionFetcher.setLastExecutedInstruction(null);
    getMemory().write((WordNumber) new WordNumber(address), (WordNumber) new WordNumber(id));
  }

  public long retro_get_memory_size(int id) {
    return 0;
  }

  public void retro_set_register_data(String register, int value) {
    if (register.equals("tstates")) {
      z80Clock.setTStates(value);
      z80.ooz80.getState().clock.setTStates(value);
    } else
      getRegister(register).write((WordNumber) new WordNumber(value));
  }

  public int retro_get_register_data(String register) {
    if (register.equals("tstates")) {
      return (int) z80Clock.getTStates();
    } else if (register.equals("R")) {
      return getRegister(register).read().valueXYZ;
    } else {
      return getRegister(register).read().valueXYZ;
    }
  }

  private Register<WordNumber> getRegister(String register) {
    return getState().getRegister(RegisterName.valueOf(register));
  }

  public void fuse_set_show_frame(boolean v) {

  }

  public int fuse_get_show_frame() {
    return 0;
  }

  public void retro_write_port(int port, int value) {
    periph.writePortInternal(port, (byte) value);

//    getState().getIo().out(WordNumber.createValue(port), WordNumber.createValue(value));
//    Periph.writePortInternal(port, (byte) value);
  }

  public void retro_select_machine(String name) {
    SpectrumMachine spectrumMachine = switch (name) {
      case "48K" -> fuse.spec48;
      case "128K" -> fuse.spec128;
      case "+3" -> fuse.specPlus3;
      case "+2" -> fuse.specPlus2;
      case "+2A" -> fuse.specPlus2a;
      case "+3E" -> fuse.specPlus3e;
      case "48K_NTSC" -> fuse.spec48Ntsc;
      default -> fuse.spec48;
    };
    machine.select(spectrumMachine);
  }

  public void retro_if1_page(boolean in) {

  }

  public int retro_read_lan_port() {
    return 0;
  }

  @Override
  public MemoryPageStructure retro_get_memory_map_read(int i) {
//    return fuse.memory.mapRead[i];
    return null;
  }

  @Override
  public int retro_get_memory_map_write(int i) {
//    return fuse.memory.mapWrite[i];
    return 0;
  }

  @Override
  public int retro_get_memory_map_read_source(int index) {
    return fuse.memory.mapRead[index].source;
  }

  @Override
  public int retro_get_memory_map_write_source(int index) {
    return fuse.memory.mapWrite[index].source;
  }

  @Override
  public int retro_get_memory_map_read_page_num(int index) {
    return fuse.memory.mapRead[index].pageNum;
  }

  @Override
  public int retro_get_memory_map_write_page_num(int index) {
    return fuse.memory.mapWrite[index].pageNum;
  }

  @Override
  public int retro_get_current_screen() {
    return fuse.memory.currentScreen;
  }

  @Override
  public int retro_ram_locked() {
    return fuse.machine.current.getRamInfo().locked ? 1 : 0;
  }

  @Override
  public int retro_get_ula_contention(int i) {
    return fuse.ula.contention[i];
  }

  @Override
  public void retro_set_late_timings(int b) {
    fuse.settings.current.lateTimings = b != 0;
  }

  @Override
  public int retro_get_late_timings() {
    return fuse.settings.current.lateTimings ? 1 : 0;
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
    return z80.ooz80.getState().isIff1();
  }
}
