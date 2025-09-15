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

package com.fpetrola.oozx.screen;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class FuseLibretroExample {

  // --- Structs de libretro ---
  public static class retro_game_info extends Structure {

    public String path;
    public Pointer data; // puede ser null si usamos path
    public NativeLong size;
    public String meta;

    @Override
    protected List<String> getFieldOrder() {
      return List.of("path", "data", "size", "meta");
    }
  }

  // --- Callbacks ---
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

  // --- API de libretro ---
  public interface LibretroCore extends Library {
    LibretroCore INSTANCE = Native.load("/home/fernando/detodo/desarrollo/m/zx/emus/fuse-libretro/fuse_libretro.so", LibretroCore.class);

    void retro_init();

    void retro_deinit();

    int retro_api_version();

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

    long retro_get_memory_size(int id);

    void fuse_set_show_frame(boolean v);

    int fuse_get_show_frame();
  }

  public static class RetroMessageExt extends Structure {

    public RetroMessageExt(Pointer data) {
      super(data);
    }

    // campos en el mismo orden que en C
    public String msg;          // const char* (JNA lo convierte automáticamente en String UTF-8)
    public int frames;          // unsigned -> int alcanza
    public int priority;        // unsigned -> int
    public int level;           // enum retro_log_level (usar int)
    public int target;          // enum retro_message_target
    public int type;            // enum retro_message_type
    public int id;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("msg", "frames", "priority", "level", "target", "type", "id");
    }
  }

  public static void main(String[] args) throws IOException {
    LibretroCore core = LibretroCore.INSTANCE;

    core.retro_set_environment((cmd, data) -> {
      if (cmd == 1234) {
        RetroMessageExt msg = new RetroMessageExt(data);
        msg.read();
        System.out.println(msg.msg);
      }
      return true;
    });

    SpectrumPanel panel = getSpectrumPanel();
    core.retro_set_video_refresh(panel::updateFrame);

    core.retro_set_audio_sample((l, r) -> { /* ignoramos */ });
    core.retro_set_audio_sample_batch((data, frames) -> frames);
    core.retro_set_input_poll(() -> {
    });
    core.retro_set_input_state((port, device, index, id) -> {
      return (short) 0xff;
    });

    loadGame1(core, "/home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");

    new Timer(20, e -> {
      core.retro_run();
    }).start();
//        core.retro_unload_game();
//        core.retro_deinit();
//        System.out.println("Ejecución terminada.");
  }

  private static void loadGame1(LibretroCore core, String gamePath) throws IOException {
    byte[] romBytes = Files.readAllBytes(Path.of(gamePath));
    Memory buffer = new Memory(romBytes.length);
    buffer.write(0, romBytes, 0, romBytes.length);

    retro_game_info game = new retro_game_info();
    game.path = gamePath;
    game.data = buffer; // dejamos que lo lea del archivo
    game.size = new NativeLong(buffer.size());
    game.meta = null;

    loadGame(core, game, gamePath);
  }

  private static SpectrumPanel getSpectrumPanel() {
    SpectrumPanel panel = new SpectrumPanel(320, 240); // tamaño inicial, se ajustará después
    JFrame frame = new JFrame("ZX Spectrum via Libretro");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(panel);
    frame.pack();
    frame.setVisible(true);
    return panel;
  }

  private static void loadGame(LibretroCore core, retro_game_info game, String gamePath) {
    if (!core.retro_load_game(game)) {
      throw new RuntimeException("No se pudo cargar el snapshot " + gamePath);
    }
    System.out.println("Juego cargado: " + gamePath);
  }

  private static void extracted(LibretroCore core) {
    // Obtener puntero a la RAM
    Pointer ramPtr = core.retro_get_memory_data(0);
    long ramSize = core.retro_get_memory_size(0);

    System.out.println("Tamaño de RAM expuesta: " + ramSize + " bytes");

// Leer pantalla (6912 bytes desde 0x4000)
    int screenOffset = 0x4000;
    int screenSize = 6912;

    byte[] screenData = new byte[screenSize];
    ramPtr.read(screenOffset, screenData, 0, screenSize);

    System.out.println("Leídos " + screenData.length + " bytes de la pantalla");

// Ejemplo: mostrar los primeros 16 bytes en hex
    for (int i = 0; i < 16; i++) {
      System.out.printf("%02X ", screenData[i]);
    }
    System.out.println();
  }
}
