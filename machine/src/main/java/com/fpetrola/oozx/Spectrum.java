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

import java.util.function.Supplier;

public class Spectrum {
  private Memory memory;
  private Display display;
  private EventManager eventManager;
  private Z80 z80;
  private TStatesHolder tStatesHolder;
  private RAMHolder ramHolder;
  private Supplier<FuseMachineInfo> fuseMachineInfoSupplier;

  public Spectrum(Memory memory, Display display, EventManager eventManager, Z80 z80, TStatesHolder tStatesHolder, RAMHolder ramHolder, Supplier<FuseMachineInfo> fuseMachineInfoSupplier) {
    this.memory = memory;
    this.display = display;
    this.eventManager = eventManager;
    this.z80 = z80;
    this.tStatesHolder = tStatesHolder;
    this.ramHolder = ramHolder;
    this.fuseMachineInfoSupplier = fuseMachineInfoSupplier;
  }

  interface PortFromUlaFunction {
    boolean apply(int port);
  }

  interface ContentionDelayFunction {
    int apply(long time);
  }

  private final int[] contentionPattern65432100 = {5, 4, 3, 2, 1, 0, 0, 6};
  private final int[] contentionPattern76543210 = {5, 4, 3, 2, 1, 0, 7, 6};

  public int spectrumFrameEvent;
  private long framesSinceReset;

  public void spectrumReset(int a) {
    framesSinceReset = 0;
  }

  public long getTstates() {
    return tStatesHolder.getTstates();
  }

  public void setTstates(long tstates) {
    tStatesHolder.setTstates(tstates);
  }

  private void spectrumFrameEventFn(long lastTstates, int type, Object userData) {
    if (Rzx.playback) eventManager.eventForceEvents();
    spectrumFrame();
    z80.interrupt();
  }

  private long getFrameCount() {
    return framesSinceReset;
  }

  int spectrumInit(Object context) {
    spectrumFrameEvent = eventManager.eventRegister(this::spectrumFrameEventFn, "End of frame");

    Module.register(new SpectrumModuleInfo(this));

    return 0;
  }

  public int spectrumFrame() {
    long frameLength = Rzx.playback ? tStatesHolder.getTstates() : getCurrent().timings.tstatesPerFrame;

    eventManager.eventFrame(frameLength);
    tStatesHolder.setTstates(tStatesHolder.getTstates() - frameLength);

    if (z80.interruptsEnabledAt >= 0) {
      z80.interruptsEnabledAt -= frameLength;
    }

    if (Sound.enabled) Sound.frame();

    if (display.frame() != 0) return 1;
    if (Profile.active) Profile.frame(frameLength);
    Printer.frame();

    if (!Rzx.playback) {
      eventManager.eventAdd(getCurrent().timings.tstatesPerFrame, spectrumFrameEvent);
    }

    Loader.frame(frameLength);
    PhantomTypist.frame();

    framesSinceReset++;

    return 0;
  }

  public int contendDelayNone(long time) {
    return 0;
  }

  private int contendDelayCommon(long time, int[] timings, int offset) {
    int line = (int) ((time - getCurrent().lineTimes[0]) / getCurrent().timings.tstatesPerLine);

    int tstatesThroughLine = (int) (time - getCurrent().lineTimes[0] +
        (getCurrent().timings.leftBorder - display.BORDER_WIDTH_COLS * 4));

    tstatesThroughLine %= getCurrent().timings.tstatesPerLine;

    if (line < display.BORDER_HEIGHT ||
        line >= display.BORDER_HEIGHT + display.HEIGHT) return 0;

    if (tstatesThroughLine < getCurrent().timings.leftBorder - offset) return 0;

    if (tstatesThroughLine >= getCurrent().timings.leftBorder +
        getCurrent().timings.horizontalScreen - offset) return 0;

    return timings[tstatesThroughLine % 8];
  }

  private FuseMachineInfo getCurrent() {
    return fuseMachineInfoSupplier.get();
  }

  public int contendDelay65432100(long time) {
    return contendDelayCommon(time, contentionPattern65432100, 1);
  }

  public int contendDelay76543210(long time) {
    return contendDelayCommon(time, contentionPattern76543210, 4);
  }

  public int spectrumUnattachedPort() {
    if (tStatesHolder.getTstates() < getCurrent().lineTimes[display.BORDER_HEIGHT]) return 0xff;

    int line = (int) ((tStatesHolder.getTstates() - getCurrent().lineTimes[display.BORDER_HEIGHT]) /
        getCurrent().timings.tstatesPerLine);

    if (line >= display.HEIGHT) return 0xff;

    int tstatesThroughLine = (int) (tStatesHolder.getTstates() -
        getCurrent().lineTimes[display.BORDER_HEIGHT + line] +
        (getCurrent().timings.leftBorder - display.BORDER_WIDTH_COLS * 4));

    if (tstatesThroughLine < getCurrent().timings.leftBorder) return 0xff;

    if (tstatesThroughLine >= getCurrent().timings.leftBorder +
        getCurrent().timings.horizontalScreen) return 0xff;

    int column = ((tstatesThroughLine - getCurrent().timings.leftBorder) / 8) * 2;

    switch (tstatesThroughLine % 8) {
      case 5:
        column++;
      case 3:
        return ramHolder.getRAM()[memory.currentScreen][display.attrStart[line] + column];

      case 4:
        column++;
      case 2:
        return ramHolder.getRAM()[memory.currentScreen][display.lineStart[line] + column];

      case 0:
      case 1:
      case 6:
      case 7:
        return 0xff;
    }

    return 0; // Keep compiler happy
  }

  public int spectrumUnattachedPortNone() {
    return 0xff;
  }
}