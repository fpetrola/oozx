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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.Fuse;
import com.fpetrola.oozx.fuse.modules.tape.Log1;
import com.fpetrola.oozx.fuse.modules.tape.Tape;
import com.fpetrola.oozx.fuse.peripherals.EmulatorCore;
import com.fpetrola.oozx.fuse.peripherals.t.DownloadAndUnzip;
import com.fpetrola.oozx.fuse.peripherals.t.ZXSpectrumDesktopApp;
import com.fpetrola.z80.memory.Memory;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.SolarizedLightTheme;
import snapshots.SpectrumState;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class OOSpectrumLauncher {
  private ScheduledExecutorService scheduledExecutorService = newScheduledThreadPool(10);

  public static void main(String[] args) {
    OOSpectrumLauncher ooSpectrumLauncher = new OOSpectrumLauncher();
    ooSpectrumLauncher.init();
  }

  public void init() {
    OOSpectrumConnector.noTest = true;
    LafManager.install(new SolarizedLightTheme());

    Function<SpectrumState, EmulatorCore> mockCoreState = spectrumState -> {
      Fuse fuse = createFuse2(spectrumState);
      return fuse.z80.mockCore;
    };
    ZXSpectrumDesktopApp zxSpectrumDesktopApp = new ZXSpectrumDesktopApp((filename) -> {
      String string = null;

      if (filename.isBlank()) {
        string = extracted();
      } else if (!filename.isBlank() && filename.contains("http")) {
        Path unzip = new DownloadAndUnzip().unzip(filename);
//      String snapshotFile = Helper.getSnapshotFile(filename);
        string = unzip.toAbsolutePath().toString();
      } else string = filename;
      Fuse fuse = createFuse(string);
      EmulatorCore mockCore = fuse.z80.mockCore;
      mockCore.setFilename(string);
      return mockCore;
    }, mockCoreState);

    showOnScreen(0, zxSpectrumDesktopApp);
//    zxSpectrumDesktopApp.setVisible(true);
  }

  private static void showOnScreen(int screen, Window frame) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] gd = ge.getScreenDevices();
    GraphicsDevice graphicsDevice;
    if (screen > -1 && screen < gd.length) {
      graphicsDevice = gd[screen];
    } else if (gd.length > 0) {
      graphicsDevice = gd[0];
    } else {
      throw new RuntimeException("No Screens Found");
    }
    Rectangle bounds = graphicsDevice.getDefaultConfiguration().getBounds();
    int screenWidth = graphicsDevice.getDisplayMode().getWidth();
    int screenHeight = graphicsDevice.getDisplayMode().getHeight();
    frame.setLocation(bounds.x + (screenWidth - frame.getPreferredSize().width) / 2,
        bounds.y + (screenHeight - frame.getPreferredSize().height) / 2);
    frame.setVisible(true);
  }

  private String extracted() {
    String[] games = {"emlyn.z80", "dynamitedan.z80", "equinox.z80", "tge.z80", "wally.z80", "jsw.z80", "agentx2.z80", "rickdangerous.z80", "darkfusion.z80", "trantor.z80"};
//    String[] games = {"rickdangerous.z80"};
//    String[] games = {"jsw.z80"};

    games = new String[]{"darkfusion.z80", "trantor.z80", "rtype.z80"};

    String randomGame = games[new Random().nextInt(games.length)];
    return "/home/fernando/detodo/desarrollo/m/zx/roms/" + randomGame;
  }

  // Spectrum system variables
  private static final int LAST_K = 23560;
  private static final int FLAGS = 23611;

  private void doAutoLoadTape(Memory memory, float coe, Runnable runnable) {
    boolean autoLoadTape = false;
    Runnable task = () -> {
      try {
        wait1(1100, coe);
        long endFrame = 100;
//        while (clock.getFrames() < endFrame) {
//          TimeUnit.MILLISECONDS.sleep(20);
//        }

        if (endFrame == 100) {
          memory.write(LAST_K, 0xEF); // LOAD keyword
          Integer wordNumber2 = memory.read(FLAGS, 0);
          memory.write(FLAGS, ((Integer) (wordNumber2 | 0x20) & 0xFFFF)); // LOAD keyword
          wait1(30, coe);
          memory.write(LAST_K, 0x22); // LOAD keyword
          Integer wordNumber1 = memory.read(FLAGS, 0);
          memory.write(FLAGS, ((Integer) (wordNumber1 | 0x20) & 0xFFFF)); // LOAD keyword
          wait1(30, coe);
          memory.write(LAST_K, 0x22); // LOAD keyword
          Integer wordNumber = memory.read(FLAGS, 0);
          memory.write(FLAGS, ((Integer) (wordNumber | 0x20) & 0xFFFF)); // LOAD keyword
          wait1(30, coe);
        }
        memory.write(LAST_K, 0x0D); // LOAD keyword
        Integer wordNumber = memory.read(FLAGS, 0);
        memory.write(FLAGS, ((Integer) (wordNumber | 0x20) & 0xFFFF)); // LOAD keyword
        wait1(3000, coe);

        runnable.run();
      } catch (final InterruptedException ex) {
        new Log1().error("", ex);
      }
    };

    new Thread(task).start();
  }

  private void wait1(int i, float coe) throws InterruptedException {
    Thread.sleep((long) (i * coe));
  }

  public Fuse createFuse(String filename) {
    Fuse fuse = new Fuse();

    boolean isTape = filename != null && filename.toLowerCase().contains("tzx") || filename.toLowerCase().contains("tap");
    if (isTape) {
      fuse.init();
      Tape tape = fuse.tape;
      tape.stop();
      tape.eject();
      doAutoLoadTape(fuse.z80.ooz80.getState().getMemory(), 1f, () -> {
        tape.insert(new File(filename));
        tape.play(false);
      });
    } else {
      fuse.settings.current.emulationSpeed = 10000;
      fuse.init();
      fuse.z80.loadSnap(filename);
//      fuse.z80.changeSpeed(100);
    }

    extracted(fuse);

    return fuse;
  }

  private Fuse createFuse2(SpectrumState spectrumState) {
    Fuse fuse = new Fuse();
    fuse.settings.current.emulationSpeed = 100;
    fuse.init();
    fuse.z80.loadSnap(spectrumState);
    fuse.z80.changeSpeed(100);

    extracted(fuse);

    return fuse;
  }

  private void extracted(Fuse fuse) {
    fuse.z80.bridgeCommand = (a, b) -> null;

    scheduledExecutorService.schedule(() -> {
      while (fuse.alive) {
        fuse.z80.doOpcodes();
        fuse.eventManager.eventDoEvents();
      }
    }, 0, MILLISECONDS);
  }

}
