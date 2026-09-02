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
package com.fpetrola.oozx.speccy.devices.fuller;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.modules.Joystick.JoystickType;
import com.fpetrola.oozx.speccy.peripherals.t.Widgets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.GridLayout;

/**
 * The Fuller Box on the desk: a light for the sound chip being written to, and the joystick
 * socket, where the keyboard's joystick can be plugged in.
 */
public class FullerFrame extends DeviceFrame<FullerPeripheral> {

  private static final int REFRESH_MILLIS = 80;

  private final JLabel chip = new JLabel("●");
  private final JToggleButton socket = Widgets.iconToggle("1F579.svg", "Joystick",
      "The keyboard's joystick, plugged into the Fuller's socket and read on port 0x7f");
  private final JLabel written = new JLabel();
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
  private long seen;

  public FullerFrame() {
    super("Fuller Box", FullerPeripheral.class);
    setSize(420, 160);
    chip.setToolTipText("Lit while a program is writing to the sound chip");
    socket.addActionListener(e -> {
      if (machine() != null) {
        machine().settings.current.joystickKeyboardOutput =
            socket.isSelected() ? JoystickType.JOYSTICK_TYPE_FULLER : JoystickType.JOYSTICK_TYPE_NONE;
      }
    });
    controls.add(chip);
    controls.add(socket);
    JPanel inside = new JPanel(new GridLayout(0, 1));
    inside.add(written);
    inside.add(new JLabel("<html>An AY-3-8912 on ports 0x3f and 0x5f, for music written for it on a 48K, "
        + "and a joystick read on 0x7f.</html>"));
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

  @Override
  protected void plugged(FullerPeripheral device) {
    socket.setEnabled(device != null);
    socket.setSelected(device != null
        && machine().settings.current.joystickKeyboardOutput == JoystickType.JOYSTICK_TYPE_FULLER);
    refresh();
  }

  private void refresh() {
    FullerPeripheral box = device();
    if (box == null) {
      chip.setForeground(Color.GRAY);
      written.setText("not plugged into a machine");
      return;
    }
    long now = box.writes();
    chip.setForeground(now != seen ? new Color(0x30c030) : Color.GRAY);
    seen = now;
    written.setText("register writes: " + now);
  }

  @Override
  protected String expandTip() {
    return "Show what the box is, or just its lights";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the box in";
  }
}
