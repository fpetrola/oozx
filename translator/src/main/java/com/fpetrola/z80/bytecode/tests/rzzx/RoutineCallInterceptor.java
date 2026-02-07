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

import com.fpetrola.z80.bytecode.tests.JetSetWilly;
import com.fpetrola.z80.bytecode.tests.JetSetWilly2;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

public class RoutineCallInterceptor {
  public static void log(@Origin Method method, @SuperCall Callable<List<String>> zuper, @AllArguments Object[] args, @This JetSetWilly2 thiz) throws Exception {
//    System.out.println("Calling: " + method.getName());
    thiz.methodStack.push(method.getName());
    zuper.call();
    thiz.methodStack.pop();
//    System.out.println("Exiting: " + method.getName());
  }
}
