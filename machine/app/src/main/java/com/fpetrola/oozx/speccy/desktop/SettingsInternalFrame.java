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
package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.peripherals.EmulatorCore;
import com.fpetrola.oozx.speccy.peripherals.SettingsPanel;
import com.fpetrola.oozx.speccy.windows.AttachedFrame;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Function;

/**
 * The settings of whatever it is clipped onto.
 * <p>
 * Attached to a machine it configures that machine, and dragging it onto another one configures
 * that one instead: which machine is being configured is a question about where the window is,
 * answered by the window itself, rather than something to be chosen inside it. Let go of every
 * machine, it configures what a new machine starts with, and says so across the top.
 */
public class SettingsInternalFrame extends AttachedFrame {
  /** Orange enough to be seen from across the desktop: what is being changed here is not any one machine. */
  private static final Color DEFAULTS = new Color(0xC0, 0x6A, 0x00);

  private final Function<JInternalFrame, EmulatorCore> coreOf;
  private final EmulatorCore defaults;
  private final OOZxConfiguration config;
  private final JLabel whom = new JLabel();
  private final JPanel body = new JPanel(new BorderLayout());

  public SettingsInternalFrame(Function<JInternalFrame, EmulatorCore> coreOf, EmulatorCore defaults,
      OOZxConfiguration config) {
    super("Settings");
    this.coreOf = coreOf;
    this.defaults = defaults;
    this.config = config;
    whom.setFont(whom.getFont().deriveFont(Font.BOLD));
    whom.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
    controls.add(whom);
    assemble(body);
    setCompact(false);
    // After being assembled: laying it out compact and then opening it leaves the frame the size
    // of its buttons, and what it has to show is a tabbed pane.
    setSize(680, 520);
    // Against the side: along the bottom it would be as wide as the machine, and seven tabs do
    // not go in three hundred pixels.
    prefersDock(Dock.RIGHT);
    showWhatIsBeingConfigured();
  }

  /** Built again against whoever it is on now: every control reads the state of what it configures. */
  @Override
  protected void attachmentChanged() {
    showWhatIsBeingConfigured();
  }

  private void showWhatIsBeingConfigured() {
    EmulatorCore core = getMachineWindow() == null ? null : coreOf.apply(getMachineWindow());
    boolean aMachine = core != null;
    whom.setText(aMachine ? "Configuring this machine: " + core.getCurrentModel()
        : "Defaults - what a machine opened from now on starts with");
    whom.setForeground(aMachine ? UIManagerForeground() : DEFAULTS);
    setTitle(aMachine ? "Settings - " + core.getCurrentModel() : "Settings - defaults");

    body.removeAll();
    body.add(new SettingsPanel(aMachine ? core : defaults, config), BorderLayout.CENTER);
    body.revalidate();
    body.repaint();
  }

  private static Color UIManagerForeground() {
    Color colour = javax.swing.UIManager.getColor("Label.foreground");
    return colour == null ? Color.BLACK : colour;
  }

  @Override
  protected String expandTip() {
    return "Show the settings, or just what is being configured";
  }

  @Override
  protected String attachTip() {
    return "Clip onto a machine to configure that one, or let go to set what new machines start with";
  }
}
