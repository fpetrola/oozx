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

package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.modules.keyboard.Keyboard;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

public class SwingKeyboard implements KeyListener {
    /** A code a keyboard sends, and the key of the host it is. */
    private record HostKey(int code, Input.InputKey key) {
      int host() {
        return code;
      }
    }


    private static Map<Integer, Input.InputKey> unicodeKeysymsHash = new HashMap<>();
    /** What each key code of this toolkit is: written here, because these are Swing's numbers. */
    private final Map<Integer, Input.InputKey> keysyms = new HashMap<>();
    private static final HostKey[] UNICODE_KEYSYMS_MAP = {
        new HostKey(' ', Input.InputKey.INPUT_KEY_space),
        new HostKey('!', Input.InputKey.INPUT_KEY_exclam),
        new HostKey('"', Input.InputKey.INPUT_KEY_quotedbl),
        new HostKey('#', Input.InputKey.INPUT_KEY_numbersign),
        new HostKey('$', Input.InputKey.INPUT_KEY_dollar),
        new HostKey('%', Input.InputKey.INPUT_KEY_percent),
        new HostKey('&', Input.InputKey.INPUT_KEY_ampersand),
        new HostKey('\'', Input.InputKey.INPUT_KEY_apostrophe),
        new HostKey('(', Input.InputKey.INPUT_KEY_parenleft),
        new HostKey(')', Input.InputKey.INPUT_KEY_parenright),
        new HostKey('*', Input.InputKey.INPUT_KEY_asterisk),
        new HostKey('+', Input.InputKey.INPUT_KEY_plus),
        new HostKey(',', Input.InputKey.INPUT_KEY_comma),
        new HostKey('-', Input.InputKey.INPUT_KEY_minus),
        new HostKey('.', Input.InputKey.INPUT_KEY_period),
        new HostKey('/', Input.InputKey.INPUT_KEY_slash),
        new HostKey('0', Input.InputKey.INPUT_KEY_0),
        new HostKey('1', Input.InputKey.INPUT_KEY_1),
        new HostKey('2', Input.InputKey.INPUT_KEY_2),
        new HostKey('3', Input.InputKey.INPUT_KEY_3),
        new HostKey('4', Input.InputKey.INPUT_KEY_4),
        new HostKey('5', Input.InputKey.INPUT_KEY_5),
        new HostKey('6', Input.InputKey.INPUT_KEY_6),
        new HostKey('7', Input.InputKey.INPUT_KEY_7),
        new HostKey('8', Input.InputKey.INPUT_KEY_8),
        new HostKey('9', Input.InputKey.INPUT_KEY_9),
        new HostKey(':', Input.InputKey.INPUT_KEY_colon),
        new HostKey(';', Input.InputKey.INPUT_KEY_semicolon),
        new HostKey('<', Input.InputKey.INPUT_KEY_less),
        new HostKey('=', Input.InputKey.INPUT_KEY_equal),
        new HostKey('>', Input.InputKey.INPUT_KEY_greater),
        new HostKey('?', Input.InputKey.INPUT_KEY_question),
        new HostKey('@', Input.InputKey.INPUT_KEY_at),
        new HostKey('A', Input.InputKey.INPUT_KEY_A),
        new HostKey('B', Input.InputKey.INPUT_KEY_B),
        new HostKey('C', Input.InputKey.INPUT_KEY_C),
        new HostKey('D', Input.InputKey.INPUT_KEY_D),
        new HostKey('E', Input.InputKey.INPUT_KEY_E),
        new HostKey('F', Input.InputKey.INPUT_KEY_F),
        new HostKey('G', Input.InputKey.INPUT_KEY_G),
        new HostKey('H', Input.InputKey.INPUT_KEY_H),
        new HostKey('I', Input.InputKey.INPUT_KEY_I),
        new HostKey('J', Input.InputKey.INPUT_KEY_J),
        new HostKey('K', Input.InputKey.INPUT_KEY_K),
        new HostKey('L', Input.InputKey.INPUT_KEY_L),
        new HostKey('M', Input.InputKey.INPUT_KEY_M),
        new HostKey('N', Input.InputKey.INPUT_KEY_N),
        new HostKey('O', Input.InputKey.INPUT_KEY_O),
        new HostKey('P', Input.InputKey.INPUT_KEY_P),
        new HostKey('Q', Input.InputKey.INPUT_KEY_Q),
        new HostKey('R', Input.InputKey.INPUT_KEY_R),
        new HostKey('S', Input.InputKey.INPUT_KEY_S),
        new HostKey('T', Input.InputKey.INPUT_KEY_T),
        new HostKey('U', Input.InputKey.INPUT_KEY_U),
        new HostKey('V', Input.InputKey.INPUT_KEY_V),
        new HostKey('W', Input.InputKey.INPUT_KEY_W),
        new HostKey('X', Input.InputKey.INPUT_KEY_X),
        new HostKey('Y', Input.InputKey.INPUT_KEY_Y),
        new HostKey('Z', Input.InputKey.INPUT_KEY_Z),
        new HostKey('a', Input.InputKey.INPUT_KEY_a),
        new HostKey('b', Input.InputKey.INPUT_KEY_b),
        new HostKey('c', Input.InputKey.INPUT_KEY_c),
        new HostKey('d', Input.InputKey.INPUT_KEY_d),
        new HostKey('e', Input.InputKey.INPUT_KEY_e),
        new HostKey('f', Input.InputKey.INPUT_KEY_f),
        new HostKey('g', Input.InputKey.INPUT_KEY_g),
        new HostKey('h', Input.InputKey.INPUT_KEY_h),
        new HostKey('i', Input.InputKey.INPUT_KEY_i),
        new HostKey('j', Input.InputKey.INPUT_KEY_j),
        new HostKey('k', Input.InputKey.INPUT_KEY_k),
        new HostKey('l', Input.InputKey.INPUT_KEY_l),
        new HostKey('m', Input.InputKey.INPUT_KEY_m),
        new HostKey('n', Input.InputKey.INPUT_KEY_n),
        new HostKey('o', Input.InputKey.INPUT_KEY_o),
        new HostKey('p', Input.InputKey.INPUT_KEY_p),
        new HostKey('q', Input.InputKey.INPUT_KEY_q),
        new HostKey('r', Input.InputKey.INPUT_KEY_r),
        new HostKey('s', Input.InputKey.INPUT_KEY_s),
        new HostKey('t', Input.InputKey.INPUT_KEY_t),
        new HostKey('u', Input.InputKey.INPUT_KEY_u),
        new HostKey('v', Input.InputKey.INPUT_KEY_v),
        new HostKey('w', Input.InputKey.INPUT_KEY_w),
        new HostKey('x', Input.InputKey.INPUT_KEY_x),
        new HostKey('y', Input.InputKey.INPUT_KEY_y),
        new HostKey('z', Input.InputKey.INPUT_KEY_z),
        new HostKey('[', Input.InputKey.INPUT_KEY_bracketleft),
        new HostKey('\\', Input.InputKey.INPUT_KEY_backslash),
        new HostKey(']', Input.InputKey.INPUT_KEY_bracketright),
        new HostKey('^', Input.InputKey.INPUT_KEY_asciicircum),
        new HostKey('_', Input.InputKey.INPUT_KEY_underscore),
        new HostKey('{', Input.InputKey.INPUT_KEY_braceleft),
        new HostKey('|', Input.InputKey.INPUT_KEY_bar),
        new HostKey('}', Input.InputKey.INPUT_KEY_braceright),
        new HostKey('~', Input.InputKey.INPUT_KEY_asciitilde),
        new HostKey(0, Input.InputKey.INPUT_KEY_NONE) // End marker
    };

