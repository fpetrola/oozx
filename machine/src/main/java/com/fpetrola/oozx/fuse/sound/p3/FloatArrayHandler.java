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

package com.fpetrola.oozx.fuse.sound.p3;

public class FloatArrayHandler {
  private float[] imp;
  private int impIdx;

  public FloatArrayHandler(float[] imp, int impIdx) {
    this.imp = imp;
    this.impIdx = impIdx;
  }

  public float get() {
    return imp[impIdx];
  }

  public void set(float i) {
    imp[impIdx] = i;
  }

  public void set(int index, float i) {
    imp[impIdx + index] = i;
  }

  public float get(int i) {
    return imp[impIdx + i];
  }
}
