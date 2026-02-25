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

import com.fpetrola.z80.bytecode.tests.JetSetWilly2Converted;
import com.fpetrola.z80.bytecode.tests.JetSetWilly2FieldAccessAnalyzer3;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.function.Predicate;

public class GameRZXInvokerConverted {
  public static void main(String[] args) {
    try {
      RZXPlayerIO miniZXIO = new RZXPlayerIO();
      Predicate<Integer> interruptionCondition = miniZXIO.getInterruptionCondition();
      String[] s = {"AF, BC", "DE", "HL", "IX", "IY", "A, B", "D", "H", "IXH", "IYH", "F, C", "E", "L", "IXL", "IYL"};
      Constructor<?>[] constructors = new ByteBuddy()
          .subclass(JetSetWilly2Converted.class)
          .method(ElementMatchers.named("pc")).intercept(MethodDelegation.to(PcInterceptor.class))
          .method(ElementMatchers.nameStartsWith("$")).intercept(MethodDelegation.to(RoutineCallInterceptor.class))
          .method(ElementMatchers.namedOneOf(s)).intercept(MethodDelegation.to(Reg16AccessInterceptor.class))
          .make()
          .load(GameRZXInvokerConverted.class.getClassLoader())
          .getLoaded()
          .getConstructors();

      Constructor<?> constructor = Arrays.stream(constructors).filter(c -> c.getParameterCount() == 2).findFirst().get();

      JetSetWilly2Converted jetSetWilly1 = (JetSetWilly2Converted) constructor.newInstance(miniZXIO, interruptionCondition);

      jetSetWilly1.$34463();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
