/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.desktop;

import javax.swing.SwingUtilities;

import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.StartsAMachineOn;
import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.fpetrola.oozx.speccy.peripherals.EmulatorCore;
import com.fpetrola.oozx.speccy.media.DownloadAndUnzip;
import com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp;
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
  /** What is still happening on the machine just started: a tape typing LOAD, or nothing. */
  private StartsAMachineOn.Going going;

  public static void main(String[] args) {
    // Windows and everything in them are built on the event thread, which is where Swing says
    // they belong: building them on this one worked by luck, and a look and feel that checks
    // (Radiance does, and refuses) left the desktop with components that were never finished.
    SwingUtilities.invokeLater(() -> new OOSpectrumLauncher().init());
  }

  public void init() {
    // The keys a Spectrum understands, written in this toolkit's key codes: the emulator has no
    // opinion about what a keyboard sends, so whoever has one says so before a machine is built.
    LookAndFeels.install(LookAndFeels.DEFAULT);

    // Some machines need a ROM this build cannot ship. Nothing is fetched without a yes, and
    // whoever is in front of the machine is the only one who can give it.
    com.fpetrola.oozx.config.RomFiles.askingFirst(RomNotShippedDialog.asking());

    ZXSpectrumDesktopApp[] appHolder = new ZXSpectrumDesktopApp[1];

    // Both ways of building a machine end here, so it is known however it was built.
    java.util.function.Function<Speccy, EmulatorCore> known = speccy -> {
      EmulatorCore core = new com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore(speccy);
      speccy.control = core;
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
    speccy.speed.emulation = Speed.REAL_TIME;
    speccy.init();
    extracted(speccy);
    return speccy;
  }

  public Speccy createSpeccy(String filename) {
    return createSpeccy(filename, null);
  }

  /**
   * A machine for a file, started by whoever knows that kind of file.
   * <p>
   * Which kinds there are is what this build has: a deck and a snapshot reader are jars, and
   * each says which files it starts a machine on and what to do with one. Nothing here knows
   * what a tape is.
   */
  public Speccy createSpeccy(String filename, String chosenMachine) {
    Speccy speccy = Speccy.create();
    File file = new File(filename);
    StartsAMachineOn opener = whoStartsOn(file);

    speccy.init();
    becomeTheMachineItWants(speccy, file, opener, chosenMachine);
    if (opener == null) nothingHereOpensThat(file);
    going = opener == null ? null : opener.start(speccy, file);

    extracted(speccy);

    return speccy;
  }

  /**
   * Says so, rather than coming up at the BASIC prompt as if nothing had been asked for. A
   * build carries the reader for one kind of file and the rest arrive as plugins, so a tape or
   * a snapshot opened without them did exactly that and said nothing at all.
   */
  /** Whether anything in this build knows how to start a machine on that kind of file. */
  public static boolean somethingOpens(File file) {
    return whoStartsOn(file) != null;
  }

  static void nothingHereOpensThat(File file) {
    com.fpetrola.oozx.TellsThePerson.thisBuildCannot(
        ("Nothing in this build knows how to open %s, so what came up is a machine at the BASIC "
            + "prompt with nothing loaded into it.\n\nReading that kind of file is what a plugin does.")
            .formatted(file.getName()));
  }

  /** What a file is, as a release says what it opens: the end of its name. */
  public static String kindOf(File file) {
    return file.getName().replaceFirst("^.*\\.", "").toLowerCase();
  }


  /** The first that says it knows that kind of file, or none. */
  private static StartsAMachineOn whoStartsOn(File file) {
    return com.fpetrola.oozx.plugins.Plugins.found(StartsAMachineOn.class).stream()
        .filter(one -> one.handles(file)).findFirst().orElse(null);
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
  /**
   * The machine somebody asked for, or the one the file says it was made for. Whether this build
   * can be that machine at all is a question for whoever is watching, which is why it is asked
   * here and not by the file.
   */
  private void becomeTheMachineItWants(Speccy speccy, File file, StartsAMachineOn opener,
                                       String chosenMachine) {
    String wanted = chosenMachine != null ? chosenMachine
        : opener == null ? null : opener.machineFor(file);
    if (wanted == null) {
      return;
    }
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getName().equals(wanted))
        .findFirst().ifPresent(type -> {
          if (!RomNotShippedDialog.readyFor(type)) return;
          speccy.machine.selectDefault();
          speccy.machine.select(type);
        });
  }

  private Speccy createSpeccy2(SpectrumState spectrumState) {
    Speccy speccy = Speccy.create();
    speccy.speed.emulation = Speed.REAL_TIME;
    speccy.init();
    Snapshots.of(speccy).load(spectrumState);
    speccy.timer.changeSpeed(100);

    extracted(speccy);

    return speccy;
  }

  private void extracted(Speccy speccy) {

    StartsAMachineOn.Going stepping = going;
    going = null;

    scheduledExecutorService.schedule(() -> {
      while (speccy.isAlive()) {
        // Stepped from this thread so the keystrokes cannot race the loop that reads them.
        if (stepping != null && !stepping.done()) {
          stepping.step();
          if (stepping.done() && stepping.wrong() != null) {
            System.err.println("Auto load failed: " + stepping.wrong());
          }
        }
        speccy.loop.doOpcodes();
        speccy.scheduler.runDue();
      }
      // On this thread, once the loop is out: nothing is still stepping what is let go of.
      speccy.end();
    }, 0, MILLISECONDS);
  }

}
