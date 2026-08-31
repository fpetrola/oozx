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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.LocalLibretroCore;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static com.fpetrola.oozx.MachineCapability.AY;
import static com.fpetrola.oozx.MachineCapability.MEMORY_128;
import static com.fpetrola.oozx.MachineCapability.PLUS3_MEMORY;
import static com.fpetrola.oozx.MachineCapability.TRDOS_DISK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Pentagon 128, a Russian clone that pages like a 128 and is timed like nothing else here.
 * <p>
 * It is the first machine added since the graph started handing out its own parts, so what these
 * check is as much the machine as whether adding one is now a matter of writing one class.
 */
class PentagonTest {

  private Speccy speccy() {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    return speccy;
  }

  private Speccy pentagon() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.pentagon);
    return speccy;
  }

  @Test
  void aPentagonIsAOneTwentyEightThatCannotReachADisk() {
    Pentagon pentagon = speccy().pentagon;
    assertTrue(pentagon.has(AY), "it has the sound chip");
    assertTrue(pentagon.has(MEMORY_128), "and the 128's paging");
    assertFalse(pentagon.has(PLUS3_MEMORY), "but not the +3's");

    // A real Pentagon has TR-DOS. This one says no, because the Beta 128 needs a WD1793 and a
    // way to read a disk image, and a capability that answers yes while nothing is behind it is
    // exactly how the +3 ended up unable to switch its drive off.
    assertFalse(pentagon.has(TRDOS_DISK), "and no disk until there is one");
  }

  @Test
  void itIsTimedLikeNoOtherMachineHere() {
    Speccy speccy = pentagon();
    assertEquals(71680, speccy.machine.current.getTimings().tstatesPerFrame,
        "224 clocks over 320 lines");
    assertEquals(3584000, speccy.machine.current.getTimings().processorSpeed,
        "which at 3.584MHz is fifty frames a second");
    assertNotEquals(speccy.spec128.getTimings().tstatesPerFrame,
        speccy.machine.current.getTimings().tstatesPerFrame,
        "a 128 runs 70908, and software written for one is what breaks here");
  }

  /** Nothing on a Pentagon is contended, which is why it runs fast and why 48K demos break on it. */
  @Test
  void nothingContends() {
    Pentagon pentagon = speccy().pentagon;
    for (long t = 0; t < 71680; t += 97) {
      assertEquals(0, pentagon.contendDelay(t), "no address is delayed, at t=" + t);
      assertEquals(0, pentagon.contendDelayNoMreq(t), "and no port either, at t=" + t);
    }
  }

  @Test
  void itCanBeAskedForByName() {
    Speccy speccy = speccy();
    new LocalLibretroCore(speccy.eventManager, speccy.display, speccy.machine, speccy.z80,
        speccy.zxClock, speccy.periph, speccy).retro_select_machine("Pentagon");
    assertSame(speccy.pentagon, speccy.machine.current, "the name reaches the machine");
    assertEquals("Pentagon", speccy.machine.current.getName());
  }

  /**
   * The one that says it is a computer and not a declaration: reset it, let the ROM run, and see
   * that it wrote the screen and its own system variables.
   */
  @Test
  void itBootsAndItsRomRuns() {
    Speccy speccy = pentagon();
    runFrames(speccy, 200);

    assertTrue(nonZeroBytesIn(speccy, 0x4000, 0x5B00) > 0, "the ROM drew something");
    assertTrue(nonZeroBytesIn(speccy, 0x5B00, 0x5D00) > 0, "and set up its system variables");
    assertSame(speccy.pentagon, speccy.machine.current, "and it is still the machine it was");
  }

  private int nonZeroBytesIn(Speccy speccy, int from, int to) {
    int count = 0;
    for (int address = from; address < to; address++) {
      if (speccy.memory.readByteInternal(address) != 0) count++;
    }
    return count;
  }

  private void runFrames(Speccy speccy, int frames) {
    long previous = speccy.zxClock.getTStates();
    int seen = 0;
    while (seen < frames) {
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
      long now = speccy.zxClock.getTStates();
      if (now < previous) seen++;
      previous = now;
    }
  }
}
