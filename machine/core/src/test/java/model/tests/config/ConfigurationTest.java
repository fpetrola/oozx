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
package model.tests.config;

import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.config.Section;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the one configuration file promises: the defaults live in the classes, the file carries
 * only what someone changed, and a section this build does not know is kept rather than lost.
 */
class ConfigurationTest {
  /** A section with something nested in it, which is where a default is easiest to lose. */
  @Section("sound")
  public static class SoundSection {
    public boolean enabled = true;
    public String device = "buffer=8192";
    public Level volume = new Level();
  }

  public static class Level {
    public int left = 70;
    public int right = 70;
  }

  @Section("sound")
  public static class AlsoCallsItselfSound {
  }

  @Section("screen")
  public static class ScreenSection {
    public String filter = "none";
  }


  @Test
  void aFileThatNamesOneSettingKeepsEveryOtherDefault(@TempDir Path dir) throws Exception {
    File file = write(dir, """
        { "sound": { "device": "alsa", "volume": { "right": 20 } } }""");

    SoundSection sound = new Configuration(file).of(SoundSection.class);

    assertEquals("alsa", sound.device, "what the file said");
    assertTrue(sound.enabled, "a setting the file does not name keeps the class's default");
    assertEquals(20, sound.volume.right, "and inside a nested one");
    assertEquals(70, sound.volume.left, "whose other defaults are kept too");
  }

  @Test
  void aSectionThisBuildDoesNotKnowIsKept(@TempDir Path dir) throws Exception {
    File file = write(dir, """
        { "sound": { "device": "alsa" }, "divide": { "eprom": "divide.rom" } }""");

    Configuration configuration = new Configuration(file);
    configuration.of(SoundSection.class).enabled = false;
    configuration.save();

    String saved = Files.readString(file.toPath());
    assertTrue(saved.contains("\"divide\""), "a section nobody asked for survives: " + saved);
    assertTrue(saved.contains("divide.rom"), "with what it held");
    assertFalse(new Configuration(file).of(SoundSection.class).enabled, "and what was changed was written");
  }

  @Test
  void everyoneAskingForASectionGetsTheSameOne(@TempDir Path dir) throws Exception {
    Configuration configuration = new Configuration(write(dir, "{}"));
    assertSame(configuration.of(ScreenSection.class), configuration.of(ScreenSection.class),
        "a setting one part changes is the setting the other part reads");
  }

  @Test
  void twoClassesCannotCallThemselvesTheSameSection(@TempDir Path dir) throws Exception {
    Configuration configuration = new Configuration(write(dir, "{}"));
    configuration.of(SoundSection.class);
    assertThrows(IllegalStateException.class, () -> configuration.of(AlsoCallsItselfSound.class));
  }

  @Test
  void aFileThatCannotBeReadStartsFromTheDefaultsRatherThanNotStarting(@TempDir Path dir) throws Exception {
    assertTrue(new Configuration(write(dir, "this is not json")).of(SoundSection.class).enabled);
    assertTrue(new Configuration(write(dir, "")).of(SoundSection.class).enabled, "an empty file, which is what a file that was just made is");
    assertTrue(new Configuration(write(dir, "[1, 2]")).of(SoundSection.class).enabled, "and something that parses but is not a configuration");
  }



  /** The shape the desktop's section has: the one section that was once the whole file. */
  @Section(value = "desktop", wasTheWholeFile = true)
  public static class DesktopSection {
    public String lastOpenDirectory;
    public List<String> recentFiles = new java.util.ArrayList<>();
    public boolean turboByDefault;
  }

  /**
   * The file was the desktop's alone before it had sections. Somebody who has been using this has
   * favourites and recent files in it, and a build that keeps more than one thing in there must not
   * be the build that loses them.
   */
  @Test
  void aFileFromBeforeThereWereSectionsIsStillTheDesktopsOwn(@TempDir Path dir) throws Exception {
    File old = write(dir, """
        { "lastOpenDirectory": "/games", "recentFiles": ["jsw.z80"], "turboByDefault": true }""");

    DesktopSection desktop = new Configuration(old).of(DesktopSection.class);

    assertEquals("/games", desktop.lastOpenDirectory);
    assertEquals(List.of("jsw.z80"), desktop.recentFiles);
    assertTrue(desktop.turboByDefault);
  }

  /** And saving moves it: what was the whole file is under the section's name and nowhere else. */
  @Test
  void savingAFileFromBeforeThereWereSectionsMovesItUnderItsName(@TempDir Path dir) throws Exception {
    File old = write(dir, """
        { "lastOpenDirectory": "/games", "turboByDefault": true }""");

    Configuration configuration = new Configuration(old);
    configuration.of(DesktopSection.class).lastOpenDirectory = "/other";
    configuration.save();

    com.fasterxml.jackson.databind.JsonNode saved = new com.fasterxml.jackson.databind.ObjectMapper().readTree(old);
    assertEquals("/other", saved.get("desktop").get("lastOpenDirectory").asText());
    assertTrue(saved.get("desktop").get("turboByDefault").asBoolean(), "and what was there came with it");
    assertNull(saved.get("lastOpenDirectory"), "and the flat copy is gone from the root: " + saved);
  }

