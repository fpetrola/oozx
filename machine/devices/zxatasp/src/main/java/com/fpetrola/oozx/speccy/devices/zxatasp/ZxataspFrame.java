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
package com.fpetrola.oozx.speccy.devices.zxatasp;

import com.fpetrola.oozx.speccy.devices.IdeBayFrame;
import com.fpetrola.oozx.speccy.peripherals.t.Widgets;

import javax.swing.JToggleButton;

/** The ZXATASP on the desk: its two drives, and the board's two jumpers. */
public class ZxataspFrame extends IdeBayFrame<ZxataspPeripheral> {

  private final JToggleButton jumper = Widgets.iconToggle("slot-lock.svg", "Protect",
      "The write-protect jumper: closed, the odd-numbered banks cannot be written");
  private final JToggleButton upload = new JToggleButton("Upload");

  public ZxataspFrame() {
    super("ZXATASP", ZxataspPeripheral.class, "master", "slave");
    upload.setToolTipText("Upload mode: reads see the machine's ROM while writes go to the bank, to fill one safely");
    jumper.addActionListener(e -> onEmulator(d -> {
      machine().settings.current.zxataspWp = jumper.isSelected();
      d.refresh();
    }));
    upload.addActionListener(e -> onEmulator(d -> {
      machine().settings.current.zxataspUpload = upload.isSelected();
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
      jumper.setSelected(machine().settings.current.zxataspWp);
      upload.setSelected(machine().settings.current.zxataspUpload);
    }
  }
}
