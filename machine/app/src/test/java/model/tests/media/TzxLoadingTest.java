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

package model.tests.media;

import model.tags.NeedsNetwork;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.api.GameEntry;
import com.fpetrola.oozx.api.Hit;
import com.fpetrola.oozx.api.ZxInfoApiHandler;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.peripherals.t.DownloadAndUnzip;
import com.fpetrola.oozx.speccy.peripherals.t.DownloadAndUnzip;
import com.fpetrola.oozx.speccy.peripherals.t.GameBrowserInternalFrame;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.modules.tape.TapeAutoLoader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Reproduces, headless, what a left click on a game in the launcher's search browser does:
 * resolve a tape URL from ZXInfo, download and unzip it, insert it into the tape deck,
 * type LOAD "" and let the ROM load it.
 * <p>
 * The one thing it does NOT reproduce is {@code OOSpectrumLauncher.doAutoLoadTape}, which
 * sequences the keystrokes with {@code Thread.sleep} on a separate thread while the emulator
 * runs at {@code emulationSpeed} (20000 by default, i.e. 200x real time). Here each keystroke
 * instead waits for the ROM to actually consume the previous one, so a failure reported by this
 * test is a tape/TZX problem rather than an autoload timing problem.
 */
public class TzxLoadingTest extends SpeccyBaseForTests {

  private static final int ROM_TOP = 0x4000;

  /** Games that load today. A regression here is a real break. */
  private static final String[] EXPECTED_TO_LOAD = {
      "head over heels",
      "jet set willy",
      "rick dangerous",
      "dark fusion",
      "cybernoid",
      "target renegade",
      "trantor",
      "manic miner",
      "the great escape",
      "renegade",
      "human killing machine",
      "three weeks in paradise",
  };

  /**
   * Games that do not load yet. They are reported, not asserted, so the suite stays green while
   * the loader is being worked on; move one up to EXPECTED_TO_LOAD as soon as it passes.
   */
  private static final String[] KNOWN_BROKEN = {};

  private static final String[] GAMES = concat(EXPECTED_TO_LOAD, KNOWN_BROKEN);

  private static String[] concat(String[] first, String[] second) {
    String[] all = new String[first.length + second.length];
    System.arraycopy(first, 0, all, 0, first.length);
    System.arraycopy(second, 0, all, first.length, second.length);
    return all;
  }

  enum Outcome {
    LOADED,          // code from the tape is running in RAM
    TAPE_REJECTED,   // Tape.insert() refused the file (TZX parse failure)
    NO_TZX_LISTED,   // ZXInfo has no "Perfect tape (TZX)" file for the entry
    WRONG_FILE,      // the zip's first entry, which is what the app uses, is not a tape
    DOWNLOAD_FAILED,
    BACK_IN_ROM,     // the tape played out but execution ended up back in the ROM
    NEVER_STARTED,   // the tape never played out
  }

  record Report(String query, String title, Outcome outcome, String detail, int blocks, int ramPercent) {
    public String toString() {
      return String.format("%-18s %-14s blocks=%-4d ram=%3d%%  %s%s",
          query, outcome, blocks, ramPercent, title == null ? "" : title,
          detail == null || detail.isEmpty() ? "" : " | " + detail);
    }
  }

  @NeedsNetwork
  @Test
  void tzxGamesLoadFromTheSearchBrowser() {
    List<Report> failures = new ArrayList<>();
    List<Report> unavailable = new ArrayList<>();

    for (String game : EXPECTED_TO_LOAD) {
      Report report = loadFromZxInfo(game);
      System.out.println(report);
      if (report.outcome() == Outcome.LOADED) {
        continue;
      }
      // A tape that could not be reached says nothing about the loader. Without this the test
      // turns red on a machine with no network, or after a clean has emptied the download
      // cache, and reads as a regression in whatever was last changed.
      if (isEnvironment(report.outcome())) {
        unavailable.add(report);
      } else {
        failures.add(report);
      }
    }

    if (!unavailable.isEmpty()) {
      System.out.println("\n--- could not be fetched, not counted ---");
      unavailable.forEach(report -> System.out.println("  " + report));
    }

    System.out.println("\n--- known broken, reported only ---");
    for (String game : KNOWN_BROKEN) {
      System.out.println(loadFromZxInfo(game));
    }

    assertTrue(failures.isEmpty(), "tapes that used to load and no longer do: " + failures);

    // Nothing was reachable, so nothing was actually verified: skip rather than pass green.
    assumeTrue(unavailable.size() < EXPECTED_TO_LOAD.length,
        "ZXInfo could not be reached, no tape was exercised");
  }

