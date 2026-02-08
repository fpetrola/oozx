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
import com.fpetrola.z80.bytecode.tests.JetSetWilly2FieldAccessAnalyzer;
import com.fpetrola.z80.bytecode.tests.JetSetWilly2FieldAccessAnalyzer3;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.util.Arrays.asList;

public class Reg16AccessInterceptor {
  private static final Map<String, List<String>> register_pairs = new HashMap<>();

  static {
    register_pairs.put("AF", asList("A", "F"));
    register_pairs.put("BC", asList("B", "C"));
    register_pairs.put("DE", asList("D", "E"));
    register_pairs.put("HL", asList("H", "L"));
    register_pairs.put("IX", asList("IXH", "IXL"));
    register_pairs.put("IY", asList("IYH", "IYL"));
  }

  private static Map<String, WriteState> writeStates = new HashMap<>();

  public static class WriteState {
    private String method;
    private final int regType;

    public WriteState(String method, int regType) {
      this.method = method;
      this.regType = regType;
    }
  }

  public static int log(@Origin Method method, @SuperCall Callable<Integer> zuper, @AllArguments Object[] args, @This JetSetWilly2FieldAccessAnalyzer3 thiz) throws Exception {
//    System.out.println("reg16: " + method.getName() + " " + args.length);
    boolean writing = args.length == 1;
    if (writing) {
      String currentMethod = (String) thiz.methodStack.peek();
      int length = method.getName().length();
      if (length == 1 || length == 3) {
        writeStates.put(method.getName(), new WriteState(currentMethod, 8));
      } else if (length == 2) {
        for (String r : register_pairs.get(method.getName())) {
          writeStates.put(r, new WriteState(currentMethod, 16));
        }
      }
    } else {

    }
    Integer call = zuper.call();
    return call != null ? call : 0;
  }
}
