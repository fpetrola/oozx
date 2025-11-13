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

import com.fpetrola.oozx.fuse.SwingKeyboard;

import javax.swing.*;
import java.awt.event.KeyListener;

// Mock implementation of the core
public class MockEmulatorCore implements EmulatorCore {
  private double emulationSpeed = 1.0;
  private String currentModel = "48K";
  private boolean paused = false;
  private boolean turboMode = true;
  private String tapeStatus = "Stopped";

  private  JComponent contentPane;

  public MockEmulatorCore(JComponent contentPane) {
    this.contentPane = contentPane;
  }

  @Override
  public void startEmulation() {
    System.out.println("Mock: Starting emulation");
    notifyStateChange("Running");
  }

  @Override
  public void stopEmulation() {
    System.out.println("Mock: Stopping emulation");
    notifyStateChange("Stopped");
  }

  @Override
  public void pauseEmulation() {
    System.out.println("Mock: Pausing emulation");
    paused = true;
    notifyPauseStateChange(true);
  }

  @Override
  public void resumeEmulation() {
    System.out.println("Mock: Resuming emulation");
    paused = false;
    notifyPauseStateChange(false);
  }

  @Override
  public void resetEmulation() {
    System.out.println("Mock: Resetting emulation");
    notifyStateChange("Reset");
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
    currentModel = model;
    notifyModelChange(model);
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
    if (option.equals("tape")) {
      tapeStatus = "Loaded";
      notifyTapeStatusChange(tapeStatus);
    }
  }

  @Override
  public void setPeripheralOption(String option, Object value) {
    System.out.println("Mock: Setting peripheral option " + option + " to " + value);
  }

  @Override
  public void setGeneralOption(String option, Object value) {
    System.out.println("Mock: Setting general option " + option + " to " + value);
    if (option.equals("turbo")) {
      turboMode = (Boolean) value;
      emulationSpeed = turboMode ? 2.0 : 1.0;
      notifyTurboModeChange(turboMode);
      notifyEmulationSpeedChange(emulationSpeed);
    }
  }

  @Override
  public void addEmulatorListener(EmulatorListener listener) {
    listeners.add(listener);
//    // Mock: Simulate initial state
//    listener.onEmulationStateChanged("Ready");
//    listener.onEmulationSpeedChanged(emulationSpeed);
//    listener.onModelChanged(currentModel);
//    listener.onPauseStateChanged(paused);
//    listener.onTurboModeChanged(turboMode);
//    listener.onTapeStatusChanged(tapeStatus);
  }

  @Override
  public double getEmulationSpeed() {
    return emulationSpeed;
  }

  @Override
  public String getCurrentModel() {
    return currentModel;
  }

  @Override
  public boolean isPaused() {
    return paused;
  }

  @Override
  public boolean isTurboMode() {
    return turboMode;
  }

  @Override
  public String getTapeStatus() {
    return tapeStatus;
  }

  private void notifyStateChange(String state) {
    // Simulate listener notification
    for (EmulatorListener listener : listeners) {
      listener.onEmulationStateChanged(state);
    }
  }

  public void notifyEmulationSpeedChange(double speed) {
    for (EmulatorListener listener : listeners) {
      listener.onEmulationSpeedChanged(speed);
    }
  }

  private void notifyModelChange(String model) {
      for (EmulatorListener listener : listeners) {
        listener.onModelChanged(model);
      }

  }

  public void notifyPauseStateChange(boolean paused) {
    for (EmulatorListener listener : listeners) {
      listener.onPauseStateChanged(paused);
    }
  }

  protected void notifyTurboModeChange(boolean turbo) {
    for (EmulatorListener listener : listeners) {
      listener.onTurboModeChanged(turbo);
    }
  }

  private void notifyTapeStatusChange(String status) {
    for (EmulatorListener listener : listeners) {
      listener.onTapeStatusChanged(status);
    }
  }

  private java.util.List<EmulatorListener> listeners = new java.util.ArrayList<>();

  @Override
  public JComponent getPanel() {
    return contentPane;
  }

  @Override
  public KeyListener getKeyListener() {
    return null;
  }

  @Override
  public void finishEmulation() {

  }
}
