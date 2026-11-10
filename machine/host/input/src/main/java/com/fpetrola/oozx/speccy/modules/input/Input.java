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

package com.fpetrola.oozx.speccy.modules.input;


import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.fpetrola.oozx.speccy.modules.keyboard.Keyboard;
import com.fpetrola.oozx.speccy.modules.keyboard.PcLayout;
import com.fpetrola.oozx.speccy.modules.keyboard.RecreatedLayout;
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import java.util.List;
import java.util.Optional;

/** The person at the machine: what they type and what they hold, as the machine's keys and joysticks. */
@Singleton
public class Input extends AbstractPeripheral {
  private final Joystick joystick;
  private final Keyboard keyboard;
  private final InputSetup setup;
  /** Which setting the keyboard's layout was made from, so a change of setting changes the layout. */
  private boolean recreatedLayout;

  @Inject
  public Input(Joystick joystick, Keyboard keyboard, InputSetup setup) {
    super(List.of());
    this.joystick = joystick;
    this.keyboard = keyboard;
    this.setup = setup;
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return true;
  }

  /** The one at that emulator, which a window or a device reaches the way it reaches any device. */
  public static Input of(Speccy speccy) {
    return speccy.peripheralRegistry.anyThatIs(Input.class);
  }

  public Keyboard keyboard() {
    return keyboard;
  }

  public Joystick joystick() {
    return joystick;
  }

  /** Handed out because what the person is doing reaches the machine through here, and windows set it. */
  public InputSetup setup() {
    return setup;
  }

