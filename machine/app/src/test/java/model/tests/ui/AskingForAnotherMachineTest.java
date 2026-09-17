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

import com.fpetrola.oozx.EmulatorListener;
import com.fpetrola.oozx.speccy.peripherals.MockEmulatorCore;
import com.fpetrola.oozx.speccy.windows.Widgets;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Changing the machine from a box, while the machine takes a moment to become it.
 * <p>
 * The real emulator answers "which machine is this" with the one that is running, and becoming
 * another one is queued onto its own thread. In between, the announcement has already set every
 * box showing the model to the new name, and a box being set looks exactly like somebody picking:
 * asking again from there starts the change over, and the model goes back and forth for as long as
 * the change takes. That is what this holds shut.
 */
class AskingForAnotherMachineTest {
  /** A listener that only cares about the model, which is what these tests are about. */
  private static EmulatorListener whenTheModelChanges(java.util.function.Consumer<String> told) {
    return new EmulatorListener() {
      public void onEmulationStateChanged(String state) {
      }

      public void onError(String message) {
      }

      public void onEmulationSpeedChanged(double speed) {
      }

      public void onModelChanged(String model) {
        told.accept(model);
      }

      public void onPauseStateChanged(boolean paused) {
      }

      public void onTurboModeChanged(boolean turbo) {
      }

      public void onTapeStatusChanged(String status) {
      }
    };
  }

  /** A core that says what is running, which is not yet what it was asked to become. */
  private static class SlowToChange extends MockEmulatorCore {
    private String running = "48K";
    private final List<String> asked = new ArrayList<>();

    SlowToChange() {
      super(null);
      // Every announcement, which is what starts the change: the real core listens to its own and
      // builds the machine, so one echo more is one machine more being built.
      addEmulatorListener(whenTheModelChanges(asked::add));
    }


    @Override
    public String getCurrentModel() {
      return running;
    }

    /** The machine's own thread gets round to it. */
    void arrives() {
      running = super.getCurrentModel();
      announceMachine(running);
    }
  }

  @Test
  void theBoxesThatFollowTheChangeDoNotStartItAgain() {
    SlowToChange core = new SlowToChange();
    List<JComboBox<String>> boxes = new ArrayList<>();
    for (int each = 0; each < 2; each++) {
      JComboBox<String> box = new JComboBox<>(new String[] {"48K", "128K", "+2"});
      box.setSelectedItem(core.getCurrentModel());
      Widgets.whenChosen(box, core::getCurrentModel, core::setMachineModel);
      boxes.add(box);
    }
    // Every box follows what the core announces, which is what the windows do.
    core.addEmulatorListener(whenTheModelChanges(model -> boxes.forEach(box -> box.setSelectedItem(model))));

    boxes.get(0).setSelectedItem("128K");

    assertEquals(List.of("128K"), core.asked,
        "while the machine is on its way, the boxes following it must not ask again");

    core.arrives();

    assertEquals(List.of("128K", "128K"), core.asked, "and the machine saying it arrived is the second");
    assertEquals("128K", boxes.get(1).getSelectedItem(), "which every box shows");
  }
}
