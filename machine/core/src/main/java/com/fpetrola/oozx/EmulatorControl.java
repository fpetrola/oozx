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

package com.fpetrola.oozx;


/**
 * Driving a machine: start it, pause it, load something into it, ask it what it is.
 * <p>
 * Everything here can be said without a screen, which is the point of it being apart from
 * {@link EmulatorCore}: the emulator carries one of these around - the timer hands it to whoever
 * is showing the speed - and carrying it must not mean depending on a window toolkit. What a
 * window additionally needs of a machine, a picture and somewhere to send keys, is over there.
 */
public interface EmulatorControl {
  void startEmulation();

  void stopEmulation();

  void pauseEmulation();

  void resumeEmulation();

  void resetEmulation();

  void loadFile(String filePath);

  void saveState(String filePath);

  void loadState(String filePath);

  void setMachineModel(String model); // "48K", "128K"

  void setVideoOption(String option, Object value); // "border", true

  void setAudioOption(String option, Object value);

  void setInputOption(String option, Object value);

  void setStorageOption(String option, Object value);

  void setPeripheralOption(String option, Object value);

  void setGeneralOption(String option, Object value);

  void addEmulatorListener(EmulatorListener listener);

  double getEmulationSpeed();

  /**
   * How fast the machine is actually running, said as it is measured.
   */
  default void notifySpeed(float currentSpeed) {
  }

  String getCurrentModel();

  /** The implementations of the processor this build has, the one this machine runs on, and moving it to another. */
  default java.util.List<String> getProcessors() {
    return java.util.List.of();
  }

  default String getProcessor() {
    return "";
  }

  default void setProcessor(String processor) {
  }

  java.util.List<String> getMachineModels();

  boolean isPaused();

  boolean isTurboMode();

  String getTapeStatus();

  void finishEmulation();

  /** The machine as it stands, packed into text, or null where there is no machine behind this. */
  default String packedState() {
    return null;
  }

  default boolean isMuted() {
    return false;
  }

  /** In percent. */
  default int getVolume() {
    return 100;
  }

  default String getFilename() {
    return "";
  }

  default void setFilename(String string) {
  }
}
