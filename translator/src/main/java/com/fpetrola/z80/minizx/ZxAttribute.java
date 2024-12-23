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

package com.fpetrola.z80.minizx;

import java.util.HashMap;
import java.util.Map;

public class ZxAttribute {
  private final LineUpdater lineUpdater;
  private Map<Integer, Integer> pixelValues = new HashMap<>();
  private ZxColor zxColor;

  public ZxAttribute(LineUpdater lineUpdater) {
    this.lineUpdater = lineUpdater;
    this.zxColor = new ZxColor(0);
  }

  public void setZxColor(ZxColor zxColor) {
    if (this.zxColor.getAttribute() != zxColor.getAttribute()) {
      this.zxColor = zxColor;
      for (int line = 0; line < 8; line++)
        updatePixels(line,  pixelValues.get(line));
    }
  }

  private void updatePixels(int line, Integer pixelsValue) {
    for (int bit = 0; bit < 8 && pixelsValue != null; bit++) {
      lineUpdater.update(zxColor, pixelsValue, line, bit);
    }
  }

  public void updateLine(int line, int pixelsValue) {
    pixelValues.put(line, pixelsValue);
    updatePixels(line, pixelsValue);
  }

  public interface LineUpdater {
    void update(ZxColor zxColor, int value, int line, int bit);
  }
}
