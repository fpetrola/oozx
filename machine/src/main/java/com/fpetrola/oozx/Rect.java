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

// Structure to represent a rectangle
public class Rect {
  int x, y; // Top-left corner
  private int w;
  private int h; // Width and height

  public Rect(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    check();
  }

  public int getW() {
    return w;
  }

  public void setW(int w) {
    this.w = w;
    check();
  }

  public int getH() {
    return h;
  }

  public void setH(int h) {
    this.h = h;
    check();
  }

  private void check() {
//    if (w == 0 || h == 0)
//      System.err.println("WARNING: UiDisplay.area called with w>0 or h>0: " + w + "x" + h);
  }
}
