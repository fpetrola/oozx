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

package com.fpetrola.oozx.speccy.modules.input;


import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.fpetrola.oozx.speccy.modules.keyboard.Keyboard;
import com.fpetrola.oozx.speccy.modules.keyboard.PcLayout;
import com.fpetrola.oozx.speccy.modules.keyboard.RecreatedLayout;
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import java.util.List;

/** The person at the machine: what they type and what they hold, as the machine's keys and joysticks. */
@Singleton
public class Input extends AbstractPeripheral {
  private final Joystick joystick;
  private final Keyboard keyboard;
  /** Handed out because what the person is doing reaches the machine through here, and windows set it. */
  public final Setup setup;

  @Inject
  public Input(Joystick joystick, Keyboard keyboard, Setup setup) {
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
    return (Input) speccy.peripheralRegistry.find(Input.class);
  }

  public Keyboard keyboard() {
    return keyboard;
  }

  public Joystick joystick() {
    return joystick;
  }

  // Enums and classes from Keyboard.java (repeated for completeness)
  public enum InputEventType {
    INPUT_EVENT_KEYPRESS,
    INPUT_EVENT_KEYRELEASE,
    INPUT_EVENT_JOYSTICK_PRESS,
    INPUT_EVENT_JOYSTICK_RELEASE
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

  public static class InputEventKey {
    public InputKey nativeKey;
    public InputKey spectrumKey;

    public InputEventKey(InputKey nativeKey, InputKey spectrumKey) {
      this.nativeKey = nativeKey;
      this.spectrumKey = spectrumKey;
    }
  }

  public class InputEventJoystick {
    public int which;
    public InputKey button;

    public InputEventJoystick(int which, InputKey button) {
      this.which = which;
      this.button = button;
    }
  }

  public static class InputEvent {
    public InputEventType type;
    /** The C union, still: an InputEventKey or an InputEventJoystick, told apart by the type. */
    public Object types;

    public InputEvent(InputEventType type, Object types) {
      this.type = type;
      this.types = types;
    }
  }

  // Main event handling method
  public int event(InputEvent event) {
    switch (event.type) {
      case INPUT_EVENT_KEYPRESS:
        return keypress((InputEventKey) event.types);
      case INPUT_EVENT_KEYRELEASE:
        return keyrelease((InputEventKey) event.types);
      case INPUT_EVENT_JOYSTICK_PRESS:
        return doJoystick((InputEventJoystick) event.types, true);
      case INPUT_EVENT_JOYSTICK_RELEASE:
        return doJoystick((InputEventJoystick) event.types, false);
      default:
        return 1;
    }
  }

  private boolean useShiftedArrowKeys(InputKey keysym) {
    return setup.arrowsShifted &&
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
    boolean recreated = keyboard.layout() instanceof RecreatedLayout;
    if (recreated != setup.recreatedSpectrum) {
      keyboard.layout(setup.recreatedSpectrum ? new RecreatedLayout() : new PcLayout());
    }
  }

  /**
   * A key set as a direction of the keyboard joystick moves it, and does not reach the machine's
   * own keyboard. A direction nobody set is null, and matches no key: a key left unset used to
   * be zero, and so was every key the layout has nothing for, so those were swallowed.
   *
   * @return whether the key was the joystick's and the keyboard should not see it
   */
  private boolean asJoystick(InputKey key, boolean press) {
    Joystick.JoystickButton button = null;
    if (key == setup.keyboard.up) button = Joystick.JoystickButton.JOYSTICK_BUTTON_UP;
    else if (key == setup.keyboard.down) button = Joystick.JoystickButton.JOYSTICK_BUTTON_DOWN;
    else if (key == setup.keyboard.left) button = Joystick.JoystickButton.JOYSTICK_BUTTON_LEFT;
    else if (key == setup.keyboard.right) button = Joystick.JoystickButton.JOYSTICK_BUTTON_RIGHT;
    else if (key == setup.keyboard.fire) button = Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE;
    return button != null && joystick.press(joystick.JOYSTICK_KEYBOARD, button, press);
  }

  private int keypress(InputEventKey event) {
//    // Joystick emulation via keyboard
    if (asJoystick(event.spectrumKey, true)) return 0;

    layoutFromConfig();
    keyboard.layout().pressed(event.spectrumKey);
    if (keyboard.layout().releasedByPressing(event.spectrumKey)) {
      sendKeyboardRelease(event.spectrumKey);
    } else {
      sendKeyboardPress(event.spectrumKey);
    }

    return 0;
  }

  private int keyrelease(InputEventKey event) {
    // A keyboard that lets go of a key by being pressed again has nothing to do here.
    if (!(keyboard.layout() instanceof RecreatedLayout)) {
      sendKeyboardRelease(event.spectrumKey);
    }

    asJoystick(event.spectrumKey, false);
    return 0;
  }

