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

import com.fpetrola.oozx.Ui;
import com.fpetrola.oozx.fuse.modules.Keyboard;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

public class SwingKeyboard implements KeyListener {

    private static Map<Integer, Input.InputKey> unicodeKeysymsHash = new HashMap<>();
    private static final KeysymsMap[] UNICODE_KEYSYMS_MAP = {
        new KeysymsMap(' ', Input.InputKey.INPUT_KEY_space),
        new KeysymsMap('!', Input.InputKey.INPUT_KEY_exclam),
        new KeysymsMap('"', Input.InputKey.INPUT_KEY_quotedbl),
        new KeysymsMap('#', Input.InputKey.INPUT_KEY_numbersign),
        new KeysymsMap('$', Input.InputKey.INPUT_KEY_dollar),
        new KeysymsMap('%', Input.InputKey.INPUT_KEY_percent),
        new KeysymsMap('&', Input.InputKey.INPUT_KEY_ampersand),
        new KeysymsMap('\'', Input.InputKey.INPUT_KEY_apostrophe),
        new KeysymsMap('(', Input.InputKey.INPUT_KEY_parenleft),
        new KeysymsMap(')', Input.InputKey.INPUT_KEY_parenright),
        new KeysymsMap('*', Input.InputKey.INPUT_KEY_asterisk),
        new KeysymsMap('+', Input.InputKey.INPUT_KEY_plus),
        new KeysymsMap(',', Input.InputKey.INPUT_KEY_comma),
        new KeysymsMap('-', Input.InputKey.INPUT_KEY_minus),
        new KeysymsMap('.', Input.InputKey.INPUT_KEY_period),
        new KeysymsMap('/', Input.InputKey.INPUT_KEY_slash),
        new KeysymsMap('0', Input.InputKey.INPUT_KEY_0),
        new KeysymsMap('1', Input.InputKey.INPUT_KEY_1),
        new KeysymsMap('2', Input.InputKey.INPUT_KEY_2),
        new KeysymsMap('3', Input.InputKey.INPUT_KEY_3),
        new KeysymsMap('4', Input.InputKey.INPUT_KEY_4),
        new KeysymsMap('5', Input.InputKey.INPUT_KEY_5),
        new KeysymsMap('6', Input.InputKey.INPUT_KEY_6),
        new KeysymsMap('7', Input.InputKey.INPUT_KEY_7),
        new KeysymsMap('8', Input.InputKey.INPUT_KEY_8),
        new KeysymsMap('9', Input.InputKey.INPUT_KEY_9),
        new KeysymsMap(':', Input.InputKey.INPUT_KEY_colon),
        new KeysymsMap(';', Input.InputKey.INPUT_KEY_semicolon),
        new KeysymsMap('<', Input.InputKey.INPUT_KEY_less),
        new KeysymsMap('=', Input.InputKey.INPUT_KEY_equal),
        new KeysymsMap('>', Input.InputKey.INPUT_KEY_greater),
        new KeysymsMap('?', Input.InputKey.INPUT_KEY_question),
        new KeysymsMap('@', Input.InputKey.INPUT_KEY_at),
        new KeysymsMap('A', Input.InputKey.INPUT_KEY_A),
        new KeysymsMap('B', Input.InputKey.INPUT_KEY_B),
        new KeysymsMap('C', Input.InputKey.INPUT_KEY_C),
        new KeysymsMap('D', Input.InputKey.INPUT_KEY_D),
        new KeysymsMap('E', Input.InputKey.INPUT_KEY_E),
        new KeysymsMap('F', Input.InputKey.INPUT_KEY_F),
        new KeysymsMap('G', Input.InputKey.INPUT_KEY_G),
        new KeysymsMap('H', Input.InputKey.INPUT_KEY_H),
        new KeysymsMap('I', Input.InputKey.INPUT_KEY_I),
        new KeysymsMap('J', Input.InputKey.INPUT_KEY_J),
        new KeysymsMap('K', Input.InputKey.INPUT_KEY_K),
        new KeysymsMap('L', Input.InputKey.INPUT_KEY_L),
        new KeysymsMap('M', Input.InputKey.INPUT_KEY_M),
        new KeysymsMap('N', Input.InputKey.INPUT_KEY_N),
        new KeysymsMap('O', Input.InputKey.INPUT_KEY_O),
        new KeysymsMap('P', Input.InputKey.INPUT_KEY_P),
        new KeysymsMap('Q', Input.InputKey.INPUT_KEY_Q),
        new KeysymsMap('R', Input.InputKey.INPUT_KEY_R),
        new KeysymsMap('S', Input.InputKey.INPUT_KEY_S),
        new KeysymsMap('T', Input.InputKey.INPUT_KEY_T),
        new KeysymsMap('U', Input.InputKey.INPUT_KEY_U),
        new KeysymsMap('V', Input.InputKey.INPUT_KEY_V),
        new KeysymsMap('W', Input.InputKey.INPUT_KEY_W),
        new KeysymsMap('X', Input.InputKey.INPUT_KEY_X),
        new KeysymsMap('Y', Input.InputKey.INPUT_KEY_Y),
        new KeysymsMap('Z', Input.InputKey.INPUT_KEY_Z),
        new KeysymsMap('a', Input.InputKey.INPUT_KEY_a),
        new KeysymsMap('b', Input.InputKey.INPUT_KEY_b),
        new KeysymsMap('c', Input.InputKey.INPUT_KEY_c),
        new KeysymsMap('d', Input.InputKey.INPUT_KEY_d),
        new KeysymsMap('e', Input.InputKey.INPUT_KEY_e),
        new KeysymsMap('f', Input.InputKey.INPUT_KEY_f),
        new KeysymsMap('g', Input.InputKey.INPUT_KEY_g),
        new KeysymsMap('h', Input.InputKey.INPUT_KEY_h),
        new KeysymsMap('i', Input.InputKey.INPUT_KEY_i),
        new KeysymsMap('j', Input.InputKey.INPUT_KEY_j),
        new KeysymsMap('k', Input.InputKey.INPUT_KEY_k),
        new KeysymsMap('l', Input.InputKey.INPUT_KEY_l),
        new KeysymsMap('m', Input.InputKey.INPUT_KEY_m),
        new KeysymsMap('n', Input.InputKey.INPUT_KEY_n),
        new KeysymsMap('o', Input.InputKey.INPUT_KEY_o),
        new KeysymsMap('p', Input.InputKey.INPUT_KEY_p),
        new KeysymsMap('q', Input.InputKey.INPUT_KEY_q),
        new KeysymsMap('r', Input.InputKey.INPUT_KEY_r),
        new KeysymsMap('s', Input.InputKey.INPUT_KEY_s),
        new KeysymsMap('t', Input.InputKey.INPUT_KEY_t),
        new KeysymsMap('u', Input.InputKey.INPUT_KEY_u),
        new KeysymsMap('v', Input.InputKey.INPUT_KEY_v),
        new KeysymsMap('w', Input.InputKey.INPUT_KEY_w),
        new KeysymsMap('x', Input.InputKey.INPUT_KEY_x),
        new KeysymsMap('y', Input.InputKey.INPUT_KEY_y),
        new KeysymsMap('z', Input.InputKey.INPUT_KEY_z),
        new KeysymsMap('[', Input.InputKey.INPUT_KEY_bracketleft),
        new KeysymsMap('\\', Input.InputKey.INPUT_KEY_backslash),
        new KeysymsMap(']', Input.InputKey.INPUT_KEY_bracketright),
        new KeysymsMap('^', Input.InputKey.INPUT_KEY_asciicircum),
        new KeysymsMap('_', Input.InputKey.INPUT_KEY_underscore),
        new KeysymsMap('{', Input.InputKey.INPUT_KEY_braceleft),
        new KeysymsMap('|', Input.InputKey.INPUT_KEY_bar),
        new KeysymsMap('}', Input.InputKey.INPUT_KEY_braceright),
        new KeysymsMap('~', Input.InputKey.INPUT_KEY_asciitilde),
        new KeysymsMap(0, Input.InputKey.INPUT_KEY_NONE) // End marker
    };

