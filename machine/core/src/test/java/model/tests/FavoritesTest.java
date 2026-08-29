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

package model.tests;

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
   * The television settings survive too, including a file written before they existed.
   * <p>
   * Smoothing is a Boolean on purpose: unticked is null, which hands the decision back to the
   * scale rather than forcing it off, so the three states have to come back as three.
   */
  @Test
  public void theTelevisionSettingsAreKept() throws Exception {
    OOZxConfiguration configuration = new OOZxConfiguration();
    configuration.setTvScreen("AERIAL");
    configuration.setScanLines(true);
    configuration.setSmoothPixels(null);
    configuration.setShowBorder(true);

    OOZxConfiguration back = MAPPER.readValue(MAPPER.writeValueAsString(configuration),
        OOZxConfiguration.class);

    assertEquals("AERIAL", back.getTvScreen());
    assertTrue(back.isScanLines());
    assertNull(back.getSmoothPixels(), "unticked has to stay 'let the scale decide'");
    assertTrue(back.isShowBorder(), "the border is kept because the effects are drawn across it");
  }

  /** A configuration written before any of this existed still opens. */
  @Test
  public void anOlderConfigurationFileStillReads() throws Exception {
    OOZxConfiguration back = MAPPER.readValue(
        "{\"recentFiles\":[],\"lastOpenDirectory\":\"/tmp\"}", OOZxConfiguration.class);

    assertNull(back.getTvScreen(), "no television setting means the default one");
    assertFalse(back.isScanLines());
    assertFalse(back.isShowBorder());
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
