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

package com.fpetrola.oozx;

public class ArrayPointer {
  private final byte[] ram;
  private final int j;

  public ArrayPointer(byte[][] ram, int i, int j) {
    this.ram = ram[i];
    this.j = j;
  }

  public ArrayPointer(byte[] ram, int i, int j) {
    this(new byte[][]{ram}, i, j);
  }

  public byte get(final int offset) {
    return ram[j + offset];
  }

  public void set(final int offset, final byte b) {
    ram[j + offset] = b;
  }
}
