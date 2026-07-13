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
package com.fpetrola.oozx.speccy.devices.zxatasp;

import com.fpetrola.oozx.speccy.devices.IdeBayFrame;
import com.fpetrola.oozx.speccy.windows.Widgets;

import javax.swing.JToggleButton;

/** The ZXATASP on the desk: its two drives, and the board's two jumpers. */
public class ZxataspFrame extends IdeBayFrame<ZxataspPeripheral> {

  private final JToggleButton jumper = Widgets.iconToggle("slot-lock.svg", "Protect",
      "The write-protect jumper: closed, the odd-numbered banks cannot be written");
  private final JToggleButton upload = new JToggleButton("Upload");

  public ZxataspFrame() {
    super("ZXATASP", ZxataspPeripheral.class, "disk", "hdf", "master", "slave");
    upload.setToolTipText("Upload mode: reads see the machine's ROM while writes go to the bank, to fill one safely");
    jumper.addActionListener(e -> onEmulator(d -> {
      d.setWriteProtect(jumper.isSelected());
      d.refresh();
    }));
    upload.addActionListener(e -> onEmulator(d -> {
      d.setUpload(upload.isSelected());
      d.refresh();
    }));
    controls.add(jumper);
    controls.add(upload);
    controls.revalidate();
    plugged(null);
  }

  @Override
  protected void plugged(ZxataspPeripheral device) {
    super.plugged(device);
    if (jumper == null) {
      return;
    }
    boolean in = device != null;
    jumper.setEnabled(in);
    upload.setEnabled(in);
    if (in) {
      jumper.setSelected(device().writeProtect());
      upload.setSelected(device().upload());
    }
  }
}