  /** True when the outcome is about reaching the tape, not about loading it. */
  private static boolean isEnvironment(Outcome outcome) {
    return outcome == Outcome.DOWNLOAD_FAILED || outcome == Outcome.NO_TZX_LISTED;
  }

  Report loadFromZxInfo(String query) {
    String tzxUrl = null;
    String title = null;
    try {
      // The browser only builds a row for a hit that actually has a downloadable file,
      // so scan the hits the same way instead of stopping at the first one.
      for (Hit hit : new ZxInfoApiHandler().search(query)) {
        GameEntry game = hit._source;
        if (game == null || !"SOFTWARE".equals(game.contentType)) {
          continue;
        }
        tzxUrl = firstTzxUrl(game);
        if (tzxUrl != null) {
          title = game.title;
          break;
        }
      }
      if (tzxUrl == null) {
        return new Report(query, null, Outcome.NO_TZX_LISTED, "no hit lists a TZX", 0, 0);
      }
    } catch (Exception e) {
      return new Report(query, null, Outcome.DOWNLOAD_FAILED, "zxinfo: " + e, 0, 0);
    }

    Path tapeFile;
    try {
      tapeFile = fetch(tzxUrl, query);
    } catch (Exception e) {
      return new Report(query, title, Outcome.DOWNLOAD_FAILED, tzxUrl + ": " + e, 0, 0);
    }

    String name = tapeFile.getFileName().toString().toLowerCase();
    if (!name.endsWith(".tzx") && !name.endsWith(".tap")) {
      return new Report(query, title, Outcome.WRONG_FILE,
          "zip's first entry is " + tapeFile.getFileName(), 0, 0);
    }

    return runTape(query, title, tapeFile.toFile());
  }

  /** Same file selection the game browser does, narrowed to TZX. */
  private String firstTzxUrl(GameEntry game) {
    if (game.releases == null) {
      return null;
    }
    List<String> candidates = new ArrayList<>();
    for (var release : game.releases) {
      if (release.files == null) {
        continue;
      }
      for (var file : release.files) {
        // /denied/ holds entries withdrawn on copyright grounds; no mirror serves them,
        // so a click on one in the browser can only ever 404.
        if ("Perfect tape (TZX)".equals(file.format) && !file.path.startsWith("/denied/")) {
          candidates.add(GameBrowserInternalFrame.getFileURL(file.path));
        }
      }
    }
    return DownloadAndUnzip.preferred(candidates, url -> url.substring(url.lastIndexOf('/') + 1));
  }

  /** Downloads through the same code the app uses, but into a per-game cache dir. */
  private Path fetch(String url, String query) throws Exception {
    Path dir = Paths.get("target", "tzx-cache", query.replaceAll("[^a-z0-9]+", "_"));
    if (Files.isDirectory(dir)) {
      try (var entries = Files.list(dir)) {
        List<Path> cached = entries.sorted().toList();
        if (!cached.isEmpty()) {
          // Pick from the cache the same way the app picks from the zip, or the test would
          // exercise a different file than a click in the browser does.
          return DownloadAndUnzip.chooseLoadable(cached);
        }
      }
    }
    Files.createDirectories(dir);
    return DownloadAndUnzip.downloadAndUnzip(url, dir);
  }

  private Report runTape(String query, String title, File tapeFile) {
    Emulation.noTest = true;
    // Plain Speccy, exactly like OOSpectrumLauncher.createSpeccy. SpeccyBaseForTests.createSpeccy
    // installs an instrumented clock that records every tState update.
    Speccy speccy = Speccy.create();
    speccy.sound.setJavaSoundDevice(new JavaSoundDevice() {
      public void sound_lowlevel_frame(int[] data, int len) {
      }
    });
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;

    Tape tape = speccy.tape;

    int[] blocks = {0};
    tape.addTapeBlockListener(block -> blocks[0] = Math.max(blocks[0], block));

    // The launcher's own auto loader: type LOAD "" on the keyboard, insert, play, and wait for
    // the load to finish. It normally drops to real Spectrum speed at that point; here it stays
    // fast, since the Timer sleeps to hold 1x and the run after the load would take minutes.
    TapeAutoLoader autoLoader = new TapeAutoLoader(speccy, tapeFile,
        TapeAutoLoader.LOADING_SPEED, TapeAutoLoader.LOADING_SPEED);
    for (long step = 0; step < 200_000_000L && !autoLoader.isDone(); step++) {
      autoLoader.step();
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
    }
    if (autoLoader.getError() != null) {
      return new Report(query, title, Outcome.TAPE_REJECTED, autoLoader.getError(), 0, 0);
    }
    boolean finished = autoLoader.isDone();

    // Judge by where execution goes once there is no more signal. A game that loaded keeps
    // running its own code in RAM; a loader that gave up (or reset the machine, which is what
    // a turbo loader does on a checksum error) is back in the ROM.
    int ramPercent = ramPercentOver(speccy, 400);
    writeScreenshot(speccy, query);

    String detail = tapeFile.getName();
    Outcome outcome;
    if (!finished) {
      outcome = Outcome.NEVER_STARTED;
      detail += " (tape never finished)";
    } else if (ramPercent >= 60) {
      outcome = Outcome.LOADED;
    } else {
      outcome = Outcome.BACK_IN_ROM;
    }
    return new Report(query, title, outcome, detail, blocks[0], ramPercent);
  }

