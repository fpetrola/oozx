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

import com.fpetrola.oozx.Fuse;
import com.fpetrola.oozx.fuse.modules.tape.Log1;
import com.fpetrola.oozx.fuse.modules.tape.Tape;
import com.fpetrola.oozx.fuse.peripherals.t.DownloadAndUnzip;
import com.fpetrola.oozx.fuse.peripherals.t.ZXSpectrumDesktopApp;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.SolarizedLightTheme;

import java.io.File;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
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

    ZXSpectrumDesktopApp zxSpectrumDesktopApp = new ZXSpectrumDesktopApp((filename) -> {
      String string = null;

      if (false) {
        string = extracted();
      } else if (!filename.isBlank()) {
        Path unzip = new DownloadAndUnzip().unzip(filename);
//      String snapshotFile = Helper.getSnapshotFile(filename);
        string = unzip.toAbsolutePath().toString();
      }
      Fuse fuse = createFuse(string);
      return fuse.z80.mockCore;
    });
    zxSpectrumDesktopApp.setVisible(true);
  }

  private String extracted() {
//    String[] games = {"emlyn.z80", "dynamitedan.z80", "equinox.z80", "tge.z80", "wally.z80", "jsw.z80"};
//    String[] games = {"rickdangerous.z80"};
    String[] games = {"emlyn.z80"};

    Random random = new Random();
    int index = random.nextInt(games.length);
    String s = "/home/fernando/detodo/desarrollo/m/zx/roms/" + games[index];
    return s;
  }

  // Spectrum system variables
  private static final int LAST_K = 23560;
  private static final int FLAGS = 23611;

  private void doAutoLoadTape(Memory<WordNumber> memory, float coe, Runnable runnable) {
    boolean autoLoadTape = false;
    Runnable task = () -> {
      try {
        wait1(1100, coe);
        long endFrame = 100;
//        while (clock.getFrames() < endFrame) {
//          TimeUnit.MILLISECONDS.sleep(20);
//        }

        if (endFrame == 100) {
          memory.write(createValue(LAST_K), createValue(0xEF)); // LOAD keyword
          memory.write(createValue(FLAGS), (memory.read(createValue(FLAGS), 0).or(0x20))); // LOAD keyword
          wait1(30, coe);
          memory.write(createValue(LAST_K), createValue(0x22)); // LOAD keyword
          memory.write(createValue(FLAGS), (memory.read(createValue(FLAGS), 0).or(0x20))); // LOAD keyword
          wait1(30, coe);
          memory.write(createValue(LAST_K), createValue(0x22)); // LOAD keyword
          memory.write(createValue(FLAGS), (memory.read(createValue(FLAGS), 0).or(0x20))); // LOAD keyword
          wait1(30, coe);
        }
        memory.write(createValue(LAST_K), createValue(0x0D)); // LOAD keyword
        memory.write(createValue(FLAGS), (memory.read(createValue(FLAGS), 0).or(0x20))); // LOAD keyword
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

  private Fuse createFuse(String filename) {
    Fuse fuse = new Fuse();
    fuse.init();

    boolean isTape;
    if (filename != null) {
      isTape = filename.toLowerCase().contains("tzx") || filename.toLowerCase().contains("tap");
      if (isTape) {
        Tape tape = fuse.tape;
        tape.stop();
        tape.eject();
        doAutoLoadTape(fuse.z80.ooz80.getState().getMemory(), 1f, () -> {
          tape.insert(new File(filename));
          tape.play(false);
        });

      } else
        fuse.z80.loadSnap(filename);
    }

    fuse.z80.bridgeCommand = (a, b) -> null;

    scheduledExecutorService.schedule(() -> {
      while (fuse.alive) {
        fuse.z80.doOpcodes();
        fuse.eventManager.eventDoEvents();
      }
    }, 0, MILLISECONDS);

    return fuse;
  }

}
