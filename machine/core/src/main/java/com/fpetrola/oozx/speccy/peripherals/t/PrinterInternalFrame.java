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

package com.fpetrola.oozx.speccy.peripherals.t;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.printer.PrinterPaper;
import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterPeripheral;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Function;

/**
 * A ZX Printer sitting next to a machine, showing what comes out of it.
 * <p>
 * Clipping it onto a machine's window is the cable: while it is attached that machine has a
 * printer and LPRINT and COPY reach this paper, and detaching it is carrying the printer to
 * another room. The machine is told on its own thread, because switching a device on rebuilds the
 * list of ports and that cannot happen in the middle of a frame.
 */
public class PrinterInternalFrame extends AttachedFrame {

  private final Function<JInternalFrame, Speccy> machineOf;
  private final PrinterPaper paper = new PrinterPaper(new Printout());

  /** The printer this window is the paper of, or none while it is not clipped to anything. */
  private ZxPrinterPeripheral printer;
  private Speccy plugged;

  public PrinterInternalFrame(Function<JInternalFrame, Speccy> machineOf) {
    super("ZX Printer");
    this.machineOf = machineOf;

    setSize(320, 460);

    JButton tearOff = new JButton("Tear off");
    JButton save = new JButton("Save...");
    JButton bigger = new JButton("Zoom");
    tearOff.setToolTipText("Tear the paper off and start a new sheet");
    save.setToolTipText("Save the printout as a PNG");
    bigger.setToolTipText("Show the dots larger");
    tearOff.addActionListener(e -> paper.printout().tearOff());
    save.addActionListener(e -> save());
    bigger.addActionListener(e -> paper.setScale(paper.scale() % 4 + 1));

    controls.add(tearOff);
    controls.add(save);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(bigger);

    JScrollPane scroll = new JScrollPane(paper);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    assemble(scroll);
    setCompact(false);
  }

  /**
   * Clipped onto a machine, or off it. Attaching switches the printer on for that machine and
   * detaching switches it off, which is the whole point of the window: an unplugged printer is not
   * a printer that machine has.
   */
  @Override
  protected void attachmentChanged() {
    Speccy machine = isAttached() ? machineOf.apply(getMachineWindow()) : null;
    if (machine == plugged) {
      return;
    }
    connect(plugged, false);
    plugged = machine;
    printer = machine == null ? null
        : (ZxPrinterPeripheral) machine.peripherals.find(ZxPrinterPeripheral.class);
    // Unplugged, the paper printed so far stays in the printer, which is where it would be.
    if (printer != null) {
      paper.setPrintout(printer.paper());
    }
    connect(plugged, true);
  }

  @Override
  protected void machineClosed() {
    connect(plugged, false);
    plugged = null;
    printer = null;
    super.machineClosed();
  }

  /** Said on the emulator's own thread: switching a device on rebuilds the machine's ports. */
  private void connect(Speccy machine, boolean connected) {
    if (machine == null || printer == null) {
      return;
    }
    ZxPrinterPeripheral wired = printer;
    machine.z80.later(() -> {
      wired.plugIn(connected);
      machine.peripherals.update();
    });
  }

  @Override
  protected String expandTip() {
    return "Show the paper, or just the controls";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the printer in";
  }

  private void save() {
    Printout printout = paper.printout();
    if (printout.height() == 0) {
      return;
    }
    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
    chooser.setSelectedFile(new File("printout.png"));
    if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
      return;
    }
    // Saved as it is shown, paper and all: a picture of a printout is a picture of the paper it
    // came out on, and the panel already knows how to draw one.
    int width = Printout.WIDTH * paper.scale();
    int height = printout.height() * paper.scale();
    BufferedImage image = new BufferedImage(width + 24, height + 24, BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D canvas = image.createGraphics();
    canvas.setColor(new java.awt.Color(0x2e, 0x30, 0x2c));
    canvas.fillRect(0, 0, image.getWidth(), image.getHeight());
    canvas.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    paper.paint(canvas, 12, 12, width, height);
    canvas.dispose();
    try {
      javax.imageio.ImageIO.write(image, "png", chooser.getSelectedFile());
    } catch (java.io.IOException couldNotWrite) {
      javax.swing.JOptionPane.showMessageDialog(this, "Could not save the printout: " + couldNotWrite.getMessage());
    }
  }
}
