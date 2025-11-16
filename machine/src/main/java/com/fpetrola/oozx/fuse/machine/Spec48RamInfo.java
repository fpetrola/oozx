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

package com.fpetrola.oozx.fuse.machine;

import com.fpetrola.oozx.Spectrum;

public class Spec48RamInfo extends RamInfo {
  protected final Spectrum spectrum;

  public Spec48RamInfo(Spectrum spectrum, int validPages) {
    this.spectrum = spectrum;
    this.validPages = validPages;
  }

  public boolean portFromUla(int port) {
    return Spec48.portFromUlaStatic(port);
  }

  public int contendDelay(long time) {
    return spectrum.contendDelay65432100(time);
  }

  public int contendDelayNoMreq(long time) {
    return spectrum.contendDelay65432100(time);
  }
}
