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


package com.fpetrola.oozx.speccy.devices.spec256;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;

/**
 * What is going on with a game in 256 colours: which one is loaded, what its own file asked for,
 * the colours it is painted in, the pictures that lie under its screen, and the switch every
 * emulator of this has - the game's colours, or the ones the machine it was written for would show.
 */
public class Spec256Frame extends MachineFrame {
  private static final int REFRESH_MILLIS = 250;
  private static final int ACROSS = 32;

  private final JLabel playing = new JLabel();
  private final JLabel said = new JLabel();
  private final JLabel under = new JLabel();
  private final JCheckBox colourful = new JCheckBox("In its own colours");
  private final JCheckBox pointers = new JCheckBox("Address with the machine's pointers");
  private final JButton previous = new JButton("<");
  private final JButton next = new JButton(">");
  private final JLabel[] swatches = new JLabel[256];
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());

  public Spec256Frame() {
    super("Spec256");
    setSize(470, 290);

    JPanel palette = new JPanel(new GridLayout(256 / ACROSS, ACROSS, 1, 1));
    for (int colour = 0; colour < swatches.length; colour++) {
      JLabel swatch = new JLabel();
      swatch.setOpaque(true);
      swatch.setToolTipText("Colour " + colour);
      swatches[colour] = swatch;
      palette.add(swatch);
    }

    colourful.addActionListener(e -> {
      Spec256Peripheral game = game();
      if (game != null) game.inItsColours(colourful.isSelected());
      refresh();
    });
    pointers.setToolTipText("<html>A follower's own pointer registers carry colours, and one addition on one of them<br>"
        + "sends a write somewhere the machine never wrote. Taking them from the machine costs<br>"
        + "the colours they were carrying: some games want it and others look worse with it.</html>");
    pointers.addActionListener(e -> {
      Spec256Peripheral game = game();
      if (game != null) game.pointersFromTheMachine(pointers.isSelected());
      refresh();
    });
    previous.addActionListener(e -> show(-1));
    next.addActionListener(e -> show(1));

    JPanel backgrounds = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    backgrounds.add(under);
    backgrounds.add(previous);
    backgrounds.add(next);

    JPanel top = new JPanel(new GridLayout(2, 1));
    top.add(playing);
    top.add(said);

    JPanel switches = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    switches.add(colourful);
    switches.add(pointers);

    JPanel bottom = new JPanel(new BorderLayout());
    bottom.add(switches, BorderLayout.WEST);
    bottom.add(backgrounds, BorderLayout.EAST);

    JPanel inside = new JPanel(new BorderLayout(0, 6));
    inside.add(top, BorderLayout.NORTH);
    inside.add(palette, BorderLayout.CENTER);
    inside.add(bottom, BorderLayout.SOUTH);
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
  protected void machineChanged(Speccy was, Speccy now) {
    refresh();
  }

  @Override
  protected String expandTip() {
    return "Show the two hundred and fifty-six colours, or just which game is being played in them";
  }

  @Override
  protected String attachTip() {
    return "Clip onto the machine in front to watch the game it is playing in 256 colours";
  }

  private Spec256Peripheral game() {
    Speccy machine = machine();
    return machine == null ? null : (Spec256Peripheral) machine.peripheralRegistry.find(Spec256Peripheral.class);
  }

  private void show(int by) {
    Spec256Peripheral game = game();
    if (game != null) game.show(game.showing() + by);
    refresh();
  }

  private void refresh() {
    Spec256Peripheral game = game();
    if (game == null || game.playing() == null) {
      playing.setText(machine() == null
          ? "Clip this onto a machine to watch a game in 256 colours"
          : "No game in 256 colours: load a snapshot with its colours in a file beside it");
      said.setText(" ");
      under.setText(" ");
      colourful.setEnabled(false);
      pointers.setEnabled(false);
      previous.setEnabled(false);
      next.setEnabled(false);
      for (JLabel swatch : swatches) swatch.setBackground(Color.DARK_GRAY);
      return;
    }
    playing.setText(game.playing() + ", on eight processors following the machine's own");
    int without = game.cellsWithNoColours();
    int cells = 24 * 32;
    said.setText(itsRules(game) + " | " + (without == 0 ? "every cell of the screen has colours of its own"
        : without + " of " + cells + " cells have no colours of their own"));
    int pictures = game.backgrounds();
    under.setText(pictures == 0 ? "Nothing under the screen"
        : "Under the screen: " + (game.showing() + 1) + " of " + pictures);
    colourful.setEnabled(true);
    colourful.setSelected(game.inItsColours());
    pointers.setEnabled(true);
    pointers.setSelected(game.pointersFromTheMachine());
    previous.setEnabled(pictures > 1 && game.showing() > 0);
    next.setEnabled(pictures > 1 && game.showing() < pictures - 1);
    int[] palette = machine().picture.palette;
    for (int colour = 0; colour < swatches.length; colour++) {
      swatches[colour].setBackground(new Color(palette[colour]));
    }
  }

  private static String itsRules(Spec256Peripheral game) {
    Rules rules = game.rules();
    StringBuilder said = new StringBuilder("Takes " + game.alignment().said() + "; mixes ")
        .append(rules.mixedFromTheTop).append(" from the top");
    if (rules.mixedFromTheBottom > 0) said.append(" and ").append(rules.mixedFromTheBottom).append(" from the bottom");
    if (rules.paperForNoneInkForAll) said.append("; first colour is the paper");
    if (rules.backgroundOverTheLast) said.append("; last colour shows the picture");
    if (!rules.hiddenWhereInkIsPaper) said.append("; a cleared cell does not cover it");
    if (rules.levelledOr || rules.levelledAnd || rules.levelledXor) said.append("; logic on levels");
    return said.toString();
  }
}