  public enum InputKey {
    INPUT_KEY_NONE(0x00),
    INPUT_KEY_Tab(0x09),
    INPUT_KEY_Return(0x0d),
    INPUT_KEY_Escape(0x1b),
    INPUT_KEY_space(0x20),
    INPUT_KEY_exclam(0x21),
    INPUT_KEY_quotedbl(0x22),
    INPUT_KEY_numbersign(0x23),
    INPUT_KEY_dollar(0x24),
    INPUT_KEY_percent(0x25),
    INPUT_KEY_ampersand(0x26),
    INPUT_KEY_apostrophe(0x27),
    INPUT_KEY_parenleft(0x28),
    INPUT_KEY_parenright(0x29),
    INPUT_KEY_asterisk(0x2a),
    INPUT_KEY_plus(0x2b),
    INPUT_KEY_comma(0x2c),
    INPUT_KEY_minus(0x2d),
    INPUT_KEY_period(0x2e),
    INPUT_KEY_slash(0x2f),
    INPUT_KEY_0(0x30),
    INPUT_KEY_1(0x31),
    INPUT_KEY_2(0x32),
    INPUT_KEY_3(0x33),
    INPUT_KEY_4(0x34),
    INPUT_KEY_5(0x35),
    INPUT_KEY_6(0x36),
    INPUT_KEY_7(0x37),
    INPUT_KEY_8(0x38),
    INPUT_KEY_9(0x39),
    INPUT_KEY_colon(0x3a),
    INPUT_KEY_semicolon(0x3b),
    INPUT_KEY_less(0x3c),
    INPUT_KEY_equal(0x3d),
    INPUT_KEY_greater(0x3e),
    INPUT_KEY_question(0x3f),
    INPUT_KEY_at(0x40),
    INPUT_KEY_A(0x41),
    INPUT_KEY_B(0x42),
    INPUT_KEY_C(0x43),
    INPUT_KEY_D(0x44),
    INPUT_KEY_E(0x45),
    INPUT_KEY_F(0x46),
    INPUT_KEY_G(0x47),
    INPUT_KEY_H(0x48),
    INPUT_KEY_I(0x49),
    INPUT_KEY_J(0x4a),
    INPUT_KEY_K(0x4b),
    INPUT_KEY_L(0x4c),
    INPUT_KEY_M(0x4d),
    INPUT_KEY_N(0x4e),
    INPUT_KEY_O(0x4f),
    INPUT_KEY_P(0x50),
    INPUT_KEY_Q(0x51),
    INPUT_KEY_R(0x52),
    INPUT_KEY_S(0x53),
    INPUT_KEY_T(0x54),
    INPUT_KEY_U(0x55),
    INPUT_KEY_V(0x56),
    INPUT_KEY_W(0x57),
    INPUT_KEY_X(0x58),
    INPUT_KEY_Y(0x59),
    INPUT_KEY_Z(0x5a),
    INPUT_KEY_bracketleft(0x5b),
    INPUT_KEY_backslash(0x5c),
    INPUT_KEY_bracketright(0x5d),
    INPUT_KEY_asciicircum(0x5e),
    INPUT_KEY_dead_circumflex(0x5e),
    INPUT_KEY_underscore(0x5f),
    INPUT_KEY_a(0x61),
    INPUT_KEY_b(0x62),
    INPUT_KEY_c(0x63),
    INPUT_KEY_d(0x64),
    INPUT_KEY_e(0x65),
    INPUT_KEY_f(0x66),
    INPUT_KEY_g(0x67),
    INPUT_KEY_h(0x68),
    INPUT_KEY_i(0x69),
    INPUT_KEY_j(0x6a),
    INPUT_KEY_k(0x6b),
    INPUT_KEY_l(0x6c),
    INPUT_KEY_m(0x6d),
    INPUT_KEY_n(0x6e),
    INPUT_KEY_o(0x6f),
    INPUT_KEY_p(0x70),
    INPUT_KEY_q(0x71),
    INPUT_KEY_r(0x72),
    INPUT_KEY_s(0x73),
    INPUT_KEY_t(0x74),
    INPUT_KEY_u(0x75),
    INPUT_KEY_v(0x76),
    INPUT_KEY_w(0x77),
    INPUT_KEY_x(0x78),
    INPUT_KEY_y(0x79),
    INPUT_KEY_z(0x7a),
    INPUT_KEY_braceleft(0x7b),
    INPUT_KEY_bar(0x7c),
    INPUT_KEY_braceright(0x7d),
    INPUT_KEY_asciitilde(0x7e),
    INPUT_KEY_BackSpace(0x7f),
    INPUT_KEY_KP_Enter(0x8d),
    INPUT_KEY_Up(0x100),
    INPUT_KEY_Down(0x101),
    INPUT_KEY_Left(0x102),
    INPUT_KEY_Right(0x103),
    INPUT_KEY_Insert(0x104),
    INPUT_KEY_Delete(0x105),
    INPUT_KEY_Home(0x106),
    INPUT_KEY_End(0x107),
    INPUT_KEY_Page_Up(0x108),
    INPUT_KEY_Page_Down(0x109),
    INPUT_KEY_Caps_Lock(0x10a),
    INPUT_KEY_F1(0x10b),
    INPUT_KEY_F2(0x10c),
    INPUT_KEY_F3(0x10d),
    INPUT_KEY_F4(0x10e),
    INPUT_KEY_F5(0x10f),
    INPUT_KEY_F6(0x110),
    INPUT_KEY_F7(0x111),
    INPUT_KEY_F8(0x112),
    INPUT_KEY_F9(0x113),
    INPUT_KEY_F10(0x114),
    INPUT_KEY_F11(0x115),
    INPUT_KEY_F12(0x116),
    INPUT_KEY_Shift_L(0x1000),
    INPUT_KEY_Shift_R(0x1001),
    INPUT_KEY_Control_L(0x1002),
    INPUT_KEY_Control_R(0x1003),
    INPUT_KEY_Alt_L(0x1004),
    INPUT_KEY_Alt_R(0x1005),
    INPUT_KEY_Meta_L(0x1006),
    INPUT_KEY_Meta_R(0x1007),
    INPUT_KEY_Super_L(0x1008),
    INPUT_KEY_Super_R(0x1009),
    INPUT_KEY_Hyper_L(0x100a),
    INPUT_KEY_Hyper_R(0x100b),
    INPUT_KEY_Mode_switch(0x100c),
    INPUT_JOYSTICK_UP(0x1100),
    INPUT_JOYSTICK_DOWN(0x1101),
    INPUT_JOYSTICK_LEFT(0x1102),
    INPUT_JOYSTICK_RIGHT(0x1103),
    INPUT_JOYSTICK_FIRE_1(0x1104),
    INPUT_JOYSTICK_FIRE_2(0x1105),
    INPUT_JOYSTICK_FIRE_3(0x1106),
    INPUT_JOYSTICK_FIRE_4(0x1107),
    INPUT_JOYSTICK_FIRE_5(0x1108),
    INPUT_JOYSTICK_FIRE_6(0x1109),
    INPUT_JOYSTICK_FIRE_7(0x110a),
    INPUT_JOYSTICK_FIRE_8(0x110b),
    INPUT_JOYSTICK_FIRE_9(0x110c),
    INPUT_JOYSTICK_FIRE_10(0x110d),
    INPUT_JOYSTICK_FIRE_11(0x110e),
    INPUT_JOYSTICK_FIRE_12(0x110f),
    INPUT_JOYSTICK_FIRE_13(0x1110),
    INPUT_JOYSTICK_FIRE_14(0x1111),
    INPUT_JOYSTICK_FIRE_15(0x1112);

