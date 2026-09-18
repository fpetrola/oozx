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

import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a machine starts with when nobody has configured it, answering as if it were a machine so
 * that the same controls can be pointed at it.
 * <p>
 * Nothing here is running, but there is a machine behind it all the same: one built and never
 * switched on, which is what "what a new machine starts with" is. Asked what machines there are,
 * which ROM sets one can be run with, which processors this build has and what every device says
 * it can be told, it asks that one, so the window is as full with no emulator open as with one.
 * Built when somebody first asks, because most sessions never open these settings; switched on is
 * the one thing it never is.
 * <p>
 * What is set here is written where the next machine will read it: the configuration file, the
 * screen's defaults, and the two statics that a machine reads as it starts - which Spectrum it
 * becomes and which processor it runs on.
 */
public class DefaultsCore extends MockEmulatorCore {
  private final OOZxConfiguration config;
  private com.fpetrola.oozx.Speccy unstarted;

  public DefaultsCore(OOZxConfiguration config) {
    super(null);
    this.config = config;
  }

  /** The machine nobody switched on, which is what every question about the build is asked of. */
  private com.fpetrola.oozx.Speccy build() {
    if (unstarted == null) {
      unstarted = com.fpetrola.oozx.Speccy.create();
    }
    return unstarted;
  }

  /** Nothing is running, so the devices answer with what the file says they will start with. */
  @Override
  public java.util.List<com.fpetrola.oozx.config.Settings.Configurable> deviceSettings() {
    build();
    return com.fpetrola.oozx.config.Settings.defaults();
  }

  @Override
  public java.util.List<String> getMachineModels() {
    return build().machine.getMachineTypes().stream()
        .map(com.fpetrola.oozx.speccy.machine.SpectrumMachine::getName).toList();
  }

  /**
   * The machine a new one becomes, which is the one the build was asked for: the binding answers
   * the file, so there is nowhere else to ask and nothing to keep in step with it. Until it is
   * built, the one just chosen, since what was built came from the file as it was then.
   */
  @Override
  public String getCurrentModel() {
    return chosenModel != null ? chosenModel : build().machine.defaultModel();
  }

  /** What has been chosen here since: what was built came from the file as it was then. */
  private String chosenModel;
  private String chosenProcessor;

  @Override
  public void setMachineModel(String model) {
    chosenModel = model;
    com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "model", model);
  }

  /** The sets for the machine a new emulator will become, which is the one being configured here. */
  @Override
  public java.util.List<String> getRomSets() {
    return chosenType().map(type -> java.util.List.copyOf(build().roms.setsFor(type).keySet()))
        .orElse(java.util.List.of());
  }

  @Override
  public String getRomSet() {
    return chosenType().map(type -> build().roms.chosenSet(type)).orElse("");
  }

  /** Only one this machine has, the way a running one also leaves its ROMs alone when asked for
   * a set it cannot be run on. */
  @Override
  public void setRomSet(String set) {
    chosenType().filter(type -> build().roms.setsFor(type).containsKey(set))
        .ifPresent(type -> build().roms.chooseSet(type, set));
  }

  private java.util.Optional<com.fpetrola.oozx.speccy.machine.Spectrum> chosenType() {
    return build().machine.forName(getCurrentModel());
  }

  @Override
  public java.util.List<String> getProcessors() {
    return build().processors.all().stream().map(com.fpetrola.z80.cpu.Core::name).toList();
  }

  /** The one a new machine starts on, which the build was asked for: the binding answers the file. */
  @Override
  public String getProcessor() {
    return chosenProcessor != null ? chosenProcessor : build().processors.current();
  }

  @Override
  public void setProcessor(String processor) {
    chosenProcessor = processor;
    com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "processor", processor);
  }

  @Override
  public void setVideoOption(String option, Object value) {
    Map<String, String> screen = new LinkedHashMap<>(config.getScreenDefaults());
    screen.put(option, String.valueOf(value));
    ScreenSettings.setDefaults(screen);
    config.setScreenDefaults(screen);
  }

  @Override
  public void setGeneralOption(String option, Object value) {
    if (option.equals("turbo")) {
      config.setTurboByDefault((Boolean) value);
    } else {
      super.setGeneralOption(option, value);
    }
  }

  @Override
  public boolean isTurboMode() {
    return config.isTurboByDefault();
  }
}
