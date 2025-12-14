package com.fpetrola.oozx.specializer;

import java.util.List;

public class Example {
    public static void main(String[] args) {
        List<String> original = new MyList<String>();
        original.add("test");

        List<String> specialized = VirtualToIndyTransformer.specialize(original);
//        List<String> specialized = Devirtualizer.specialize(original);

        // Ahora todas las llamadas a métodos de List sobre 'specialized'
        // resuelven directamente a las implementaciones de ArrayList,
        // y el JIT las inlineará agresivamente.
        specialized.size();
        specialized.add("foo");

        for (String s : specialized) {
            System.out.println(s);
        }
        // etc.
    }
}