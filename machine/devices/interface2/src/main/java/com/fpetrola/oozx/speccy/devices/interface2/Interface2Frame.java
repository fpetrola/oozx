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
package com.fpetrola.oozx.speccy.devices.interface2;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.devices.MediaSlot;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick.JoystickType;
import com.fpetrola.oozx.speccy.windows.Widgets;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;

/**
 * The Interface 2 on the desk: the cartridge slot on top, and the two joystick sockets on the
 * side, which is where the keyboard's joystick can be plugged in.
 */
public class Interface2Frame extends DeviceFrame<Interface2Peripheral> {

  private final MediaSlot slot;
  private final JToggleButton socket1 = Widgets.iconToggle("1F579.svg", "1", "Joystick 1: the keyboard's joystick reads as keys 6 to 0");
  private final JToggleButton socket2 = Widgets.iconToggle("1F579.svg", "2", "Joystick 2: the keyboard's joystick reads as keys 1 to 5");

  public Interface2Frame() {
    super("ZX Interface 2", Interface2Peripheral.class);
    setSize(420, 200);

    slot = new MediaSlot("cartridge", "cartridge.svg",
        new FileNameExtensionFilter("ROM cartridge (16K)", "rom", "bin"), this::insert, this::eject);
    controls.add(slot);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(new JLabel("Joystick"));
    controls.add(socket1);
    controls.add(socket2);
    socket1.addActionListener(e -> plugJoystick(socket1.isSelected() ? JoystickType.JOYSTICK_TYPE_SINCLAIR_1 : JoystickType.JOYSTICK_TYPE_NONE));
    socket2.addActionListener(e -> plugJoystick(socket2.isSelected() ? JoystickType.JOYSTICK_TYPE_SINCLAIR_2 : JoystickType.JOYSTICK_TYPE_NONE));

    JPanel sockets = new JPanel(new GridLayout(0, 1, 0, 4));
    sockets.add(new JLabel("<html>A cartridge takes the place of the ROM: the machine restarts on it "
        + "when it goes in, and on its own ROM when it comes out.</html>"));
    sockets.add(new JLabel("<html>Socket 1 reads as the keys 6 (left), 7 (right), 8 (down), 9 (up) and 0 (fire); "
        + "socket 2 as 1, 2, 3, 4 and 5. Whichever socket the keyboard's joystick is in, the machine sees those keys.</html>"));
    assemble(sockets);
    plugged(null);
  }

  @Override
  protected void plugged(Interface2Peripheral device) {
    slot.show(device == null || device.cartridge() == null ? null : device.cartridge().name());
    JoystickType type = device == null ? JoystickType.JOYSTICK_TYPE_NONE
        : Input.of(machine()).setup.keyboard.output;
    socket1.setSelected(type == JoystickType.JOYSTICK_TYPE_SINCLAIR_1);
    socket2.setSelected(type == JoystickType.JOYSTICK_TYPE_SINCLAIR_2);
    slot.setEnabled(device != null);
    socket1.setEnabled(device != null);
    socket2.setEnabled(device != null);
  }

  private void insert(File file) {
    if (device() == null) {
      return;
    }
    try {
      Cartridge cartridge = Cartridge.read(file);
      Interface2Peripheral into = device();
      machine().loop.later(() -> into.insert(cartridge));
      slot.show(cartridge.name());
    } catch (IOException notACartridge) {
      JOptionPane.showMessageDialog(this, notACartridge.getMessage());
    }
  }

  private void eject() {
    if (device() == null) {
      return;
    }
    Interface2Peripheral from = device();
    machine().loop.later(from::eject);
    slot.show(null);
  }

  private void plugJoystick(JoystickType where) {
    if (machine() != null) {
      Input.of(machine()).setup.keyboard.output = where;
    }
    socket1.setSelected(where == JoystickType.JOYSTICK_TYPE_SINCLAIR_1);
    socket2.setSelected(where == JoystickType.JOYSTICK_TYPE_SINCLAIR_2);
  }

  @Override
  protected String expandTip() {
    return "Show what the sockets read as, or just the slot";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the interface in";
  }
}
