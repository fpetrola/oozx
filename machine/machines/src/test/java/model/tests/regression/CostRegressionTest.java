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

package model.tests.regression;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.Wiring;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.PortHandler;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cost-regression companion to {@link EmulationRegressionTest}: that one pins behaviour, this
 * one pins allocation and handler-dispatch counts. Wall-clock timing is unreliable (~15%
 * run-to-run variance from scaling/JIT/background load), so deterministic counts are used
 * instead: handlers reached per port access, and bytes allocated per frame (measured as the
 * minimum of several post-warmup blocks, since escape analysis keeps improving for a while).
 */
public class CostRegressionTest extends MachineTest {
  private static final int START = 0x8000;
  private static final int PORT = 0xffff;
  /** Simulated handler count matching a fully-loaded machine, none decoding the test port. */
  private static final int DEAF_HANDLERS = 24;
  private static final int WARMUP_FRAMES = 400;
  private static final int BLOCK_FRAMES = 40;
  private static final int BLOCKS = 5;
  private static final int BOOT_FRAMES = 268;

  /** Golden figures from commit 65e25f5ba on JDK 21; any change here must be a deliberate
   * decision, not an unnoticed drift. */
  private static final long PORT_READS = 114983;
  private static final long PORT_WRITES = 114983;
  private static final long BYTES_PER_IDLE_FRAME = 512;
  private static final long BYTES_PER_PORT_ACCESS = 8;
  private static final long BYTES_PER_BOOT_FRAME = 768;

  /** Loop body: LD BC,0xffff followed by repeated IN A,(C) / OUT (C),D. */
  private static final int UNROLL = 32;

  @Test
  public void aPortAccessReachesOnlyTheHandlersThatDecodeIt() {
    Speccy speccy = silentMachine();
    Bank bank = plug(speccy);
    load(speccy, hammer());
    runFrames(speccy, WARMUP_FRAMES);

    bank.forget();
    runFrames(speccy, BLOCK_FRAMES);

    assertEquals(0, bank.deafReads + bank.deafWrites,
        "a port access asked a handler that does not decode that port: the bus is walking its list again");
    System.out.printf("%d frames of the hammer: %d reads, %d writes, %d decodes%n", BLOCK_FRAMES, bank.reads, bank.writes, bank.decodes);
    assertEquals(PORT_READS, bank.reads, "port reads in " + BLOCK_FRAMES + " frames");
    assertEquals(PORT_WRITES, bank.writes, "port writes in " + BLOCK_FRAMES + " frames");
    assertEquals(0, bank.decodes,
        "handlers asked to decode " + bank.decodes + " times for a port already asked for: who answers a port is worked out once, not on every access");
  }

  @Test
  public void aFrameAndAPortAccessCostWhatTheyCost() {
    Speccy idle = silentMachine();
    load(idle, loop());
    long bytesPerIdleFrame = bytesPerFrame(idle);

    Speccy hammering = silentMachine();
    plug(hammering);
    load(hammering, hammer());
    long bytesPerHammeredFrame = bytesPerFrame(hammering);

    long accesses = (PORT_READS + PORT_WRITES) / BLOCK_FRAMES;
    long bytesPerAccess = Math.max(0, bytesPerHammeredFrame - bytesPerIdleFrame) / accesses;

    Speccy booting = silentMachine();
    long bytesPerBootFrame = bytesPerFrame(booting, BOOT_FRAMES / 2, BOOT_FRAMES);

    System.out.printf("idle %d bytes/frame, ports %d bytes/access, boot %d bytes/frame%n",
        bytesPerIdleFrame, bytesPerAccess, bytesPerBootFrame);

    assertTrue(bytesPerIdleFrame <= BYTES_PER_IDLE_FRAME,
        "a frame with the processor in a loop allocates " + bytesPerIdleFrame + " bytes, budget " + BYTES_PER_IDLE_FRAME);
    assertTrue(bytesPerAccess <= BYTES_PER_PORT_ACCESS,
        "a port access allocates " + bytesPerAccess + " bytes, budget " + BYTES_PER_PORT_ACCESS);
    assertTrue(bytesPerBootFrame <= BYTES_PER_BOOT_FRAME,
        "a frame of the ROM booting allocates " + bytesPerBootFrame + " bytes, budget " + BYTES_PER_BOOT_FRAME);
  }