  /** And once it is a section, it is read as one and the flat file is not looked at again. */
  @Test
  void onceItIsASectionItIsReadAsOne(@TempDir Path dir) throws Exception {
    File file = write(dir, """
        { "lastOpenDirectory": "/old", "desktop": { "lastOpenDirectory": "/new" } }""");

    Configuration configuration = new Configuration(file);
    assertEquals("/new", configuration.of(DesktopSection.class).lastOpenDirectory);

    configuration.save();
    assertNull(new com.fasterxml.jackson.databind.ObjectMapper().readTree(file).get("lastOpenDirectory"),
        "and saving takes the copy nobody reads with it, so a file already saying it twice says it once");
  }

  @Section("radio")
  public static class RadioSection {
    public boolean on = true;
  }

  @Section("radio.tuner")
  public static class TunerSection {
    public int station = 3;
  }

  /**
   * A name says where a section sits, so what belongs inside something is inside it rather than
   * beside it under a name with a dot in. A section that holds others is written without taking
   * them with it.
   */
  @Test
  void aSectionInsideAnotherIsInsideItInTheFile(@TempDir Path dir) throws Exception {
    File file = write(dir, """
        { "radio": { "on": false, "tuner": { "station": 7 } } }""");

    Configuration configuration = new Configuration(file);
    assertFalse(configuration.of(RadioSection.class).on, "the outer one");
    assertEquals(7, configuration.of(TunerSection.class).station, "and the one inside it");

    configuration.of(TunerSection.class).station = 9;
    configuration.save();

    Configuration again = new Configuration(file);
    assertEquals(9, again.of(TunerSection.class).station);
    assertFalse(again.of(RadioSection.class).on, "writing the inner one did not take the outer one with it");
  }

  /** A device knows no section: its module names its properties, the file sets them and reads them back. */
  public static class Knob {
    private int level = 70;

    public int level() {
      return level;
    }

    public void setLevel(int level) {
      this.level = level;
    }
  }

  @Test
  void aPropertyFilledByNameIsWrittenBackUnderIt(@TempDir Path dir) throws Exception {
    File file = write(dir, """
        { "covox": { "other": 1 } }""");
    Configuration configuration = new Configuration(file);
    Knob knob = new Knob();
    configuration.fill("covox", knob, "level");
    assertEquals(70, knob.level(), "a key the file leaves out keeps what the device was built with");

    knob.setLevel(10);
    configuration.put("covox", knob, "level");
    configuration.save();
    Knob again = new Knob();
    new Configuration(file).fill("covox", again, "level");
    assertEquals(10, again.level(), "and what the device held is what the file has");
  }

  /** The ROM table is the file's: an entry it names goes over what shipped, and the rest stay. */
  @Test
  void aRomTheFileNamesGoesOverTheOneThatShippedAndTheRestStay(@TempDir Path dir) throws Exception {
    com.fpetrola.oozx.config.RomFiles roms = new Configuration(write(dir, """
        { "roms": { "files": { "DivIdePeripheral": ["/mine/fatware.rom"] } } }""")).of(com.fpetrola.oozx.config.RomFiles.class);
    assertEquals(List.of("/mine/fatware.rom"), roms.files.get("DivIdePeripheral"), "one nothing shipped with");
    // What this build carries is the 48K and its ROM: every other machine and every board says
    // what it is made with in its own jar, so this is the whole of what a file can leave alone.
    assertEquals(List.of("48.rom"), roms.files.get("Spec48"), "the one the file did not name");
  }

  private static File write(Path dir, String json) throws Exception {
    File file = dir.resolve("config.json").toFile();
    Files.writeString(file.toPath(), json);
    return file;
  }

  /**
   * There is one configuration for the whole program and every machine ever built registers with
   * it to be saved. If it held those callbacks, it would hold the machine behind each of them: the
   * suite that builds a machine per test ran out of two gigabytes of heap doing exactly that.
   */
  @Test
  void aMachineNobodyHasAnyMoreIsNotKeptAliveByHavingAskedToBeSaved(@TempDir Path where) {
    Configuration configuration = new Configuration(where.resolve("config.json").toFile());
    java.util.List<String> ran = new java.util.ArrayList<>();

    Runnable kept = () -> ran.add("kept");
    configuration.beforeSave(kept);
    Runnable dropped = () -> ran.add("dropped");
    configuration.beforeSave(dropped);
    java.lang.ref.WeakReference<Runnable> watching = new java.lang.ref.WeakReference<>(dropped);
    dropped = null;

    for (int tries = 0; tries < 100 && watching.get() != null; tries++) System.gc();
    assertNull(watching.get(), "nothing but the configuration was holding it");

    configuration.beforeSave(() -> {
    });
    configuration.save();
    assertEquals(java.util.List.of("kept"), ran, "the one still held ran, the one let go did not");
  }
}