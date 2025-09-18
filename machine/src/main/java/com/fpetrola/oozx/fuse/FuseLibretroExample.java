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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FuseLibretroExample {
  LibretroCore core = LibretroCore.INSTANCE;

  private int acounter;
  static SpectrumPanel panel = getSpectrumPanel();

  public static void main(String[] args) throws Exception {
    FuseLibretroExample fuseLibretroExample = new FuseLibretroExample();
    fuseLibretroExample.init();
  }

  private void init() throws IOException {

    core.retro_set_environment((cmd, data) -> {
      if (cmd == 1234) {
        RetroMessageExt msg = new RetroMessageExt(data);
        msg.read();
        System.out.println(msg.msg);
      } else if (cmd == 1235 && acounter++ % 1 == 0) {
        EmulatorState msg = new EmulatorState(data);
        msg.read();
//        System.out.println(msg.tstates & 0xffffff);
      }
      return true;
    });
    core.retro_set_video_refresh((data1, width, height, pitch) -> {
      panel.updateFrame(data1, width, height, pitch);
    });

    core.retro_set_bridge_command((cmd, data) -> {
//      EmulatorState msg = new EmulatorState(data);
//      msg.read();
//      System.out.println(msg.tstates & 0xffffff);
      int i = ++acounter % 3;
      // Respuesta
      BridgeResponse resp = new BridgeResponse();
      if (i == 2) {
        resp.count = new NativeLong(0);
        return resp;
      } else {
        int i1 = core.retro_api_version();
        System.out.println(i1);
        addCommands(resp, createWriteMemoryCommand(), createChangePCCommand());
        resp.write();
      }

      return resp;
    });

    core.retro_set_audio_sample((l, r) -> { /* ignoramos */ });
    core.retro_set_audio_sample_batch((data, frames) -> frames);
    core.retro_set_input_poll(() -> {
    });
    core.retro_set_input_state((port, device, index, id) -> {
      return (short) 0x00;
    });

    loadGame(core, "/home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");
    core.retro_init();
    new Timer(10, e -> {
      core.retro_run();
    }).start();
//        core.retro_unload_game();
//        core.retro_deinit();
//        System.out.println("Ejecución terminada.");
  }

  private static void addCommands(BridgeResponse resp, BridgeCommand... commands) {
    BridgeCommand[] cmds = (BridgeCommand[]) (new BridgeCommand()).toArray(commands.length);
    for (int i = 0; i < commands.length; i++) {
      setCommandAt(cmds, i, commands[i]);
    }

    resp.count = new NativeLong(commands.length);
    resp.commands = cmds[0].getPointer();
  }

  private static void setCommandAt(BridgeCommand[] cmds, int x, BridgeCommand cmd1) {
    cmds[x].type = cmd1.type;
    cmds[x].data = cmd1.data;
    cmds[x].write();
  }

  private static BridgeCommand createChangePCCommand() {
    BridgeCommand cmd2 = new BridgeCommand();
    cmd2.type = CommandType.CMD_CHANGE_PC;
    cmd2.data = new CommandData();
    cmd2.data.setType(ChangePCCommand.class);
    cmd2.data.changePC = new ChangePCCommand(0x5678);
    cmd2.data.changePC.write();
    return cmd2;
  }

  private static BridgeCommand createWriteMemoryCommand() {
    BridgeCommand cmd1 = new BridgeCommand();
    cmd1.type = CommandType.CMD_WRITE_MEMORY;
    cmd1.data = new CommandData();
    cmd1.data.setType(WriteMemoryCommand.class);
    cmd1.data.writeMemory = new WriteMemoryCommand(1234, (byte) 42);
    cmd1.data.writeMemory.write();
    return cmd1;
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
