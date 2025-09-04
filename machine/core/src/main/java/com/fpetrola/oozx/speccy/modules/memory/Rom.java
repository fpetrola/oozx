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

public final class Rom extends Storage {
  /** Null unless the emulator was told to allow ROM writes (e.g. for a debugger patching code). */
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

  @Singleton
  public static class Protection {
    /** Real hardware ignores writes to ROM; this lets a debugger patch it anyway. */
    public boolean writableRoms;
  }
}
