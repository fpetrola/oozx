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
import com.fpetrola.oozx.fuse.peripherals.t.DownloadAndUnzip;
import com.fpetrola.oozx.fuse.peripherals.t.ZXSpectrumDesktopApp;
import com.fpetrola.z80.helpers.Helper;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.SolarizedLightTheme;

import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

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
//      extracted();

      String string = null;
      if (!filename.isBlank()) {
        Path unzip = new DownloadAndUnzip().unzip(filename);
//      String snapshotFile = Helper.getSnapshotFile(filename);
        string = unzip.toAbsolutePath().toString();
      }
      Fuse fuse = createFuse(string);
      return fuse.z80.mockCore;
    });
    zxSpectrumDesktopApp.setVisible(true);
  }

  private void extracted() {
    String[] games = {"emlyn.z80", "dynamitedan.z80", "equinox.z80", "tge.z80", "wally.z80", "jsw.z80"};

    Random random = new Random();
    int index = random.nextInt(games.length);
    String s = "/home/fernando/detodo/desarrollo/m/zx/roms/" + games[index];
  }

  private Fuse createFuse(String s) {
    Fuse fuse = new Fuse();
    fuse.init();
    if (s != null)
      fuse.z80.loadSnap(s);

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
