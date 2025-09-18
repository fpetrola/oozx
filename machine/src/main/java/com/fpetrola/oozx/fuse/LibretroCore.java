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

import com.sun.jna.*;
import com.sun.jna.ptr.ShortByReference;

import java.util.List;

// --- API de libretro ---
public interface LibretroCore extends Library {
  LibretroCore INSTANCE = Native.load("/home/fernando/detodo/desarrollo/m/zx/emus/fuse-libretro/fuse_libretro.so", LibretroCore.class);

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

  Pointer retro_get_memory_data(int id);

  void retro_set_memory_data(int address, int id);

  long retro_get_memory_size(int id);

  void retro_set_register_data(String register, int value);

  void fuse_set_show_frame(boolean v);

  int fuse_get_show_frame();

  public interface bridge_command extends Callback {
    BridgeResponse invoke(int cmd, Pointer data);
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
}
