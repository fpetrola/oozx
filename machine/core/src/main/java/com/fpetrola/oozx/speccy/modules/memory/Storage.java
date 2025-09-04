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

/** MemoryPart that holds bytes: what a ROM and a RAM have in common, which is everything but what a write does. */
public sealed abstract class Storage extends MemoryPart permits Rom, Ram {
  public final byte[] bytes;

  protected Storage(byte[] bytes) {
    this.bytes = bytes;
  }

  public int size() {
    return bytes.length;
  }

  public void fill(byte[] image) {
    System.arraycopy(image, 0, bytes, 0, Math.min(image.length, bytes.length));
  }

  public int read(int offset) {
    return bytes[offset] & 0xff;
  }
}
