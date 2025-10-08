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

package com.fpetrola.oozx.speccy.modules.keyboard;

/**
 * What one press by the person at the keyboard puts down on the Spectrum: a key, a shift and a
 * key - a full stop is symbol shift and M - or a shift alone.
 */
public record Combination(SpectrumKey... keys) {
  public static final Combination NONE = new Combination();

  public static Combination of(SpectrumKey key) {
    return new Combination(key);
  }

  public static Combination shifted(SpectrumKey shift, SpectrumKey key) {
    return new Combination(shift, key);
  }

  public void pressOn(KeyMatrix matrix) {
    for (SpectrumKey key : keys) {
      matrix.press(key);
    }
  }

  public void releaseOn(KeyMatrix matrix) {
    for (SpectrumKey key : keys) {
      matrix.release(key);
    }
  }
}
