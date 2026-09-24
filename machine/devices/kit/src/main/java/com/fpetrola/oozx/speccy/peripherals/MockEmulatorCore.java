/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.EmulatorListener;

import javax.swing.*;
import java.awt.event.KeyListener;

// Mock implementation of the core
public class MockEmulatorCore implements EmulatorCore {
  private double emulationSpeed = 1.0;
  private String currentModel = "Spectrum 48K";
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

  /** Nothing is running behind this one, so there is nothing to write. */
  @Override
  public void saveState(String filePath) {
  }

  @Override
  public void loadState(String filePath) {
    System.out.println("Mock: Loading state from " + filePath);
  }

  /**
   * Become this machine. Asking for the one it was already asked for does nothing.
   * <p>
   * Becoming it takes a moment - the machine is built on its own thread - and while that is in
   * flight every box showing the model has already been set to the new name, which is
   * indistinguishable from somebody asking for it again. Each of those asks starts the change
   * over, and that is what made the model flicker back and forth on its way to the new one.
   */
  @Override
  public void setMachineModel(String model) {
    if (model == null || model.equals(currentModel)) {
      return;
    }
    currentModel = model;
    notifyModelChange(model);
  }

  @Override
  public void setVideoOption(String option, Object value) {
    setScreenOption(option, value);
  }

  /**
   * The knobs that belong to the panel rather than to the machine. Whoever asks knows it wants a
   * border or scan lines, not which component draws them, so both the video options and the
   * general ones arrive here.
   */
  private void setScreenOption(String option, Object value) {
    if (contentPane instanceof com.fpetrola.oozx.speccy.screen.SpeccyScreen screen) {
      switch (option) {
        case "border" -> screen.setBorderVisible((Boolean) value);
        case "tv" -> screen.setTvScreen(com.fpetrola.oozx.speccy.screen.TvScreen.byName(String.valueOf(value)));
        case "scanlines" -> screen.setScanLines((Boolean) value);
        // null is a real answer here and means what it has always meant: decide by the scale.
        case "smoothing" -> screen.setSmoothing((Boolean) value);
        default -> { }
      }
    }
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
    setScreenOption(option, value);
  }

  @Override
  public void addEmulatorListener(EmulatorListener listener) {
    listeners.add(listener);
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
  public java.util.List<String> getMachineModels() {
    return java.util.List.of(currentModel);
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

  protected void notifyStateChange(String state) {
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

  /**
   * The machine has become this; say so, without asking anybody to become anything.
   * <p>
   * setMachineModel is the other half and is a command: it is what a menu calls. Announcing
   * through it made the announcement mean "become this", which the machine already was.
   */
  public void announceMachine(String model) {
    currentModel = model;
    notifyModelChange(model);
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

  /**
   * Onto the event thread on the way in, because this is where the answer becomes a label and the
   * measuring is done on the emulator's thread.
   */
  @Override
  public void notifySpeed(float currentSpeed) {
    javax.swing.SwingUtilities.invokeLater(() -> notifyEmulationSpeedChange(currentSpeed));
  }
}
