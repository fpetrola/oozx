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

package com.fpetrola.oozx.speccy.devices.printer;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.windows.Widgets;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * ZX Printer window clipped to a machine: while attached, LPRINT/COPY write to this paper.
 * Device activation happens on the machine's own thread, since it rebuilds the port list and
 * cannot safely run mid-frame.
 */
public class PrinterInternalFrame extends DeviceFrame<ZxPrinterPeripheral> {

  private final PrinterPaper paper = new PrinterPaper(new Printout());

  public PrinterInternalFrame() {
    super("ZX Printer", ZxPrinterPeripheral.class);

    setSize(320, 460);

    JButton tearOff = Widgets.iconButton("printer-tear.svg", "Tear off",
        "Tear the paper off and start a new sheet");
    JButton save = Widgets.iconButton("printer-save.svg", "Save...",
        "Save the printout as a PNG");
    JButton fit = Widgets.iconButton("printer-fit.svg", "Fit",
        "Fit the paper across the window; ctrl and the wheel zooms, dragging moves it");
    JToggleButton filter = Widgets.iconToggle("printer-filter.svg", "Paper",
        "Show it as paper out of a printer, or as the dots the printer was sent");
    filter.setSelected(true);
    filter.addActionListener(e -> paper.setFiltered(filter.isSelected()));
    tearOff.addActionListener(e -> paper.printout().tearOff());
    save.addActionListener(e -> save());
    fit.addActionListener(e -> paper.fitWidth());

    controls.add(tearOff);
    controls.add(save);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(fit);
    controls.add(filter);

    JScrollPane scroll = new JScrollPane(paper);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    assemble(scroll);
    setCompact(false);
  }

  /** Detaching leaves the already-printed paper in place, matching a real unplugged printer. */
  @Override
  protected void plugged(ZxPrinterPeripheral printer) {
    if (printer != null) {
      paper.setPrintout(printer.paper());
    }
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
    // Captures the full paper rendering (not just dots), at a fixed 3px/dot regardless of the
    // window's current zoom, since the saved image should not depend on view state.
    int width = Printout.WIDTH * 3;
    int height = printout.height() * 3;
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
