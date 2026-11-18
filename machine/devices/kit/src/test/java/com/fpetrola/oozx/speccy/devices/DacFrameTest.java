package com.fpetrola.oozx.speccy.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fpetrola.oozx.speccy.modules.sound.DacDevice;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

class DacFrameTest {
  interface ABox extends Peripheral, Pluggable, DacDevice {
  }

  private static List<String> labels(Container in) {
    List<String> found = new ArrayList<>();
    for (Component each : in.getComponents()) {
      if (each instanceof JLabel label && label.getText() != null && !label.getText().isBlank()) found.add(label.getText());
      if (each instanceof Container inner) found.addAll(labels(inner));
    }
    return found;
  }

  @Test
  void aBoxAddsItsOwnRowsUnderTheStatusAndTheVolume() {
    LabelledRows box = new DacFrame<ABox>("A box", ABox.class, 255 * 128);
    box.row("Say", new JTextField());
    List<String> rows = labels((Container) box).stream().filter(List.of("Status", "Volume", "Say")::contains).toList();
    assertEquals(List.of("Status", "Volume", "Say"), rows, "labelled rows, in the order they were added");
  }
}
