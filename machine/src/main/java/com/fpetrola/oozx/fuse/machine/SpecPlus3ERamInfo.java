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

// ===================================================================
// RamInfo para +3e
// ===================================================================
class SpecPlus3ERamInfo extends RamInfo {
  private SpecPlus3 specPlus3;

  public SpecPlus3ERamInfo(int validPages, SpecPlus3 specPlus3) {
    this.validPages = validPages;
    this.specPlus3 = specPlus3;
  }

  public boolean portFromUla(int port) {
    return specPlus3.portFromUla(port);
  }

  public int contendDelay(long time) {
    return specPlus3.contendDelay76543210(time);
  }

  public int contendDelayNoMreq(long time) {
    return specPlus3.contendDelayNone(time);
  }
}
