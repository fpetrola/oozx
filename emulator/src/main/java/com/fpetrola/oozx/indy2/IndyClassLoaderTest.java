// src/test/java/agent/IndyClassLoaderTest.java
package com.fpetrola.oozx.indy2;

public class IndyClassLoaderTest {
  public String callSpeak(Animal a) {
    return a.speak(); // Esta llamada será transformada en invokedynamic
  }

  public static void main(String[] args) {
    Dog a = new Dog();
    IndyClassLoaderTest indyClassLoaderTest = new IndyClassLoaderTest();
    indyClassLoaderTest.callSpeak(a);
  }
}

