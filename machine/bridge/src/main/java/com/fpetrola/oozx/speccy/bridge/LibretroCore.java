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

package com.fpetrola.oozx.speccy.bridge;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

// --- API de libretro ---
public interface LibretroCore extends Library {
  int retro_get_beam_x();
  int retro_get_beam_y();

//  LibretroCore INSTANCE = Native.load("/home/fernando/detodo/desarrollo/m/zx/emus/fuse-libretro/fuse_libretro.so", LibretroCore.class);
  LibretroCore INSTANCE = null;

  void retro_init();

  void retro_deinit();

  int retro_api_version();

  void retro_set_bridge_command(bridge_command cb);

  void retro_set_environment(retro_environment_t cb);

  void retro_set_video_refresh(retro_video_refresh_t cb);

  void retro_set_audio_sample(retro_audio_sample_t cb);

  void retro_set_audio_sample_batch(retro_audio_sample_batch_t cb);

  void retro_set_input_poll(retro_input_poll_t cb);

  void retro_set_input_state(retro_input_state_t cb);

  void retro_get_system_info(Pointer info);

  void retro_get_system_av_info(Pointer avInfo);

  boolean retro_load_game(retro_game_info game);

  void retro_unload_game();

  void retro_run();

  void retro_reset();

  int retro_get_memory_data(int id);
  int retro_get_memory_data_contended(int id);

  void retro_set_memory_data(int address, int id);
  void retro_set_memory_data_contended(int address, int id);

  long retro_get_memory_size(int id);

  void retro_set_register_data(String register, int value);
  int retro_get_register_data(String register);

  void fuse_set_show_frame(boolean v);

  int fuse_get_show_frame();

  void retro_write_port(int port, int value);
  void retro_select_machine(String type);

  void retro_if1_page(boolean in);

  int retro_read_lan_port();

  MemoryPageStructure retro_get_memory_map_read(int index);
  int retro_get_memory_map_write(int index);
  int retro_get_memory_map_read_source(int index);
  int retro_get_memory_map_write_source(int index);

  int retro_get_memory_map_read_page_num(int index);
  int retro_get_memory_map_write_page_num(int index);

  int retro_get_current_screen();
  int retro_ram_locked();

  int retro_get_ula_contention(int i);

  void retro_set_late_timings(int b);

  int retro_get_late_timings();

  public interface bridge_command extends Callback {
    BridgeResponse invoke(int cmd, Pointer data);

    /** Nobody on the other side: what a machine runs with unless a driver is connected to it. */
    bridge_command NONE = (cmd, data) -> null;
  }

  public interface retro_environment_t extends Callback {
    boolean invoke(int cmd, Pointer data);
  }

  public interface retro_video_refresh_t extends Callback {
    void invoke(Pointer data, int width, int height, long pitch);
  }

  public interface retro_audio_sample_t extends Callback {
    void invoke(short left, short right);
  }

  public interface retro_audio_sample_batch_t extends Callback {
    long invoke(ShortByReference data, long frames);
  }

  public interface retro_input_poll_t extends Callback {
    void invoke();
  }

  public interface retro_input_state_t extends Callback {
    short invoke(int port, int device, int index, int id);
  }

  Pointer retro_tstates_history();
  void retro_tstates_history_init();

  default boolean retro_is_intruption_enabled(){
      return false;
  }
}
