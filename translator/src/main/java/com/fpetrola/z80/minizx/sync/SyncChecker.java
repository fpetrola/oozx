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

package com.fpetrola.z80.minizx.sync;

import com.fpetrola.z80.minizx.SpectrumApplication;

public interface SyncChecker {
  default int getByteFromEmu(Integer index) {
    return 0;
  }

  default void init(SpectrumApplication spectrumApplication) {
  }

  default void checkSyncEmu(int address, int value, int pc, boolean write) {
  }

  default void checkSyncJava(int address, int value, int pc) {
  }

  default void checkMatching(int pc, int address, boolean write) {
  }

  default void checkSyncInJava(int port, int pc) {
  }

  default int getR() {
    return (int) (Math.random() * 65535);
  }
}
