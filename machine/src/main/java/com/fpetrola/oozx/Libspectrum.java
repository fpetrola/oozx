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

public class Libspectrum {
  public static Object errorFunction;

  public static void snapSetOut128Memoryport(Snap snap, byte lastByte) {
  }

  public static void snapSetOutPlus3Memoryport(Snap snap, byte lastByte2) {
  }

  public static void snapSetPages(Snap snap, int i, byte[] buffer) {
  }

  public static void end() {
  }

  public static void creatorFree(LibspectrumCreator creator) {

  }

  public static int creatorSetCustom(LibspectrumCreator creator, byte[] bytes, int length) {
    return 0;
  }

  public static Object version() {
    return null;
  }

  public static String gcryptVersion() {
    return null;
  }

  public static int creatorSetMinor(LibspectrumCreator creator, int i) {
    return 0;
  }

  public static int creatorSetMajor(LibspectrumCreator creator, int i) {
    return 0;
  }

  public static int creatorSetProgram(LibspectrumCreator creator, String fuse) {
    return 0;
  }

  public static LibspectrumCreator creatorAlloc() {
    return null;
  }

  public static boolean checkVersion(String libspectrumMinVersion) {
    return false;
  }

  public static int init() {
    return 0;
  }

  enum Machine {
    _48,
    // Other machine types as needed
    UNKNOWN, _128, PLUS2;
  }
  public static MachineCapability MachineCapability;

  public static long timingsProcessorSpeed(int machine) {
    return 0;
  }

  public static int timingsLeftBorder(int machine) {
    return machine;
  }

  public static int timingsHorizontalScreen(int machine) {
    return 0;
  }

  public static int timingsRightBorder(int machine) {
    return 0;
  }

  public static int timingsTstatesPerLine(int machine) {
    return 0;
  }

  public static int timingsInterruptLength(int machine) {
    return 0;
  }

  public static long timingsTstatesPerFrame(int machine) {
    return 0;
  }

  public static long timingsTopLeftPixel(int machine) {
    return 0;
  }

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

  public static int machineCapabilities(Machine machine) {
    return 0;
  }

  public class Snap {
  }

  public class Error extends RuntimeException {
  }
}
