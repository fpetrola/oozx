/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.emustudio.plugins.cpu.zilogZ80;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.WordNumber;
import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
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
  private OOZ80 ooz80;
  private MyIO io;
  private final List<FakeByteDevice> devices = new ArrayList<>();
  CpuRunnerImpl cpuRunnerImpl;
  CpuVerifierImpl cpuVerifierImpl;
  protected CpuImpl cpu;
  protected MyByteMemoryStub memory;

  public InstructionsTest() {
    try {
      Capture<Context8080> cpuContext = Capture.newInstance();
      ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
      expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
      contextPool.register(anyLong(), capture(cpuContext), same(Context8080.class));
      expectLastCall().anyTimes();
      replay(contextPool);

      ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
      expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
      replay(applicationApi);


      io = new MyIO();
      ooz80 = Helper.createOOZ80(io);
      memory = new MyByteMemoryStub();
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
    } catch (ContextAlreadyRegisteredException | PluginInitializationException e) {
      throw new RuntimeException(e);
    }


  }

  @After
  public void tearDown() {
    cpu.destroy();
  }

  protected static class MyIO<T extends WordNumber> implements IO<T> {
    private final Map<Integer, FakeByteDevice> devices = new HashMap<>();

    public T in(T port) {
      FakeByteDevice fakeByteDevice = devices.get(port.intValue());
      return WordNumber.createValue(fakeByteDevice.getValue());
    }

    public void out(T port, T value) {
      devices.get(port.intValue()).setValue((byte) value.intValue());
    }

    public void addDevice(int port, FakeByteDevice device) {
      devices.put(port, device);
    }
  }
}
