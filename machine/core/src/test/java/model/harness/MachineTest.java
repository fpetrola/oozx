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

package model.harness;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.google.inject.Module;

/**
 * A test that runs a machine: how to build one that waits on nothing, and the loops every such
 * test was writing for itself. Static and given the machine, since a test may run more than one.
 */
public abstract class MachineTest {
  /**
   * A machine with no sound card and no picture, ready to run. Silent rather than muted: the
   * real device opens an audio line on init, and a crash inside the platform's audio server
   * takes the JVM down with it. Anything more a test wants bound goes after.
   */
  public static Speccy silentMachine(Module... more) {
    Module[] modules = new Module[more.length + 1];
    modules[0] = binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class);
    System.arraycopy(more, 0, modules, 1, more.length);
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(), modules);
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  /** One turn of the machine's own loop: the processor up to the next task, then what is due. */
  public static void step(Speccy speccy) {
    speccy.loop.doOpcodes();
    speccy.scheduler.runDue();
  }

  /**
   * Whole frames, counted by the machine. Not by watching the clock go back: it goes back by a
   * frame at every frame end, and a program whose loop divides the frame leaves it standing
   * still, which once hung a build for ten minutes.
   */
  public static void runFrames(Speccy speccy, int frames) {
    long until = speccy.machine.current.frameCount() + frames;
    while (speccy.machine.current.frameCount() < until) {
      step(speccy);
    }
  }

  /**
   * Time passing with the processor stopped: the clock moves and the tasks it set run. In slices,
   * so a task due in the middle runs when it is due and not at the end.
   */
  public static void advance(Speccy speccy, int tstates) {
    while (tstates > 0) {
      int slice = Math.min(tstates, 1000);
      speccy.zxClock.addTStates(slice);
      speccy.scheduler.runDue();
      tstates -= slice;
    }
  }

  /** The map as a program sees it: which ROM is at the bottom and which RAM page is in each of the other three slots. */
  public static void assertMap(Speccy speccy, int rom, int slot1, int slot2, int slot3) {
    assertSame(speccy.banks.rom(rom), speccy.memory.reading(0x0000).memory(), "rom at 0x0000");
    assertRamPages(speccy, slot1, slot2, slot3);
  }

  /** The +3's special modes: RAM in every slot, the bottom one included. */
  public static void assertAllRam(Speccy speccy, int slot0, int slot1, int slot2, int slot3) {
    assertSame(speccy.banks.ram(slot0), speccy.memory.reading(0x0000).memory(), "page at 0x0000");
    assertRamPages(speccy, slot1, slot2, slot3);
  }

  /**
   * The three RAM slots above the bottom one. A peripheral that pages its ROM in at 0x0000 holds
   * /ROMCS and nothing else, so these are what it must leave exactly as it found them.
   */
  public static void assertRamPages(Speccy speccy, int slot1, int slot2, int slot3) {
    assertSame(speccy.banks.ram(slot1), speccy.memory.reading(0x4000).memory(), "page at 0x4000");
    assertSame(speccy.banks.ram(slot2), speccy.memory.reading(0x8000).memory(), "page at 0x8000");
    assertSame(speccy.banks.ram(slot3), speccy.memory.reading(0xc000).memory(), "page at 0xc000");
  }

  /** Which RAM page the picture is being shown from. */
  public static int shownPage(Speccy speccy) {
    for (int page = 0; page < 8; page++) if (speccy.banks.shown() == speccy.banks.ram(page)) return page;
    throw new AssertionError("the screen is not in any page");
  }
}
