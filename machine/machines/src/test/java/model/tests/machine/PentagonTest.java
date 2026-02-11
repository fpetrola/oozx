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

import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import model.harness.MachineTest;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import org.junit.jupiter.api.Test;

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
class PentagonTest extends MachineTest {

  private Speccy speccy() {
    Speccy speccy = silentMachine();
    return speccy;
  }

  private Speccy pentagon() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.machine.model(Pentagon.class));
    return speccy;
  }

  @Test
  void aPentagonIsAOneTwentyEightWithADisk() {
    Pentagon pentagon = speccy().machine.model(Pentagon.class);
    assertTrue(pentagon.hasOnBoard(AyPeripheral.class), "it has the sound chip");
    assertTrue(pentagon.pagesThrough7ffd(), "and the 128's paging");
    assertFalse(pentagon.pagesThrough1ffd(), "but not the +3's");
    assertTrue(pentagon.hasOnBoard(Beta128Peripheral.class), "and TR-DOS, which is what its third ROM is");
  }

  @Test
  void itIsTimedLikeNoOtherMachineHere() {
    Speccy speccy = pentagon();
    assertEquals(71680, speccy.machine.current.getTimings().tstatesPerFrame(),
        "224 clocks over 320 lines");
    assertEquals(3584000, speccy.machine.current.getTimings().processorSpeed(),
        "which at 3.584MHz is fifty frames a second");
    assertNotEquals(speccy.machine.model(Spec128.class).getTimings().tstatesPerFrame(),
        speccy.machine.current.getTimings().tstatesPerFrame(),
        "a 128 runs 70908, and software written for one is what breaks here");
  }

  /** Nothing on a Pentagon is contended, which is why it runs fast and why 48K demos break on it. */
  @Test
  void nothingContends() {
    Pentagon pentagon = speccy().machine.model(Pentagon.class);
    for (long t = 0; t < 71680; t += 97) {
      assertEquals(0, pentagon.contendDelay(t), "no address is delayed, at t=" + t);
      assertEquals(0, pentagon.contendDelayNoMreq(t), "and no port either, at t=" + t);
    }
  }

  @Test
  void itCanBeAskedForByName() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.machine.forShortName("Pentagon").orElseThrow());
    assertSame(speccy.machine.model(Pentagon.class), speccy.machine.current, "the name reaches the machine");
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
    assertSame(speccy.machine.model(Pentagon.class), speccy.machine.current, "and it is still the machine it was");
  }

  private int nonZeroBytesIn(Speccy speccy, int from, int to) {
    int count = 0;
    for (int address = from; address < to; address++) {
      if (speccy.memory.peek(address) != 0) count++;
    }
    return count;
  }

}
