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
package com.fpetrola.oozx.speccy.devices.interface2;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.devices.MediaSlot;
import com.fpetrola.oozx.speccy.modules.Joystick.JoystickType;
import com.fpetrola.oozx.speccy.peripherals.t.Widgets;

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
        : machine().settings.current.joystickKeyboardOutput;
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
      machine().z80.later(() -> into.insert(cartridge));
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
    machine().z80.later(from::eject);
    slot.show(null);
  }

  private void plugJoystick(JoystickType where) {
    if (machine() != null) {
      machine().settings.current.joystickKeyboardOutput = where;
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