    public static final KeysymsMap[] KEYSYMS_MAP = {
        new KeysymsMap(KeyEvent.VK_TAB, Input.InputKey.INPUT_KEY_Tab),
        new KeysymsMap(KeyEvent.VK_ENTER, Input.InputKey.INPUT_KEY_Return),
        new KeysymsMap(KeyEvent.VK_ESCAPE, Input.InputKey.INPUT_KEY_Escape),
        new KeysymsMap(KeyEvent.VK_SPACE, Input.InputKey.INPUT_KEY_space),
        new KeysymsMap(KeyEvent.VK_EXCLAMATION_MARK, Input.InputKey.INPUT_KEY_exclam),
        new KeysymsMap(KeyEvent.VK_QUOTE, Input.InputKey.INPUT_KEY_quotedbl),
        new KeysymsMap(KeyEvent.VK_NUMBER_SIGN, Input.InputKey.INPUT_KEY_numbersign),
        new KeysymsMap(KeyEvent.VK_DOLLAR, Input.InputKey.INPUT_KEY_dollar),
//        new KeysymsMap(KeyEvent.VK_PERCENT, Input.InputKey.INPUT_KEY_percent),
        new KeysymsMap(KeyEvent.VK_AMPERSAND, Input.InputKey.INPUT_KEY_ampersand),
        new KeysymsMap(KeyEvent.VK_QUOTE, Input.InputKey.INPUT_KEY_apostrophe),
        new KeysymsMap(KeyEvent.VK_LEFT_PARENTHESIS, Input.InputKey.INPUT_KEY_parenleft),
        new KeysymsMap(KeyEvent.VK_RIGHT_PARENTHESIS, Input.InputKey.INPUT_KEY_parenright),
        new KeysymsMap(KeyEvent.VK_ASTERISK, Input.InputKey.INPUT_KEY_asterisk),
        new KeysymsMap(KeyEvent.VK_PLUS, Input.InputKey.INPUT_KEY_plus),
        new KeysymsMap(KeyEvent.VK_COMMA, Input.InputKey.INPUT_KEY_comma),
        new KeysymsMap(KeyEvent.VK_MINUS, Input.InputKey.INPUT_KEY_minus),
        new KeysymsMap(KeyEvent.VK_PERIOD, Input.InputKey.INPUT_KEY_period),
        new KeysymsMap(KeyEvent.VK_SLASH, Input.InputKey.INPUT_KEY_slash),
        new KeysymsMap(KeyEvent.VK_0, Input.InputKey.INPUT_KEY_0),
        new KeysymsMap(KeyEvent.VK_1, Input.InputKey.INPUT_KEY_1),
        new KeysymsMap(KeyEvent.VK_2, Input.InputKey.INPUT_KEY_2),
        new KeysymsMap(KeyEvent.VK_3, Input.InputKey.INPUT_KEY_3),
        new KeysymsMap(KeyEvent.VK_4, Input.InputKey.INPUT_KEY_4),
        new KeysymsMap(KeyEvent.VK_5, Input.InputKey.INPUT_KEY_5),
        new KeysymsMap(KeyEvent.VK_6, Input.InputKey.INPUT_KEY_6),
        new KeysymsMap(KeyEvent.VK_7, Input.InputKey.INPUT_KEY_7),
        new KeysymsMap(KeyEvent.VK_8, Input.InputKey.INPUT_KEY_8),
        new KeysymsMap(KeyEvent.VK_9, Input.InputKey.INPUT_KEY_9),
        new KeysymsMap(KeyEvent.VK_COLON, Input.InputKey.INPUT_KEY_colon),
        new KeysymsMap(KeyEvent.VK_SEMICOLON, Input.InputKey.INPUT_KEY_semicolon),
        new KeysymsMap(KeyEvent.VK_LESS, Input.InputKey.INPUT_KEY_less),
        new KeysymsMap(KeyEvent.VK_EQUALS, Input.InputKey.INPUT_KEY_equal),
        new KeysymsMap(KeyEvent.VK_GREATER, Input.InputKey.INPUT_KEY_greater),
//        new KeysymsMap(KeyEvent.VK_QUESTION, Input.InputKey.INPUT_KEY_question),
        new KeysymsMap(KeyEvent.VK_AT, Input.InputKey.INPUT_KEY_at),
        new KeysymsMap(KeyEvent.VK_A, Input.InputKey.INPUT_KEY_a),
        new KeysymsMap(KeyEvent.VK_B, Input.InputKey.INPUT_KEY_b),
        new KeysymsMap(KeyEvent.VK_C, Input.InputKey.INPUT_KEY_c),
        new KeysymsMap(KeyEvent.VK_D, Input.InputKey.INPUT_KEY_d),
        new KeysymsMap(KeyEvent.VK_E, Input.InputKey.INPUT_KEY_e),
        new KeysymsMap(KeyEvent.VK_F, Input.InputKey.INPUT_KEY_f),
        new KeysymsMap(KeyEvent.VK_G, Input.InputKey.INPUT_KEY_g),
        new KeysymsMap(KeyEvent.VK_H, Input.InputKey.INPUT_KEY_h),
        new KeysymsMap(KeyEvent.VK_I, Input.InputKey.INPUT_KEY_i),
        new KeysymsMap(KeyEvent.VK_J, Input.InputKey.INPUT_KEY_j),
        new KeysymsMap(KeyEvent.VK_K, Input.InputKey.INPUT_KEY_k),
        new KeysymsMap(KeyEvent.VK_L, Input.InputKey.INPUT_KEY_l),
        new KeysymsMap(KeyEvent.VK_M, Input.InputKey.INPUT_KEY_m),
        new KeysymsMap(KeyEvent.VK_N, Input.InputKey.INPUT_KEY_n),
        new KeysymsMap(KeyEvent.VK_O, Input.InputKey.INPUT_KEY_o),
        new KeysymsMap(KeyEvent.VK_P, Input.InputKey.INPUT_KEY_p),
        new KeysymsMap(KeyEvent.VK_Q, Input.InputKey.INPUT_KEY_q),
        new KeysymsMap(KeyEvent.VK_R, Input.InputKey.INPUT_KEY_r),
        new KeysymsMap(KeyEvent.VK_S, Input.InputKey.INPUT_KEY_s),
        new KeysymsMap(KeyEvent.VK_T, Input.InputKey.INPUT_KEY_t),
        new KeysymsMap(KeyEvent.VK_U, Input.InputKey.INPUT_KEY_u),
        new KeysymsMap(KeyEvent.VK_V, Input.InputKey.INPUT_KEY_v),
        new KeysymsMap(KeyEvent.VK_W, Input.InputKey.INPUT_KEY_w),
        new KeysymsMap(KeyEvent.VK_X, Input.InputKey.INPUT_KEY_x),
        new KeysymsMap(KeyEvent.VK_Y, Input.InputKey.INPUT_KEY_y),
        new KeysymsMap(KeyEvent.VK_Z, Input.InputKey.INPUT_KEY_z),
        new KeysymsMap(KeyEvent.VK_OPEN_BRACKET, Input.InputKey.INPUT_KEY_bracketleft),
        new KeysymsMap(KeyEvent.VK_BACK_SLASH, Input.InputKey.INPUT_KEY_backslash),
        new KeysymsMap(KeyEvent.VK_CLOSE_BRACKET, Input.InputKey.INPUT_KEY_bracketright),
        new KeysymsMap(KeyEvent.VK_CIRCUMFLEX, Input.InputKey.INPUT_KEY_asciicircum),
        new KeysymsMap(KeyEvent.VK_UNDERSCORE, Input.InputKey.INPUT_KEY_underscore),
        new KeysymsMap(KeyEvent.VK_BACK_SPACE, Input.InputKey.INPUT_KEY_BackSpace),
//        new KeysymsMap(KeyEvent.VK_KP_ENTER, Input.InputKey.INPUT_KEY_KP_Enter),
        new KeysymsMap(KeyEvent.VK_UP, Input.InputKey.INPUT_KEY_Up),
        new KeysymsMap(KeyEvent.VK_DOWN, Input.InputKey.INPUT_KEY_Down),
        new KeysymsMap(KeyEvent.VK_LEFT, Input.InputKey.INPUT_KEY_Left),
        new KeysymsMap(KeyEvent.VK_RIGHT, Input.InputKey.INPUT_KEY_Right),
        new KeysymsMap(KeyEvent.VK_INSERT, Input.InputKey.INPUT_KEY_Insert),
        new KeysymsMap(KeyEvent.VK_DELETE, Input.InputKey.INPUT_KEY_Delete),
        new KeysymsMap(KeyEvent.VK_HOME, Input.InputKey.INPUT_KEY_Home),
        new KeysymsMap(KeyEvent.VK_END, Input.InputKey.INPUT_KEY_End),
        new KeysymsMap(KeyEvent.VK_PAGE_UP, Input.InputKey.INPUT_KEY_Page_Up),
        new KeysymsMap(KeyEvent.VK_PAGE_DOWN, Input.InputKey.INPUT_KEY_Page_Down),
        new KeysymsMap(KeyEvent.VK_CAPS_LOCK, Input.InputKey.INPUT_KEY_Caps_Lock),
        new KeysymsMap(KeyEvent.VK_F1, Input.InputKey.INPUT_KEY_F1),
        new KeysymsMap(KeyEvent.VK_F2, Input.InputKey.INPUT_KEY_F2),
        new KeysymsMap(KeyEvent.VK_F3, Input.InputKey.INPUT_KEY_F3),
        new KeysymsMap(KeyEvent.VK_F4, Input.InputKey.INPUT_KEY_F4),
        new KeysymsMap(KeyEvent.VK_F5, Input.InputKey.INPUT_KEY_F5),
        new KeysymsMap(KeyEvent.VK_F6, Input.InputKey.INPUT_KEY_F6),
        new KeysymsMap(KeyEvent.VK_F7, Input.InputKey.INPUT_KEY_F7),
        new KeysymsMap(KeyEvent.VK_F8, Input.InputKey.INPUT_KEY_F8),
        new KeysymsMap(KeyEvent.VK_F9, Input.InputKey.INPUT_KEY_F9),
        new KeysymsMap(KeyEvent.VK_F10, Input.InputKey.INPUT_KEY_F10),
        new KeysymsMap(KeyEvent.VK_F11, Input.InputKey.INPUT_KEY_F11),
        new KeysymsMap(KeyEvent.VK_F12, Input.InputKey.INPUT_KEY_F12),
        new KeysymsMap(KeyEvent.VK_SHIFT, Input.InputKey.INPUT_KEY_Shift_L),
        new KeysymsMap(KeyEvent.VK_CONTROL, Input.InputKey.INPUT_KEY_Control_L),
        new KeysymsMap(KeyEvent.VK_ALT, Input.InputKey.INPUT_KEY_Alt_L),
        new KeysymsMap(KeyEvent.VK_META, Input.InputKey.INPUT_KEY_Meta_L),
        new KeysymsMap(0, Input.InputKey.INPUT_KEY_NONE) // End marker
    };
    private Keyboard keyboard;
    private Input input;

