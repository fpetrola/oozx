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

@SuppressWarnings("ALL")
public abstract class MiniZX extends SyncSpectrumApplication {
  public MiniZX() {
    init();
  }

  public void init() {
    this.mem = new int[65536];
    MiniZX.createScreen(((MiniZXIO) io).miniZXKeyboard, new MiniZXScreen(this.getMemFunction()));
    final byte[] rom = MiniZXWithEmulationBase.createROM();
    final byte[] bytes = MiniZXWithEmulationBase.gzipDecompressFromBase64(this.getProgramBytes());
    for (int i = 0; i < 65536; ++i) {
      this.getMem()[i] = ((i < 16384) ? rom[i] : bytes[i]) & 0xff;
    }

    customizeMemory();

    syncChecker.init(this);
  }

  protected void customizeMemory() {
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> syncChecker.getByteFromEmu(index);
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
