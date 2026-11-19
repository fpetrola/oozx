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

package com.fpetrola.oozx.speccy.devices;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a plugin promises when it says it has something of a game: its name, how many for that game, and their page. */
class APartOfAGameTest {
  /** Hints, of which only the game numbered 2589 has any. */
  private final AboutAGame hints = new AboutAGame() {
    public String what() {
      return "Hints";
    }

    public int howMany(Desk.Game game) {
      return "2589".equals(game.id()) ? 3 : 0;
    }

    public JComponent page(Desk.Game game) {
      return new JLabel(howMany(game) + " hints for " + game.title());
    }
  };

  @Test
  void itCountsWhatThatGameHasAndNoOtherAndItsPageIsOfThatGame() {
    Desk.Game willy = new Desk.Game("cinta3.tzx", null, "2589", "Jet Set Willy");

    assertEquals("Hints", hints.what());
    assertEquals(3, hints.howMany(willy));
    assertEquals(0, hints.howMany(new Desk.Game("other.tap", null, "1257", "Dark Fusion")), "with none, nothing is shown of it");
    assertEquals("3 hints for Jet Set Willy", hints.page(willy).getAccessibleContext().getAccessibleName());
  }
}
