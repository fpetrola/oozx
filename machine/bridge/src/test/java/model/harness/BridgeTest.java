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

import com.fpetrola.oozx.speccy.bridge.DefaultCommandHandler;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import model.connected.ConnectedInterface1;
import model.connected.ConnectedMemory;
import model.connected.ConnectedMicrodrive;
import model.connected.ConnectedSpectrumBus;
import model.connected.ConnectedULA;
import model.connected.ConnectedZ80CPU;
import model.interfaces.IMicrodrive;
import model.interfaces.ISpectrumBus;
import model.interfaces.IZ80CPU;
import model.interfaces.IZXInterface1;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A machine reached the way Fuse's own tests reach one: through the bridge, with a clock that
 * writes down every T-state it hands out, so that a run can be compared with Fuse's to the cycle.
 * <p>
 * The wiring is here rather than in {@link SpeccyBaseForTests}, which makes the Speccy and knows
 * nothing of the driver or the objects a test holds it by. This is to the bridge what
 * {@link MachineTest} is to the core.
 */
public abstract class BridgeTest {
  protected static IZ80CPU cpu;
  protected static TestDriver testDriver;
  protected static ISpectrumBus bus;
  protected static IZXInterface1 interface1;
  protected static IMicrodrive microdrive;

  @BeforeAll
  public static void connectTheMachine() {
    testDriver = new TestDriver(DefaultCommandHandler.createCommandHandler(SpeccyBaseForTests.createSpeccy()));
    bus = new ConnectedSpectrumBus(new ConnectedMemory(testDriver), new ConnectedULA(testDriver), testDriver);
    interface1 = new ConnectedInterface1(testDriver);
    microdrive = new ConnectedMicrodrive(testDriver);
    bus.connectComponent(interface1);
    interface1.connectMicrodrive(microdrive);
    cpu = new ConnectedZ80CPU(testDriver);
    bus.getULA().setScreenActive(true);
  }

  @BeforeEach
  void startTheTest() {
    interface1.reset();
    testDriver.reset();
    cpu.reset();
    testDriver.updatePC(0xA000);
    testDriver.tstatesHistoryInit();
  }

  /** That model, with the program counter out of the ROM and the clock at that T-state. */
  protected static int setupModel(String model, int startTState) {
    testDriver.setModel(model);
    testDriver.updatePC(0xA000);
    testDriver.if1Page(false);
    cpu.setTStates(startTState);
    return startTState;
  }

  /** Every T-state the machine handed out, as text, against what Fuse hands out for the same run. */
  protected static void assertTStatesHistory(String expected) {
    assertEquals(expected.trim(), testDriver.getTstatesHistory().toString().trim());
  }
}
