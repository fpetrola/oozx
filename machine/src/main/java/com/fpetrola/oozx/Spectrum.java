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
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.function.Supplier;

public class Spectrum implements ZxModule {
  private Memory memory;
  private Display display;
  private EventManager eventManager;
  private Z80 z80;
  private Z80Clock z80Clock;
  private RAMHolder ramHolder;
  private Supplier<SpectrumMachine> fuseMachineInfoSupplier;

  private final int[] contentionPattern65432100 = {5, 4, 3, 2, 1, 0, 0, 6};
  private final int[] contentionPattern76543210 = {5, 4, 3, 2, 1, 0, 7, 6};

  public int spectrumFrameEvent;
  private long framesSinceReset;
  private Timer timer;

  public Spectrum(Memory memory, Display display, EventManager eventManager, Z80 z80, Z80Clock z80Clock, RAMHolder ramHolder, Supplier<SpectrumMachine> fuseMachineInfoSupplier, Timer timer) {
    this.memory = memory;
    this.display = display;
    this.eventManager = eventManager;
    this.z80 = z80;
    this.z80Clock = z80Clock;
    this.ramHolder = ramHolder;
    this.fuseMachineInfoSupplier = fuseMachineInfoSupplier;
    this.timer = timer;
  }

  public void spectrumReset(int a) {
    framesSinceReset = 0;
  }

  private void spectrumFrameEventFn(long lastTstates, int type, Object userData) {
    spectrumFrame();
    z80.interrupt();
    timer.estimateSpeed();
  }

  private long getFrameCount() {
    return framesSinceReset;
  }

  public int init(Object context) {
    spectrumFrameEvent = eventManager.eventRegister(this::spectrumFrameEventFn, "End of frame");

    Module.register(new SpectrumModuleInfo(this));

    return 0;
  }

  @Override
  public void end() {

  }

  public int spectrumFrame() {
    int frameLength = getCurrent().getTimings().tstatesPerFrame;

    eventManager.eventFrame(frameLength);
    z80Clock.addTStates(-frameLength);

    if (z80.interruptsEnabledAt >= 0) {
      z80.interruptsEnabledAt -= frameLength;
    }

    if (Sound.enabled) Sound.frame();

    if (display.frame() != 0) return 1;

    eventManager.eventAdd(getCurrent().getTimings().tstatesPerFrame, spectrumFrameEvent);

    PhantomTypist.frame();

    framesSinceReset++;

    return 0;
  }

  public int contendDelayNone(long time) {
    return 0;
  }

  private int contendDelayCommon(long time, int[] timings, int offset) {
    int line = (int) ((time - getCurrent().getLineTimes()[0]) / getCurrent().getTimings().tstatesPerLine);

    int tstatesThroughLine = (int) (time - getCurrent().getLineTimes()[0] +
        (getCurrent().getTimings().leftBorder - display.BORDER_WIDTH_COLS * 4));

    tstatesThroughLine %= getCurrent().getTimings().tstatesPerLine;

    if (line < display.BORDER_HEIGHT ||
        line >= display.BORDER_HEIGHT + display.HEIGHT) return 0;

    if (tstatesThroughLine < getCurrent().getTimings().leftBorder - offset) return 0;

    if (tstatesThroughLine >= getCurrent().getTimings().leftBorder +
        getCurrent().getTimings().horizontalScreen - offset) return 0;

    return timings[tstatesThroughLine % 8];
  }

  private SpectrumMachine getCurrent() {
    return fuseMachineInfoSupplier.get();
  }

  public int contendDelay65432100(long time) {
    return contendDelayCommon(time, contentionPattern65432100, 1);
  }

  public int contendDelay76543210(long time) {
    return contendDelayCommon(time, contentionPattern76543210, 4);
  }

  public int spectrumUnattachedPort() {
    if (z80Clock.getTStates() < getCurrent().getLineTimes()[display.BORDER_HEIGHT]) return 0xff;

    int line = (int) ((z80Clock.getTStates() - getCurrent().getLineTimes()[display.BORDER_HEIGHT]) /
        getCurrent().getTimings().tstatesPerLine);

    if (line >= display.HEIGHT) return 0xff;

    int tstatesThroughLine = (int) (z80Clock.getTStates() -
        getCurrent().getLineTimes()[display.BORDER_HEIGHT + line] +
        (getCurrent().getTimings().leftBorder - display.BORDER_WIDTH_COLS * 4));

    if (tstatesThroughLine < getCurrent().getTimings().leftBorder) return 0xff;

    if (tstatesThroughLine >= getCurrent().getTimings().leftBorder +
        getCurrent().getTimings().horizontalScreen) return 0xff;

    int column = ((tstatesThroughLine - getCurrent().getTimings().leftBorder) / 8) * 2;

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