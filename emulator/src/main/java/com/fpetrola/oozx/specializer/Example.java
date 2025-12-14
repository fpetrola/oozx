package com.fpetrola.oozx.specializer;

import java.util.List;

public class Example {
    public static void main(String[] args) {
        List<String> original = new MyList<String>();
        original.add("test");
        original.add("bar");

        // Especializamos la instancia original
        // La clase resultante tiene invokevirtual/invokeinterface → invokedynamic
        List<String> specialized = VirtualToIndyTransformer.specialize(original);

        System.out.println("Original: " + original.size() + " items");
        System.out.println("Specialized: " + specialized.size() + " items");

        // Todas las llamadas ahora usan invokedynamic vinculado a la instancia real
        specialized.add("foo");
        specialized.add("baz");

        System.out.println("\nItems en specialized:");
        for (String s : specialized) {
            System.out.println("  - " + s);
        }

        System.out.println("\nTipos:");
        System.out.println("  Original: " + original.getClass().getName());
        System.out.println("  Specialized: " + specialized.getClass().getName());
    }
}