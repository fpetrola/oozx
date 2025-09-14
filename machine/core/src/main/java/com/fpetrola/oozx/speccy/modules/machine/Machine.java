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
  /** Handed out because how this machine differs from another of its model is the machine's to say. */
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

  /** The one machine of that model in this emulator: exactly that class, not a machine derived from it. */
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
   * The machine a snapshot taken on this model should load into: the one that says it is that
   * model, or failing that the nearest one running the same code - a 16K snapshot has no 16K here
   * to go to and lands on the 48K.
   */
  public Optional<Spectrum> forSnapshotModel(MachineTypes model) {
    return machineTypes.stream().filter(m -> m.snapshotModel() == model).findFirst()
        .or(() -> machineTypes.stream()
            .filter(m -> m.snapshotModel() != null && m.snapshotModel().codeModel == model.codeModel).findFirst());
  }

  /** The machine that goes by this short name, the one a snapshot format or a test names it by. */
  public Optional<Spectrum> forShortName(String shortName) {
    return machineTypes.stream().filter(m -> m.shortName().equals(shortName)).findFirst();
  }

  /** Switching models goes through the default one first, so the new machine starts from a known state. */
  public void selectDefault() {
    select(defaultMachine);
  }

  /** Runs this machine as that model, which has to be one this build has. */
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

    // The new machine's own frame event, not the old one's: with the old one a Pentagon ran on
    // the 48K's frame length and counted its frames on the 48K.
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

  /** Where the first displayed line starts: a border above and beside the first pixel, which a late unit puts a T-state later. */
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

  /** Which of the machines a machine of a given model is: the differences between units of the same model. */
  @Singleton
  public static class Unit {
    /** Some units start a scanline one T-state later than others, and a few programs can tell. */
    public boolean lateTimings;
    /** A 48K of issue 2 reads the two spare keyboard bits differently from an issue 3. */
    public boolean issue2;
  }
}
