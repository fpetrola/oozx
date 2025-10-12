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

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.sun.jna.Pointer;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class LocalLibretroCore implements LibretroCore {
  private static EventManager eventManager= Fuse.eventManager;

  public static boolean noContended = false;
  public static retro_input_state_t retroInputStateT;
  private Display display= Fuse.display;

  public LocalLibretroCore() {
  }

  public int retro_get_beam_x() {
    return getBeam()[0];
  }

  private int[] getBeam() {
    return display.getBeamPosition();
  }

  public int retro_get_beam_y() {
    return getBeam()[1];
  }

  public void retro_init() {
    Fuse.fuseInit(new String[]{});
  }

  public void retro_deinit() {

  }

  public int retro_api_version() {
    return 0;
  }

  public void retro_set_bridge_command(bridge_command bridgeCommand) {
    Z80.bridgeCommand = bridgeCommand;
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
    Z80.loadSnap(game.path);
    return true;
  }

  public void retro_unload_game() {

  }

  public void retro_run() {
    com.fpetrola.oozx.Z80.doOpcodes();
    eventManager.eventDoEvents();
  }

  public void retro_reset() {

  }

  private Memory<WordNumber> getMemory() {
    return getState().getMemory();
  }

  private State<WordNumber> getState() {
    return Z80.ooz80.getState();
  }

  public int retro_get_memory_data(int id) {
    noContended = true;
    int i = getMemory().getData()[id].intValue();
    noContended = false;
    return i;
  }

  public int retro_get_memory_data_contended(int id) {
    int i = getMemory().read(createValue(id), 0).intValue();
    return i;
  }

  public void retro_set_memory_data(int address, int id) {
    noContended = true;
    getMemory().write(createValue(address), createValue(id));
    noContended = false;
  }

  public void retro_set_memory_data_contended(int address, int id) {
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) Z80.ooz80.getInstructionFetcher();
    instructionFetcher.instruction2 = null;
    getMemory().write(createValue(address), createValue(id));
  }

  public long retro_get_memory_size(int id) {
    return 0;
  }

  public void retro_set_register_data(String register, int value) {
    if (register.equals("tstates")) {
      Spectrum.tstates = value;
      Z80.ooz80.getState().tstates = value;
    } else
      getRegister(register).write(createValue(value));
  }

  public int retro_get_register_data(String register) {
    if (register.equals("tstates")) {
      return (int) Spectrum.tstates;
    } else if (register.equals("R")) {
      return getRegister(register).read().intValue();
    } else
      return getRegister(register).read().intValue();
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
    Periph.writePortInternal(port, (byte) value);

//    getState().getIo().out(WordNumber.createValue(port), WordNumber.createValue(value));
//    Periph.writePortInternal(port, (byte) value);
  }

  public void retro_select_machine(String name) {
    Libspectrum.Machine a = Libspectrum.Machine._48K;

    if (name.equals("48K")) {
      a = Libspectrum.Machine._48K;
    } else if (name.equals("48K NTSC")) {
      a = Libspectrum.Machine._48K_NTSC;
    } else if (name.equals("128K")) {
      a = Libspectrum.Machine._128K;
    } else if (name.equals("PLUS2")) {
      a = Libspectrum.Machine.PLUS2;
    } else if (name.equals("+3")) {
      a = Libspectrum.Machine.PLUS3;
    }

    Machine.select(a.ordinal());
  }

  public void retro_if1_page(boolean in) {

  }

  public int retro_read_lan_port() {
    return 0;
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
    return Z80.ooz80.getState().isIff1();
  }
}
