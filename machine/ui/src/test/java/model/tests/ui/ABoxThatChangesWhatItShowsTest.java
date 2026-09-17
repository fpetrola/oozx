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
