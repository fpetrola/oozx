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
package com.fpetrola.oozx.speccy.devices.multiface;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.peripherals.t.Widgets;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

/**
 * A Multiface on the desk: the red button, and next to it whether the ROM is paged in, which is
 * the one thing about it you cannot see on the screen until its menu appears.
 */
public class MultifaceFrame extends DeviceFrame<MultifacePeripheral> {

  private static final int REFRESH_MILLIS = 100;

  private final MultifaceModel model;
  private final JButton button = Widgets.iconButton("multiface-button.svg", "NMI",
      "The red button: stops whatever is running and takes it to the Multiface");
  private final JButton rom = Widgets.iconButton("multiface-rom.svg", "ROM...",
      "Choose the Multiface's ROM, which is not something this emulator can ship");
  private final JToggleButton stealth = new JToggleButton("Stealth");
  private final JLabel led = new JLabel("●");
  private final JLabel status = new JLabel();
  private final JTextArea contents = new JTextArea();
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());

  public MultifaceFrame(MultifaceModel model) {
    super(model.title, model.peripheral);
    this.model = model;
    setSize(420, 300);

    button.addActionListener(e -> press());
    rom.addActionListener(e -> chooseRom());
    stealth.setToolTipText("The One's switch: off, the port cannot page it in and the button does nothing"
        + " - a program looking for a Multiface finds none");
    stealth.addActionListener(e -> {
      if (machine() != null) {
        machine().settings.current.multiface1Stealth = stealth.isSelected();
        resetHard();
      }
    });
    led.setToolTipText("Lit while the Multiface's ROM is paged in over the machine's");
    controls.add(button);
    controls.add(led);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(rom);
    if (model == MultifaceModel.ONE) {
      controls.add(stealth);
    }
    controls.add(status);

    contents.setEditable(false);
    contents.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    assemble(new JScrollPane(contents));
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
    refresh.start();
    plugged(null);
  }

  @Override
  protected void plugged(MultifacePeripheral device) {
    boolean in = device != null;
    button.setEnabled(in);
    rom.setEnabled(in);
    stealth.setEnabled(in);
    if (in) {
      stealth.setSelected(machine().settings.current.multiface1Stealth);
    }
    refresh();
  }

  private void press() {
    MultifacePeripheral pressed = device();
    if (pressed != null) {
      machine().z80.later(pressed::redButton);
    }
  }

  private void chooseRom() {
    if (machine() == null) {
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("The " + model.title + "'s ROM (8K)");
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File chosen = chooser.getSelectedFile();
      model.chooseRom(machine().settings, chosen.getPath());
      resetHard();
    }
  }

  /** The ROM is read at a reset, the way the cartridge is. */
  private void resetHard() {
    machine().z80.later(() -> machine().machine.reset(true));
  }

  private void refresh() {
    MultifacePeripheral device = device();
    if (device == null) {
      led.setForeground(Color.GRAY);
      status.setText("not plugged into a machine");
      return;
    }
    led.setForeground(device.isPaged() ? Color.RED : Color.GRAY);
    status.setText(device.isAvailable() ? (device.isPaged() ? "paged in" : "ready")
        : "no ROM: " + model.rom(machine().settings));
    if (contents.isShowing()) {
      contents.setText(dump(device));
    }
  }

  /** The 8K of RAM, which is where the ROM keeps what it saw of the machine. */
  private static String dump(MultifacePeripheral device) {
    StringBuilder text = new StringBuilder();
    for (int row = 0; row < MultifacePeripheral.RAM_SIZE; row += 16) {
      text.append(String.format("%04X ", 0x2000 + row));
      for (int i = 0; i < 16; i++) {
        text.append(String.format(" %02X", device.ram(row + i)));
      }
      text.append('\n');
    }
    return text.toString();
  }

  @Override
  protected String expandTip() {
    return "Show the Multiface's RAM, or just the button";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the Multiface in";
  }
}
