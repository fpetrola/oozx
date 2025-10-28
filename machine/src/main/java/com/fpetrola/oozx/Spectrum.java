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

import com.fpetrola.oozx.fuse.machine.MachineTimings;
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
  private Module module;

  public Spectrum(Memory memory, Display display, EventManager eventManager, Z80 z80, Z80Clock z80Clock, RAMHolder ramHolder, Supplier<SpectrumMachine> fuseMachineInfoSupplier, Timer timer, Module module) {
    this.memory = memory;
    this.display = display;
    this.eventManager = eventManager;
    this.z80 = z80;
    this.z80Clock = z80Clock;
    this.ramHolder = ramHolder;
    this.fuseMachineInfoSupplier = fuseMachineInfoSupplier;
    this.timer = timer;
    this.module = module;
  }

  public void spectrumReset(int a) {
    framesSinceReset = 0;
  }

  private void spectrumFrameEventFn(long lastTstates, int type, Object userData) {
    spectrumFrame();
    z80.interrupt();
    timer.estimateSpeed(z80);
  }

  private long getFrameCount() {
    return framesSinceReset;
  }

  public int init(Object context) {
    spectrumFrameEvent = eventManager.eventRegister(this::spectrumFrameEventFn, "End of frame");

    module.register(new SpectrumModuleInfo(this));

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
    SpectrumMachine spectrumMachine = getCurrent();

    int line = (int) ((time - spectrumMachine.getLineTimes()[0]) / spectrumMachine.getTimings().tstatesPerLine);

    int tstatesThroughLine = (int) (time - spectrumMachine.getLineTimes()[0] +
        (spectrumMachine.getTimings().leftBorder - display.BORDER_WIDTH_COLS * 4));

    tstatesThroughLine %= spectrumMachine.getTimings().tstatesPerLine;

    if (line < display.BORDER_HEIGHT ||
        line >= display.BORDER_HEIGHT + display.HEIGHT) return 0;

    if (tstatesThroughLine < spectrumMachine.getTimings().leftBorder - offset) return 0;

    if (tstatesThroughLine >= spectrumMachine.getTimings().leftBorder +
        spectrumMachine.getTimings().horizontalScreen - offset) return 0;

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
    SpectrumMachine spectrumMachine = getCurrent();
    MachineTimings timings = spectrumMachine.getTimings();
    long[] lineTimes = spectrumMachine.getLineTimes();

    if (z80Clock.getTStates() < lineTimes[display.BORDER_HEIGHT]) return 0xff;
    int line = (int) ((z80Clock.getTStates() - lineTimes[display.BORDER_HEIGHT]) / timings.tstatesPerLine);
    if (line >= display.HEIGHT) return 0xff;
    int tstatesThroughLine = (int) (z80Clock.getTStates() - lineTimes[display.BORDER_HEIGHT + line] + (timings.leftBorder - display.BORDER_WIDTH_COLS * 4));
    if (tstatesThroughLine < timings.leftBorder) return 0xff;
    if (tstatesThroughLine >= timings.leftBorder + timings.horizontalScreen) return 0xff;
    int column = ((tstatesThroughLine - timings.leftBorder) / 8) * 2;
    byte[] bytes = ramHolder.getRAM()[memory.currentScreen];

    switch (tstatesThroughLine % 8) {
      case 5:
        column++;
      case 3:
        return bytes[display.attrStart[line] + column];
      case 4:
        column++;
      case 2:
        return bytes[display.lineStart[line] + column];
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


  private static byte lastFloatingBusAmstradValue = (byte) 0xFF;

  /**
   * Implementa el comportamiento del "floating bus" en máquinas Amstrad (+2A, +3)
   * Referencias:
   *   http://sky.relative-path.com/zx/floating_bus.html
   *   https://sinclair.wiki.zxnet.co.uk/wiki/Floating_bus
   *   Ramsoft Floating Bus Technical Guide
   *
   * Probado con:
   *   - A Yankee in Irak 1.3.3
   *   - Mr. Kung Fu 1.3 (+2A/+3)
   *   - MONJAS 1.6 (ES/EN)
   *   - Hell Yeah! v210131
   *   - Sidewize (+2A/+3 fix)
   */
  public byte unattachedPortAmstrad(int port) {
    SpectrumMachine spectrumMachine = getCurrent();

    int game = 1; // 1 = modo "juego" (devuelve 0xFF en idle), 0 = modo "emulación precisa"
    int line;
    long tstatesThroughLine, column;

    // Check port pattern: 1 + (4 * n)
    if (port <= 1 || port > 0x1000 || ((port - 1) & 3) != 0) {
      lastFloatingBusAmstradValue = (byte) 0xFF;
      return (byte) 0xFF;
    }

    long tstates = z80Clock.getTStates();

    // Top border?
    if (tstates < spectrumMachine.getLineTimes()[display.BORDER_HEIGHT]) {
      lastFloatingBusAmstradValue = (byte) 0xFF;
      return (byte) 0xFF;
    }

    // Linea relativa al inicio de pantalla
    line = (int) ((tstates - spectrumMachine.getLineTimes()[display.BORDER_HEIGHT]) /
        spectrumMachine.getTimings().tstatesPerLine);

    // Lower border?
    if (line >= display.HEIGHT) {
      lastFloatingBusAmstradValue = (byte) 0xFF;
      return game == 1 ? (byte) 0xFF : lastFloatingBusAmstradValue;
    }

    // Posición dentro de la línea
    tstatesThroughLine = tstates -
        spectrumMachine.getLineTimes()[(int) (display.BORDER_HEIGHT + line)] +
        (spectrumMachine.getTimings().leftBorder - display.BORDER_WIDTH_COLS * 4);

    // Left border?
    if (tstatesThroughLine < spectrumMachine.getTimings().leftBorder) {
      return game == 1 ? (byte) 0xFF : lastFloatingBusAmstradValue;
    }

    // Right border o retrace?
    if (tstatesThroughLine >= spectrumMachine.getTimings().leftBorder + spectrumMachine.getTimings().horizontalScreen) {
      return game == 1 ? (byte) 0xFF : lastFloatingBusAmstradValue;
    }

    // Columna en pantalla (2 bytes por celda)
    column = ((tstatesThroughLine - spectrumMachine.getTimings().leftBorder) / 8) * 2;

    int screen = memory.currentScreen;
    int[] displayLineStart = display.lineStart;
    int[] displayAttrStart = display.attrStart;

    switch ((int) ((tstatesThroughLine - game) % 8)) {
      case 5:
        column++; // Attribute byte
      case 3:
        lastFloatingBusAmstradValue = (byte) (ramHolder.getRAM()[screen][(int) (displayAttrStart[line] + column)] | 0x01);
        return lastFloatingBusAmstradValue;

      case 4:
        column++; // Screen data
      case 2:
        lastFloatingBusAmstradValue = (byte) (ramHolder.getRAM()[screen][(int) (displayLineStart[line] + column)] | 0x01);
        return lastFloatingBusAmstradValue;

      case 0:
      case 1:
      case 6:
      case 7: // Idle bus
        return game == 1 ? (byte) 0xFF : lastFloatingBusAmstradValue;

      default:
        return game == 1 ? (byte) 0xFF : lastFloatingBusAmstradValue;
    }
  }

  // Getter para pruebas unitarias
  public static byte getLastFloatingBusAmstradValue() {
    return lastFloatingBusAmstradValue;
  }

  public static void resetLastFloatingBus() {
    lastFloatingBusAmstradValue = (byte) 0xFF;
  }
}