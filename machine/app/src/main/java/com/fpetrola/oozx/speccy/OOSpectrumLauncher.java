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
import com.fpetrola.oozx.speccy.peripherals.t.TapeHardware;
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
    Emulation.noTest = true;
    // The keys a Spectrum understands, written in this toolkit's key codes: the emulator has no
    // opinion about what a keyboard sends, so whoever has one says so before a machine is built.
    com.fpetrola.oozx.speccy.modules.Keyboard.KEYSYMS_MAP = SwingKeyboard.KEYSYMS_MAP;
    LafManager.install(new SolarizedLightTheme());

    ZXSpectrumDesktopApp[] appHolder = new ZXSpectrumDesktopApp[1];

    // Both ways of building a machine end here, so its deck is known however it was built.
    // Only the one that starts from a file used to say so, and a machine restored from a
    // snapshot arrived with no deck anybody could find: the cassette windows had nothing to
    // plug into and quietly did nothing.
    java.util.function.Function<Speccy, EmulatorCore> known = speccy -> {
      EmulatorCore core = new com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore(speccy);
      speccy.z80.mockCore = core;
      appHolder[0].registerTape(core, speccy.tape);
      appHolder[0].registerMachine(core, speccy);
      return core;
    };

    Function<SpectrumState, EmulatorCore> mockCoreState =
        spectrumState -> known.apply(createSpeccy2(spectrumState));
    ZXSpectrumDesktopApp zxSpectrumDesktopApp = new ZXSpectrumDesktopApp((filename, chosenMachine) -> {
      Speccy speccy;
      String string = null;

      if (filename == null || filename.isBlank()) {
        // Nothing asked for: a machine at the BASIC prompt, which is what "New Emulator" means.
        speccy = createBareSpeccy();
      } else {
        string = filename.contains("http")
            ? new DownloadAndUnzip().unzip(filename).toAbsolutePath().toString()
            : filename;
        speccy = createSpeccy(string, chosenMachine);
      }

      EmulatorCore mockCore = known.apply(speccy);
      if (string != null) {
        mockCore.setFilename(string);
      }
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
    return createSpeccy(filename, null);
  }

  public Speccy createSpeccy(String filename, String chosenMachine) {
    Speccy speccy = Speccy.create();

    if (isTape(filename)) {
      speccy.init();
      becomeTheMachineTheTapeWants(speccy, filename, chosenMachine);
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
   * The machine a tape asks for, asked of the tape first and of its name second.
   * <p>
   * A TZX says so itself: its hardware type block lists the machines it runs on and marks the
   * ones whose features it uses. That is the statement to go by - a game marked as using the
   * 128K's has music there and silence on a 48K, and starting it on the smaller machine is how
   * somebody never hears it. Where several are named, the biggest is taken, except that one it
   * says it uses beats a bigger one it merely runs on.
   * <p>
   * A TAP says nothing - it is blocks and no statement about anything - so for those the only
   * thing left is what the archive called the file. Where an entry offers both releases they are
   * named for it, and loading the 128K one into a 48K machine reaches the end of the tape and
   * answers "out of memory": a 48K BASIC error, and the machine saying exactly what is wrong.
   * The same word the scorer reads when it puts one release ahead of the other, so the two
   * cannot come to different conclusions about which is which.
   */
  private void becomeTheMachineTheTapeWants(Speccy speccy, String filename, String chosenMachine) {
    File tape = new File(filename);
    String wanted = chosenMachine != null ? chosenMachine
        : TapeHardware.bestMachineFor(tape)
            .orElseGet(() -> tape.getName().toLowerCase().contains("128") ? "Spectrum 128K" : null);
    if (wanted == null) {
      return;
    }
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getName().equals(wanted))
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
