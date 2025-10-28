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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.Fuse;
import com.fpetrola.oozx.fuse.peripherals.EmulatorCore;
import com.fpetrola.oozx.fuse.peripherals.t.ZXSpectrumDesktopApp;

import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class OOSpectrumLauncher {
  private ScheduledExecutorService scheduledExecutorService = newSingleThreadScheduledExecutor();

  public static void main(String[] args) {
    OOSpectrumLauncher ooSpectrumLauncher = new OOSpectrumLauncher();
    ooSpectrumLauncher.init();
  }

  public void init() {
    OOSpectrumConnector.noTest = true;

    ZXSpectrumDesktopApp zxSpectrumDesktopApp = new ZXSpectrumDesktopApp(() -> {
      Fuse fuse = createFuse();
      return fuse.z80.mockCore;
    });
    zxSpectrumDesktopApp.setVisible(true);

//    createFuse();
  }

  private Fuse createFuse() {
    Fuse fuse = new Fuse();
    fuse.fuseInit();
    fuse.z80.bridgeCommand = (a, b) -> null;

    scheduledExecutorService.scheduleAtFixedRate(() -> {
      while (true) {
        fuse.z80.doOpcodes();
        fuse.eventManager.eventDoEvents();
      }
    }, 0, 1, MILLISECONDS);

    return fuse;
  }

}
