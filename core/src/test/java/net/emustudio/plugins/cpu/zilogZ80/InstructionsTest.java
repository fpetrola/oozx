/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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
package net.emustudio.plugins.cpu.zilogZ80;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.WordNumber;
import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuVerifierImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

public class InstructionsTest {
  static final int REG_PAIR_BC = 0;
  static final int REG_PAIR_DE = 1;
  static final int REG_PAIR_HL = 2;
  static final int REG_SP = 3;
  private static final long PLUGIN_ID = 0L;
  private static OOZ80 ooz80;
  private static MyIO io;
  private final List<FakeByteDevice> devices = new ArrayList<>();
  CpuRunnerImpl cpuRunnerImpl;
  CpuVerifierImpl cpuVerifierImpl;
  protected CpuImpl cpu;
  protected static MyByteMemoryStub memory;

  public InstructionsTest() {

  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    io = new MyIO();
    ooz80 = Helper.createOOZ80(io);
    memory = new MyByteMemoryStub();
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    Capture<Context8080> cpuContext = Capture.newInstance();
    ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
    expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
    contextPool.register(anyLong(), capture(cpuContext), same(Context8080.class));
    expectLastCall().anyTimes();
    replay(contextPool);

    ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
    expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
    replay(applicationApi);

    cpu = new CpuImpl(PLUGIN_ID, applicationApi, PluginSettings.UNAVAILABLE, ooz80);

    MockedMemory<WordNumber> memory1 = (MockedMemory<WordNumber>) this.cpu.ooz80.getState().getMemory();
    memory1.canDisable(false);
    memory.init(memory1);
    assertTrue(cpuContext.hasCaptured());

    for (int i = 0; i < 256; i++) {
      FakeByteDevice device = new FakeByteDevice(i, io);
      devices.add(device);
      cpuContext.getValue().attachDevice(i, device);
    }

    cpu.initialize();

    cpuRunnerImpl = new CpuRunnerImpl(cpu, memory, devices);
    cpuVerifierImpl = new CpuVerifierImpl(cpu, memory, devices);

    Generator.setRandomTestsCount(10);
  }

  @After
  public void tearDown() {
    cpu.destroy();
  }

  protected static class MyIO<T extends WordNumber> implements IO<T> {
    private Map<Integer, FakeByteDevice> devices = new HashMap<>();

    public T in(T port) {
      FakeByteDevice fakeByteDevice = devices.get(port.intValue());
      return  WordNumber.createValue(fakeByteDevice.getValue());
    }

    public void out(T port, T value) {
      devices.get(port.intValue()).setValue((byte) value.intValue());
    }

    public void addDevice(int port, FakeByteDevice device) {
      devices.put(port, device);
    }
  }
}
