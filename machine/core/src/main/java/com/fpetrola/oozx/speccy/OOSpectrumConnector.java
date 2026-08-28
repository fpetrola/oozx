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

package com.fpetrola.oozx.speccy;

import com.fpetrola.oozx.speccy.bridge.ContinueExecutionCommand;
import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.EmulatorCommand;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class OOSpectrumConnector {
  public static LibretroCore core = LibretroCore.INSTANCE;
  public static boolean noTest;
  public static List<double[]> localData = new ArrayList<>();
  public static List<double[]> remoteData = new ArrayList<>();
  String device = "buffer=8192,frames=4,verbose";
  private int counter;

  public static void main(String[] args) {
    noTest = true;
    new OOSpectrumConnector();
    DefaultCommandHandler.createCommandHandler(SpeccyBaseForTests.createSpeccy());
  }

  public static void sendData(double data) {
//    localData.add(new double[]{data});
  }

  public static void sendData(double[] data) {
    localData.add(data);
  }

  public void init(DefaultCommandHandler commandHandler, LibretroCore aCore) {

    JavaSoundDevice javaSoundDevice = new JavaSoundDevice();

    int[] freq = {44100};
    int[] ay = {0};
    javaSoundDevice.sound_lowlevel_init(device, freq, ay);

//    core.retro_set_register_data("name", 1);
//    core.retro_tstates_history_init();

//    SpectrumPanel panel = getSpectrumPanel(noTest);
//    aCore.retro_set_video_refresh((data1, width, height, pitch) -> {
////      if (noTest)
////        panel.updateFrame(data1, width, height, pitch);
//    });
//    aCore.retro_set_environment((cmd, data) -> {
//      return true;
//    });

    LibretroCore.bridge_command bridgeCommand;

    LibretroCore.bridge_command bridgeCommand1 = (cmd, data) -> {
      DefaultCommandHandler commandHandler1 = commandHandler;
      if (commandHandler1.lastCommand instanceof ContinueExecutionCommand) {
        commandHandler1.addResultFor(commandHandler1.lastCommand, 0);
        commandHandler1.lastCommand = null;
      }
      while (true) {
        if (!commandHandler1.noCommands()) {
          EmulatorCommand command = commandHandler1.pollCommand();
          if (command != null) {
            if (command instanceof ContinueExecutionCommand) {
              commandHandler1.lastCommand = command;
              return createBridgeResponse();
            } else {
              Object value = command.execute(aCore);
              if (value != null) {
                commandHandler1.addResultFor(command, value);
              }
            }
          }
        }
      }
    };
    bridgeCommand = bridgeCommand1;

//    if (testSteps != 0) {
//      counter = testSteps;
//      bridgeCommand = (cmd, data) -> {
//        counter--;
//        if (counter > 0)
//          return null;
//        else {
//          counter = testSteps;
//          return bridgeCommand1.invoke(cmd, data);
//        }
//      };
//    }

    if (noTest)
      bridgeCommand = (cmd, data) -> null;

    aCore.retro_set_bridge_command(bridgeCommand);

    aCore.retro_set_audio_sample((l, r) -> {
    });
    aCore.retro_set_audio_sample_batch((data, frames) -> {
      Pointer pointer = data.getPointer();
      double[] shortArray = pointer.getDoubleArray(0, (int) frames);
      remoteData.add(shortArray);
//      System.out.println("remote:" + Arrays.toString(shortArray));
      int[] intArray = new int[shortArray.length];
      for (int i = 0; i < shortArray.length; i++) {
        intArray[i] = (int) shortArray[i];
      }
      javaSoundDevice.sound_lowlevel_frame(intArray, shortArray.length);
      return frames;
    });
    aCore.retro_set_input_poll(() -> {
    });
    aCore.retro_set_input_state((port, device, index, id) -> {
//      System.out.printf("%d %d %d %d %d\n", port, device, index, id, port & 0xff);
      return (short) 0x01;
    });


//    aCore.retro_api_version();
    aCore.retro_init();

//    loadGame(aCore, "/home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80");

//    while (true)
//    {
//      aCore.retro_run();
//    }

//    new Timer(40, (a) -> {
//      aCore.retro_run();
//    }).start();

    newSingleThreadScheduledExecutor()
        .submit(() -> {
          while (true)
            aCore.retro_run();
        });
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

  private static SpectrumPanel getSpectrumPanel(boolean noTest) {
    SpectrumPanel panel = new SpectrumPanel(320, 240);

    if (noTest)
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
