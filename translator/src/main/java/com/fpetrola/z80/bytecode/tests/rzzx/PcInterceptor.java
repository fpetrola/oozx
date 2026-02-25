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

package com.fpetrola.z80.bytecode.tests.rzzx;

import com.fpetrola.z80.bytecode.tests.*;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.util.List;
import java.util.concurrent.Callable;

public class PcInterceptor {
  private static boolean initializing = true;

  public static void log(@SuperCall Callable<List<String>> zuper, @AllArguments Object[] args, @This JetSetWilly2Converted thiz) throws Exception {
//        SpectrumApplication.waitNanos(2000);

    if (initializing) {
      int address = (int) args[0];
      if (address == 34629)
        thiz.F(0);

      if (address == 34637)
        thiz.F(1);

      if (address == 34720)
        thiz.F(1);

      if (address == 34726)
        thiz.F(1);
      if (address == 34732)
        thiz.F(1);

      if (address == 34738) {
        String url = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/jsw/Jet Set Willy - Mildly Patched.rzx";

        EmulatedMiniZX.setupRzx(new MyRegistersSetter(thiz), (MiniZXIO) thiz.io, url, new MyMemorySetter(thiz));
        initializing = false;
        thiz.fetchCounter = -2;
      }
    }
    zuper.call();
  }
}
