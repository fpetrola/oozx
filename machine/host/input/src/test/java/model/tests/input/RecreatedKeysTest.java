package model.tests.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fpetrola.oozx.speccy.modules.input.Input.InputKey;
import com.fpetrola.oozx.speccy.modules.keyboard.Combination;
import com.fpetrola.oozx.speccy.modules.keyboard.RecreatedLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every letter the Recreated keyboard sends, with shift and without, presses or lets go of the same Spectrum key. */
class RecreatedKeysTest {
  @Test
  void everyLetterMeansWhatItMeant() {
    List<String> meant = new ArrayList<>();
    for (boolean shifted : new boolean[] {false, true}) {
      for (InputKey key : InputKey.values()) {
        RecreatedLayout layout = new RecreatedLayout();
        if (shifted) layout.pressed(InputKey.INPUT_KEY_Shift_L);
        layout.pressed(key);
        Combination produced = layout.produces(key);
        if (layout.releasedByPressing(key) || produced.keys().length > 0) {
          meant.add((shifted ? "shift " : "") + key + " " + layout.releasedByPressing(key) + " "
              + Arrays.toString(produced.keys()));
        }
      }
    }
    assertEquals("80 25599271", meant.size() + " " + String.join("|", meant).hashCode());
  }
}
