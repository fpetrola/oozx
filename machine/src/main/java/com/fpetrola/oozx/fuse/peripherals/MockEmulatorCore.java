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

// Mock implementation of the core
public class MockEmulatorCore implements EmulatorCore {
  private  JComponent contentPane;

  public MockEmulatorCore(JComponent contentPane) {
    this.contentPane = contentPane;
  }

    @Override
    public void startEmulation() {
        System.out.println("Mock: Starting emulation");
    }

    @Override
    public void stopEmulation() {
        System.out.println("Mock: Stopping emulation");
    }

    @Override
    public void pauseEmulation() {
        System.out.println("Mock: Pausing emulation");
    }

    @Override
    public void resumeEmulation() {
        System.out.println("Mock: Resuming emulation");
    }

    @Override
    public void resetEmulation() {
        System.out.println("Mock: Resetting emulation");
    }

    @Override
    public void loadFile(String filePath) {
        System.out.println("Mock: Loading file " + filePath);
    }

    @Override
    public void saveState(String filePath) {
        System.out.println("Mock: Saving state to " + filePath);
    }

    @Override
    public void loadState(String filePath) {
        System.out.println("Mock: Loading state from " + filePath);
    }

    @Override
    public void setMachineModel(String model) {
        System.out.println("Mock: Setting machine model to " + model);
    }

    @Override
    public void setVideoOption(String option, Object value) {
        System.out.println("Mock: Setting video option " + option + " to " + value);
    }

    @Override
    public void setAudioOption(String option, Object value) {
        System.out.println("Mock: Setting audio option " + option + " to " + value);
    }

    @Override
    public void setInputOption(String option, Object value) {
        System.out.println("Mock: Setting input option " + option + " to " + value);
    }

    @Override
    public void setStorageOption(String option, Object value) {
        System.out.println("Mock: Setting storage option " + option + " to " + value);
    }

    @Override
    public void setPeripheralOption(String option, Object value) {
        System.out.println("Mock: Setting peripheral option " + option + " to " + value);
    }

    @Override
    public void setGeneralOption(String option, Object value) {
        System.out.println("Mock: Setting general option " + option + " to " + value);
    }

    @Override
    public void addEmulatorListener(EmulatorListener listener) {
        // Mock: Simulate state change
        listener.onEmulationStateChanged("Running");
    }

  @Override
  public JComponent getPanel() {
    return contentPane;
  }
}