    private static final HostKey[] KEYSYMS_MAP = {
        new HostKey(KeyEvent.VK_TAB, Input.InputKey.INPUT_KEY_Tab),
        new HostKey(KeyEvent.VK_ENTER, Input.InputKey.INPUT_KEY_Return),
        new HostKey(KeyEvent.VK_ESCAPE, Input.InputKey.INPUT_KEY_Escape),
        new HostKey(KeyEvent.VK_SPACE, Input.InputKey.INPUT_KEY_space),
        new HostKey(KeyEvent.VK_EXCLAMATION_MARK, Input.InputKey.INPUT_KEY_exclam),
        new HostKey(KeyEvent.VK_QUOTE, Input.InputKey.INPUT_KEY_quotedbl),
        new HostKey(KeyEvent.VK_NUMBER_SIGN, Input.InputKey.INPUT_KEY_numbersign),
        new HostKey(KeyEvent.VK_DOLLAR, Input.InputKey.INPUT_KEY_dollar),
//        new HostKey(KeyEvent.VK_PERCENT, Input.InputKey.INPUT_KEY_percent),
        new HostKey(KeyEvent.VK_AMPERSAND, Input.InputKey.INPUT_KEY_ampersand),
        new HostKey(KeyEvent.VK_QUOTE, Input.InputKey.INPUT_KEY_apostrophe),
        new HostKey(KeyEvent.VK_LEFT_PARENTHESIS, Input.InputKey.INPUT_KEY_parenleft),
        new HostKey(KeyEvent.VK_RIGHT_PARENTHESIS, Input.InputKey.INPUT_KEY_parenright),
        new HostKey(KeyEvent.VK_ASTERISK, Input.InputKey.INPUT_KEY_asterisk),
        new HostKey(KeyEvent.VK_PLUS, Input.InputKey.INPUT_KEY_plus),
        new HostKey(KeyEvent.VK_COMMA, Input.InputKey.INPUT_KEY_comma),
        new HostKey(KeyEvent.VK_MINUS, Input.InputKey.INPUT_KEY_minus),
        new HostKey(KeyEvent.VK_PERIOD, Input.InputKey.INPUT_KEY_period),
        new HostKey(KeyEvent.VK_SLASH, Input.InputKey.INPUT_KEY_slash),
        new HostKey(KeyEvent.VK_0, Input.InputKey.INPUT_KEY_0),
        new HostKey(KeyEvent.VK_1, Input.InputKey.INPUT_KEY_1),
        new HostKey(KeyEvent.VK_2, Input.InputKey.INPUT_KEY_2),
        new HostKey(KeyEvent.VK_3, Input.InputKey.INPUT_KEY_3),
        new HostKey(KeyEvent.VK_4, Input.InputKey.INPUT_KEY_4),
        new HostKey(KeyEvent.VK_5, Input.InputKey.INPUT_KEY_5),
        new HostKey(KeyEvent.VK_6, Input.InputKey.INPUT_KEY_6),
        new HostKey(KeyEvent.VK_7, Input.InputKey.INPUT_KEY_7),
        new HostKey(KeyEvent.VK_8, Input.InputKey.INPUT_KEY_8),
        new HostKey(KeyEvent.VK_9, Input.InputKey.INPUT_KEY_9),
        new HostKey(KeyEvent.VK_COLON, Input.InputKey.INPUT_KEY_colon),
        new HostKey(KeyEvent.VK_SEMICOLON, Input.InputKey.INPUT_KEY_semicolon),
        new HostKey(KeyEvent.VK_LESS, Input.InputKey.INPUT_KEY_less),
        new HostKey(KeyEvent.VK_EQUALS, Input.InputKey.INPUT_KEY_equal),
        new HostKey(KeyEvent.VK_GREATER, Input.InputKey.INPUT_KEY_greater),
//        new HostKey(KeyEvent.VK_QUESTION, Input.InputKey.INPUT_KEY_question),
        new HostKey(KeyEvent.VK_AT, Input.InputKey.INPUT_KEY_at),
        new HostKey(KeyEvent.VK_A, Input.InputKey.INPUT_KEY_a),
        new HostKey(KeyEvent.VK_B, Input.InputKey.INPUT_KEY_b),
        new HostKey(KeyEvent.VK_C, Input.InputKey.INPUT_KEY_c),
        new HostKey(KeyEvent.VK_D, Input.InputKey.INPUT_KEY_d),
        new HostKey(KeyEvent.VK_E, Input.InputKey.INPUT_KEY_e),
        new HostKey(KeyEvent.VK_F, Input.InputKey.INPUT_KEY_f),
        new HostKey(KeyEvent.VK_G, Input.InputKey.INPUT_KEY_g),
        new HostKey(KeyEvent.VK_H, Input.InputKey.INPUT_KEY_h),
        new HostKey(KeyEvent.VK_I, Input.InputKey.INPUT_KEY_i),
        new HostKey(KeyEvent.VK_J, Input.InputKey.INPUT_KEY_j),
        new HostKey(KeyEvent.VK_K, Input.InputKey.INPUT_KEY_k),
        new HostKey(KeyEvent.VK_L, Input.InputKey.INPUT_KEY_l),
        new HostKey(KeyEvent.VK_M, Input.InputKey.INPUT_KEY_m),
        new HostKey(KeyEvent.VK_N, Input.InputKey.INPUT_KEY_n),
        new HostKey(KeyEvent.VK_O, Input.InputKey.INPUT_KEY_o),
        new HostKey(KeyEvent.VK_P, Input.InputKey.INPUT_KEY_p),
        new HostKey(KeyEvent.VK_Q, Input.InputKey.INPUT_KEY_q),
        new HostKey(KeyEvent.VK_R, Input.InputKey.INPUT_KEY_r),
        new HostKey(KeyEvent.VK_S, Input.InputKey.INPUT_KEY_s),
        new HostKey(KeyEvent.VK_T, Input.InputKey.INPUT_KEY_t),
        new HostKey(KeyEvent.VK_U, Input.InputKey.INPUT_KEY_u),
        new HostKey(KeyEvent.VK_V, Input.InputKey.INPUT_KEY_v),
        new HostKey(KeyEvent.VK_W, Input.InputKey.INPUT_KEY_w),
        new HostKey(KeyEvent.VK_X, Input.InputKey.INPUT_KEY_x),
        new HostKey(KeyEvent.VK_Y, Input.InputKey.INPUT_KEY_y),
        new HostKey(KeyEvent.VK_Z, Input.InputKey.INPUT_KEY_z),
        new HostKey(KeyEvent.VK_OPEN_BRACKET, Input.InputKey.INPUT_KEY_bracketleft),
        new HostKey(KeyEvent.VK_BACK_SLASH, Input.InputKey.INPUT_KEY_backslash),
        new HostKey(KeyEvent.VK_CLOSE_BRACKET, Input.InputKey.INPUT_KEY_bracketright),
        new HostKey(KeyEvent.VK_CIRCUMFLEX, Input.InputKey.INPUT_KEY_asciicircum),
        new HostKey(KeyEvent.VK_UNDERSCORE, Input.InputKey.INPUT_KEY_underscore),
        new HostKey(KeyEvent.VK_BACK_SPACE, Input.InputKey.INPUT_KEY_BackSpace),
//        new HostKey(KeyEvent.VK_KP_ENTER, Input.InputKey.INPUT_KEY_KP_Enter),
        new HostKey(KeyEvent.VK_UP, Input.InputKey.INPUT_KEY_Up),
        new HostKey(KeyEvent.VK_DOWN, Input.InputKey.INPUT_KEY_Down),
        new HostKey(KeyEvent.VK_LEFT, Input.InputKey.INPUT_KEY_Left),
        new HostKey(KeyEvent.VK_RIGHT, Input.InputKey.INPUT_KEY_Right),
        new HostKey(KeyEvent.VK_INSERT, Input.InputKey.INPUT_KEY_Insert),
        new HostKey(KeyEvent.VK_DELETE, Input.InputKey.INPUT_KEY_Delete),
        new HostKey(KeyEvent.VK_HOME, Input.InputKey.INPUT_KEY_Home),
        new HostKey(KeyEvent.VK_END, Input.InputKey.INPUT_KEY_End),
        new HostKey(KeyEvent.VK_PAGE_UP, Input.InputKey.INPUT_KEY_Page_Up),
        new HostKey(KeyEvent.VK_PAGE_DOWN, Input.InputKey.INPUT_KEY_Page_Down),
        new HostKey(KeyEvent.VK_CAPS_LOCK, Input.InputKey.INPUT_KEY_Caps_Lock),
        new HostKey(KeyEvent.VK_F1, Input.InputKey.INPUT_KEY_F1),
        new HostKey(KeyEvent.VK_F2, Input.InputKey.INPUT_KEY_F2),
        new HostKey(KeyEvent.VK_F3, Input.InputKey.INPUT_KEY_F3),
        new HostKey(KeyEvent.VK_F4, Input.InputKey.INPUT_KEY_F4),
        new HostKey(KeyEvent.VK_F5, Input.InputKey.INPUT_KEY_F5),
        new HostKey(KeyEvent.VK_F6, Input.InputKey.INPUT_KEY_F6),
        new HostKey(KeyEvent.VK_F7, Input.InputKey.INPUT_KEY_F7),
        new HostKey(KeyEvent.VK_F8, Input.InputKey.INPUT_KEY_F8),
        new HostKey(KeyEvent.VK_F9, Input.InputKey.INPUT_KEY_F9),
        new HostKey(KeyEvent.VK_F10, Input.InputKey.INPUT_KEY_F10),
        new HostKey(KeyEvent.VK_F11, Input.InputKey.INPUT_KEY_F11),
        new HostKey(KeyEvent.VK_F12, Input.InputKey.INPUT_KEY_F12),
        new HostKey(KeyEvent.VK_SHIFT, Input.InputKey.INPUT_KEY_Shift_L),
        new HostKey(KeyEvent.VK_CONTROL, Input.InputKey.INPUT_KEY_Control_L),
        new HostKey(KeyEvent.VK_ALT, Input.InputKey.INPUT_KEY_Alt_L),
        new HostKey(KeyEvent.VK_META, Input.InputKey.INPUT_KEY_Meta_L),
        new HostKey(0, Input.InputKey.INPUT_KEY_NONE) // End marker
    };
    private Keyboard keyboard;
    private Input input;

