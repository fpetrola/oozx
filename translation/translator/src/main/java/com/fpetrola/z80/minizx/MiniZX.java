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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("ALL")
public abstract class MiniZX extends SpectrumApplication {
  private Predicate<Integer> interruptionCondition;
  public int fetchCounter;
  private long interrupts;

  public MiniZX() {
    init();
  }

  public MiniZX(MiniZXIO miniZXIO) {
    io = miniZXIO;
    init();
  }

  public MiniZX(MiniZXIO miniZXIO, Predicate<Integer> interruptionCondition) {
    this(miniZXIO);
    this.interruptionCondition = interruptionCondition;
  }

  public void setInterruptionCondition(Predicate<Integer> interruptionCondition) {
    this.interruptionCondition = interruptionCondition;
  }

  public void enterMethod(String methodName) {
  }

  public void exitMethod(String name) {
  }

  public void pc(int address, int rdelta) {
//    super.pc(address, rdelta);
//    if (address > 0) {
//      int stackDelta = getStackDelta();
//      if (stackDelta != 0)
//        System.out.println(address);
//    }

    PC = address;
    int early = enteringHandler ? rdelta : 0;
    enteringHandler = false;
    fetchCounter += early;
    for (boolean accepting = acceptsInterrupt(); frameEnds() && accepting; accepting = iff) {
      interrupt();
      PC = address;
    }
    R = R & 0x80 | R + rdelta & 0x7f;
    fetchCounter += rdelta - early;
  }

  public void run(int entry) {
    for (int address = entry; ; )
      try {
        invokeMethod(address);
        address = pop();
      } catch (StackException unwound) {
        address = unwound.getNextPC();
      }
  }

  private record Parked(Thread thread, int top) {}

  private final Map<Integer, Parked> stacks = new HashMap<>();
  private volatile int runningStack, leavingSp;
  private volatile Throwable failure;

  public void leavingStack() {
    leavingSp = SP;
  }

  public void switchStack() {
    int mine = leavingSp;
    stacks.values().removeIf(parked -> parked.thread() == Thread.currentThread());
    stacks.put(mine, new Parked(Thread.currentThread(), mem16(mine, -1)));
    runningStack = SP;
    Parked other = stacks.get(SP);
    if (other == null || other.top() != mem16(SP, -1))
      stacks.put(SP, new Parked(Thread.ofVirtual().start(() -> {
        try {
          run(pop());
        } catch (Throwable e) {
          failure = e;
        }
        runningStack = mine;
        LockSupport.unpark(stacks.get(mine).thread());
      }), mem16(SP, -1)));
    else
      LockSupport.unpark(other.thread());
    while (runningStack != mine)
      LockSupport.park();
    if (failure != null)
      throw MiniZX.<RuntimeException>sneaky(failure);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> T sneaky(Throwable failure) throws T {
    throw (T) failure;
  }

  public void halt(int address) {
    for (long accepted = interrupts; interrupts == accepted; ) {
      PC = address;
      if (frameEnds() && acceptsInterrupt()) {
        PC = address + 1;
        interrupt();
      } else {
        R = R & 0x80 | R + 1 & 0x7f;
        fetchCounter++;
        tstates += 4;
      }
    }
  }

  private boolean enteringHandler;

  private boolean frameEnds() {
    boolean ended = interruptionCondition != null && interruptionCondition.test(fetchCounter);
    if (ended && sound != null)
      sound.frame(tstates);
    return ended;
  }

  private void interrupt() {
    iff = iff2 = false;
    int vector = I << 8 | 0xff;
    fetchCounter++;
    R = R & 0x80 | R + 1 & 0x7f;
    tstates += interruptMode == 2 ? 19 : 13;
    enteringHandler = true;
    invokeMethod(interruptMode == 2 ? mem[vector] | mem[vector + 1 & 0xffff] << 8 : 0x38);
    interrupts++;
  }

  public void init() {
    this.mem = new int[65536];
    // -Dminizx.headless=true: analysis runs must not open the live screen — its frame
    // uses EXIT_ON_CLOSE, so closing it would System.exit(0) mid-analysis
    if (!Boolean.getBoolean("minizx.headless")) {
      MiniZX.createScreen(((MiniZXIO) io).getMiniZXKeyboard(), new MiniZXScreen(this.getMemFunction()));
      sound = new MiniZXSound(new com.fpetrola.oozx.speccy.modules.sound.JavaSoundDevice());
    }
    final byte[] rom = MiniZXWithEmulationBase.createROM();
    final byte[] bytes = MiniZXWithEmulationBase.gzipDecompressFromBase64(this.getProgramBytes());
    for (int i = 0; i < 65536; ++i) {
      mem[i] = ((i < 16384) ? rom[i] : bytes[i]) & 0xff;
    }

    IY(0x5c3a);
    customizeMemory();

    syncChecker.init(this);
  }

  protected void customizeMemory() {
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> syncChecker.getByteFromEmu(index);
//    return index -> mem[index];

  }

  protected abstract String getProgramBytes();

  public static JFrame createScreen(KeyListener keyListener, Container miniZXScreen1) {
    JFrame frame = new JFrame("Mini ZX Spectrum");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setContentPane(miniZXScreen1);
    frame.setLocationRelativeTo(null);
    frame.setSize(512, 384);
    frame.pack();
    frame.setVisible(true);
    frame.addKeyListener(keyListener);
    return frame;
  }

}