  /** What a fire button was set to hit, or null for one left as the joystick's own fire. */
  private SpectrumKey getFireButtonKey(int which, InputKey button) {
    if (which != 0 && which != 1) {
      throw new RuntimeException("Invalid joystick button");
    }
    int fire = button.getValue() - InputKey.INPUT_JOYSTICK_FIRE_1.getValue();
    SpectrumKey[] buttons = (which == 0 ? setup.joystick1 : setup.joystick2).fire;
    return fire < 0 || fire >= buttons.length ? null : buttons[fire];
  }

  private int doJoystick(InputEventJoystick joystickEvent, boolean press) {
    int which = joystickEvent.which;

    if (joystickEvent.button.getValue() < InputKey.INPUT_JOYSTICK_FIRE_1.getValue()) {
      Joystick.JoystickButton button;
      switch (joystickEvent.button) {
        case INPUT_JOYSTICK_UP:
          button = Joystick.JoystickButton.JOYSTICK_BUTTON_UP;
          break;
        case INPUT_JOYSTICK_DOWN:
          button = Joystick.JoystickButton.JOYSTICK_BUTTON_DOWN;
          break;
        case INPUT_JOYSTICK_LEFT:
          button = Joystick.JoystickButton.JOYSTICK_BUTTON_LEFT;
          break;
        case INPUT_JOYSTICK_RIGHT:
          button = Joystick.JoystickButton.JOYSTICK_BUTTON_RIGHT;
          break;
        default:
          throw new RuntimeException("Invalid joystick button");
      }
      joystick.press(which, button, press);
    } else {
      SpectrumKey key = getFireButtonKey(which, joystickEvent.button);
      if (key == null) {
        joystick.press(which, Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE, press);
      } else {
        if (press) {
          keyboard.press(key);
        } else {
          keyboard.release(key);
        }
      }
    }
    return 0;
  }

  /** Everything the person at the keyboard reaches the machine through. */
  @Singleton
  public static class Setup {
    @JsonMerge
    public Pad joystick1 = new Pad();
    @JsonMerge
    public Pad joystick2 = new Pad();
    @JsonMerge
    public Keys keyboard = new Keys();
    /** Whether the arrow keys are read shifted, which is what a Spectrum's own cursor keys are. */
    public boolean arrowsShifted;
    /** Whether a Kempston joystick interface is fitted. */
    public boolean kempstonJoystick;
    /** Whether a Kempston mouse is fitted. */
    public boolean kempstonMouse;
    /** A Recreated ZX Spectrum is a keyboard that sends its own codes rather than a keyboard's. */
    public boolean recreatedSpectrum;

    public Pad joystick1() { return joystick1; }
    public Pad joystick2() { return joystick2; }
    public Keys keyboard() { return keyboard; }
    public boolean arrowsShifted() { return arrowsShifted; }
    public boolean kempstonJoystick() { return kempstonJoystick; }
    public boolean kempstonMouse() { return kempstonMouse; }
    public boolean recreatedSpectrum() { return recreatedSpectrum; }
    public void setJoystick1(Pad chosen) { joystick1.take(chosen); }
    public void setJoystick2(Pad chosen) { joystick2.take(chosen); }
    public void setKeyboard(Keys chosen) { keyboard.take(chosen); }
    public void setArrowsShifted(boolean on) { arrowsShifted = on; }
    public void setKempstonJoystick(boolean on) { kempstonJoystick = on; }
    public void setKempstonMouse(boolean on) { kempstonMouse = on; }
    public void setRecreatedSpectrum(boolean on) { recreatedSpectrum = on; }

    /**
     * What a joystick is to the machine: which of the joysticks a Spectrum understands it answers
     * as. What moves it differs - a pad has buttons, the keyboard has keys - and that is what the
     * two shapes below add.
     */
    public static abstract class Stick {
      public Joystick.JoystickType output;

      /** What the file names, over what this had; a key it leaves out keeps the machine's own. */
      void take(Stick chosen) {
        if (chosen != null && chosen.output != null) output = chosen.output;
      }
    }

    /** A pad: fifteen fire buttons, each hitting a key of the machine's keyboard, or none. */
    public static class Pad extends Stick {
      public static final int FIRE_BUTTONS = 15;
      public SpectrumKey[] fire = new SpectrumKey[FIRE_BUTTONS];

      void take(Pad chosen) {
        super.take(chosen);
        if (chosen == null) return;
        for (int button = 0; button < chosen.fire.length && button < fire.length; button++)
          if (chosen.fire[button] != null) fire[button] = chosen.fire[button];
      }
    }

    /**
     * The keys of whoever is typing used as a joystick: four directions and a fire. Kept by the
     * name of the key rather than by its number, so a file someone reads says "INPUT_KEY_a".
     */
    public static class Keys extends Stick {
      public InputKey up;
      public InputKey down;
      public InputKey left;
      public InputKey right;
      public InputKey fire;

      void take(Keys chosen) {
        super.take(chosen);
        if (chosen == null) return;
        if (chosen.up != null) up = chosen.up;
        if (chosen.down != null) down = chosen.down;
        if (chosen.left != null) left = chosen.left;
        if (chosen.right != null) right = chosen.right;
        if (chosen.fire != null) fire = chosen.fire;
      }
    }
  }
}
