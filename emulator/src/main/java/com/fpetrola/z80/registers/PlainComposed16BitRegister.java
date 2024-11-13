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

package com.fpetrola.z80.registers;

/**
 * Camino rapido para el caso comun: las dos mitades son registros planos de 8
 * bits, asi que el par toca su almacenamiento directamente en vez de pasar por
 * read/write.
 * <p>
 * Solo es valido cuando ninguna mitad redefine el contrato de Register. RRegister
 * (que enmascara a 7 bits y conserva el bit 7), los registros envueltos por un
 * InstructionSpy y los registros virtuales tienen que usar Composed16BitRegister,
 * o sus redefiniciones quedarian sin efecto.
 */
public class PlainComposed16BitRegister extends Composed16BitRegister<Plain8BitRegister> {

  public PlainComposed16BitRegister(String name, Plain8BitRegister h, Plain8BitRegister l) {
    super(name, h, l);
  }

  public int read() {
    return high.data << 8 | low.data;
  }

  public void write(final int value) {
    this.high.data = value >>> 8;
    this.low.data = value & 0xFF;
  }

  public void increment() {
    if (++low.data < 0x100)
      return;
    low.data = 0;
    if (++high.data < 0x100)
      return;
    high.data = 0;
  }

  public void decrement() {
    if (--low.data >= 0)
      return;
    low.data = 0xff;

    if (--high.data >= 0)
      return;
    high.data = 0xff;
  }
}