  private long bytesPerFrame(Speccy speccy) {
    return bytesPerFrame(speccy, WARMUP_FRAMES, BLOCK_FRAMES);
  }

  /** Minimum allocation across several measured blocks, taken as the JIT-settled figure. */
  private long bytesPerFrame(Speccy speccy, int warmup, int frames) {
    runFrames(speccy, warmup);
    long least = Long.MAX_VALUE;
    for (int block = 0; block < BLOCKS; block++) {
      long before = allocated();
      runFrames(speccy, frames);
      least = Math.min(least, (allocated() - before) / frames);
    }
    return least;
  }

  private static long allocated() {
    return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getCurrentThreadAllocatedBytes();
  }

  private Bank plug(Speccy speccy) {
    Bank bank = new Bank();
    speccy.peripheralRegistry.register(bank);
    speccy.peripheralRegistry.activateType(Bank.class, true);
    return bank;
  }

  private void load(Speccy speccy, int[] program) {
    for (int i = 0; i < program.length; i++) {
      speccy.memory.poke(START + i, (byte) program[i]);
    }
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
  }

  private int[] loop() {
    return new int[]{0xc3, START & 0xff, START >> 8};
  }

  private int[] hammer() {
    int[] program = new int[3 + UNROLL * 4 + 3];
    program[0] = 0x01;
    program[1] = PORT & 0xff;
    program[2] = PORT >> 8;
    for (int i = 0; i < UNROLL; i++) {
      program[3 + i * 4] = 0xed;
      program[4 + i * 4] = 0x78;   // ED 78: IN A,(C)
      program[5 + i * 4] = 0xed;
      program[6 + i * 4] = 0x51;   // ED 51: OUT (C),D
    }
    int loop = START + 3;
    program[3 + UNROLL * 4] = 0xc3;
    program[4 + UNROLL * 4] = loop & 0xff;
    program[5 + UNROLL * 4] = loop >> 8;
    return program;
  }

  /** Fake peripheral bank: one handler answering the hammered port, plus many decoy handlers
   * on other ports. Counters are increment-only, so measurement itself allocates nothing. */
  private static class Bank implements Peripheral {
    long reads, writes, deafReads, deafWrites, decodes;
    final Wired[] ports = new Wired[DEAF_HANDLERS + 1];

    Bank() {
      ports[0] = handler(0, 0, true);
      for (int i = 0; i < DEAF_HANDLERS; i++) {
        ports[i + 1] = handler(0xffff, 0xe000 + i, false);
      }
    }

    void forget() {
      reads = writes = deafReads = deafWrites = decodes = 0;
    }

    private Wired handler(int mask, int value, boolean answers) {
      Wiring wiring = port -> {
        decodes++;
        return (port & mask) == value;
      };
      return new Wired(wiring, new PortHandler() {
        public BusAnswer read(int port) {
          if (answers) reads++;
          else deafReads++;
          return BusAnswer.of(0xff);
        }

        public void write(int port, byte b) {
          if (answers) writes++;
          else deafWrites++;
        }

        public boolean isReader() {
          return true;
        }

        public boolean isWriter() {
          return true;
        }
      });
    }

    public void activate(SpectrumMachine machine) {
    }

    public void deactivate() {
    }

    public Wired[] getPorts() {
      return ports;
    }

    public boolean fitsOn(SpectrumMachine machine) {
      return true;
    }

    public boolean isWanted() {
      return true;
    }

    public boolean hasHardReset() {
      return false;
    }
  }
}
