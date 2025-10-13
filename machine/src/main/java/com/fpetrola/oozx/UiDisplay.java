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

import com.fpetrola.oozx.fuse.bridge.GetTStatesHistory;

import static com.fpetrola.oozx.Fuse.tStatesHolder;
import static java.lang.String.format;

public class UiDisplay {
  public static byte[][] screenMatrix;

  public static void plot8(int x, int y, byte data, byte ink, byte paper) {
    String format = format("uidisplay_plot8: x=%d y=%d data=%02x ink=%d paper=%d", x, y, data, ink, paper);// Formatea el string
    GetTStatesHistory.addTStateUpdate((byte) (data&0xff), format, (int) tStatesHolder.getTstates());

    for (int i = 0; i < 8; i++) {
      int i1 = data & (0x80 >> i);
      screenMatrix[x * 8 + i][y] = i1 != 0 ? ink : paper;
    }
//    System.out.println("plot8 " + x + " " + y + " " + (data & 0xFF) + " " + (ink & 0xFF) + " " + (paper & 0xFF));
  }

  public static void area(int x, int y, int w, int h) {
//    if (w == 0 || h == 0)
//      System.err.println("WARNING: UiDisplay.area called with w>0 or h>0: " + w + "x" + h);
//    for (int i = x; i < x + w ; i++) {
//      for (int j = y; j < y + h ; j++) {
//        screenMatrix[i][j] = 0;
//      }
//    }

//    System.out.println("area " + x + " " + y + " " + w + " " + h);
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
