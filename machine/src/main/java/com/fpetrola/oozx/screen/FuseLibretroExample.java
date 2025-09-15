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

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FuseLibretroExample {

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

    loadGame(core, "/home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");

    new Timer(20, e -> {
      core.retro_run();
    }).start();
//        core.retro_unload_game();
//        core.retro_deinit();
//        System.out.println("Ejecución terminada.");
  }

  private static void loadGame(LibretroCore core, String gamePath) throws IOException {
    byte[] romBytes = Files.readAllBytes(Path.of(gamePath));
    Memory buffer = new Memory(romBytes.length);
    buffer.write(0, romBytes, 0, romBytes.length);

    retro_game_info game = new retro_game_info();
    game.path = gamePath;
    game.data = buffer;
    game.size = new NativeLong(buffer.size());
    game.meta = null;

    if (!core.retro_load_game(game)) {
      throw new RuntimeException("No se pudo cargar el snapshot " + gamePath);
    }
    System.out.println("Juego cargado: " + gamePath);
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
