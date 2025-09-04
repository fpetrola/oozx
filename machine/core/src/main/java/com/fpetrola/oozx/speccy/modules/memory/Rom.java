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

import com.google.inject.Singleton;

/** Read-only memory: a write goes nowhere, unless whoever reads its code asked the emulator to let it through. */
public final class Rom extends Storage {
  /** What the emulator was told about writing ROMs, or null for one that only the hardware rules apply to. */
  public final Protection protection;

  public Rom(int size) {
    this(size, null);
  }

  public Rom(int size, Protection protection) {
    super(new byte[size]);
    this.protection = protection;
  }

  public void write(int offset, byte value) {
    if (protection != null && protection.writableRoms) {
      bytes[offset] = value;
    }
  }

  /** What the machine's ROMs allow that the hardware does not. */
  @Singleton
  public static class Protection {
    /** Whether a write to a ROM page sticks. A real machine ignores it; someone reading the ROM's code wants it. */
    public boolean writableRoms;
  }
}
