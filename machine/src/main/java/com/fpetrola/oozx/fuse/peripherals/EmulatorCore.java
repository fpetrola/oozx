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

package com.fpetrola.oozx.fuse.peripherals;

import javax.swing.*;

public interface EmulatorCore {
    void startEmulation();
    void stopEmulation();
    void pauseEmulation();
    void resumeEmulation();
    void resetEmulation();
    void loadFile(String filePath);
    void saveState(String filePath);
    void loadState(String filePath);
    void setMachineModel(String model); // e.g., "48K", "128K"
    void setVideoOption(String option, Object value); // e.g., "border", true
    void setAudioOption(String option, Object value);
    void setInputOption(String option, Object value);
    void setStorageOption(String option, Object value);
    void setPeripheralOption(String option, Object value);
    void setGeneralOption(String option, Object value);
    // Add more as needed for peripherals, etc.
    void addEmulatorListener(EmulatorListener listener);
    double getEmulationSpeed(); // New: Get emulation speed
    String getCurrentModel(); // New: Get current machine model
    boolean isPaused(); // New: Check if paused
    boolean isTurboMode(); // New: Check if turbo mode is on
    String getTapeStatus(); // New: Get tape status
    JComponent getPanel();
}
