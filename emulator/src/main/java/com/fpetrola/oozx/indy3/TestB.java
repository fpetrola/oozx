/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  agent.IndyBootstrap
 *  com.fpetrola.oozx.indy2.Animal
 *  com.fpetrola.oozx.indy2.Dog
 */
package com.fpetrola.oozx.indy3;

import com.fpetrola.oozx.indy2.*;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TestB {
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  private Object callSpeak(Animal a) {
    Animal animal = a;

    MethodType methodType = getMethodType();
    try {
      CallSite callSpeak = IndyBootstrap.bootstrapVirtual(LOOKUP, "com/fpetrola/oozx/indy2/Animal", methodType, "com/fpetrola/oozx/indy2/IndyClassLoaderTest", "callSpeak", "(Lcom/fpetrola/oozx/indy2/Animal;)Ljava/lang/String;");
      return callSpeak.dynamicInvoker().invoke();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static MethodType getMethodType() {
    return MethodType.methodType(String.class, IndyClassLoaderTest.class, Animal.class);
  }

  public static void main(String[] args) {
    Animal a = new Cat();
    IndyClassLoaderTest indyClassLoaderTest = new IndyClassLoaderTest();
    MethodType methodType = getMethodType();
    try {
      CallSite callSpeak = IndyBootstrap.bootstrapVirtual(LOOKUP, "com/fpetrola/oozx/indy2/Animal", methodType, "com/fpetrola/oozx/indy2/IndyClassLoaderTest", "callSpeak", "(Lcom/fpetrola/oozx/indy2/Animal;)Ljava/lang/String;");
      String invoke = (String) callSpeak.dynamicInvoker().invokeExact(indyClassLoaderTest, a);
      System.out.println(invoke);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }  }
}
