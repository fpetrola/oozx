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

public class UiDisplay {
  public static byte[][] screenMatrix;

  public static void plot8(int x, int y, byte data, byte ink, byte paper) {
    for (int i = 0; i < 8; i++) {
      int i1 = data & (0x80 >> i);
      screenMatrix[x * 8 + i][y] = i1 != 0 ? ink : paper;
    }
//    System.out.println("plot8 " + x + " " + y + " " + (data & 0xFF) + " " + (ink & 0xFF) + " " + (paper & 0xFF));
  }

  public static void area(int x, int y, int w, int h) {
//    for (int i = 0; i < w; i++) {
//      for (int j = 0; j < h; j++) {
//        screenMatrix[x + i][y + j] = 0;
//      }
//    }

//    System.out.println("area " + i + " " + i1 + " " + i2 + " " + i3);
  }

  public static void frameEnd() {
//    System.out.println("frameEnd");
  }

  public static int end() {
    return 0;
  }

  public static int init(int width, int height) {
    return 0;
  }
}
