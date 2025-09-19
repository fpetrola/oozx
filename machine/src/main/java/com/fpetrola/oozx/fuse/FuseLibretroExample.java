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
  private LibretroCore.bridge_command bridgeCommand;
  private EmulatorCommand lastCommand;
  private CommandHandler commandHandler;

  public static void main(String[] args) throws Exception {
    FuseLibretroExample fuseLibretroExample = new FuseLibretroExample();
    CommandHandler.createCommandHandler();
  }

  public void init(CommandHandler commandHandler) {
    this.commandHandler = commandHandler;

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

    bridgeCommand = (cmd, data) -> {
//      EmulatorState msg = new EmulatorState(data);
//      msg.read();
//      System.out.println(msg.tstates & 0xffffff);
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
              Integer value = executeCommand(command);
              if (value != null) {
                commandHandler.addResultFor(command, value);
              }
            }
          }
        }
      }
    };
    core.retro_set_bridge_command(bridgeCommand);

    core.retro_set_audio_sample((l, r) -> { /* ignoramos */ });
    core.retro_set_audio_sample_batch((data, frames) -> frames);
    core.retro_set_input_poll(() -> {
    });
    core.retro_set_input_state((port, device, index, id) -> {
      return (short) 0x00;
    });

    core.retro_init();

    try {
      loadGame(core, "/home/fernando/detodo/desarrollo/m/zx/roms/aqua.z80");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    new Timer(10, e -> {
      core.retro_run();
    }).start();
//        core.retro_unload_game();
//        core.retro_deinit();
//        System.out.println("Ejecución terminada.");
  }

  private BridgeResponse createBridgeResponse() {
    BridgeResponse resp = new BridgeResponse();
    resp.count = new NativeLong(0);
    return resp;
  }

  private Integer executeCommand(EmulatorCommand command) {
    if (command instanceof WriteMemoryCommand writeMemory) {
      if (writeMemory.contended) {
        core.retro_set_memory_data_contended(writeMemory.address, writeMemory.value);
      } else
        core.retro_set_memory_data(writeMemory.address, writeMemory.value);
      return null;
    } else if (command instanceof ReadMemoryCommand readMemoryCommand) {
      if (readMemoryCommand.contended) {
        return core.retro_get_memory_data_contended(readMemoryCommand.address);
      } else
        return core.retro_get_memory_data(readMemoryCommand.address);
    } else if (command instanceof ContinueExecutionCommand) {
      return null;
    } else if (command instanceof SetRegisterValue setRegisterValue) {
      core.retro_set_register_data(setRegisterValue.name, setRegisterValue.value);
      return null;
    } else if (command instanceof GetRegisterValue getRegisterValue) {
      return core.retro_get_register_data(getRegisterValue.name);
    } else if (command instanceof WritePortCommand writePortCommand) {
      if (writePortCommand.contended) {
        core.retro_write_port(writePortCommand.port, writePortCommand.value);
      } else
        core.retro_write_port(writePortCommand.port, writePortCommand.value);
      return null;
    } else if (command instanceof SetMachineModel setMachineModel) {
      core.retro_select_machine(setMachineModel.model);
      return null;
    } else if (command instanceof If1Page if1PageIn) {
        core.retro_if1_page(if1PageIn.in);
      return null;
    } else if (command instanceof ReadLanPortCommand readLanPortCommand) {
      return core.retro_read_lan_port();
    } else if (command instanceof GetBeamX getBeamPosition) {
      return core.retro_get_beam_x();
    } else if (command instanceof GetBeamY getBeamPosition) {
      return core.retro_get_beam_y();
    }

    return null;
  }

  private BridgeCommand createCommandWrapper(EmulatorCommand command) {
    return null;
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
    cmd2.data.setType(SetRegisterValue.class);
    cmd2.data.changePC = new SetRegisterValue("PC", 0x5678);
//    cmd2.data.changePC.write();
    return cmd2;
  }

  private static BridgeCommand createWriteMemoryCommand(WriteMemoryCommand writeMemory) {
    BridgeCommand cmd1 = new BridgeCommand();
    cmd1.type = CommandType.CMD_WRITE_MEMORY;
    cmd1.data = new CommandData();
    cmd1.data.setType(WriteMemoryCommand.class);
    cmd1.data.writeMemory = writeMemory;
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
