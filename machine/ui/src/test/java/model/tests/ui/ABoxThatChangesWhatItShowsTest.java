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

import com.fpetrola.oozx.speccy.windows.Widgets;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A box that shows what something is and also changes it hears its own echo: the thing changes,
 * says so, every box showing it is set to the new value, and each of those looks like somebody
 * picking it again. With two boxes on the same thing the echo goes round between them, which is
 * what made changing the machine flicker through the old model on its way to the new one.
 */
class ABoxThatChangesWhatItShowsTest {
  private final List<String> asked = new ArrayList<>();
  private String model = "48K";

  private JComboBox<String> boxShowingTheModel() {
    JComboBox<String> box = new JComboBox<>(new String[] {"48K", "128K", "+2"});
    box.setSelectedItem(model);
    Widgets.whenChosen(box, () -> model, chosen -> {
      asked.add(chosen);
      model = chosen;
    });
    return box;
  }

  @Test
  void beingToldWhatItAlreadySaysIsNotSomebodyPickingIt() {
    JComboBox<String> box = boxShowingTheModel();

    box.setSelectedItem("48K");

    assertEquals(List.of(), asked, "nothing was picked, so nothing should have been asked for");
  }

  @Test
  void pickingSomethingElseAsksForItOnce() {
    JComboBox<String> box = boxShowingTheModel();

    box.setSelectedItem("128K");

    assertEquals(List.of("128K"), asked);
  }

  @Test
  void twoBoxesOnTheOneThingDoNotAnswerEachOther() {
    JComboBox<String> one = boxShowingTheModel();
    JComboBox<String> other = boxShowingTheModel();
    // What the window does when the machine says it changed: every box showing it is set to it.
    Runnable saidSo = () -> {
      one.setSelectedItem(model);
      other.setSelectedItem(model);
    };

    one.setSelectedItem("128K");
    saidSo.run();
    saidSo.run();

    assertEquals(List.of("128K"), asked, "asked for once, by the one that was picked");
  }
}
