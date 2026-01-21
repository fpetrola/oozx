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

package model.tests.cpu;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import model.harness.MachineTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A machine pulls /INT down at the top of every frame and lets it go a fixed number of T-states
 * later, and that number is the machine's own. A program with interrupts enabled takes one while
 * the line is down and takes nothing after it comes back up, which is how a game that spends the
 * start of its frame with interrupts off still gets the interrupt, and how one that dawdles too
 * long misses it altogether.
 * <p>
 * Not in {@code NmiAndPcTrapsTest}: that one is about the line that cannot be masked and about
 * the debugger's traps, and neither is asked of every machine.
 */
class TheInterruptLineTest extends MachineTest {

  /** Somewhere in RAM with nothing in it, so that the only thing that can move the PC is the interrupt. */
  private static final int IN_RAM = 0x8000;

  static Stream<String> machines() {
    return silentMachine().machine.getMachineTypes().stream().map(Spectrum::getName);
  }

  /**
   * A machine whose ROM has finished starting up, so that it is in interrupt mode 1: that it has
   * reached its own handler at 0x0038 is what says so. Whether interrupts happen to be enabled at
   * the moment the frames stop is another matter - they are off inside the handler, which is
   * where a machine stopped at a frame boundary tends to be - so the test enables them itself.
   */
  private Speccy readyToBeInterrupted(String model) {
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getName().equals(model)).findFirst().orElseThrow());
    boolean[] handled = {false};
    var watch = speccy.cpu.beforeFetch().watch(0x0038, pc -> handled[0] = true);
    for (int frames = 0; frames < 400 && !handled[0]; frames += 20) {
      runFrames(speccy, 20);
    }
    watch.off();
    assertTrue(handled[0], "the ROM never took an interrupt, so it is not in mode 1 yet");
    return speccy;
  }

  @ParameterizedTest
  @MethodSource("machines")
  void oneIsTakenWhileTheLineIsDownAndNothingAfterItComesBackUp(String model) {
    Speccy speccy = readyToBeInterrupted(model);
    int length = speccy.machine.current.getTimings().interruptLength();
    assertTrue(length > 0, model + " holds its interrupt line down for no time at all");

    var state = speccy.cpu.getOoz80().getState();
    state.getPc().write(IN_RAM);
    state.setIff1(true);
    speccy.zxClock.setTStates(length - 1);
    speccy.cpu.interrupt(length);
    assertEquals(0x0038, state.getPc().read(), "the last T-state of the pulse did not interrupt");

    state.getPc().write(IN_RAM);
    state.setIff1(true);
    speccy.zxClock.setTStates(length);
    speccy.cpu.interrupt(length);
    assertEquals(IN_RAM, state.getPc().read(), "the line was back up and it interrupted anyway");
  }
}
