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

package com.fpetrola.oozx.speccy.devices.interface1;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.devices.MediaSlot;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The Interface 1 on the desk: its eight Microdrives, each a slot with the motor's light, and a
 * lamp for the shadow ROM. Expanded, the far end of its RS232 as a terminal - what the Spectrum
 * sends appears, what is typed goes to it - and, for talking to another emulator the way Fuse
 * does, the files the RS232 and the ZX Net can be plugged into instead.
 */
public class Interface1Frame extends DeviceFrame<Interface1Peripheral> {

  private static final int REFRESH_MILLIS = 100;
  private static final FileNameExtensionFilter CARTRIDGES = new FileNameExtensionFilter("Microdrive cartridge (MDR)", "mdr");

  private final MediaSlot[] slots = new MediaSlot[Interface1Peripheral.DRIVES];
  private final JLabel paged = new JLabel("●");
  private final JLabel status = new JLabel();
  private final JTextArea terminal = new JTextArea();
  private final JButton rxd = new JButton("RxD file...");
  private final JButton txd = new JButton("TxD file...");
  private final JButton network = new JButton("ZX Net file...");
  private final JButton unplug = new JButton("Unplug files");
  private final JLabel wires = new JLabel();
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
  private final IntConsumer onReceived = this::print;

  public Interface1Frame() {
    super("Interface 1", Interface1Peripheral.class);
    setSize(720, 200);

    JPanel bays = new JPanel(new GridLayout(0, 2, 8, 2));
    bays.setOpaque(false);
    for (int i = 0; i < slots.length; i++) {
      int which = i;
      slots[i] = new MediaSlot("cartridge", "cartridge.svg", CARTRIDGES, file -> insert(which, file), () -> eject(which))
          .withLed("Lit while Microdrive " + (i + 1) + "'s motor is running")
          .withNew(() -> onEmulator(d -> d.insertBlank(which)))
          .withSave(file -> save(which, file))
          .withWriteProtect(on -> onEmulator(d -> d.writeProtect(which, on)));
      bays.add(slots[i]);
    }
    controls.add(bays);
    controls.add(Box.createHorizontalStrut(10));
    paged.setToolTipText("Lit while the Interface 1's shadow ROM is paged in over the machine's");
    controls.add(paged);
    controls.add(status);

    terminal.setEditable(false);
    terminal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    terminal.setToolTipText("The RS232's far end: what the Spectrum sends shows here, and what you type here goes to it");
    terminal.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent key) {
        char typed = key.getKeyChar();
        if (typed != KeyEvent.CHAR_UNDEFINED) {
          onEmulator(d -> d.rs232().type(typed == '\n' ? '\r' : typed));
        }
      }
    });
    rxd.setToolTipText("Plug the RS232's receive side into a file or FIFO, the way Fuse does");
    txd.setToolTipText("Plug the RS232's transmit side into a file or FIFO");
    network.setToolTipText("Plug the ZX Net into a file shared with another emulator");
    rxd.addActionListener(e -> plug("RxD", file -> d().rs232().plugRx(file)));
    txd.addActionListener(e -> plug("TxD", file -> d().rs232().plugTx(file)));
    network.addActionListener(e -> plug("ZX Net", file -> d().net().plug(file, machine().settings.current.rawSNet)));
    unplug.addActionListener(e -> onEmulator(d -> {
      d.rs232().unplugRx();
      d.rs232().unplugTx();
      d.net().unplug();
    }));
    JPanel plugs = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    plugs.setOpaque(false);
    plugs.add(rxd);
    plugs.add(txd);
    plugs.add(network);
    plugs.add(unplug);
    plugs.add(wires);
    JPanel inside = new JPanel(new BorderLayout());
    inside.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    inside.add(new JScrollPane(terminal), BorderLayout.CENTER);
    inside.add(plugs, BorderLayout.SOUTH);
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

  private interface Plugging {
    void into(File file) throws IOException;
  }

  private Interface1Peripheral d() {
    return device();
  }

  private void onEmulator(Consumer<Interface1Peripheral> work) {
    Interface1Peripheral plugged = device();
    if (plugged != null) {
      machine().z80.later(() -> work.accept(plugged));
    }
  }

  @Override
  protected void plugged(Interface1Peripheral device) {
    boolean in = device != null;
    for (MediaSlot slot : slots) {
      slot.setEnabled(in);
    }
    for (JButton button : new JButton[] {rxd, txd, network, unplug}) {
      button.setEnabled(in);
    }
    if (in) {
      device.rs232().terminal(onReceived);
    }
    refresh();
  }

  @Override
  protected void machineClosed() {
    Interface1Peripheral device = device();
    if (device != null) {
      device.rs232().terminal(null);
    }
    super.machineClosed();
  }

  private void print(int b) {
    SwingUtilities.invokeLater(() -> {
      terminal.append(String.valueOf((char) (b == '\r' ? '\n' : b)));
      terminal.setCaretPosition(terminal.getDocument().getLength());
    });
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

  private void save(int which, File file) {
    Interface1Peripheral plugged = device();
    if (plugged == null) {
      return;
    }
    try {
      plugged.save(which, file);
    } catch (IOException cannot) {
      complain(cannot);
    }
  }

  private void plug(String what, Plugging plugging) {
    if (device() == null) {
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("The file the " + what + " is plugged into");
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File chosen = chooser.getSelectedFile();
      onEmulator(d -> {
        try {
          plugging.into(chosen);
        } catch (IOException cannot) {
          complain(cannot);
        }
      });
    }
  }

  private void complain(IOException cannot) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, cannot.getMessage()));
  }

  private void refresh() {
    Interface1Peripheral plugged = device();
    if (plugged == null) {
      status.setText("not plugged into a machine");
      paged.setForeground(Color.GRAY);
      wires.setText("");
      return;
    }
    paged.setForeground(plugged.isPaged() ? Color.RED : Color.GRAY);
    status.setText(plugged.isAvailable() ? "" : "no ROM: " + plugged.romName());
    for (int i = 0; i < slots.length; i++) {
      slots[i].led(plugged.motorOn(i));
      slots[i].show(plugged.inserted(i) ? nameOf(plugged, i) : null);
      slots[i].showProtected(plugged.writeProtected(i));
    }
    wires.setText((plugged.rs232().rxPlugged() ? "RxD on a file. " : "")
        + (plugged.rs232().txPlugged() ? "TxD on a file. " : "")
        + (plugged.net().plugged() ? "ZX Net on a file." : ""));
  }

  private static String nameOf(Interface1Peripheral plugged, int which) {
    String name = plugged.cartridgeName(which) == null ? "blank cartridge"
        : new File(plugged.cartridgeName(which)).getName();
    return name + " (" + plugged.sectors(which) + " sectors" + (plugged.modified(which) ? ", changed" : "") + ")";
  }

  @Override
  protected String expandTip() {
    return "Show the RS232 terminal and the net, or just the Microdrives";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the Interface 1 in";
  }
}
