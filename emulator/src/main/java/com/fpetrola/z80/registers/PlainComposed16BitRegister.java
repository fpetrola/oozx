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
 * Reaches both halves' storage without going through read and write.
 * <p>
 * Sound only while neither half means anything of its own by them: R keeps its top bit and masks
 * the rest, a watched register reports, a virtual one answers from elsewhere. Pair any of those
 * this way and what makes it itself is skipped, so they belong in {@link Composed16BitRegister}.
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
