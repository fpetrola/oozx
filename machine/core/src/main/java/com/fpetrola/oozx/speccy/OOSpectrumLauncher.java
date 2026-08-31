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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.tape.TapeAutoLoader;
import com.fpetrola.oozx.speccy.peripherals.EmulatorCore;
import com.fpetrola.oozx.speccy.peripherals.t.DownloadAndUnzip;
import com.fpetrola.oozx.speccy.peripherals.t.ZXSpectrumDesktopApp;
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
      Speccy speccy = createSpeccy2(spectrumState);
      return speccy.z80.mockCore;
    };
    ZXSpectrumDesktopApp[] appHolder = new ZXSpectrumDesktopApp[1];
    ZXSpectrumDesktopApp zxSpectrumDesktopApp = new ZXSpectrumDesktopApp((filename) -> {
      Speccy speccy;
      String string = null;

      if (filename == null || filename.isBlank()) {
        // Nothing asked for: a machine at the BASIC prompt, which is what "New Emulator" means.
        speccy = createBareSpeccy();
      } else {
        string = filename.contains("http")
            ? new DownloadAndUnzip().unzip(filename).toAbsolutePath().toString()
            : filename;
        speccy = createSpeccy(string);
      }

      EmulatorCore mockCore = speccy.z80.mockCore;
      if (string != null) {
        mockCore.setFilename(string);
      }
      // Every machine has a deck, whether or not it was started with a tape, so the cassette
      // browser can drive whichever emulator is in front.
      appHolder[0].registerTape(mockCore, speccy.tape);
      if (string != null && isTape(string)) {
        appHolder[0].showTapeBrowser(new File(string), speccy.tape);
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
  public Speccy createBareSpeccy() {
    Speccy speccy = Speccy.create();
    speccy.settings.current.emulationSpeed = 100;
    speccy.init();
    extracted(speccy);
    return speccy;
  }

  public Speccy createSpeccy(String filename) {
    Speccy speccy = Speccy.create();

    if (isTape(filename)) {
      speccy.init();
      becomeTheMachineTheTapeWants(speccy, filename);
      autoLoader = new TapeAutoLoader(speccy, new File(filename));
    } else {
      speccy.settings.current.emulationSpeed = 10000;
      speccy.init();
      speccy.z80.loadSnap(filename);
//      speccy.z80.changeSpeed(100);
    }

    extracted(speccy);

    return speccy;
  }

  /**
   * A 128K release needs a 128K machine, and nothing was choosing one.
   * <p>
   * A tape carries no statement of the machine it was made for - a snapshot does, and that is
   * why snapshots have always arrived on the right one - so the only thing to go on is what the
   * archive called the file. Where an entry offers both, they are named for it: one release says
   * 128K in its name and the other says 48K.
   * <p>
   * Loading the 128K one into a 48K machine gets as far as the end of the tape and then answers
   * "out of memory", which is a 48K BASIC error and the machine saying exactly what is wrong.
   * The same tape on a 128K machine goes from eighty-three per cent of its time in RAM to all of
   * it.
   * <p>
   * The same word the scorer reads when it puts a 48K release ahead of a 128K one, so the two
   * cannot come to different conclusions about which is which.
   */
  private void becomeTheMachineTheTapeWants(Speccy speccy, String filename) {
    String name = new File(filename).getName().toLowerCase();
    if (!name.contains("128")) {
      return;
    }
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getClass().getSimpleName().equals("Spec128"))
        .findFirst().ifPresent(type -> {
          speccy.machine.selectDefault();
          speccy.machine.select(type);
        });
  }

  private Speccy createSpeccy2(SpectrumState spectrumState) {
    Speccy speccy = Speccy.create();
    speccy.settings.current.emulationSpeed = 100;
    speccy.init();
    speccy.z80.loadSnap(spectrumState);
    speccy.z80.changeSpeed(100);

    extracted(speccy);

    return speccy;
  }

  private static boolean isTape(String filename) {
    if (filename == null) {
      return false;
    }
    String name = filename.toLowerCase();
    return name.endsWith(".tzx") || name.endsWith(".tap") || name.endsWith(".csw");
  }

  private void extracted(Speccy speccy) {
    speccy.z80.bridgeCommand = (a, b) -> null;

    TapeAutoLoader tapeAutoLoader = autoLoader;
    autoLoader = null;

    scheduledExecutorService.schedule(() -> {
      while (speccy.isAlive()) {
        // Stepped from this thread so the keystrokes cannot race the loop that reads them.
        if (tapeAutoLoader != null && !tapeAutoLoader.isDone()) {
          tapeAutoLoader.step();
          if (tapeAutoLoader.isDone() && tapeAutoLoader.getError() != null) {
            System.err.println("Auto load failed: " + tapeAutoLoader.getError());
          }
        }
        speccy.z80.doOpcodes();
        speccy.eventManager.eventDoEvents();
      }
    }, 0, MILLISECONDS);
  }

}
