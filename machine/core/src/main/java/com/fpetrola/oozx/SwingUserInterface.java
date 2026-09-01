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

package com.fpetrola.oozx;

import com.fpetrola.oozx.speccy.peripherals.EmulatorControl;
import com.fpetrola.oozx.speccy.peripherals.MockEmulatorCore;
import com.google.inject.Singleton;

import javax.swing.SwingUtilities;

/**
 * The desktop's answer. Only the speed readout is wired for now; the rest is inherited from
 * {@link NullUserInterface} because the static class this replaced left it unimplemented too —
 * the difference is that the gaps are now visible as methods nobody overrode.
 */
@Singleton
public class SwingUserInterface extends NullUserInterface {

  @Override
  public void statusbarUpdateSpeed(float currentSpeed, EmulatorControl core) {
    SwingUtilities.invokeLater(() -> {
      if (core != null) ((MockEmulatorCore) core).notifyEmulationSpeedChange(currentSpeed);
    });
  }
}
