/*
 * Copyright (c) 2026 Fernando Petrola
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

package com.fpetrola.oozx.speccy;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.Joystick.JoystickButton;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import javax.swing.Timer;
import java.util.EnumSet;
import java.util.function.Supplier;

import static com.fpetrola.oozx.speccy.modules.Joystick.JoystickButton.*;

/**
 * A gamepad on the desktop, driving the joystick of whichever machine is in front the way the
 * keyboard follows it. Read through Jamepad, which brings SDL along for every desktop, so
 * nothing here knows which one it is running on. The d-pad or the left stick steer and any face
 * button fires; plugged in, it is the Kempston interface of the machine it drives.
 */
public class Gamepad {
  private static final int FIRST_PHYSICAL_JOYSTICK = 0;
  private final ControllerManager controllers = new ControllerManager();
  private final Supplier<Speccy> machineInFront;
  private EnumSet<JoystickButton> held = EnumSet.noneOf(JoystickButton.class);
  private ControllerState pad;
  private Speccy driving;

  public Gamepad(Supplier<Speccy> machineInFront) {
    this.machineInFront = machineInFront;
    controllers.initSDLGamepad();
    new Timer(10, e -> poll()).start();
  }

  /** The gamepad in use, by the name SDL gives it, or null while there is none. */
  public String controller() {
    return pad != null && pad.isConnected ? pad.controllerType : null;
  }

  private void poll() {
    pad = controllers.getState(0);
    Speccy machine = pad.isConnected ? machineInFront.get() : null;
    if (machine != driving) {
      move(driving, held, false);
      driving = machine;
      if (machine != null) {
        machine.settings.current.joyKempston = true;
        machine.peripherals.update();
      }
      move(machine, held, true);
    }
    EnumSet<JoystickButton> now = pressed(pad);
    for (JoystickButton button : JoystickButton.values()) {
      if (now.contains(button) != held.contains(button)) move(machine, EnumSet.of(button), now.contains(button));
    }
    held = now;
  }

  private static void move(Speccy machine, EnumSet<JoystickButton> buttons, boolean pressed) {
    if (machine != null) {
      for (JoystickButton button : buttons) machine.joystick.press(FIRST_PHYSICAL_JOYSTICK, button, pressed);
    }
  }

  private static EnumSet<JoystickButton> pressed(ControllerState pad) {
    EnumSet<JoystickButton> pressed = EnumSet.noneOf(JoystickButton.class);
    if (pad.dpadUp || pad.leftStickY < -0.5f) pressed.add(JOYSTICK_BUTTON_UP);
    if (pad.dpadDown || pad.leftStickY > 0.5f) pressed.add(JOYSTICK_BUTTON_DOWN);
    if (pad.dpadLeft || pad.leftStickX < -0.5f) pressed.add(JOYSTICK_BUTTON_LEFT);
    if (pad.dpadRight || pad.leftStickX > 0.5f) pressed.add(JOYSTICK_BUTTON_RIGHT);
    if (pad.a || pad.b || pad.x || pad.y) pressed.add(JOYSTICK_BUTTON_FIRE);
    return pressed;
  }
}
