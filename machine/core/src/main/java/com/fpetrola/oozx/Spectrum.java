/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.machine.AbstractSpectrumMachine;
import com.fpetrola.oozx.speccy.machine.MachineTimings;
import com.fpetrola.oozx.speccy.machine.RamInfo;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.*;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.PeripheralBus;
import com.fpetrola.z80.cpu.Z80Clock;


public abstract class Spectrum extends AbstractSpectrumMachine implements ZxModule {
  protected final Memory memory;
  protected final Display display;
  protected final PeripheralBus peripherals;
  private final EventManager eventManager;
  private final Cpu cpu;
  protected final Z80Clock z80Clock;

  private final int[] contentionPattern65432100 = {5, 4, 3, 2, 1, 0, 0, 6};
  private final int[] contentionPattern76543210 = {5, 4, 3, 2, 1, 0, 7, 6};

  public int spectrumFrameEvent = -1;

  private long frames;
  private final Timer timer;
  private final Module module;
  private final int[][] ram;
  protected Sound sound;

  private final UserInterface userInterface;

  public Spectrum(Memory memory, Display display, EventManager eventManager, Cpu cpu, Timer timer, Module module, Settings settings1, RamInfo ramInfo1, PeripheralBusDelegate peripherals, Sound sound, UserInterface userInterface) {
    super(display, settings1, ramInfo1);
    this.userInterface = userInterface;
    this.memory = memory;
    this.display = display;
    this.eventManager = eventManager;
    this.cpu = cpu;
    this.z80Clock = cpu.getClock();
    this.timer = timer;
    this.module = module;
    this.ram = memory.getRAM();
    this.peripherals = peripherals;
    this.sound = sound;
  }

  public void loadRomBank(MemoryPage[] bankMap, int pageNum, String filename, String fallback, int expectedLength) {
    boolean custom = fallback != null && !filename.equals(fallback);
    try {
      memory.loadRomBank(bankMap, pageNum, filename, expectedLength, custom);
    } catch (RomNotLoadedException e) {
      // The settings named a ROM this machine does not have; fall back to the one it shipped
      // with. If that is missing too there is nothing left to try, so it goes up.
      if (fallback == null || !custom) throw e;
      memory.loadRomBank(bankMap, pageNum, fallback, expectedLength, false);
    }
  }

  public void loadRom(int pageNum, String filename, String fallback, int expectedLength) {
    loadRomBank(memory.mapRom, pageNum, filename, fallback, expectedLength);
  }

  private void spectrumFrameEventFn(long lastTstates, int type, Object userData) {
    spectrumFrame();
    cpu.interrupt();
  }

  public long frameCount() {
    return frames;
  }

  /** This machine's own end of frame, registered the first time anyone needs it. */
  public int frameEvent() {
    if (spectrumFrameEvent == -1) {
      spectrumFrameEvent = eventManager.eventRegister(this::spectrumFrameEventFn, "End of frame");
    }
    return spectrumFrameEvent;
  }

  public void start() {
    frameEvent();

    module.register(this);

    return;
  }

  @Override
  public void end() {

  }

  public void spectrumFrame() {
    int frameLength = getTimings().tstatesPerFrame;

    eventManager.eventFrame(frameLength);
    z80Clock.addTStates(-frameLength);

    cpu.rebaseInterruptWindow(frameLength);

    if (presentFrame() != 0) return;

    eventManager.eventAdd(getTimings().tstatesPerFrame, spectrumFrameEvent);

    PhantomTypist.frame();

    frames++;
  }

  /** What a frame of the clock shows: the sound, how fast it is going, and the picture. */
  public int presentFrame() {
    if (sound.soundEnabled)
      sound.frame();
    timer.estimateSpeed(cpu);
    return display.frame();
  }

  public int contendDelayNone(long time) {
    return 0;
  }

  private int contendDelayCommon(long time, int[] timingsPattern, int offset) {

    int line = (int) ((time - lineTimes[0]) / timings.tstatesPerLine);

    int tstatesThroughLine = (int) (time - lineTimes[0] + (timings.leftBorder - display.BORDER_WIDTH_COLS * 4));

    tstatesThroughLine %= timings.tstatesPerLine;

    if (line < display.BORDER_HEIGHT
        || line >= display.BORDER_HEIGHT + display.HEIGHT
        || tstatesThroughLine < timings.leftBorder - offset
        || tstatesThroughLine >= timings.leftBorder + timings.horizontalScreen - offset)
      return 0;

    return timingsPattern[tstatesThroughLine % 8];
  }

  public int contendDelay65432100(long time) {
    return contendDelayCommon(time, contentionPattern65432100, 1);
  }

  public int contendDelay76543210(long time) {
    return contendDelayCommon(time, contentionPattern76543210, 4);
  }

  public int spectrumUnattachedPort() {
    SpectrumMachine spectrumMachine = this;
    MachineTimings timings = spectrumMachine.getTimings();
    long[] lineTimes = spectrumMachine.getLineTimes();

    if (z80Clock.getTStates() < lineTimes[display.BORDER_HEIGHT]) return 0xff;
    int line = (int) ((z80Clock.getTStates() - lineTimes[display.BORDER_HEIGHT]) / timings.tstatesPerLine);
    if (line >= display.HEIGHT) return 0xff;
    int tstatesThroughLine = (int) (z80Clock.getTStates() - lineTimes[display.BORDER_HEIGHT + line] + (timings.leftBorder - display.BORDER_WIDTH_COLS * 4));
    if (tstatesThroughLine < timings.leftBorder) return 0xff;
    if (tstatesThroughLine >= timings.leftBorder + timings.horizontalScreen) return 0xff;
    int column = ((tstatesThroughLine - timings.leftBorder) / 8) * 2;
    int[] bytes = ram[memory.currentScreen];

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
    SpectrumMachine spectrumMachine = this;

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
        lastFloatingBusAmstradValue = (byte) (ram[screen][(int) (displayAttrStart[line] + column)] | 0x01);
        return lastFloatingBusAmstradValue;

      case 4:
        column++; // Screen data
      case 2:
        lastFloatingBusAmstradValue = (byte) (ram[screen][(int) (displayLineStart[line] + column)] | 0x01);
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

  // Check if a port is handled by the ULA
  public boolean portFromUla(int port) {
    // All even ports supplied by ULA
    return (port & 0x0001) == 0;
  }


  public int contendDelay(long time) {
    return contendDelay65432100(time);
  }

  public int contendDelayNoMreq(long time) {
    return contendDelay65432100(time);
  }
}