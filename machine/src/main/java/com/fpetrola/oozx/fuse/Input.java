/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.UIErrorLevel;
import com.fpetrola.oozx.Ui;
import com.fpetrola.oozx.fuse.modules.Joystick;
import com.fpetrola.oozx.fuse.modules.Keyboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Input {
  private Joystick joystick;
  private Keyboard keyboard;
  private Settings settings;

  public Input(Joystick joystick, Keyboard keyboard, Settings settings) {
    this.joystick = joystick;
    this.keyboard = keyboard;
    this.settings = settings;
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
    InputKey nativeKey;
    InputKey spectrumKey;

    InputEventKey(InputKey nativeKey, InputKey spectrumKey) {
      this.nativeKey = nativeKey;
      this.spectrumKey = spectrumKey;
    }
  }

  public class InputEventJoystick {
    int which;
    InputKey button;

    InputEventJoystick(int which, InputKey button) {
      this.which = which;
      this.button = button;
    }
  }

  public static class InputEvent {
    InputEventType type;
    Object types; // Union of InputEventKey or InputEventJoystick

    InputEvent(InputEventType type, Object types) {
      this.type = type;
      this.types = types;
    }
  }

  //  fields for recreated Spectrum key mapping
  private int recreatedKeyDown = 0;

  private final Map<Integer, InputKey> RECREATED_DOWNKEY_MAP = new HashMap<>();
  private final Map<Integer, InputKey> RECREATED_UPKEY_MAP = new HashMap<>();

  {
    // Populate RECREATED_DOWNKEY_MAP
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_a.getValue(), InputKey.INPUT_KEY_1);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_c.getValue(), InputKey.INPUT_KEY_2);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_e.getValue(), InputKey.INPUT_KEY_3);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_g.getValue(), InputKey.INPUT_KEY_4);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_i.getValue(), InputKey.INPUT_KEY_5);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_k.getValue(), InputKey.INPUT_KEY_6);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_m.getValue(), InputKey.INPUT_KEY_7);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_o.getValue(), InputKey.INPUT_KEY_8);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_q.getValue(), InputKey.INPUT_KEY_9);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_s.getValue(), InputKey.INPUT_KEY_0);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_u.getValue(), InputKey.INPUT_KEY_q);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_w.getValue(), InputKey.INPUT_KEY_w);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_y.getValue(), InputKey.INPUT_KEY_e);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_a.getValue(), InputKey.INPUT_KEY_r);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_c.getValue(), InputKey.INPUT_KEY_t);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_e.getValue(), InputKey.INPUT_KEY_y);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_g.getValue(), InputKey.INPUT_KEY_u);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_i.getValue(), InputKey.INPUT_KEY_i);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_k.getValue(), InputKey.INPUT_KEY_o);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_m.getValue(), InputKey.INPUT_KEY_p);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_o.getValue(), InputKey.INPUT_KEY_a);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_q.getValue(), InputKey.INPUT_KEY_s);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_s.getValue(), InputKey.INPUT_KEY_d);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_u.getValue(), InputKey.INPUT_KEY_f);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_w.getValue(), InputKey.INPUT_KEY_g);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_y.getValue(), InputKey.INPUT_KEY_h);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_0.getValue(), InputKey.INPUT_KEY_j);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_2.getValue(), InputKey.INPUT_KEY_k);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_4.getValue(), InputKey.INPUT_KEY_l);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_6.getValue(), InputKey.INPUT_KEY_Return);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_8.getValue(), InputKey.INPUT_KEY_Shift_L);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_comma.getValue(), InputKey.INPUT_KEY_z);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_minus.getValue(), InputKey.INPUT_KEY_x);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_bracketleft.getValue(), InputKey.INPUT_KEY_c);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_semicolon.getValue(), InputKey.INPUT_KEY_v);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_comma.getValue(), InputKey.INPUT_KEY_b);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_slash.getValue(), InputKey.INPUT_KEY_n);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_bracketleft.getValue(), InputKey.INPUT_KEY_m);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_1.getValue(), InputKey.INPUT_KEY_Control_R);
    RECREATED_DOWNKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_5.getValue(), InputKey.INPUT_KEY_space);

    // Populate RECREATED_UPKEY_MAP
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_b.getValue(), InputKey.INPUT_KEY_1);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_d.getValue(), InputKey.INPUT_KEY_2);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_f.getValue(), InputKey.INPUT_KEY_3);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_h.getValue(), InputKey.INPUT_KEY_4);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_j.getValue(), InputKey.INPUT_KEY_5);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_l.getValue(), InputKey.INPUT_KEY_6);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_n.getValue(), InputKey.INPUT_KEY_7);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_p.getValue(), InputKey.INPUT_KEY_8);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_r.getValue(), InputKey.INPUT_KEY_9);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_t.getValue(), InputKey.INPUT_KEY_0);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_v.getValue(), InputKey.INPUT_KEY_q);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_x.getValue(), InputKey.INPUT_KEY_w);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_z.getValue(), InputKey.INPUT_KEY_e);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_b.getValue(), InputKey.INPUT_KEY_r);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_d.getValue(), InputKey.INPUT_KEY_t);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_f.getValue(), InputKey.INPUT_KEY_y);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_h.getValue(), InputKey.INPUT_KEY_u);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_j.getValue(), InputKey.INPUT_KEY_i);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_l.getValue(), InputKey.INPUT_KEY_o);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_n.getValue(), InputKey.INPUT_KEY_p);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_p.getValue(), InputKey.INPUT_KEY_a);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_r.getValue(), InputKey.INPUT_KEY_s);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_t.getValue(), InputKey.INPUT_KEY_d);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_v.getValue(), InputKey.INPUT_KEY_f);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_x.getValue(), InputKey.INPUT_KEY_g);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_z.getValue(), InputKey.INPUT_KEY_h);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_1.getValue(), InputKey.INPUT_KEY_j);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_3.getValue(), InputKey.INPUT_KEY_k);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_5.getValue(), InputKey.INPUT_KEY_l);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_7.getValue(), InputKey.INPUT_KEY_Return);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_9.getValue(), InputKey.INPUT_KEY_Shift_L);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_period.getValue(), InputKey.INPUT_KEY_z);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_equal.getValue(), InputKey.INPUT_KEY_x);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_bracketright.getValue(), InputKey.INPUT_KEY_c);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_semicolon.getValue(), InputKey.INPUT_KEY_v);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_period.getValue(), InputKey.INPUT_KEY_b);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_slash.getValue(), InputKey.INPUT_KEY_n);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_bracketright.getValue(), InputKey.INPUT_KEY_m);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_4.getValue(), InputKey.INPUT_KEY_Control_R);
    RECREATED_UPKEY_MAP.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_6.getValue(), InputKey.INPUT_KEY_space);
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
        Ui.error(UIErrorLevel.UI_ERROR_ERROR, "Unknown input event type %d", event.type.ordinal());
        return 1;
    }
  }

  private boolean useShiftedArrowKeys(InputKey keysym) {
    return settings.current.keyboardArrowsShifted &&
        (keysym == InputKey.INPUT_KEY_Up || keysym == InputKey.INPUT_KEY_Down ||
            keysym == InputKey.INPUT_KEY_Left || keysym == InputKey.INPUT_KEY_Right);
  }

  private void sendKeyboardPress(InputKey keysym) {
    SpectrumKeys ptr = keyboard.getSpectrumKeys(keysym);
    if (ptr != null) {
      keyboard.press(ptr.key1);
      keyboard.press(ptr.key2);
    }
    if (useShiftedArrowKeys(keysym)) {
      keyboard.press(KeyboardKeyName.KEYBOARD_Caps);
    }
  }

  private void sendKeyboardRelease(InputKey keysym) {
    SpectrumKeys ptr = keyboard.getSpectrumKeys(keysym);
    if (ptr != null) {
      keyboard.release(ptr.key1);
      keyboard.release(ptr.key2);
    }
    if (useShiftedArrowKeys(keysym)) {
      keyboard.release(KeyboardKeyName.KEYBOARD_Caps);
    }
  }

  private void recreatedKeypress(InputKey k) {
    if (k == InputKey.INPUT_KEY_Shift_L) {
      recreatedKeyDown |= InputKey.INPUT_KEY_Shift_L.getValue();
    }

    if (k.getValue() >= 0 && k.getValue() < 256) {
      recreatedKeyDown = (recreatedKeyDown & ~255) | k.getValue();
    }

    InputKey o = RECREATED_UPKEY_MAP.getOrDefault(recreatedKeyDown, InputKey.INPUT_KEY_NONE);
    if (o != InputKey.INPUT_KEY_NONE) {
      sendKeyboardRelease(o);
      recreatedKeyDown = 0;
      return;
    }

    o = RECREATED_DOWNKEY_MAP.getOrDefault(recreatedKeyDown, InputKey.INPUT_KEY_NONE);
    if (o != InputKey.INPUT_KEY_NONE) {
      sendKeyboardPress(o);
      recreatedKeyDown = 0;
    }
  }

  private int keypress(InputEventKey event) {
//    if (Ui.widgetLevel >= 0) {
//      Ui.widgetKeyhandler(event.nativeKey.getValue());
//      return 0;
//    }

    // Handle Escape to release mouse grab
    if (event.nativeKey == InputKey.INPUT_KEY_Escape && Ui.mouseGrabbed) {
      Ui.mouseGrabbed = Ui.mouseRelease(false);
      if (Ui.mouseGrabbed) return 0;
    }

//    // Joystick emulation via keyboard
    boolean swallow = false;
    int ordinal = event.spectrumKey.getValue();
    if (ordinal == settings.current.joystickKeyboardUp) {
      swallow = joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_UP, true);
    } else if (ordinal == settings.current.joystickKeyboardDown) {
      swallow = joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_DOWN, true);
    } else if (ordinal == settings.current.joystickKeyboardLeft) {
      swallow = joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_LEFT, true);
    } else if (ordinal == settings.current.joystickKeyboardRight) {
      swallow = joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_RIGHT, true);
    } else if (ordinal == settings.current.joystickKeyboardFire) {
      swallow = joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE, true);
    }

    if (swallow) return 0;

    // Handle keyboard press
    if (settings.current.recreatedSpectrum) {
      recreatedKeypress(event.spectrumKey);
    } else {
      sendKeyboardPress(event.spectrumKey);
    }

    Ui.popupMenu(event.nativeKey.getValue());
    return 0;
  }

  private int keyrelease(InputEventKey event) {
    if (!settings.current.recreatedSpectrum) {
      sendKeyboardRelease(event.spectrumKey);
    }

    // Joystick emulation via keyboard
    int value = event.spectrumKey.getValue();

    if (value == settings.current.joystickKeyboardUp) {
      joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_UP, false);
    } else if (value == settings.current.joystickKeyboardDown) {
      joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_DOWN, false);
    } else if (value == settings.current.joystickKeyboardLeft) {
      joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_LEFT, false);
    } else if (value == settings.current.joystickKeyboardRight) {
      joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_RIGHT, false);
    } else if (value == settings.current.joystickKeyboardFire) {
      joystick.press(joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE, false);
    }

    return 0;
  }

  private KeyboardKeyName getFireButtonKey(int which, InputKey button) {
    int key = KeyboardKeyName.KEYBOARD_NONE.ordinal();
    switch (which) {
      case 0:
        switch (button) {
          case INPUT_JOYSTICK_FIRE_1:
            key = settings.current.joystick1Fire1;
            break;
          case INPUT_JOYSTICK_FIRE_2:
            key = settings.current.joystick1Fire2;
            break;
          case INPUT_JOYSTICK_FIRE_3:
            key = settings.current.joystick1Fire3;
            break;
          case INPUT_JOYSTICK_FIRE_4:
            key = settings.current.joystick1Fire4;
            break;
          case INPUT_JOYSTICK_FIRE_5:
            key = settings.current.joystick1Fire5;
            break;
          case INPUT_JOYSTICK_FIRE_6:
            key = settings.current.joystick1Fire6;
            break;
          case INPUT_JOYSTICK_FIRE_7:
            key = settings.current.joystick1Fire7;
            break;
          case INPUT_JOYSTICK_FIRE_8:
            key = settings.current.joystick1Fire8;
            break;
          case INPUT_JOYSTICK_FIRE_9:
            key = settings.current.joystick1Fire9;
            break;
          case INPUT_JOYSTICK_FIRE_10:
            key = settings.current.joystick1Fire10;
            break;
          case INPUT_JOYSTICK_FIRE_11:
            key = settings.current.joystick1Fire11;
            break;
          case INPUT_JOYSTICK_FIRE_12:
            key = settings.current.joystick1Fire12;
            break;
          case INPUT_JOYSTICK_FIRE_13:
            key = settings.current.joystick1Fire13;
            break;
          case INPUT_JOYSTICK_FIRE_14:
            key = settings.current.joystick1Fire14;
            break;
          case INPUT_JOYSTICK_FIRE_15:
            key = settings.current.joystick1Fire15;
            break;
          default:
            break;
        }
        break;
      case 1:
        switch (button) {
          case INPUT_JOYSTICK_FIRE_1:
            key = settings.current.joystick2Fire1;
            break;
          case INPUT_JOYSTICK_FIRE_2:
            key = settings.current.joystick2Fire2;
            break;
          case INPUT_JOYSTICK_FIRE_3:
            key = settings.current.joystick2Fire3;
            break;
          case INPUT_JOYSTICK_FIRE_4:
            key = settings.current.joystick2Fire4;
            break;
          case INPUT_JOYSTICK_FIRE_5:
            key = settings.current.joystick2Fire5;
            break;
          case INPUT_JOYSTICK_FIRE_6:
            key = settings.current.joystick2Fire6;
            break;
          case INPUT_JOYSTICK_FIRE_7:
            key = settings.current.joystick2Fire7;
            break;
          case INPUT_JOYSTICK_FIRE_8:
            key = settings.current.joystick2Fire8;
            break;
          case INPUT_JOYSTICK_FIRE_9:
            key = settings.current.joystick2Fire9;
            break;
          case INPUT_JOYSTICK_FIRE_10:
            key = settings.current.joystick2Fire10;
            break;
          case INPUT_JOYSTICK_FIRE_11:
            key = settings.current.joystick2Fire11;
            break;
          case INPUT_JOYSTICK_FIRE_12:
            key = settings.current.joystick2Fire12;
            break;
          case INPUT_JOYSTICK_FIRE_13:
            key = settings.current.joystick2Fire13;
            break;
          case INPUT_JOYSTICK_FIRE_14:
            key = settings.current.joystick2Fire14;
            break;
          case INPUT_JOYSTICK_FIRE_15:
            key = settings.current.joystick2Fire15;
            break;
          default:
            break;
        }
        break;
      default:
        Ui.error(UIErrorLevel.UI_ERROR_ERROR, "getFireButtonKey: which = %d, button = %d", which, button.getValue() + "");
        throw new RuntimeException("Invalid joystick button");
    }
    int finalKey = key;
    return Arrays.stream(KeyboardKeyName.values()).filter(k -> k.getValue() == finalKey).findFirst().get();
  }

  private int doJoystick(InputEventJoystick joystickEvent, boolean press) {
    if (Ui.widgetLevel >= 0) {
      if (press) Ui.widgetKeyhandler(joystickEvent.button.getValue());
      return 0;
    }

    // Handle joystick fire button as F1 for popup menu (excluding Wii-specific GEKKO)
    if (joystickEvent.button == InputKey.INPUT_JOYSTICK_FIRE_2 && press) {
      Ui.popupMenu(InputKey.INPUT_KEY_F1.getValue());
    }

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
          Ui.error(UIErrorLevel.UI_ERROR_ERROR, "doJoystick: unknown button %d", joystickEvent.button.getValue());
          throw new RuntimeException("Invalid joystick button");
      }
      joystick.press(which, button, press);
    } else {
      KeyboardKeyName key = getFireButtonKey(which, joystickEvent.button);
      if (key == KeyboardKeyName.KEYBOARD_JOYSTICK_FIRE) {
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
}
