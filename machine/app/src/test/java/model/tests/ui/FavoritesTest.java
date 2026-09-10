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

package model.tests.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Favourites are only worth keeping if they come back.
 * <p>
 * The round trip goes through Jackson directly rather than through save and load, which would
 * write over whoever is running the test's own configuration in their home directory.
 */
public class FavoritesTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void aFavouriteSurvivesBeingWrittenDownAndReadBack() throws Exception {
    OOZxConfiguration.Favorite kept = new OOZxConfiguration.Favorite(
        "https://www.rzxarchive.co.uk/j/jetsetwilly128k.rzx", "Jet Set Willy 128K",
        "RECORDING", "2594");

    String json = MAPPER.writeValueAsString(kept);
    OOZxConfiguration.Favorite back = MAPPER.readValue(json, OOZxConfiguration.Favorite.class);

    assertEquals(kept.getSource(), back.getSource(), "the source is what makes it launchable");
    assertEquals(kept.getTitle(), back.getTitle());
    assertEquals(kept.getGameId(), back.getGameId());
    assertTrue(back.isRecording(), "a recording has to come back as one, or it goes to the "
        + "emulator instead of the player");
  }

  @Test
  public void theWholeConfigurationCarriesThemAlong() throws Exception {
    OOZxConfiguration configuration = new OOZxConfiguration();
    configuration.setFavorites(List.of(
        new OOZxConfiguration.Favorite("/games/manic.tzx", "Manic Miner", "GAME", null)));

    OOZxConfiguration back = MAPPER.readValue(MAPPER.writeValueAsString(configuration),
        OOZxConfiguration.class);

    assertEquals(1, back.getFavorites().size());
    assertEquals("Manic Miner", back.getFavorites().get(0).getTitle());
    assertFalse(back.getFavorites().get(0).isRecording());
  }

  /**
   * A recording inside a zip has to remember which file it was.
   * <p>
   * Recordings often come several to an archive, and a favourite that kept only the URL would
   * come back asking again which part was meant, or quietly open a different one.
   */
  @Test
  public void aRecordingRemembersWhichFileInTheArchiveItWas() throws Exception {
    OOZxConfiguration.Favorite kept = new OOZxConfiguration.Favorite(
        "https://www.rzxarchive.co.uk/j/jabato.zip", "Jabato", "RECORDING", null, "jabato-2.rzx");

    OOZxConfiguration.Favorite back = MAPPER.readValue(MAPPER.writeValueAsString(kept),
        OOZxConfiguration.Favorite.class);

    assertEquals("jabato-2.rzx", back.getEntry(), "without this it reopens the wrong part");
    assertTrue(back.isRecording());
  }

  @Test
  public void theChosenLookAndFeelIsKept() throws Exception {
    OOZxConfiguration configuration = new OOZxConfiguration();
    configuration.setLookAndFeel("Darcula");

    OOZxConfiguration back = MAPPER.readValue(MAPPER.writeValueAsString(configuration),
        OOZxConfiguration.class);

    assertEquals("Darcula", back.getLookAndFeel());
  }

  /**
   * A configuration written by another version still opens, keeping everything else in it.
   * <p>
   * This is the one that matters most, and it is not obvious why: load() catches the read
   * failure and answers a fresh configuration, so a field the code no longer knows would not
   * crash — it would quietly return an empty file, and the favourites, the recent files and the
   * window positions would all be gone at the next start. Refusing to read is worse here than
   * ignoring what is not understood.
   */
  @Test
  public void aConfigurationFromAnotherVersionStillOpens() throws Exception {
    String written = "{\"recentFiles\":[\"/games/manic.tzx\"],"
        + "\"tvScreen\":\"AERIAL\",\"scanLines\":true,\"showBorder\":true,"
        + "\"somethingNobodyHasWrittenYet\":42}";

    OOZxConfiguration back = OOZxConfiguration.read(written);

    assertEquals(1, back.getRecentFiles().size(), "the rest of the file has to survive");
    assertEquals("/games/manic.tzx", back.getRecentFiles().get(0));
  }

  /**
   * What a new emulator starts its picture with survives a restart.
   * <p>
   * Kept as plain text on purpose: a file written by another version of the screen engine still
   * opens, an unknown knob is ignored, and a missing one leaves that knob alone. The alternative
   * — refusing to load a configuration because one effect was renamed — costs everything else in
   * the file for nothing.
   */
  @Test
  public void whatNewEmulatorsStartWithIsKept() throws Exception {
    OOZxConfiguration configuration = new OOZxConfiguration();
    configuration.setScreenDefaults(new java.util.LinkedHashMap<>(java.util.Map.of(
        "scaler", "Scale2x", "lead", "COMPOSITE", "scanlines", "0.35")));

    OOZxConfiguration back = MAPPER.readValue(MAPPER.writeValueAsString(configuration),
        OOZxConfiguration.class);

    assertEquals("Scale2x", back.getScreenDefaults().get("scaler"));
    assertEquals("0.35", back.getScreenDefaults().get("scanlines"));
    assertEquals(3, back.getScreenDefaults().size());
  }

  /**
   * A look someone saved comes back by name and by values.
   * <p>
   * Stored as plain maps rather than as the profile objects: the same reason the defaults are,
   * and one more — a record whose shape changes would stop the whole list from reading, so a
   * knob renamed in the engine would cost every saved look instead of that one setting.
   */
  @Test
  public void savedLooksComeBack() throws Exception {
    OOZxConfiguration configuration = new OOZxConfiguration();
    configuration.setKeptScreenProfiles(new java.util.LinkedHashMap<>(java.util.Map.of(
        "My telly", new java.util.LinkedHashMap<>(java.util.Map.of(
            "lead", "Composite Video", "scanlines", "0.4", "scaler", "xBRZ 2x")))));

    OOZxConfiguration back = OOZxConfiguration.read(MAPPER.writeValueAsString(configuration));

    assertEquals(1, back.getKeptScreenProfiles().size());
    assertEquals("0.4", back.getKeptScreenProfiles().get("My telly").get("scanlines"));
    assertEquals("xBRZ 2x", back.getKeptScreenProfiles().get("My telly").get("scaler"));
  }

  /** Favouriting the same thing twice should not list it twice. */
  @Test
  public void theSameSourceIsKeptOnlyOnce() {
    OOZxConfiguration configuration = new OOZxConfiguration() {
      public void save() {           // the real one writes to the user's home
      }
    };

    assertTrue(configuration.addFavorite(
        new OOZxConfiguration.Favorite("/games/manic.tzx", "Manic Miner", "GAME", null)));
    assertFalse(configuration.addFavorite(
        new OOZxConfiguration.Favorite("/games/manic.tzx", "Manic Miner again", "GAME", null)));

    assertEquals(1, configuration.getFavorites().size());
    assertTrue(configuration.isFavorite("/games/manic.tzx"));

    configuration.removeFavorite("/games/manic.tzx");
    assertEquals(0, configuration.getFavorites().size());
  }
}
