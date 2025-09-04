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

package com.fpetrola.oozx.speccy.modules.memory;

import java.util.function.IntConsumer;

/**
 * MemoryPart that is written. What it is right now is state, not kind: write protected while a
 * device's jumper or control bit says so, and shown while the picture comes from it - then a
 * write that changes a byte of the picture is told to whoever draws it.
 */
public final class Ram extends Storage {
  private static final int SCREEN_BYTES = 0x1b00;
  public boolean writeProtected;
  /** Who to tell when a byte of the picture changes, or null while this is not the screen. */
  public IntConsumer shownTo;

  public Ram(int size) {
    this(new byte[size]);
  }

  public Ram(byte[] bytes) {
    super(bytes);
  }

  public void write(int offset, byte value) {
    if (writeProtected) {
      return;
    }
    if (shownTo != null && offset < SCREEN_BYTES && bytes[offset] != value) {
      shownTo.accept(offset);
    }
    bytes[offset] = value;
  }
}