    private final int value;

    InputKey(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  /** A key of the host going down. */
  public void press(InputKey key) {
    if (asJoystick(key, true)) {
      return;
    }
    layoutFromConfig();
    keyboard.layout().pressed(key);
    if (keyboard.layout().releasedByPressing(key)) {
      sendKeyboardRelease(key);
    } else {
      sendKeyboardPress(key);
    }
  }

  /** A key of the host coming up. A keyboard that lets go of a key by pressing it again has nothing to do here. */
  public void release(InputKey key) {
    if (keyboard.layout().releasedByLettingGo()) {
      sendKeyboardRelease(key);
    }
    setup.keyboard().pushes(key, true)
        .filter(button -> joystick.takes(joystick.JOYSTICK_KEYBOARD))
        .ifPresent(button -> joystick.press(joystick.JOYSTICK_KEYBOARD, button, false));
  }

  private boolean useShiftedArrowKeys(InputKey keysym) {
    return setup.arrowsShifted() &&
        (keysym == InputKey.INPUT_KEY_Up || keysym == InputKey.INPUT_KEY_Down ||
            keysym == InputKey.INPUT_KEY_Left || keysym == InputKey.INPUT_KEY_Right);
  }

  private void sendKeyboardPress(InputKey keysym) {
    keyboard.produces(keysym).pressOn(keyboard.matrix());
    if (useShiftedArrowKeys(keysym)) {
      keyboard.press(SpectrumKey.CAPS_SHIFT);
    }
  }

  private void sendKeyboardRelease(InputKey keysym) {
    keyboard.produces(keysym).releaseOn(keyboard.matrix());
    if (useShiftedArrowKeys(keysym)) {
      keyboard.release(SpectrumKey.CAPS_SHIFT);
    }
  }

  /** The keyboard being typed on, as the setting has it: a PC's, or a Recreated ZX. */
  private void layoutFromConfig() {
    if (recreatedLayout != setup.recreatedSpectrum()) {
      recreatedLayout = setup.recreatedSpectrum();
      keyboard.layout(recreatedLayout ? new RecreatedLayout() : new PcLayout());
    }
  }

  /** @return whether the key was the joystick's and the machine's own keyboard should not see it */
  private boolean asJoystick(InputKey key, boolean press) {
    Optional<Joystick.JoystickButton> button = setup.keyboard().pushes(key, !press || joystick.inUse() != null)
        .filter(pushed -> joystick.takes(joystick.JOYSTICK_KEYBOARD));
    button.ifPresent(pushed -> joystick.press(joystick.JOYSTICK_KEYBOARD, pushed, press));
    return button.isPresent();
  }
}
