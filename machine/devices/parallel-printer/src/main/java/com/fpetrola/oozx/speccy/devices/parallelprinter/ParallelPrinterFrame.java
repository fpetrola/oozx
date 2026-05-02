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
package com.fpetrola.oozx.speccy.devices.parallelprinter;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.windows.Widgets;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A dot-matrix printer on the desk: continuous paper with whatever the machine sent, which is
 * text - LPRINT on a +3, or the +D's port. Tear it off, or save it as a text file.
 */
public class ParallelPrinterFrame extends DeviceFrame<ParallelPrinterPeripheral> {

  private final JTextArea paper = new JTextArea();
  private final Runnable follow = () -> SwingUtilities.invokeLater(this::refresh);
  private ParallelPrinter printer;

  public ParallelPrinterFrame() {
    super("Parallel printer", ParallelPrinterPeripheral.class);
    setSize(480, 320);
    JButton tearOff = Widgets.iconButton("printer-tear.svg", "Tear off", "Tear the paper off and start a new sheet");
    JButton save = Widgets.iconButton("printer-save.svg", "Save...", "Save the printout as a text file");
    tearOff.addActionListener(e -> {
      if (printer != null) printer.tearOff();
    });
    save.addActionListener(e -> save());
    controls.add(tearOff);
    controls.add(save);

    paper.setEditable(false);
    paper.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    paper.setLineWrap(true);
    assemble(new JScrollPane(paper));
    setCompact(false);
  }

  @Override
  protected void plugged(ParallelPrinterPeripheral device) {
    printer = device == null ? null : device.printer();
    if (printer != null) {
      printer.onChange(follow);
    }
    refresh();
  }

  private void refresh() {
    paper.setText(printer == null ? "" : printer.text());
    paper.setCaretPosition(paper.getDocument().getLength());
  }

  private void save() {
    if (printer == null) {
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File("printout.txt"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    try {
      Files.writeString(chooser.getSelectedFile().toPath(), printer.text());
    } catch (IOException couldNotWrite) {
      JOptionPane.showMessageDialog(this, "Could not save the printout: " + couldNotWrite.getMessage());
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
}
