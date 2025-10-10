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

import com.fpetrola.oozx.fuse.AbstractStartupModule;

public class DisplayStartupModule extends AbstractStartupModule {
  private final Display.DisplayStartupContext context;

  public DisplayStartupModule(Display.DisplayStartupContext context) {
    super();
    this.context = context;
  }

  public Object getInitContext() {
    return context;
  }

  public int initFn(Object initContext) {
    Display.DisplayStartupContext typedContext = (Display.DisplayStartupContext) initContext;
    int i, j, k, x, y;

    if (Ui.init(typedContext.argv.length, typedContext.argv) != 0) return 1;

    // Set up the 'all pixels must be refreshed' marker
    Display.allDirty = 0;
    for (i = 0; i < Display.SCREEN_WIDTH_COLS; i++) {
      Display.allDirty = (Display.allDirty << 1) | 0x01;
    }

    for (i = 0; i < 3; i++) {
      for (j = 0; j < 8; j++) {
        for (k = 0; k < 8; k++) {
          Display.lineStart[(64 * i) + (8 * j) + k] = 32 * ((64 * i) + j + (k * 8));
        }
      }
    }

    for (y = 0; y < Display.HEIGHT; y++) {
      Display.attrStart[y] = 6144 + (32 * (y / 8));
    }

    for (y = 0; y < Display.HEIGHT; y++) {
      for (x = 0; x < Display.WIDTH_COLS; x++) {
        Display.dirtyYtable[Display.lineStart[y] + x] = y;
        Display.dirtyXtable[Display.lineStart[y] + x] = x;
      }
    }

    for (y = 0; y < Display.HEIGHT_ROWS; y++) {
      for (x = 0; x < Display.WIDTH_COLS; x++) {
        Display.dirtyYtable2[(32 * y) + x] = y * 8;
        Display.dirtyXtable2[(32 * y) + x] = x;
      }
    }

    Display.frameCount = 0;
    Display.flashReversed = false;

    Display.refreshAll();

    Display.borderChanges.clear();
    int error = Display.addBorderSentinel();
    if (error != 0) return error;
    Display.lastBorder = Scld.lastDec.name.hires ? Display.hiresBorder : Display.loresBorder;

    return 0;
  }

  public void endFn() {
  }

}
