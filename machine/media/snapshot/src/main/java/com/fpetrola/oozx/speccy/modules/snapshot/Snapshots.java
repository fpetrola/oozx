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

package com.fpetrola.oozx.speccy.modules.snapshot;

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.emulation.helpers.snapshots.AY8912State;
import com.fpetrola.emulation.helpers.snapshots.SnapshotLoader;
import com.fpetrola.emulation.helpers.snapshots.SnapshotSaver;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.z80.bytecode.RegistersBase;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This machine written out as a snapshot, and a snapshot put back into it.
 * <p>
 * It is the seam between a file format and a machine that is running, and it belongs to neither:
 * the readers and writers in the snapshots package turn bytes into a {@link SpectrumState} and
 * back and know nothing of this emulator, and the processor knows nothing of files. What is
 * between them is here - which machine to be, which banks go where, and what has to be written
 * through a port rather than poked, so that the machine ends up in the state the file describes
 * and not merely holding its bytes.
 */
@Singleton
public class Snapshots extends AbstractPeripheral {
  private final Machine machine;
  private final MemoryBus memory;
  private final SpectrumMemory banks;
  private final IO io;
  private final Display display;
  /** The processor itself, not the {@link com.fpetrola.oozx.speccy.modules.z80.Cpu} a machine drives: what goes back into it is its registers. */
  private final Cpu cpu;
  private final com.fpetrola.oozx.speccy.modules.keyboard.KeyMatrix keys;

  @Inject
  public Snapshots(Machine machine, MemoryBus memory, SpectrumMemory banks, IO io, Display display, Cpu cpu,
                   com.fpetrola.oozx.speccy.modules.keyboard.KeyMatrix keys) {
    super(java.util.List.of());
    this.banks = banks;
    this.machine = machine;
    this.memory = memory;
    this.io = io;
    this.display = display;
    this.cpu = cpu;
    this.keys = keys;
  }

  /** The machine as it stands, written where it can be opened again like any other snapshot. */
  public boolean fitsOn(SpectrumMachine machine) {
    return true;
  }

  /** The one at that emulator, which a window or a recording reaches the way it reaches any device. */
  public static Snapshots of(Speccy speccy) {
    return (Snapshots) speccy.peripheralRegistry.find(Snapshots.class);
  }

  public void save(String fileName) {
    SnapshotSaver.setupSnapshotWithState(registersOf(state()), fileName, state());
  }

  /** The same, packed into text short enough for a settings file to carry. */
  public String packed() {
    return SnapshotSaver.getSnapshotAsUnicodePacked(registersOf(state()), state());
  }

  public void load(String url) {
    SpectrumState snapshot = SnapshotLoader.readSnapshot(url);
    if (snapshot == null) {
      return;
    }
    load(snapshot);
    state().clock.setTStates(snapshot.getTstates());
  }

  /**
   * Puts a snapshot into the machine, on the machine it was taken on.
   * <p>
   * A snapshot is not just registers and bytes: it names a model, and a 128K one cannot be
   * poured into a 48K map. It has eight banks where a 48K machine has three, and the game goes
   * on paging them - writing a bank number to 0x7FFD and carrying on at 0xC000 expecting to find
   * it there. Flattening banks 5, 2 and 0 into a 48K machine and dropping the rest gets you a
   * screen that looks right and a game that runs off into whatever the one bank it kept happens
   * to hold, a few frames later. So the machine is chosen first and the snapshot loaded into it,
   * bank for bank, with the paging it was saved under put back.
   */
  public void load(SpectrumState spectrumState) {
    selectMachineFor(spectrumState);
    // What somebody is holding down has nothing to do with the state being poured in, and a key
    // left held is read by the game that arrives: it walks into a wall, or never leaves its menu.
    keys.releaseAll();

    State state = state();
    RegistersBase registersBase = registersOf(state);
    if (spectrumState.getSpectrumModel().codeModel == MachineTypes.CodeModel.SPECTRUM48K) {
      SnapshotLoader.setupFromSpectrumState(registersBase, state, spectrumState);
    } else {
      loadPagedRam(spectrumState);
      SnapshotLoader.setZ80State(registersBase, spectrumState.getZ80State());
      state.clock.setTStates(spectrumState.getTstates());
    }
    display.refreshAll();
  }

  /** The processor's registers, which a snapshot both reads and writes through the same object. */
  private RegistersBase registersOf(State state) {
    return new RegistersBase(state);
  }

  private State state() {
    return cpu.getOoz80().getState();
  }

  /**
   * Becomes the machine the snapshot was taken on, if it is not already it.
   * <p>
   * Only when it differs: selecting a model resets it, and a 48K snapshot arriving at a machine
   * that is already a 48K one has nothing to gain from that. Going through the default first is
   * how a model change is done everywhere else here, so that the new machine starts from a state
   * that is known rather than from the leftovers of the last one.
   */
  private void selectMachineFor(SpectrumState snapshot) {
    MachineTypes wanted = snapshot.getSpectrumModel();
    if (machine.current != null && machine.current.snapshotModel() == wanted) {
      return;
    }
    machine.forSnapshotModel(wanted).ifPresentOrElse(type -> {
      machine.selectDefault();
      machine.select(type);
    }, () -> System.out.printf("oozx: this build has no %s, so the snapshot is loaded into the machine already running%n", wanted));
  }

  /**
   * Copies the snapshot's eight banks into the machine's, then puts back the paging it was saved
   * under - which is the half that matters, because it decides what the game finds at 0xC000 when
   * it resumes, and which of the two screens is the one being shown.
   * <p>
   * The paging goes back through the machine's own port rather than by mapping pages here, so
   * that whatever else a model hangs off that port - the ROM it selects, the shadow screen, the
   * lock bit that a game sets once and relies on - happens the way it does when a game writes it.
   */
  private void loadPagedRam(SpectrumState spectrumState) {
    for (int bank = 0; bank < 8; bank++) {
      byte[] page = spectrumState.getMemoryState().getPageRam(bank);
      if (page == null) {
        continue;
      }
      banks.ram(bank).fill(page);
    }
    if (spectrumState.getSpectrumModel().codeModel == MachineTypes.CodeModel.SPECTRUMPLUS3) {
      io.out(0x1ffd, spectrumState.getPort1ffd() & 0xff);
    }
    io.out(0x7ffd, spectrumState.getPort7ffd() & 0xff);
    restoreSoundChip(spectrumState);
  }

  /**
   * The AY's sixteen registers, put back the way the paging is: through the chip's own ports.
   * <p>
   * A snapshot is taken mid-tune and carries the chip's state, and this was dropping it. What was
   * restored was a machine playing whatever the last chip to be written had been playing, until
   * the game happened to rewrite each register - which for a held note or an envelope is not soon,
   * and for a recording is never, because the recording replays the writes that came after.
   * <p>
   * Through the ports rather than into the synthesis, so that the register file the machine can
   * read back and the sound coming out of it cannot disagree about what the chip was set to.
   */
  private void restoreSoundChip(SpectrumState snapshot) {
    if (machine.current == null || !machine.current.hasOnBoard(AyPeripheral.class)) {
      return;
    }
    AY8912State chip = snapshot.getAY8912State();
    if (chip == null || chip.getRegAY() == null) {
      return;
    }
    int[] registers = chip.getRegAY();
    for (int register = 0; register < 16 && register < registers.length; register++) {
      io.out(0xfffd, register);
      io.out(0xbffd, registers[register] & 0xff);
    }
    io.out(0xfffd, chip.getAddressLatch() & 0x0f);
  }
}
