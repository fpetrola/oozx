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
 * El IO de un Spectrum chico: puertos, más el teclado y el PC que el reproductor necesita.
 *
 * <p>Sin parámetro de tipo desde la migración a `emulator:0.0.2-alu`: esa rama dejó `IO` en
 * `int in(int)` / `void out(int, int)`, así que el valor que cruza un puerto ya no es un tipo
 * que se pueda parametrizar.
 */
public interface MiniZXIO extends IO {
  MiniZXKeyboard getMiniZXKeyboard();

  void setPc(Register pc);
}
