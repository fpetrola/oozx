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
import com.fpetrola.oozx.fuse.modules.tape.TapeAutoLoader;
import com.fpetrola.oozx.fuse.peripherals.EmulatorCore;
import com.fpetrola.oozx.fuse.peripherals.t.DownloadAndUnzip;
import com.fpetrola.oozx.fuse.peripherals.t.ZXSpectrumDesktopApp;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.SolarizedLightTheme;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class OOSpectrumLauncher {
  private ScheduledExecutorService scheduledExecutorService = newScheduledThreadPool(10);
  private TapeAutoLoader autoLoader;

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
    ZXSpectrumDesktopApp[] appHolder = new ZXSpectrumDesktopApp[1];
    ZXSpectrumDesktopApp zxSpectrumDesktopApp = new ZXSpectrumDesktopApp((filename) -> {
      Fuse fuse;
      String string = null;

      if (filename == null || filename.isBlank()) {
        // Nothing asked for: a machine at the BASIC prompt, which is what "New Emulator" means.
        fuse = createBareFuse();
      } else {
        string = filename.contains("http")
            ? new DownloadAndUnzip().unzip(filename).toAbsolutePath().toString()
            : filename;
        fuse = createFuse(string);
      }

      EmulatorCore mockCore = fuse.z80.mockCore;
      if (string != null) {
        mockCore.setFilename(string);
      }
      // Every machine has a deck, whether or not it was started with a tape, so the cassette
      // browser can drive whichever emulator is in front.
      appHolder[0].registerTape(mockCore, fuse.tape);
      if (string != null && isTape(string)) {
        appHolder[0].showTapeBrowser();
      }
      return mockCore;
    }, mockCoreState);

    appHolder[0] = zxSpectrumDesktopApp;
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

  /** A machine with nothing loaded, sitting at the BASIC prompt, running at real speed. */
  public Fuse createBareFuse() {
    Fuse fuse = new Fuse();
    fuse.settings.current.emulationSpeed = 100;
    fuse.init();
    extracted(fuse);
    return fuse;
  }

  public Fuse createFuse(String filename) {
    Fuse fuse = new Fuse();

    if (isTape(filename)) {
      fuse.init();
      autoLoader = new TapeAutoLoader(fuse, new File(filename));
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

  private static boolean isTape(String filename) {
    if (filename == null) {
      return false;
    }
    String name = filename.toLowerCase();
    return name.endsWith(".tzx") || name.endsWith(".tap") || name.endsWith(".csw");
  }

  private void extracted(Fuse fuse) {
    fuse.z80.bridgeCommand = (a, b) -> null;

    TapeAutoLoader tapeAutoLoader = autoLoader;
    autoLoader = null;

    scheduledExecutorService.schedule(() -> {
      while (fuse.isAlive()) {
        // Stepped from this thread so the keystrokes cannot race the loop that reads them.
        if (tapeAutoLoader != null && !tapeAutoLoader.isDone()) {
          tapeAutoLoader.step();
          if (tapeAutoLoader.isDone() && tapeAutoLoader.getError() != null) {
            System.err.println("Auto load failed: " + tapeAutoLoader.getError());
          }
        }
        fuse.z80.doOpcodes();
        fuse.eventManager.eventDoEvents();
      }
    }, 0, MILLISECONDS);
  }

}
