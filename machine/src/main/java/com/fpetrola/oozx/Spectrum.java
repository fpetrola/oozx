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

import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;

public class Spectrum {
  private static Memory memory = Fuse.memory;
  // RAM array: 65 pages of 16KB each (from SPECTRUM_RAM_PAGES)
  public static byte[][] RAM = new byte[memory.SPECTRUM_RAM_PAGES][0x4000];
  private static Display display = Fuse.display;
  private static EventManager eventManager = Fuse.eventManager;
  private static Machine machine = Fuse.machine;
  private static Z80 z80 = Fuse.z80;
  private static TStatesHolder tStatesHolder;

  public Spectrum(TStatesHolder tStatesHolder) {
    this.tStatesHolder = tStatesHolder;
  }

  // Functional interface for checking if a port is handled by the ULA
  @FunctionalInterface
  interface PortFromUlaFunction {
    boolean apply(int port);
  }

  // Functional interface for contention delay calculation
  @FunctionalInterface
  interface ContentionDelayFunction {
    int apply(long time);
  }

  // Instance of RamInfo
  public static RamInfo ramInfo = new RamInfo();


  // Contention patterns
  private static int[] contentionPattern65432100 = {5, 4, 3, 2, 1, 0, 0, 6};
  private static int[] contentionPattern76543210 = {5, 4, 3, 2, 1, 0, 7, 6};

  // Event
  public static int spectrumFrameEvent;

  // Debugger variable prefix
  private static final String DEBUGGER_TYPE_STRING = "spectrum";

  // Debugger variable for frame count
  private static final String FRAME_COUNT_NAME = "frames";

  // Count of frames since last reset
  private static long framesSinceReset;

  public static void spectrumReset(int a) {
    framesSinceReset = 0;
  }

  public long getTstates() {
    return tStatesHolder.getTstates();
  }

  public void setTstates(long tstates) {
    tStatesHolder.setTstates(tstates);
  }

  private static void spectrumFrameEventFn(long lastTstates, int type, Object userData) {
    if (Rzx.playback) eventManager.eventForceEvents();
    Rzx.frame();
    Psg.frame();
    spectrumFrame();
    z80.interrupt();
    UiJoystick.poll();
    Timer.estimateSpeed();
    Ui.event();
    Ui.errorFrame();
  }

  private static long getFrameCount() {
    return framesSinceReset;
  }

  static int spectrumInit(Object context) {
    spectrumFrameEvent = eventManager.eventRegister(Spectrum::spectrumFrameEventFn, "End of frame");

    Module.register(new SpectrumModuleInfo());

    return 0;
  }

  //    private static void reg1() {
//        StartupManagerModule[] dependencies = {
//            StartupManagerModule.DEBUGGER,
//            StartupManagerModule.EVENT,
//            StartupManagerModule.SETUID
//        };
//        StartupManager.register(StartupManagerModule.SPECTRUM, dependencies, Spectrum::spectrumInit, null, null);
//    }

  public static int spectrumFrame() {
    long frameLength = Rzx.playback ? tStatesHolder.getTstates() : machine.current.timings.tstatesPerFrame;

    eventManager.eventFrame(frameLength);
    tStatesHolder.setTstates(tStatesHolder.getTstates()- frameLength);

    if (z80.interruptsEnabledAt >= 0) {
      z80.interruptsEnabledAt -= frameLength;
    }

    if (Sound.enabled) Sound.frame();

    if (display.frame() != 0) return 1;
    if (Profile.active) Profile.frame(frameLength);
    Printer.frame();

    if (!Rzx.playback) {
      eventManager.eventAdd(machine.current.timings.tstatesPerFrame, spectrumFrameEvent);
    }

    Loader.frame(frameLength);
    PhantomTypist.frame();

    framesSinceReset++;

    return 0;
  }

  public static int contendDelayNone(long time) {
    return 0;
  }

  private static int contendDelayCommon(long time, int[] timings, int offset) {
    int line = (int) ((time - machine.current.lineTimes[0]) / machine.current.timings.tstatesPerLine);

    int tstatesThroughLine = (int) (time - machine.current.lineTimes[0] +
        (machine.current.timings.leftBorder - display.BORDER_WIDTH_COLS * 4));

    tstatesThroughLine %= machine.current.timings.tstatesPerLine;

    if (line < display.BORDER_HEIGHT ||
        line >= display.BORDER_HEIGHT + display.HEIGHT) return 0;

    if (tstatesThroughLine < machine.current.timings.leftBorder - offset) return 0;

    if (tstatesThroughLine >= machine.current.timings.leftBorder +
        machine.current.timings.horizontalScreen - offset) return 0;

    return timings[tstatesThroughLine % 8];
  }

  public static int contendDelay65432100(long time) {
    return contendDelayCommon(time, contentionPattern65432100, 1);
  }

  public static int contendDelay76543210(long time) {
    return contendDelayCommon(time, contentionPattern76543210, 4);
  }

  public static int spectrumUnattachedPort() {
    if (tStatesHolder.getTstates() < machine.current.lineTimes[display.BORDER_HEIGHT]) return 0xff;

    int line = (int) ((tStatesHolder.getTstates() - machine.current.lineTimes[display.BORDER_HEIGHT]) /
        machine.current.timings.tstatesPerLine);

    if (line >= display.HEIGHT) return 0xff;

    int tstatesThroughLine = (int) (tStatesHolder.getTstates() -
        machine.current.lineTimes[display.BORDER_HEIGHT + line] +
        (machine.current.timings.leftBorder - display.BORDER_WIDTH_COLS * 4));

    if (tstatesThroughLine < machine.current.timings.leftBorder) return 0xff;

    if (tstatesThroughLine >= machine.current.timings.leftBorder +
        machine.current.timings.horizontalScreen) return 0xff;

    int column = ((tstatesThroughLine - machine.current.timings.leftBorder) / 8) * 2;

    switch (tstatesThroughLine % 8) {
      case 5:
        column++;
      case 3:
        return RAM[memory.currentScreen][display.attrStart[line] + column];

      case 4:
        column++;
      case 2:
        return RAM[memory.currentScreen][display.lineStart[line] + column];

      case 0:
      case 1:
      case 6:
      case 7:
        return 0xff;
    }

    return 0; // Keep compiler happy
  }

  public static int spectrumUnattachedPortNone() {
    return 0xff;
  }
}