  /**
   * Presses one key the way the ROM expects: put the code in LAST_K and raise bit 5 of FLAGS,
   * then wait for the ROM to clear that bit, which is its acknowledgement that it read the key.
   * The launcher sleeps a fixed 30ms instead, which is what makes the autoload racy.
   */
  /**
   * Percentage of PC samples in RAM over the given number of frames. Sampling happens on every
   * doOpcodes slice rather than once per frame, because a frame boundary always lands in the ISR.
   */
  private int ramPercentOver(Speccy speccy, int frames) {
    int[] samples = new int[2];
    runSampling(speccy, frames, samples);
    return samples[1] == 0 ? 0 : samples[0] * 100 / samples[1];
  }

  /**
   * Runs for the given number of frames. The clock is not monotonic - Spectrum.spectrumFrame
   * subtracts a frame's worth of tStates at every frame boundary - so frames are counted by
   * watching for that wrap rather than by waiting for an absolute tState count.
   */
  private void run(Speccy speccy, int frames) {
    runSampling(speccy, frames, null);
  }

  private void runSampling(Speccy speccy, int frames, int[] pcSamples) {
    var pc = speccy.z80.ooz80.getState().getPc();
    long previous = speccy.zxClock.getTStates();
    int seen = 0;
    while (seen < frames) {
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();

      if (pcSamples != null) {
        pcSamples[1]++;
        if (pc.read() >= ROM_TOP) {
          pcSamples[0]++;
        }
      }

      long now = speccy.zxClock.getTStates();
      if (now < previous) {
        seen++;
      }
      previous = now;
    }
  }


  private static final int SCREEN_BASE = 0x4000;
  private static final int ATTR_BASE = 0x5800;

  /** ZX Spectrum palette: 8 colours, normal then bright. */
  private static final int[] PALETTE = {
      0x000000, 0x0000D7, 0xD70000, 0xD700D7, 0x00D700, 0x00D7D7, 0xD7D700, 0xD7D7D7,
      0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};

  /**
   * Dumps the Spectrum display file as a PNG so a load can be judged by looking at it,
   * rather than only by where the PC happens to be.
   */
  private void writeScreenshot(Speccy speccy, String query) {
    try {
      BufferedImage image = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < 192; y++) {
        // The display file is not linear: third, then pixel row within a character, then char row.
        int third = y >> 6;
        int line = (y >> 3) & 7;
        int pixelRow = y & 7;
        int rowBase = SCREEN_BASE + (third << 11) + (pixelRow << 8) + (line << 5);

        for (int charColumn = 0; charColumn < 32; charColumn++) {
          int bits = speccy.memory.readByteInternal(rowBase + charColumn) & 0xFF;
          int attribute = speccy.memory.readByteInternal(ATTR_BASE + (y >> 3) * 32 + charColumn) & 0xFF;
          int bright = (attribute & 0x40) != 0 ? 8 : 0;
          int ink = PALETTE[(attribute & 0x07) + bright];
          int paper = PALETTE[((attribute >> 3) & 0x07) + bright];

          for (int bit = 0; bit < 8; bit++) {
            boolean set = (bits & (0x80 >> bit)) != 0;
            image.setRGB(charColumn * 8 + bit, y, set ? ink : paper);
          }
        }
      }
      Path out = Paths.get("target", "tzx-screens");
      Files.createDirectories(out);
      Path file = out.resolve(query.replaceAll("[^a-z0-9]+", "_") + ".png");
      ImageIO.write(image, "png", file.toFile());
      System.out.println("  screen -> " + file.toAbsolutePath());
    } catch (Exception e) {
      System.out.println("  screenshot failed: " + e);
    }
  }

  public static void main(String[] args) {
    TzxLoadingTest test = new TzxLoadingTest();
    for (String game : args.length > 0 ? args : GAMES) {
      System.out.println(test.loadFromZxInfo(game));
    }
    System.exit(0);
  }
}
