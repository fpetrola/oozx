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

package com.fpetrola.oozx.fuse;

public enum KeyboardKeyName {
    KEYBOARD_NONE(0x00),
    KEYBOARD_space(0x20),
    KEYBOARD_0(0x30),
    KEYBOARD_1(0x31),
    KEYBOARD_2(0x32),
    KEYBOARD_3(0x33),
    KEYBOARD_4(0x34),
    KEYBOARD_5(0x35),
    KEYBOARD_6(0x36),
    KEYBOARD_7(0x37),
    KEYBOARD_8(0x38),
    KEYBOARD_9(0x39),
    KEYBOARD_a(0x61),
    KEYBOARD_b(0x62),
    KEYBOARD_c(0x63),
    KEYBOARD_d(0x64),
    KEYBOARD_e(0x65),
    KEYBOARD_f(0x66),
    KEYBOARD_g(0x67),
    KEYBOARD_h(0x68),
    KEYBOARD_i(0x69),
    KEYBOARD_j(0x6a),
    KEYBOARD_k(0x6b),
    KEYBOARD_l(0x6c),
    KEYBOARD_m(0x6d),
    KEYBOARD_n(0x6e),
    KEYBOARD_o(0x6f),
    KEYBOARD_p(0x70),
    KEYBOARD_q(0x71),
    KEYBOARD_r(0x72),
    KEYBOARD_s(0x73),
    KEYBOARD_t(0x74),
    KEYBOARD_u(0x75),
    KEYBOARD_v(0x76),
    KEYBOARD_w(0x77),
    KEYBOARD_x(0x78),
    KEYBOARD_y(0x79),
    KEYBOARD_z(0x7a),
    KEYBOARD_Enter(0x100),
    KEYBOARD_Caps(0x101),
    KEYBOARD_Symbol(0x102),
    KEYBOARD_JOYSTICK_FIRE(0x1000);

    private final int value;

    KeyboardKeyName(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
