/*
 * /*
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
 *  */

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.registers.Register;

/**
 * A minizx Spectrum's IO: ports, plus the keyboard and PC the player needs. Not generic since
 * the `emulator:0.0.2-alu` migration fixed {@code IO} to {@code int in(int)}/{@code void
 * out(int, int)}, so a port's value is no longer a parameterizable type.
 */
public interface MiniZXIO extends IO {
  MiniZXKeyboard getMiniZXKeyboard();

  void setPc(Register pc);
}
