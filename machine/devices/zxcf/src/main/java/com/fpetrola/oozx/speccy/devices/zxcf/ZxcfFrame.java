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
package com.fpetrola.oozx.speccy.devices.zxcf;

import com.fpetrola.oozx.speccy.devices.IdeBayFrame;

import javax.swing.JToggleButton;

/** The ZXCF on the desk: its card, and the upload jumper; write protection is the card's own register. */
public class ZxcfFrame extends IdeBayFrame<ZxcfPeripheral> {

  private final JToggleButton upload = new JToggleButton("Upload");

  public ZxcfFrame() {
    super("ZXCF CompactFlash", ZxcfPeripheral.class, "card");
    upload.setToolTipText("Upload mode: reads see the machine's ROM while writes go to the bank, to fill one safely");
    upload.addActionListener(e -> onEmulator(d -> {
      machine().settings.current.zxcfUpload = upload.isSelected();
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
      upload.setSelected(machine().settings.current.zxcfUpload);
    }
  }
}
