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

import com.fpetrola.oozx.speccy.screen.Scaler;
import com.fpetrola.oozx.speccy.screen.Scalers;
import com.fpetrola.oozx.speccy.screen.ScreenContext;
import com.fpetrola.oozx.speccy.screen.ScreenEffect;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A way of drawing the screen that arrived rather than being written here: it is offered, it
 * runs, and which side of the scaler it runs on is its own to say.
 */
class AWayOfDrawingThatArrivedTest {

  /** What ran, in order, so the two sides of the scaler can be told apart by the sizes it saw. */
  private final List<String> ran = new ArrayList<>();

  @AfterEach
  void nothingArrivedAfterAll() {
    ScreenSettings.thereAre(List.of());
  }

  /** An effect that does nothing to the picture but says it was there, and how big it was. */
  private class Marking implements ScreenEffect {
    private final String name;
    private final When when;

    Marking(String name, When when) {
      this.name = name;
      this.when = when;
    }

    public String label() {
      return name;
    }

    public When when() {
      return when;
    }

    public BufferedImage apply(BufferedImage picture, ScreenContext context) {
      ran.add(name + "@" + picture.getWidth());
      return picture;
    }
  }

  private static BufferedImage picture() {
    return new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
  }

  @Test
  void itRunsOnTheSideOfTheScalerItSaysItIsOn() {
    ScreenSettings.thereAre(List.of(new Marking("before", ScreenEffect.When.ON_THE_PICTURE),
        new Marking("after", ScreenEffect.When.ON_THE_SCREEN)));

    new ScreenSettings().render(picture(), 256, 256, new ScreenContext());

    assertEquals(List.of("before@64", "after@256"), ran,
        "one runs on the picture the machine drew and the other on the screen it landed on");
  }

  @Test
  void aScalerThatArrivedIsOfferedWithTheRest() {
    class Doubling implements Scaler {
      public String label() {
        return "From a jar";
      }

      public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
        return picture;
      }
    }
    int written = Scalers.all().size();
    ScreenSettings.thereAre(List.of(new Doubling()));

    assertEquals(written + 1, Scalers.all().size(), "the one that arrived is not offered");
    assertTrue(Scalers.all().stream().anyMatch(one -> one.label().equals("From a jar")));
    assertEquals("From a jar", Scalers.byName("From a jar").label(), "it cannot be chosen by name");
  }

  /** A scaler is picked, not stacked: it must not also run as a step of its own. */
  @Test
  void aScalerThatArrivedIsNotAlsoRunAsAStep() {
    class Marking2 implements Scaler {
      public String label() {
        return "counted";
      }

      public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
        ran.add("scaled");
        return picture;
      }
    }
    ScreenSettings.thereAre(List.of(new Marking2()));

    new ScreenSettings().render(picture(), 256, 256, new ScreenContext());

    assertEquals(List.of(), ran, "a scaler nobody chose ran anyway");
  }
}
