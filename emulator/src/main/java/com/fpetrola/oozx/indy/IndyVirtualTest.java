// src/test/java/agent/IndyVirtualTest.java
package com.fpetrola.oozx.indy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Animal {
    String speak() {
        return "???";
    }
}

class Dog extends Animal {
    @Override
    String speak() {
        return "Woof!";
    }
}

class Cat extends Animal {
    @Override
    String speak() {
        return "Meow!";
    }
}

public class IndyVirtualTest {

    private String callSpeak(Animal a) {
        // Esta llamada será transformada en invokedynamic por el agente
        return a.speak();
    }

    @Test
    void testVirtualCallTransformed() {
        Animal dog = new Dog();
        Animal cat = new Cat();

        assertEquals("Woof!", callSpeak(dog));
        assertEquals("Meow!", callSpeak(cat));

        // Llamamos muchas veces para forzar optimización JIT
        for (int i = 0; i < 20_000; i++) {
            callSpeak(i % 2 == 0 ? dog : cat);
        }

        // Verificamos que sigue funcionando después de warmup
        assertEquals("Woof!", callSpeak(dog));
        assertEquals("Meow!", callSpeak(cat));
    }
}