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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("ALL")
public abstract class MiniZX extends SpectrumApplication {
  private Predicate<Integer> interruptionCondition;
  public int fetchCounter;

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

    if (interruptionCondition != null)
      interruptionCondition.test(fetchCounter);
    fetchCounter += rdelta;
  }

  public void init() {
    this.mem = new int[65536];
    // -Dminizx.headless=true: analysis runs must not open the live screen — its frame
    // uses EXIT_ON_CLOSE, so closing it would System.exit(0) mid-analysis
    if (!Boolean.getBoolean("minizx.headless"))
      MiniZX.createScreen(((MiniZXIO) io).getMiniZXKeyboard(), new MiniZXScreen(this.getMemFunction()));
    final byte[] rom = MiniZXWithEmulationBase.createROM();
    final byte[] bytes = MiniZXWithEmulationBase.gzipDecompressFromBase64(this.getProgramBytes());
    for (int i = 0; i < 65536; ++i) {
      mem[i] = ((i < 16384) ? rom[i] : bytes[i]) & 0xff;
    }

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
