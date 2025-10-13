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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.modules.Joystick;

public class Libspectrum {
  public enum LibspectrumJoystick {
    LIBSPECTRUM_JOYSTICK_CURSOR,
    LIBSPECTRUM_JOYSTICK_KEMPSTON,
    LIBSPECTRUM_JOYSTICK_SINCLAIR_1,
    LIBSPECTRUM_JOYSTICK_SINCLAIR_2,
    LIBSPECTRUM_JOYSTICK_TIMEX_1,
    LIBSPECTRUM_JOYSTICK_TIMEX_2,
    LIBSPECTRUM_JOYSTICK_FULLER
  }

  public static final int LIBSPECTRUM_JOYSTICK_INPUT_NONE = 0;
  public static final int LIBSPECTRUM_JOYSTICK_INPUT_KEYBOARD = 1;
  public static final int LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_1 = 2;
  public static final int LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_2 = 3;

  public static String joystickName(LibspectrumJoystick joystick) {
    return joystick.toString();
  }

  public static Object errorFunction;

  public static void snapSetOut128Memoryport(Snap snap, byte lastByte) {
  }

  public static void snapSetOutPlus3Memoryport(Snap snap, byte lastByte2) {
  }

  public static void snapSetPages(Snap snap, int i, byte[] buffer) {
  }

  public static void end() {
  }

  public static Object version() {
    return null;
  }

  public static int init() {
    return 0;
  }
  public enum Machine {
    _48K,
    _128K,
    PLUS2,
    PLUS3
  }
  public static MachineCapability MachineCapability;

  public static byte snapOutUla(Snap snap) {
    return 0;
  }

  public static long snapTstates(Snap snap) {
    return 0;
  }

  public static boolean snapIssue2(Snap snap) {
    return false;
  }

  public static void snapSetOutUla(Snap snap, byte lastByte) {

  }

  public static void snapSetTstates(Snap snap, long tstates) {

  }

  public static void snapSetIssue2(Snap snap, boolean issue2) {

  }

  public static byte snapOut128Memoryport(Snap snap) {
    return 0;
  }

  public static byte snapOutPlus3Memoryport(Snap snap) {
    return 0;
  }

  public static byte[] snapPages(Snap snap, int i) {
    return null;
  }

  public static boolean snapCustomRom(Snap snap) {
    return false;
  }

  public static int snapCustomRomPages(Snap snap) {
    return 0;
  }

  public static byte[] snapRoms(Snap snap, int i) {
    return null;
  }

  public static int snapRomLength(Snap snap, int i) {
    return 0;
  }

  public static void snapSetCustomRom(Snap snap, boolean b) {

  }

  public static void snapSetRoms(Snap snap, int currentRomNum, byte[] currentRom) {

  }

  public static void snapSetRomLength(Snap snap, int currentRomNum, int romLength) {

  }

  public static void snapSetCustomRomPages(Snap snap, int currentRomNum) {

  }

  public static int machineCapabilities(SpectrumMachine machine) {
    return 0;
  }

  public class Snap {
    public int joystickActiveCount() {
      return 0;
    }

    public Joystick.JoystickType joystickList(int i) {
      return null;
    }

    public int joystickInputs(int i) {
      return 0;
    }

    public void setJoystickInputs(int i, int i1) {

    }

    public void setJoystickList(int numJoysticks, Joystick.JoystickType libspectrumType) {

    }

    public void setJoystickActiveCount(int i) {

    }
  }

  public class Error extends RuntimeException {
  }
}
