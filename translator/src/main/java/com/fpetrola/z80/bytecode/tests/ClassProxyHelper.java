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

import com.fpetrola.z80.minizx.NotSolvedStackException;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.minizx.StackException;
import org.easymock.bytebuddy.ByteBuddy;
import org.easymock.bytebuddy.implementation.MethodDelegation;
import org.easymock.bytebuddy.implementation.bind.annotation.*;
import org.easymock.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ClassProxyHelper {
  static <T extends SpectrumApplication> T createGameInstance(Class<T> superType) throws InstantiationException {
    ZxGame1 o;
    try {
      Class<?> type = new ByteBuddy()
          .subclass(superType)
          .method(ElementMatchers.any()).intercept(MethodDelegation.to(Interceptor.class))
          .make()
          .load(superType.getClassLoader())
          .getLoaded();

      o = (ZxGame1) type.newInstance();
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return (T) o;
  }

  static Object invokeMethod(Object self, Method method, Object[] args, Method superMethod) throws Throwable {
    if (method.getName().startsWith("$")) {
      Object invoke = null;
      try {
        invoke = superMethod.invoke(self, args);
      } catch (InvocationTargetException e) {
        processException(self, args, superMethod, e);
      }
      return invoke;
    } else
      return superMethod.invoke(self, args);
  }

  private static void processException(Object self, Object[] args, Method superMethod, InvocationTargetException e) throws Throwable {
    SpectrumApplication spectrumApplication = (SpectrumApplication) self;
    if (e.getCause() instanceof StackException stackException) {
      spectrumApplication.setNextAddress(stackException.getNextPC());
      try {
        superMethod.invoke(self, args);
      } catch (InvocationTargetException ex) {
        processException(self, args, superMethod, ex);
      }
    } else if (e.getCause() instanceof NotSolvedStackException stackException) {
      spectrumApplication.setNextAddress(stackException.getNextPC());
      throw new StackException(stackException.getNextPC());
    }
    throw e.getCause();
  }

  public class Interceptor {
    @RuntimeType
    public static Object intercept(@This Object self,
                                   @Origin Method method,
                                   @AllArguments Object[] args,
                                   @SuperMethod Method superMethod) throws Throwable, IllegalAccessException {
      return ClassProxyHelper.invokeMethod(self, method, args, superMethod);
    }
  }
}
