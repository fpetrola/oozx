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

import com.fpetrola.z80.minizx.sync.DefaultSyncChecker;
import org.easymock.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;

public class GameInvoker {
  public static void main(String[] args) throws InstantiationException {
    ZxGame1 zxGame1 = new ZxGame1();
    zxGame1.setSyncChecker(new DefaultSyncChecker());
//    zxGame1.$34762(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0);
    zxGame1.$8185();
//    zxGame1.$35090();

//    ZxGame1 o= new ZxGame1();
//    o = ClassProxyHelper.createGameInstance(ZxGame1.class);
//    o.$C804();
  }
}
