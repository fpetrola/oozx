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
package com.fpetrola.oozx.speccy.devices.zxcf;

import com.fpetrola.oozx.speccy.devices.IdeBayFrame;

import javax.swing.JToggleButton;

/** The ZXCF on the desk: its card, and the upload jumper; write protection is the card's own register. */
public class ZxcfFrame extends IdeBayFrame<ZxcfPeripheral> {

  private final JToggleButton upload = new JToggleButton("Upload");

  public ZxcfFrame() {
    super("ZXCF CompactFlash", ZxcfPeripheral.class, "card", "hdf", "card");
    upload.setToolTipText("Upload mode: reads see the machine's ROM while writes go to the bank, to fill one safely");
    upload.addActionListener(e -> onEmulator(d -> {
      d.setUpload(upload.isSelected());
      d.refresh();
    }));
    controls.add(upload);
    controls.revalidate();
    plugged(null);
  }

  @Override
  protected void plugged(ZxcfPeripheral device) {
    super.plugged(device);
    if (upload == null) {
      return;
    }
    upload.setEnabled(device != null);
    if (device != null) {
      upload.setSelected(device().upload());
    }
  }
}
