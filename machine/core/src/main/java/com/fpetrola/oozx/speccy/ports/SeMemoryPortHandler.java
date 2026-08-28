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

package com.fpetrola.oozx.speccy.ports;

import com.fpetrola.oozx.Spectrum;

public class SeMemoryPortHandler extends DefaultPortHandler {
  private final Spectrum spectrum;

  public SeMemoryPortHandler(Spectrum spectrum) {
    super(0xffff, 0x7ffd, false, true);
    this.spectrum = spectrum;
  }

  @Override
  public void write(int port, byte value) {
    spectrum.getRamInfo().lastByte = value;
    spectrum.memoryMap();
  }
}
