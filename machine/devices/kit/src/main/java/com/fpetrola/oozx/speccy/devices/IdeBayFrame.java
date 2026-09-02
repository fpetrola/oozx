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

import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.devices.ide.IdeInterface;
import com.fpetrola.oozx.speccy.devices.ide.MassStorage;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * An IDE interface on the desk: a slot per drive for its hard disk image, whose "save" is the
 * commit - what was written stays in memory until then, the way Fuse has it - a lamp for its
 * memory being paged in, and a line about its registers. Expanded, how big each disk is.
 */
public class IdeBayFrame<P extends Peripheral & Pluggable & IdeInterface> extends DeviceFrame<P> {

  private static final int REFRESH_MILLIS = 100;
  private final FileNameExtensionFilter images;

  private final String name;
  private final String slotKind;
  private final String[] units;
  private final MediaSlot[] slots;
  private final JLabel[] sizes;
  private final JLabel paged = new JLabel("●");
  private final JLabel status = new JLabel();
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());

  public IdeBayFrame(String name, Class<? extends P> kind, String kind0, String extension, String... units) {
    super(name, kind);
    this.name = name;
    this.slotKind = kind0;
    images = new FileNameExtensionFilter(kind0 + " image (" + extension.toUpperCase() + ")", extension);
    this.units = units;
    slots = new MediaSlot[units.length];
    sizes = new JLabel[units.length];
    setSize(560, 130 + 30 * units.length);

    JPanel bays = new JPanel(new GridLayout(0, 1, 0, 2));
    bays.setOpaque(false);
    for (int i = 0; i < slots.length; i++) {
      int which = i;
      slots[i] = new MediaSlot(units[i] + " " + slotKind, "floppy.svg", images, file -> insert(which, file), () -> eject(which))
          .withSave(file -> commit(which, file));
      bays.add(slots[i]);
    }
    controls.add(bays);
    controls.add(Box.createHorizontalStrut(10));
    paged.setToolTipText("Lit while the " + name + "'s memory is paged in over the machine's");
    controls.add(paged);
    controls.add(status);

    JPanel inside = new JPanel(new GridLayout(0, 1, 0, 4));
    for (int i = 0; i < sizes.length; i++) {
      sizes[i] = new JLabel();
      inside.add(sizes[i]);
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

  /** On the emulator's thread, which is where the channel's registers and buffers live. */
  protected void onEmulator(Consumer<P> work) {
    P plugged = device();
    if (plugged != null) {
      machine().z80.later(() -> work.accept(plugged));
    }
  }

  @Override
  protected void plugged(P device) {
    boolean in = device != null;
    for (MediaSlot slot : slots) {
      slot.setEnabled(in);
    }
    refresh();
  }

  private void insert(int which, File file) {
    onEmulator(d -> {
      try {
        d.insert(which, file);
      } catch (IOException cannot) {
        complain(cannot);
      }
    });
  }

  private void eject(int which) {
    onEmulator(d -> d.eject(which));
  }

  private void commit(int which, File file) {
    onEmulator(d -> {
      try {
        d.drive(which).commit(file);
      } catch (IOException cannot) {
        complain(cannot);
      }
    });
  }

  protected void complain(IOException cannot) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, cannot.getMessage()));
  }

  /** What the subclass shows besides the drives, refreshed with them. */
  protected void refreshed(P device) {
  }

  private void refresh() {
    P plugged = device();
    if (plugged == null) {
      status.setText("not plugged into a machine");
      paged.setForeground(Color.GRAY);
      return;
    }
    paged.setForeground(plugged.isPaged() ? Color.RED : Color.GRAY);
    status.setText(plugged.status());
    for (int i = 0; i < slots.length; i++) {
      MassStorage drive = plugged.drive(i);
      slots[i].show(drive.present() ? new File(drive.filename()).getName() + (drive.dirty() ? " (changed)" : "") : null);
      sizes[i].setText(units[i] + ": " + (drive.present()
          ? drive.sectors() + " sectors, " + drive.sectors() * IdeChannel.SECTOR / 1024 / 1024 + " MB" : "empty"));
    }
    refreshed(plugged);
  }

  @Override
  protected String expandTip() {
    return "Show how big the disks are, or just the slots";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the " + name + " in";
  }
}
