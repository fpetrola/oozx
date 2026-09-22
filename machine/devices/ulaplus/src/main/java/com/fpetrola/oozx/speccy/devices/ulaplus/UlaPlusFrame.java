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

package com.fpetrola.oozx.speccy.devices.ulaplus;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.modules.display.Picture;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * The sixty-four colours a machine has been given, on the desk: four rows of sixteen, which is
 * how the chip is laid out - the two bits that used to be bright and flash name the row, the ink
 * is the first half of it and the paper the second.
 * <p>
 * Clipping this window onto a machine is what gives that machine the chip, the way clipping any
 * device's window plugs the device in. Machines that came with it have it already and go on
 * having it when this window is closed.
 */
public class UlaPlusFrame extends DeviceFrame<UlaPlusPeripheral> {
  private static final int REFRESH_MILLIS = 120;
  private static final String[] ROWS = {"plain", "bright", "flash", "both"};

  private final JLabel[] swatches = new JLabel[UlaPlusPeripheral.COLOURS];
  private final JLabel painting = new JLabel();
  private final JLabel told = new JLabel();
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());

  public UlaPlusFrame() {
    super("ULAplus", UlaPlusPeripheral.class);
    setSize(460, 230);

    JPanel tables = new JPanel(new GridLayout(ROWS.length, 1, 0, 4));
    for (int row = 0; row < ROWS.length; row++) {
      JPanel line = new JPanel(new BorderLayout(6, 0));
      JLabel name = new JLabel(ROWS[row]);
      name.setPreferredSize(new Dimension(48, 18));
      name.setToolTipText("What the two bits that were bright and flash name now");
      line.add(name, BorderLayout.WEST);
      JPanel sixteen = new JPanel(new GridLayout(1, 16, 2, 0));
      for (int colour = 0; colour < 16; colour++) {
        JLabel swatch = new JLabel();
        swatch.setOpaque(true);
        swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        swatch.setToolTipText(colour < 8 ? "Ink " + colour : "Paper " + (colour - 8));
        swatches[row * 16 + colour] = swatch;
        sixteen.add(swatch);
      }
      line.add(sixteen, BorderLayout.CENTER);
      tables.add(line);
    }

    JPanel inside = new JPanel(new BorderLayout(0, 6));
    inside.add(painting, BorderLayout.NORTH);
    inside.add(tables, BorderLayout.CENTER);
    inside.add(told, BorderLayout.SOUTH);
    assemble(inside);

    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
    refresh.start();
    refresh();
  }

  @Override
  protected void plugged(UlaPlusPeripheral device) {
    refresh();
  }

  private void refresh() {
    UlaPlusPeripheral chip = device();
    if (chip == null || machine() == null) {
      painting.setText("Clip this onto a machine to give it these colours");
      told.setText(" ");
      for (JLabel swatch : swatches) swatch.setBackground(Color.DARK_GRAY);
      return;
    }
    int ofItsOwn = 0;
    for (int colour = 0; colour < UlaPlusPeripheral.COLOURS; colour++) {
      int rgb = chip.colourOf(colour);
      swatches[colour].setBackground(new Color(rgb));
      if (rgb != Picture.SINCLAIR[colour & 0x0f]) ofItsOwn++;
    }
    painting.setText(chip.inUse()
        ? "Painting in these, and " + ofItsOwn + " of the sixty-four are not the Sinclair colours"
        : "Fitted, and painting in the usual sixteen until a program says otherwise");
    told.setText("Last register named: " + named(chip.named()));
  }

  @Override
  protected String expandTip() {
    return "Show the sixty-four colours, or just whether the machine is painting in them";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what gives that machine these colours";
  }

  /** What a byte written to the register port asked for, said the way the chip reads it. */
  private static String named(int register) {
    return (register & 0x40) != 0 ? "the one that says whether to paint in them"
        : "colour " + (register & (UlaPlusPeripheral.COLOURS - 1));
  }
}