    public SwingKeyboard(Keyboard keyboard, Input input) {
      this.keyboard = keyboard;
      this.input = input;
        for (HostKey entry : UNICODE_KEYSYMS_MAP) {
            if (entry.host() == 0) break;
            unicodeKeysymsHash.put(entry.host(), entry.key());
        }
        for (HostKey entry : KEYSYMS_MAP) {
            if (entry.host() == 0) break;
            keysyms.put(entry.host(), entry.key());
        }

        // Enable key repeat (Swing handles this via OS settings, but we can note it)
        // No direct equivalent to SDL_EnableKeyRepeat in Swing; relies on system settings

    }

    public void end() {
        unicodeKeysymsHash.clear();
    }

    private Input.InputKey unicodeKeysymsRemap(int uiKeysym) {
        return unicodeKeysymsHash.getOrDefault(uiKeysym, Input.InputKey.INPUT_KEY_NONE);
    }

    private void getKeysyms(Input.InputEvent event, int keycode, char keyChar) {
        // Map keycode to Speccy keysym
        Input.InputKey speccyKeysym = keysyms.getOrDefault(keycode, Input.InputKey.INPUT_KEY_NONE);

        // Map character (Unicode) to Speccy keysym for ASCII characters
        Input.InputKey unicodeKeysym = keyChar <= 0x7F ? unicodeKeysymsRemap(keyChar) : Input.InputKey.INPUT_KEY_NONE;

        // Set native_key and spectrum_key
        event.types = new Input.InputEventKey(
            unicodeKeysym != Input.InputKey.INPUT_KEY_NONE ? unicodeKeysym : speccyKeysym,
            speccyKeysym
        );
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Special handling for F1 to suspend mouse (optional, as per gtkkeyboard.c)
        if (e.getKeyCode() == KeyEvent.VK_F1 && e.getModifiersEx() == 0) {
        }

        Input.InputEvent speccyEvent = new Input.InputEvent(Input.InputEventType.INPUT_EVENT_KEYPRESS, null);
        getKeysyms(speccyEvent, e.getKeyCode(), e.getKeyChar());

        // Only process if we have a valid keysym
        if (((Input.InputEventKey) speccyEvent.types).nativeKey != Input.InputKey.INPUT_KEY_NONE ||
            ((Input.InputEventKey) speccyEvent.types).spectrumKey != Input.InputKey.INPUT_KEY_NONE) {
            input.event(speccyEvent);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Input.InputEvent speccyEvent = new Input.InputEvent(Input.InputEventType.INPUT_EVENT_KEYRELEASE, null);
        getKeysyms(speccyEvent, e.getKeyCode(), e.getKeyChar());

        // Only process if we have a valid keysym
        if (((Input.InputEventKey) speccyEvent.types).spectrumKey != Input.InputKey.INPUT_KEY_NONE) {
            input.event(speccyEvent);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used for Speccy keyboard handling, as we handle press/release separately
    }
}