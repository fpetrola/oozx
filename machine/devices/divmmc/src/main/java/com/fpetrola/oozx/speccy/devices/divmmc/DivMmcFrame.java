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
package com.fpetrola.oozx.speccy.devices.divmmc;

import com.fpetrola.oozx.speccy.devices.IdeBayFrame;
import com.fpetrola.oozx.speccy.peripherals.t.Widgets;

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
      machine().settings.current.divmmcWp = jumper.isSelected();
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
      jumper.setSelected(machine().settings.current.divmmcWp);
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
      machine().settings.current.romDivmmc = chosen.getPath();
      machine().z80.later(() -> machine().machine.reset(true));
    }
  }
}
