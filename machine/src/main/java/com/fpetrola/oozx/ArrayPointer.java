/*
 *
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
 *
 */

package com.fpetrola.oozx;

public class ArrayPointer {
  private byte[][] ram = new byte[1][1];
  private int i;
  private int j;

  public ArrayPointer(byte[] page) {
    ram[0] = page;
  }

  public ArrayPointer(byte[][] ram, int i, int j) {
    this.ram = ram;
    this.i = i;
    this.j = j;
  }

  public byte get(int i2) {
    return ram[i][j + i2];
  }

  public void set(int offset, byte b) {
    ram[i][j + offset] = b;
  }
}
