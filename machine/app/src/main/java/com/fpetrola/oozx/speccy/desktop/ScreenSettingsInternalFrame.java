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
package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.speccy.screen.ScreenSettings;

import javax.swing.JInternalFrame;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A window on one machine's screen settings, open while the game is played: what it shows is
 * {@link ScreenSettingsPanel}, which is also what the Video tab of the settings shows. The window
 * is the frame around it and nothing else.
 */
public class ScreenSettingsInternalFrame extends JInternalFrame {

  public ScreenSettingsInternalFrame(String machineName, ScreenSettings settings,
                                     Consumer<Map<String, String>> keepAsDefault,
                                     Runnable profilesChanged) {
    super("Screen - " + machineName, true, true, true, true);
    setLayout(new BorderLayout());
    add(new ScreenSettingsPanel(settings, keepAsDefault, profilesChanged), BorderLayout.CENTER);
    setSize(430, 560);
  }
}
