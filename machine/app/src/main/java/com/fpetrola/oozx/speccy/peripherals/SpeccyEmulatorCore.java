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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.EmulatorListener;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.screen.SpeccyScreen;
import com.fpetrola.oozx.speccy.desktop.SwingKeyboard;
import com.fpetrola.oozx.speccy.pokes.PokFile;

import java.awt.event.KeyListener;

import com.fpetrola.oozx.speccy.pokes.PokInstruction;
import javax.swing.SwingUtilities;
import java.io.File;

/**
 * What a window talks to when it drives a machine: pause, speed, the model, the screen, saving.
 * <p>
 * It used to be built inside the Z80, from a method called createScreen that created no screen and
 * returned null - so the processor named Swing types and held the adapter its own windows use. An
 * emulator does not have a user interface; something with a user interface has an emulator, and
 * this is that something's side of the wire.
 */
public class SpeccyEmulatorCore extends MockEmulatorCore {
  private final Speccy speccy;

  public SpeccyEmulatorCore(Speccy speccy) {
    super(new SpeccyScreen(speccy.picture.pixels, com.fpetrola.oozx.speccy.modules.display.Picture.STRIDE));
    this.speccy = speccy;
    // Clicking the picture puts the keyboard on the machine. Without it, whatever was clicked last
    // keeps the focus - a toolbar button, usually - and then Enter presses that button instead of
    // reaching the Spectrum.
    getPanel().setFocusable(true);
    getPanel().addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mousePressed(java.awt.event.MouseEvent clicked) {
        getPanel().requestFocusInWindow();
      }
    });
    // Whatever changes the machine - a tape named for a 128K, a snapshot, the box itself - says so
    // once, here, instead of each caller remembering to announce it.
    speccy.machine.addMachineChangeListener(newMachine -> announceMachine(newMachine.getName()));
    turbo = speccy.speed.emulation != 100;
  }

  private String filename;
  private boolean turbo;

  /**
   * Asked of the machine rather than remembered.
   * <p>
   * A copy of the answer has to be kept in step by everything that changes the machine, and
   * it was not: choosing a machine for a snapshot updated it, choosing one for a 128K tape
   * did not, and the indicator went on naming whatever the emulator had started as. There is
   * no keeping a copy honest; there is only not keeping one.
   */
  @Override
  public String getCurrentModel() {
    return speccy.machine.current == null ? super.getCurrentModel() : speccy.machine.current.getName();
  }

  /** The processors this build has, and moving this machine onto one, which it does between instructions. */
  @Override
  public java.util.List<String> getProcessors() {
    return speccy.processors.all().stream().map(com.fpetrola.z80.cpu.Core::name).toList();
  }

  @Override
  public String getProcessor() {
    return speccy.processors.current();
  }

  @Override
  public void setProcessor(String processor) {
    speccy.processors.use(processor);
  }

  /** The ROM sets this machine can be run on, asked of the machine that is in. */
  @Override
  public java.util.List<String> getRomSets() {
    return speccy.machine.current == null ? java.util.List.of()
        : java.util.List.copyOf(speccy.roms.setsFor(speccy.machine.current).keySet());
  }

  @Override
  public String getRomSet() {
    String running = speccy.machine.current == null ? null : speccy.roms.chosenSet(speccy.machine.current);
    return running == null ? "" : running;
  }

  /**
   * Puts other ROMs in this machine and starts it again with them, which is the only way a ROM
   * change means anything: a machine reads its ROMs when it is switched on.
   * <p>
   * What is not on this disk yet is brought here, before anything is built, and if it does not
   * arrive the machine is left running what it was running.
   */
  @Override
  public void setRomSet(String set) {
    com.fpetrola.oozx.speccy.machine.Spectrum machine = speccy.machine.current;
    if (machine == null || set == null || set.equals(getRomSet())) return;
    java.util.List<String> before = speccy.roms.running(machine);
    speccy.roms.chooseSet(machine, set);
    if (!com.fpetrola.oozx.speccy.desktop.RomNotShippedDialog.readyFor(machine)) {
      speccy.roms.runOn(machine, before);
      return;
    }
    speccy.loop.later(() -> speccy.machine.select(machine));
  }

  /** The machines this build has, so the list is not a copy of them that can go stale. */
  @Override
  public java.util.List<String> getMachineModels() {
    return speccy.machine.getMachineTypes().stream().map(com.fpetrola.oozx.speccy.machine.SpectrumMachine::getName).toList();
  }



  public void applyMod(PokFile.PokeMod mod) {
    PokInstruction parsedInstruction = mod.getParsedInstruction();
    parsedInstruction.apply(new PokInstruction.EmulatorMemoryWriter() {
      public void writeMemory(int bank, int address, int value) {
        speccy.cpu.getOoz80().getState().getMemory().write(address, value);
      }

      public int readMemory(int bank, int address) {
        return speccy.cpu.getOoz80().getState().getMemory().read(address);
      }
    });
  }

  public void revertMod(PokFile.PokeMod mod) {
    PokInstruction parsedInstruction = mod.getParsedInstruction();
    parsedInstruction.revert(new PokInstruction.EmulatorMemoryWriter() {
      public void writeMemory(int bank, int address, int value) {
        speccy.cpu.getOoz80().getState().getMemory().write(address, value);
      }

      public int readMemory(int bank, int address) {
        return speccy.cpu.getOoz80().getState().getMemory().read(address);
      }
    });
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getFilename() {
    return filename;
  }

  public KeyListener getKeyListener() {
    return new SwingKeyboard(Input.of(speccy).keyboard(), Input.of(speccy));
  }

  public void finishEmulation() {
    speccy.session.finish();
  }

  public boolean isPaused() {
    return speccy.loop.isPaused();
  }

  public void pauseEmulation() {
    speccy.loop.setPaused(!speccy.loop.isPaused());
    notifyPauseStateChange(speccy.loop.isPaused());
  }

  @Override
  public void resumeEmulation() {
    speccy.loop.setPaused(false);
    notifyPauseStateChange(speccy.loop.isPaused());
  }

  public void setGeneralOption(String option, Object value) {
    if (option.equals("turbo")) {
      turbo = (boolean) value;
      // Turbo is as fast as it can, not a number: at any number the sound is the clock and holds
      // the machine to exactly that, which at 15000 was half of what it can do.
      int emulationSpeed = turbo ? Speed.UNLIMITED : 100;
      changeSpeed1(emulationSpeed);
//          speccy.timer.estimateReset();
    } else if (option.equals("speed")) {
      int emulationSpeed = (int) value;
      turbo = emulationSpeed != 100;
      changeSpeed1(emulationSpeed);
    } else if (option.equals("mute")) {
      speccy.sound.output.enabled = !(boolean) value;
//          speccy.timer.estimateReset();
    } else if (option.equals("pause")) {
      speccy.loop.setPaused((boolean) value);
      notifyPauseStateChange(speccy.loop.isPaused());
    } else {
      // Anything this one has no opinion on goes to the core it overrides, which is where
      // the options that are about the panel rather than the machine are answered. Without
      // this the override swallowed them: an option nobody handled and nobody reported.
      super.setGeneralOption(option, value);
    }
  }

  /**
   * The machine from cold, as if it had just been switched on. On the emulation thread, like
   * everything else that touches it: resetting from the event thread is a machine being taken
   * apart while it is running an instruction.
   */
  @Override
  public void resetEmulation() {
    speccy.loop.later(() -> speccy.machine.reset(true));
    notifyStateChange("Reset");
  }

  /** This machine's own devices, so a control changes what is running rather than a file. */
  @Override
  public java.util.List<com.fpetrola.oozx.config.Settings.Configurable> deviceSettings() {
    return speccy.settings() == null ? java.util.List.of() : speccy.settings().devices();
  }

  public void changeSpeed1(int emulationSpeed) {
    speccy.loop.later(() -> speccy.timer.changeSpeed(emulationSpeed));
    notifyTurboModeChange(turbo);
    notifyEmulationSpeedChange(emulationSpeed);
  }

  @Override
  public double getEmulationSpeed() {
    return speccy.speed.emulation;
  }

  @Override
  public boolean isTurboMode() {
    return turbo;
  }

  public boolean isMuted() {
    return !speccy.sound.output.enabled;
  }

  @Override
  public int getVolume() {
    return speccy.sound.volume();
  }

  @Override
  public void setAudioOption(String option, Object value) {
    if (option.equals("volume")) {
      speccy.sound.setVolume((int) value);
    } else {
      super.setAudioOption(option, value);
    }
  }

  /**
   * Writes the machine as it stands to a file, which can be opened again like any other.
   * <p>
   * Handed straight to the machine's own snapshots: reaching the processor's registers from here
   * to feed a writer, which is what this did, put the emulator's window in the business of
   * knowing what a snapshot is made of.
   */
  @Override
  public void saveState(String filePath) {
    Snapshots.of(speccy).save(filePath);
  }

  @Override
  public String packedState() {
    return Snapshots.of(speccy).packed();
  }

  /** What this one does about what it is told, which the window does not have to know. */
  {
    addEmulatorListener(new EmulatorListener() {
      @Override
      public void onEmulationStateChanged(String state) {
        SwingUtilities.invokeLater(() -> {
          if (state.equals("Reset")) {
            Tape.of(speccy).insert(new File("/tmp/zxinfo_extracted/SOLARINV.TAP"));
            Tape.of(speccy).play(true);
          }
        });
      }

      @Override
      public void onError(String message) {

      }

      @Override
      public void onEmulationSpeedChanged(double speed) {

      }

      @Override
      public void onModelChanged(String model) {
        // Also reached when the machine announces itself, which is the machine saying it already
        // is this. Selecting it again from there resets the machine that just started, and the
        // reset announces itself, and so on.
        if (speccy.machine.current != null && model.equals(speccy.machine.current.getName())) {
          return;
        }
        speccy.machine.getMachineTypes().stream().filter(m -> m.getName().equals(model)).findFirst()
            .ifPresent(type -> {
              // Whatever this machine needs is brought here and now, where a question can be asked:
              // by the time the emulator's own thread builds it, everything it reads is on this disk.
              if (!com.fpetrola.oozx.speccy.desktop.RomNotShippedDialog.readyFor(type)) return;
              // Straight to the one asked for. Passing through the default first meant the
              // machine became a 48K on the way, said so, and every box showing the model flicked
              // to "Spectrum 48K" and back - and selecting a machine already clears the
              // peripherals, the clock and the timetable, so the trip bought nothing.
              speccy.loop.later(() -> speccy.machine.select(type));
            });
      }

      @Override
      public void onPauseStateChanged(boolean paused) {

      }

      @Override
      public void onTurboModeChanged(boolean turbo) {

      }

      @Override
      public void onTapeStatusChanged(String status) {

      }
    });
  }
}
