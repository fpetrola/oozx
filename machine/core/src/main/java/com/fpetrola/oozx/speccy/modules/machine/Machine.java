/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.modules.machine;

import com.fpetrola.oozx.speccy.machine.MachineTimings;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import java.util.Optional;
import java.util.Set;
import com.google.inject.Singleton;

@Singleton
public class Machine {
  private final Scheduler scheduler;
  private final MemoryBus memory;
  private final Display display;
  private final Ula ula;
  private final Cpu cpu;
  private final PeripheralRegistry peripherals;
  public final Unit unit;
  private final Sound.Output soundOutput;
  public Spectrum current;
  private final Z80Clock z80Clock;
  private final Picture picture;
  private final Timer timer;

  private final List<Spectrum> machineTypes = new ArrayList<>();
  private final Spectrum defaultMachine;
  private final List<MachineChangeListener> machineChangeListeners = new ArrayList<>();
  private Sound sound;

  @Inject
  public Machine(Set<Spectrum> models, @DefaultMachine Spectrum defaultMachine, Scheduler scheduler, MemoryBus memory, Display display, Ula ula, Z80Clock z80Clock, Picture picture, Timer timer, Cpu cpu, PeripheralRegistry peripherals, Unit unit, Sound.Output soundOutput, Sound sound) {
    this.defaultMachine = defaultMachine;
    this.scheduler = scheduler;
    this.memory = memory;
    this.display = display;
    this.ula = ula;
    this.cpu = cpu;
    this.peripherals = peripherals;
    this.z80Clock = z80Clock;
    this.picture = picture;
    this.timer = timer;
    this.unit = unit;
    this.soundOutput = soundOutput;
    this.sound = sound;
    models.forEach(this::addMachine);
  }

  /** Matches the exact class {@code type}, not a subclass. */
  public <T extends Spectrum> T model(Class<T> type) {
    return type.cast(getMachineTypes().stream().filter(m -> m.getClass() == type).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("no " + type.getSimpleName() + " on this emulator")));
  }

  public List<Spectrum> getMachineTypes() {
    return machineTypes;
  }

  private void addMachine(Spectrum spectrumMachine) {
    machineTypes.add(spectrumMachine);
  }

  /**
   * The build's exact match for a snapshot's model, or else one running the same code —
   * e.g. a 16K snapshot lands on the 48K when this build has no 16K.
   */
  public Optional<Spectrum> forSnapshotModel(MachineTypes model) {
    return machineTypes.stream().filter(m -> m.snapshotModel() == model).findFirst()
        .or(() -> machineTypes.stream()
            .filter(m -> m.snapshotModel() != null && m.snapshotModel().codeModel == model.codeModel).findFirst());
  }

  public Optional<Spectrum> forShortName(String shortName) {
    return machineTypes.stream().filter(m -> m.shortName().equals(shortName)).findFirst();
  }

  public void selectDefault() {
    select(defaultMachine);
  }

  public void select(SpectrumMachine type) {
    for (Spectrum candidate : machineTypes) {
      if (candidate == type) {
        selectMachine(candidate);
        return;
      }
    }
    throw new IllegalStateException("this build has no " + type.getName());
  }

  private void selectMachine(Spectrum machine) {

    // Must be the new machine's own event: reusing the old one made a switched-to Pentagon
    // still run and count frames on the 48K's frame length.
    Task endOfFrame = machine.endOfFrame();

    current = machine;
    peripherals.clear();
    machineChangeListeners.forEach(listener -> listener.machineChanged(current));

    z80Clock.setTStates(0);

    scheduler.clear();
    timer.addEvent();

    scheduler.schedule(endOfFrame, machine.getTimings().tstatesPerFrame());

    sound.init();

    machine.reset();

    reset(false);
  }

  public void reset(boolean hardReset) {

    setVariableTimings(current);

    current.reset();

    cpu.machineWasReset(hardReset);
    peripherals.machineWasReset(hardReset);

    current.memoryMap();

    ula.contention.forMachine(current);

    display.refreshAll();
  }

  /** Backs off from the first pixel by the border size to find where the first displayed line starts. */
  private void setVariableTimings(SpectrumMachine machine) {
    MachineTimings timings = unit.lateTimings ? machine.getTimings().late() : machine.getTimings();
    machine.firstLineAt(timings.firstPixel() - display.BORDER_HEIGHT * timings.tstatesPerLine() - 4 * display.BORDER_WIDTH_COLS);
  }

  public void addMachineChangeListener(MachineChangeListener listener) {
    machineChangeListeners.add(listener);
  }

  public void addMachineChangeListeners(MachineChangeListener... listeners) {
    machineChangeListeners.addAll(Arrays.asList(listeners));
  }

  /** Hardware differences between individual units of the same model. */
  @Singleton
  public static class Unit {
    /** Some units start a scanline one T-state later; a few programs detect this. */
    public boolean lateTimings;
    /** A 48K issue 2 reads the two spare keyboard bits differently from an issue 3. */
    public boolean issue2;
  }
}
