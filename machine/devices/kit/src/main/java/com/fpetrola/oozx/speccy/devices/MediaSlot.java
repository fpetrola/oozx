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
package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.oozx.speccy.windows.Widgets;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.util.function.Consumer;

/**
 * A slot something goes into - a cartridge, a disk, a microdrive cartridge, a card - with what
 * is in it written next to it. Its shape is the same on every device; what changes is what is
 * inserted, and that is the device's business. A drive adds what a drive has: a light, a blank
 * to format, saving what was written, turning the disk over, the write-protect tab.
 */
public class MediaSlot extends JPanel {

  private final String kind;
  private final JLabel led = new JLabel("●");
  private final JLabel holding = new JLabel();
  private final JButton insert;
  private final JButton eject;
  private final FileNameExtensionFilter filter;
  private final Consumer<File> onInsert;
  private JToggleButton flip;
  private JToggleButton protect;

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
    led.setVisible(false);
    add(led);
    add(insert);
    add(eject);
    add(holding);
    Widgets.tighten(this);
    show(null);
  }

  /** A light that is on while the drive is turning. */
  public MediaSlot withLed(String tip) {
    led.setToolTipText(tip);
    led.setVisible(true);
    led(false);
    return this;
  }

  /** A blank one, for the machine to format. */
  public MediaSlot withNew(Runnable onNew) {
    JButton blank = Widgets.iconButton("slot-new.svg", "New", "A blank " + kind + ", for the machine to format");
    blank.addActionListener(e -> onNew.run());
    add(blank, 2);
    Widgets.tighten(this);
    return this;
  }

  /** Writes what is on it back to a file. */
  public MediaSlot withSave(Consumer<File> onSaveAs) {
    JButton save = Widgets.iconButton("slot-save.svg", "Save...", "Save the " + kind + " to a file");
    save.addActionListener(e -> {
      JFileChooser chooser = new JFileChooser(lastDirectory);
      chooser.setDialogTitle("Save the " + kind);
      chooser.setFileFilter(filter);
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        lastDirectory = chooser.getCurrentDirectory();
        onSaveAs.accept(chooser.getSelectedFile());
      }
    });
    add(save);
    Widgets.tighten(this);
    return this;
  }

  /** Turned over, which is how a single-sided drive reads the other side. */
  public MediaSlot withFlip(Consumer<Boolean> onFlip) {
    flip = Widgets.iconToggle("slot-flip.svg", "Flip", "Turn the " + kind + " over");
    flip.addActionListener(e -> onFlip.accept(flip.isSelected()));
    add(flip);
    Widgets.tighten(this);
    return this;
  }

  /** The write-protect tab. */
  public MediaSlot withWriteProtect(Consumer<Boolean> onProtect) {
    protect = Widgets.iconToggle("slot-lock.svg", "Protect", "Write-protect the " + kind);
    protect.addActionListener(e -> onProtect.accept(protect.isSelected()));
    add(protect);
    Widgets.tighten(this);
    return this;
  }

  /** What is in the slot now, or null for nothing. */
  public void show(String name) {
    holding.setText(name == null ? "no " + kind : name);
    holding.setEnabled(name != null);
    eject.setEnabled(name != null);
    if (flip != null) flip.setEnabled(name != null);
    if (protect != null) protect.setEnabled(name != null);
  }

  public void showFlipped(boolean flipped) {
    if (flip != null) flip.setSelected(flipped);
  }

  public void showProtected(boolean writeProtected) {
    if (protect != null) protect.setSelected(writeProtected);
  }

  public void led(boolean on) {
    led.setForeground(on ? new Color(0x30c030) : Color.GRAY);
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