    public SwingKeyboard(JFrame component, Keyboard keyboard, Input input) {
      this.keyboard = keyboard;
      this.input = input;
      // Initialize unicode_keysyms_hash
        for (KeysymsMap entry : UNICODE_KEYSYMS_MAP) {
            if (entry.ui == 0) break;
            unicodeKeysymsHash.put(entry.ui, entry.fuse);
        }

        // Enable key repeat (Swing handles this via OS settings, but we can note it)
        // No direct equivalent to SDL_EnableKeyRepeat in Swing; relies on system settings

        // Register this as a KeyListener to the component
        component.addKeyListener(this);
        component.setFocusable(true);
        component.requestFocusInWindow();
    }

    public void end() {
        unicodeKeysymsHash.clear();
    }

    private Input.InputKey unicodeKeysymsRemap(int uiKeysym) {
        return unicodeKeysymsHash.getOrDefault(uiKeysym, Input.InputKey.INPUT_KEY_NONE);
    }

    private void getKeysyms(Input.InputEvent event, int keycode, char keyChar) {
        // Map keycode to Fuse keysym
        Input.InputKey fuseKeysym = keyboard.remap(keycode);

        // Map character (Unicode) to Fuse keysym for ASCII characters
        Input.InputKey unicodeKeysym = keyChar <= 0x7F ? unicodeKeysymsRemap(keyChar) : Input.InputKey.INPUT_KEY_NONE;

        // Set native_key and spectrum_key
        event.types = new Input.InputEventKey(
            unicodeKeysym != Input.InputKey.INPUT_KEY_NONE ? unicodeKeysym : fuseKeysym,
            fuseKeysym
        );
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Special handling for F1 to suspend mouse (optional, as per gtkkeyboard.c)
        if (e.getKeyCode() == KeyEvent.VK_F1 && e.getModifiersEx() == 0) {
            Ui.mouseSuspend();
        }

        Input.InputEvent fuseEvent = new Input.InputEvent(Input.InputEventType.INPUT_EVENT_KEYPRESS, null);
        getKeysyms(fuseEvent, e.getKeyCode(), e.getKeyChar());

        // Only process if we have a valid keysym
        if (((Input.InputEventKey) fuseEvent.types).nativeKey != Input.InputKey.INPUT_KEY_NONE ||
            ((Input.InputEventKey) fuseEvent.types).spectrumKey != Input.InputKey.INPUT_KEY_NONE) {
            input.event(fuseEvent);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Input.InputEvent fuseEvent = new Input.InputEvent(Input.InputEventType.INPUT_EVENT_KEYRELEASE, null);
        getKeysyms(fuseEvent, e.getKeyCode(), e.getKeyChar());

        // Only process if we have a valid keysym
        if (((Input.InputEventKey) fuseEvent.types).spectrumKey != Input.InputKey.INPUT_KEY_NONE) {
            input.event(fuseEvent);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used for Fuse keyboard handling, as we handle press/release separately
    }
}