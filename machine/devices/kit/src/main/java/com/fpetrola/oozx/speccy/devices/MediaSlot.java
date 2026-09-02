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
package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.oozx.speccy.peripherals.t.Widgets;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.FlowLayout;
import java.io.File;
import java.util.function.Consumer;

/**
 * A slot something goes into - a cartridge, a disk, a microdrive cartridge, a card - with what
 * is in it written next to it. Its shape is the same on every device; what changes is what is
 * inserted, and that is the device's business.
 */
public class MediaSlot extends JPanel {

  private final String kind;
  private final JLabel holding = new JLabel();
  private final JButton insert;
  private final JButton eject;
  private final FileNameExtensionFilter filter;
  private final Consumer<File> onInsert;

  /** Remembered between choosers, so the next disk is looked for where the last one was. */
  private static File lastDirectory;

  public MediaSlot(String kind, String icon, FileNameExtensionFilter filter, Consumer<File> onInsert,
                   Runnable onEject) {
    super(new FlowLayout(FlowLayout.LEFT, 4, 0));
    this.kind = kind;
    this.filter = filter;
    this.onInsert = onInsert;
    setOpaque(false);
    insert = Widgets.iconButton(icon, "Insert", "Insert a " + kind);
    eject = Widgets.iconButton("23CF.svg", "Eject", "Take the " + kind + " out");
    insert.addActionListener(e -> choose());
    eject.addActionListener(e -> onEject.run());
    add(insert);
    add(eject);
    add(holding);
    Widgets.tighten(this);
    show(null);
  }

  /** What is in the slot now, or null for nothing. */
  public void show(String name) {
    holding.setText(name == null ? "no " + kind : name);
    holding.setEnabled(name != null);
    eject.setEnabled(name != null);
  }

  private void choose() {
    JFileChooser chooser = new JFileChooser(lastDirectory);
    chooser.setDialogTitle("Insert a " + kind);
    chooser.setFileFilter(filter);
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      lastDirectory = chooser.getCurrentDirectory();
      onInsert.accept(chooser.getSelectedFile());
    }
  }
}
