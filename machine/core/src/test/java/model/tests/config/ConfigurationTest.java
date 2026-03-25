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
        { "roms": { "files": { "Spec48": ["/mine/48.rom"], "DivIdePeripheral": ["/mine/fatware.rom"] } } }""")).of(com.fpetrola.oozx.config.RomFiles.class);
    assertEquals(List.of("/mine/48.rom"), roms.files.get("Spec48"));
    assertEquals(List.of("/mine/fatware.rom"), roms.files.get("DivIdePeripheral"), "one nothing shipped with");
    assertEquals(List.of("128-0.rom", "128-1.rom"), roms.files.get("Spec128"), "one the file did not name");
  }

  private static File write(Path dir, String json) throws Exception {
    File file = dir.resolve("config.json").toFile();
    Files.writeString(file.toPath(), json);
    return file;
  }
}
