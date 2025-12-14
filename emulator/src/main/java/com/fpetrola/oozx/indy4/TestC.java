/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  agent.IndyBootstrap
 *  com.fpetrola.oozx.indy2.Animal
 *  com.fpetrola.oozx.indy2.Dog
 */
package com.fpetrola.oozx.indy4;

import com.fpetrola.oozx.indy2.*;

public class TestC {

  public static void main(String[] args) {
    Animal a = new Dog();
    Animal b = new Cat();
    Test1 test = new Test1(a, b);

    Helper.optimizeInstance(test);  // ¡Magia!

    test.method1(); // Ambas llamadas: a.speak() → Dog.speak() directo
    //                 b.speak() → Cat.speak() directo
    // Totalmente inlineable por la JVM
  }
}
