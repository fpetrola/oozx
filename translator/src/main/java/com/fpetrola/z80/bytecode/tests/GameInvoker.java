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

package com.fpetrola.z80.bytecode.tests;

import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.minizx.ZXScreenComponent;
import com.fpetrola.z80.opcodes.references.WordNumber;

import javax.swing.*;

public class GameInvoker {
  public static void main(String[] args) throws InstantiationException {
    DD0 zxGame1 = new DD0(){
      public int in(int port, int pc) {
        try {
          Thread.sleep(20L);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        return super.in(port, pc);
      }
    };
//    zxGame1.setSyncChecker(new DefaultSyncChecker());
//    zxGame1.$34762(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0);
//    zxGame1.$C804();
    zxGame1.getMem()[33824] = 29;
    ZXScreenComponent<WordNumber> zxScreenComponent = zxGame1.getZxScreenComponent();
    new Timer(20, e -> {
      zxScreenComponent.repaint();
    }).start();
    MiniZX.createScreen(((DefaultMiniZXIO) SpectrumApplication.io).miniZXKeyboard, zxScreenComponent);

    zxGame1.$C804();

//    ZxGame1 o= new ZxGame1();
//    o = ClassProxyHelper.createGameInstance(ZxGame1.class);
//    o.$C804();
  }
}
