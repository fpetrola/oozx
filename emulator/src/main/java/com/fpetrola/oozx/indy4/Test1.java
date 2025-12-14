package com.fpetrola.oozx.indy4;

import com.fpetrola.oozx.indy2.Animal;

public class Test1 {
  Animal a;
  Animal b;

  public Test1(Animal a, Animal b) {
    this.a = a;
    this.b = b;
  }

  void method1() {
    a.speak();
    b.speak();
  }
}