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

import com.fpetrola.z80.cpu.RegistersSetter;
import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.minizx.sync.DefaultSyncChecker;
import com.fpetrola.z80.opcodes.references.WordNumber;
import fj.test.Bool;
import org.easymock.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class GameInvoker {
  public static void main(String[] args) throws InstantiationException {
//    ZxGame1C zxGame1 = new ZxGame1C();
//    zxGame1.setSyncChecker(new DefaultSyncChecker());
//    zxGame1.$34762(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0);
//    zxGame1.$8185();
    RZXPlayerIO miniZXIO = new RZXPlayerIO() {
      @Override
      public synchronized WordNumber in(WordNumber port) {
        WordNumber in = super.in(port);
//        System.out.println(in);
        return in;
      }
    };
    String url = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/jsw/Jet Set Willy - Mildly Patched.rzx";
    Predicate<Integer> interruptionCondition = miniZXIO.getInterruptionCondition();
//    JetSetWilly spectrumApplication = new JetSetWilly(miniZXIO, interruptionCondition);
//    EmulatedMiniZX.setupRzx(new MyRegistersSetter(spectrumApplication), miniZXIO, url, new MyMemorySetter(spectrumApplication));

    DefaultMiniZXIO<WordNumber> rzxPlayerIO = new DefaultMiniZXIO<>();
    JetSetWilly zxGame2 = new JetSetWilly(rzxPlayerIO, interruptionCondition) {
      @Override
      public void pc(int address, int rdelta) {
        SpectrumApplication.waitNanos(7000);
        super.pc(address, rdelta);
      }
    };
    zxGame2.$34463();


    JetSetWilly zxGame1 = new JetSetWilly(miniZXIO, interruptionCondition) {
      private boolean initializing = true;

      public void pc(int address, int rdelta) {
        SpectrumApplication.waitNanos(3000);

        if (initializing) {
          if (address == 34629)
            F = 0;

          if (address == 34637)
            F = 1;

          if (address == 34720)
            F = 1;

          if (address == 34726)
            F = 1;
          if (address == 34732)
            F = 1;

          if (address == 34738) {
            EmulatedMiniZX.setupRzx(new MyRegistersSetter(this), miniZXIO, url, new MyMemorySetter(this));
            initializing = false;
            fetchCounter = -2;
          }
        }
        super.pc(address, rdelta);
      }
    };
    zxGame1.$34463();


//    ZxGame1 o= new ZxGame1();
//    o = ClassProxyHelper.createGameInstance(ZxGame1.class);
//    zxGame1.$C804();
//    zxGame1.$8185();
  }
}
