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

import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskInterface;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;
import com.fpetrola.oozx.speccy.peripherals.t.Widgets;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;

/**
 * A disk interface on the desk: a bay per drive, each with its light and its slot, the button
 * the board has, and a lamp for its ROM being paged in. Expanded, where each head is.
 */
public class DriveBayFrame<P extends Peripheral & Pluggable & DiskInterface> extends DeviceFrame<P> {

  private static final int REFRESH_MILLIS = 100;

  private final String name;
  private final MediaSlot[] slots;
  private final JLabel[] heads;
  private final JButton button;
  private final JLabel paged = new JLabel("●");
  private final JLabel status = new JLabel();
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());

  /**
   * @param shape what the board has, before one is plugged in: how many drives, which images,
   *              what its button is called. The one plugged in later says the same things.
   */
  public DriveBayFrame(String name, Class<? extends P> kind, DiskInterface shape) {
    super(name, kind);
    this.name = name;
    slots = new MediaSlot[shape.drives()];
    heads = new JLabel[shape.drives()];
    button = Widgets.iconButton("multiface-button.svg", shape.buttonName(), shape.buttonTip());
    button.setVisible(shape.buttonName() != null);
    FileNameExtensionFilter disks = new FileNameExtensionFilter(
        "Disk image (" + String.join(", ", shape.imageExtensions()).toUpperCase() + ")", shape.imageExtensions());
    setSize(560, 160 + 30 * shape.drives());

    JPanel bays = new JPanel(new GridLayout(0, 1, 0, 2));
    bays.setOpaque(false);
    for (int i = 0; i < slots.length; i++) {
      int which = i;
      slots[i] = new MediaSlot("disk " + (i + 1), "floppy.svg", disks, file -> insert(which, file), () -> eject(which))
          .withLed("Lit while drive " + (i + 1) + " is turning")
          .withNew(() -> insertBlank(which))
          .withSave(file -> save(which, file))
          .withFlip(over -> onEmulator(d -> d.drive(which).flip(over)))
          .withWriteProtect(on -> onEmulator(d -> d.drive(which).writeProtect(on)));
      bays.add(slots[i]);
    }
    controls.add(bays);
    controls.add(Box.createHorizontalStrut(10));
    button.addActionListener(e -> onEmulator(DiskInterface::button));
    paged.setToolTipText("Lit while the " + name + "'s ROM is paged in over the machine's");
    controls.add(button);
    controls.add(paged);
    controls.add(status);

    JPanel inside = new JPanel(new GridLayout(0, 1, 0, 4));
    for (int i = 0; i < heads.length; i++) {
      heads[i] = new JLabel();
      inside.add(heads[i]);
    }
    assemble(inside);
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
    refresh.start();
    plugged(null);
  }

  private interface Work {
    void on(DiskInterface device);
  }

  /** On the emulator's thread: the drives and the controller run there, on its clock. */
  private void onEmulator(Work work) {
    DiskInterface plugged = device();
    if (plugged != null) {
      machine().z80.later(() -> work.on(plugged));
    }
  }

  @Override
  protected void plugged(P device) {
    boolean in = device != null;
    for (MediaSlot slot : slots) {
      slot.setEnabled(in);
    }
    button.setEnabled(in);
    refresh();
  }

  private void insert(int which, File file) {
    try {
      Disk disk = Disk.open(file);
      onEmulator(d -> d.insert(which, disk));
    } catch (IOException cannot) {
      JOptionPane.showMessageDialog(this, cannot.getMessage());
    }
  }

  private void insertBlank(int which) {
    onEmulator(d -> {
      try {
        d.insertBlank(which);
      } catch (IOException cannot) {
        JOptionPane.showMessageDialog(this, cannot.getMessage());
      }
    });
  }

  private void eject(int which) {
    onEmulator(d -> d.eject(which));
  }

  private void save(int which, File file) {
    DiskInterface plugged = device();
    if (plugged == null) {
      return;
    }
    try {
      plugged.drive(which).disk.write(file);
    } catch (IOException cannot) {
      JOptionPane.showMessageDialog(this, cannot.getMessage());
    }
  }

  private void refresh() {
    DiskInterface plugged = device();
    if (plugged == null) {
      status.setText("not plugged into a machine");
      paged.setForeground(Color.GRAY);
      return;
    }
    paged.setForeground(plugged.isPaged() ? Color.RED : Color.GRAY);
    status.setText(plugged.isAvailable() ? "" : "no ROM: " + plugged.romName());
    for (int i = 0; i < slots.length; i++) {
      Fdd drive = plugged.drive(i);
      slots[i].led(drive.motoron);
      slots[i].show(drive.loaded ? nameOf(drive.disk) : null);
      slots[i].showFlipped(drive.upsidedown);
      slots[i].showProtected(drive.wrprot);
      heads[i].setText("Drive " + (i + 1) + ": " + (drive.loaded
          ? "track " + drive.cylinder() + (drive.disk.dirty ? ", changed since it was saved" : "") : "empty"));
    }
  }

  private static String nameOf(Disk disk) {
    return disk.filename == null ? "blank disk" : new File(disk.filename).getName();
  }

  @Override
  protected String expandTip() {
    return "Show where the heads are, or just the bays";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the " + name + " in";
  }
}
