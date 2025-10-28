/*
 * specplus2.c: Spectrum +2 specific routines
 * Copyright (c) 1999-2011 Philip Kendall
 *
 * Converted to Java (OOZX) by Fernando Damián Petrola – 2025
 * Licensed under GNU GPL v2 or later
 */

package com.fpetrola.oozx.fuse.machine;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;

public class SpecPlus2 extends AbstractSpectrumMachine {

  private Memory memory;
  private Display display;
  private MachinesPeriph machinesPeriph;
  private Spectrum spectrum;
  private Spec48 spec48;
  private Spec128 spec128;
  private IPeriph periph;

  public SpecPlus2(Memory memory, Display display, Machine machine, MachinesPeriph machinesPeriph, Spectrum spectrum, Spec48 spec48, Spec128 spec128, IPeriph periph) {
    super(display, machine);
    this.memory = memory;
    this.display = display;
    this.machinesPeriph = machinesPeriph;
    this.spectrum = spectrum;
    this.spec48 = spec48;
    this.spec128 = spec128;
    this.periph = periph;
    this.ramInfo = new SpecPlus2RamInfo(8);
    init();
  }

  // ===================================================================
  // specplus2_init() – Configura la máquina +2
  // ===================================================================
  private void init() {
  }

  // ===================================================================
  // reset() – Reinicia la máquina +2
  // ===================================================================
  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = machine.loadRom(0, Settings.current.romPlus20, Settings.defaults.romPlus20, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 1 (0x4000-0x7FFF)
    error = machine.loadRom(1, Settings.current.romPlus21, Settings.defaults.romPlus21, 0x4000);
    if (error != 0) return error;

    // Reset común de 128K (con RAM lock = 1)
    error = spec128.commonReset(true);
    if (error != 0) return error;

    // Limpiar y configurar periféricos 128K
    periph.clear();
    machinesPeriph.machinesPeriph128();
    periph.update();

    // +2 no tiene Beta integrado
    Beta.builtin = false;

    // Configurar pantalla como en 48K
    spec48.commonDisplaySetup();

    return 0;
  }

  @Override
  public void memoryMap() {
    spec128.memoryMap();
  }

  // ===================================================================
  // RamInfo para +2
  // ===================================================================
  private class SpecPlus2RamInfo extends RamInfo {
    public SpecPlus2RamInfo(int validPages) {
      this.validPages = validPages;
    }

    @Override
    public boolean portFromUla(int port) {
      return spec48.portFromUla(port);
    }

    @Override
    public int contendDelay(long time) {
      return spectrum.contendDelay65432100(time);
    }

    @Override
    public int contendDelayNoMreq(long time) {
      return spectrum.contendDelay65432100(time);
    }
  }

  // ===================================================================
  // unattachedPort()
  // ===================================================================
  @Override
  public int unattachedPort() {
    return spectrum.spectrumUnattachedPort();
  }

  @Override
  public String getName() {
    return "Spectrum Plus 2";
  }

  // ===================================================================
  // getBaseTiming()
  // ===================================================================
  @Override
  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.AMSTRAD_ASIC);
  }
}