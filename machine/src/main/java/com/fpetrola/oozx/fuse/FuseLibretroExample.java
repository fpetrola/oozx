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

import javax.swing.*;
import javax.swing.Timer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FuseLibretroExample {
  LibretroCore core = LibretroCore.INSTANCE;

  private int acounter;
  static SpectrumPanel panel;
  private LibretroCore.bridge_command bridgeCommand;
  private EmulatorCommand lastCommand;
  public static boolean noTest = false;

  public static void main(String[] args) {
    FuseLibretroExample fuseLibretroExample = new FuseLibretroExample();
    CommandHandler.createCommandHandler();
  }

  public void init(CommandHandler commandHandler, LibretroCore aCore) {
    if (noTest)
      panel = getSpectrumPanel();
    aCore.retro_set_environment((cmd, data) -> true);
    aCore.retro_set_video_refresh((data1, width, height, pitch) -> {
      if (noTest)
        panel.updateFrame(data1, width, height, pitch);
    });

    bridgeCommand = (cmd, data) -> {
      if (lastCommand != null && lastCommand instanceof ContinueExecutionCommand) {
        commandHandler.addResultFor(lastCommand, 0);
        lastCommand = null;
      }
      while (true) {
        if (!commandHandler.noCommands()) {
          EmulatorCommand command = commandHandler.pollCommand();
          if (command != null) {
            if (command instanceof ContinueExecutionCommand) {
              lastCommand = command;
              return createBridgeResponse();
            } else {
              Object value = command.execute(aCore);
              if (value != null) {
                commandHandler.addResultFor(command, value);
              }
            }
          }
        }
      }
    };

    if (noTest)
      bridgeCommand = (cmd, data) -> null;
    aCore.retro_set_bridge_command(bridgeCommand);

    aCore.retro_set_audio_sample((l, r) -> { /* ignoramos */ });
    aCore.retro_set_audio_sample_batch((data, frames) -> frames);
    aCore.retro_set_input_poll(() -> {
    });
    aCore.retro_set_input_state((port, device, index, id) -> {
      return (short) 0x00;
    });

    aCore.retro_init();

//    loadGame(aCore, "/home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");
    new Timer(40, e -> {
      aCore.retro_run();
    }).start();
//        aCore.retro_unload_game();
//        aCore.retro_deinit();
//        System.out.println("Ejecución terminada.");
  }

  private BridgeResponse createBridgeResponse() {
    BridgeResponse resp = new BridgeResponse();
    resp.count = new NativeLong(0);
    return resp;
  }

  public static void loadGame(LibretroCore core, String gamePath) {
    try {
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static SpectrumPanel getSpectrumPanel() {
    SpectrumPanel panel = new SpectrumPanel(320, 240);

    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("ZX Spectrum via Libretro");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(panel);
      frame.pack();
      frame.setVisible(true);
    });

    return panel;
  }

  private static void extracted(LibretroCore core) {
    // Obtener puntero a la RAM
    int ramPtr = core.retro_get_memory_data(0);
    long ramSize = core.retro_get_memory_size(0);

    System.out.println("Tamaño de RAM expuesta: " + ramSize + " bytes");

// Leer pantalla (6912 bytes desde 0x4000)
    int screenOffset = 0x4000;
    int screenSize = 6912;

    byte[] screenData = new byte[screenSize];
//    ramPtr.read(screenOffset, screenData, 0, screenSize);

    System.out.println("Leídos " + screenData.length + " bytes de la pantalla");

// Ejemplo: mostrar los primeros 16 bytes en hex
    for (int i = 0; i < 16; i++) {
      System.out.printf("%02X ", screenData[i]);
    }
    System.out.println();
  }
}
