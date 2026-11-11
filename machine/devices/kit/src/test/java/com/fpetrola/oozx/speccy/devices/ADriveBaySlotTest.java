package com.fpetrola.oozx.speccy.devices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.junit.jupiter.api.Test;

/**
 * A drive's slot built the way the disk and Microdrive plugins build theirs: a light, a blank disk, saving, turning
 * the disk over and the write-protect tab, each answering what the plugin is told by the drive.
 */
class ADriveBaySlotTest {
  @Test
  void aDriveSlotOffersWhatThePluginsAskOfIt() {
    List<String> told = new ArrayList<>();
    MediaSlot slot = new MediaSlot("disk", "floppy.svg", new FileNameExtensionFilter("Disk image", "dsk"),
        file -> told.add("insert"), () -> told.add("eject"))
        .withLed("Lit while drive 1 is turning")
        .withNew(() -> told.add("blank"))
        .withSave(file -> told.add("save"))
        .withFlip(over -> told.add("flip " + over))
        .withWriteProtect(on -> told.add("protect " + on));

    slot.show("game.dsk");
    slot.led(true);
    slot.showFlipped(true);
    slot.showProtected(true);
    button(slot, "A blank disk, for the machine to format").doClick();
    AbstractButton flip = button(slot, "Turn the disk over");
    AbstractButton protect = button(slot, "Write-protect the disk");

    assertTrue(flip.isSelected() && flip.isEnabled());
    assertTrue(protect.isSelected() && protect.isEnabled());
    flip.doClick();
    assertEquals(List.of("blank", "flip false"), told);
  }

  private static AbstractButton button(Container in, String tip) {
    for (Component one : in.getComponents()) {
      if (one instanceof AbstractButton button && tip.equals(button.getToolTipText())) return button;
    }
    throw new AssertionError("no button saying " + tip);
  }
}
