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

import java.awt.*;

public class ZxColor {

  public static Color[] colors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};

  boolean FLASH;
  boolean BRIGHT;
  byte PAPER;
  byte INK;

  public int getAttribute() {
    return attribute;
  }

  private final int attribute;

  public ZxColor(int attribute) {
    this.FLASH = (attribute & 0x80) != 0;
    this.BRIGHT = (attribute & 0x40) != 0;
    this.PAPER = (byte) ((attribute >> 3) & 0x07);
    this.INK = (byte) (attribute & 0x07);
    this.attribute = attribute;
  }

  public Color getInkColor() {
    return BRIGHT ? colors[INK] : colors[INK].darker();
  }

  public Color getPaperColor() {
    return BRIGHT ? colors[PAPER] : colors[PAPER].darker();
  }

  public Color getStateColor(boolean set) {
    Color color = set ? getInkColor() : getPaperColor();
//    color= Color.WHITE;
    return color;
  }
}
