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

public class UiDisplay {
  public byte[][] screenMatrix;
  private byte[] cache;

  public UiDisplay() {
//    cache = createCache();
  }

  public void plot8B(int x, int y, byte data, byte ink, byte paper) {
    final int xIndex = x << 3;
    final int dataIndex = data & 0xff;
    final int inkIndex = ink << 8;
    final int paperIndex = paper << 12;
    for (int i = 0; i < 8; i++) {
      screenMatrix[xIndex + i][y] = cache[dataIndex | inkIndex | paperIndex | i << 0x10];
    }
  }

  public void plot8(int x, int y, byte data, byte ink, byte paper) {
    for (int i = 0; i < 8; i++)
      screenMatrix[(x << 3) + i][y] = (data & (0x80 >> i)) != 0 ? ink : paper;
  }

  private byte[] createCache() {
    byte[] cache = new byte[0x100 * 16 * 16 * 8];
    for (int i = 0; i < 8; i++)
      for (int data = 0; data < 256; data++) {
        for (byte ink = 0; ink < 16; ink++) {
          for (byte paper = 0; paper < 16; paper++) {
            cache[data | ink << 8 | paper << 12 | i << 16] = ((data & (0x80 >> i)) != 0 ? ink : paper);
          }
        }
      }
    return cache;
  }

  public void area(int x, int y, int w, int h) {
  }

  public void frameEnd() {
  }

  public int end() {
    return 0;
  }

  public int init(int width, int height) {
    return 0;
  }
}
