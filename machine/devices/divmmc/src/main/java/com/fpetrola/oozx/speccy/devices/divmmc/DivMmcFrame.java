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
package com.fpetrola.oozx.speccy.devices.divmmc;

import com.fpetrola.oozx.speccy.devices.IdeBayFrame;
import com.fpetrola.oozx.speccy.windows.Widgets;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import java.io.File;

/**
 * The DivMMC on the desk: its card, the EPROM esxDOS is read from at a hard reset, the
 * write-protect jumper the automapper needs, and the button.
 */
public class DivMmcFrame extends IdeBayFrame<DivMmcPeripheral> {

  private final JButton eprom = Widgets.iconButton("multiface-rom.svg", "EPROM...",
      "Choose the firmware the EPROM is filled with at the next hard reset");
  private final JToggleButton jumper = Widgets.iconToggle("slot-lock.svg", "Protect",
      "The write-protect jumper: closed, the EPROM cannot be written and the automapper works");
  private final JButton button = Widgets.iconButton("multiface-button.svg", "NMI",
      "The button on the board: stops the program and brings up the firmware's menu");

  public DivMmcFrame() {
    super("DivMMC", DivMmcPeripheral.class, "card", "mmc", "card");
    eprom.addActionListener(e -> chooseEprom());
    jumper.addActionListener(e -> onEmulator(d -> {
      d.setWriteProtect(jumper.isSelected());
      d.refresh();
    }));
    button.addActionListener(e -> onEmulator(DivMmcPeripheral::nmi));
    controls.add(eprom);
    controls.add(jumper);
    controls.add(button);
    controls.revalidate();
    plugged(null);
  }

  @Override
  protected void plugged(DivMmcPeripheral device) {
    super.plugged(device);
    if (eprom == null) {
      return;
    }
    boolean in = device != null;
    eprom.setEnabled(in);
    jumper.setEnabled(in);
    button.setEnabled(in);
    if (in) {
      jumper.setSelected(device().writeProtect());
    }
  }

  private void chooseEprom() {
    if (machine() == null) {
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("The DivMMC's EPROM (8K)");
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File chosen = chooser.getSelectedFile();
      machine().roms.choose(device(), chosen.getPath());
      machine().loop.later(() -> machine().machine.reset(true));
    }
  }
}
