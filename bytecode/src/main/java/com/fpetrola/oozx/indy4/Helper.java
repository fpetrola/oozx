package com.fpetrola.oozx.indy4;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

public class Helper {

    private static Instrumentation instrumentation;

    // Mapa: instancia → mapa de field name → tipo concreto del valor actual
    public static final Map<Object, Map<String, Class<?>>> instanceFieldTypes = new WeakHashMap<>();

    public static void setInstrumentation(Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * Optimiza TODAS las llamadas virtuales dentro de la clase de esta instancia
     * cuyo receiver provenga de un field con valor conocido en runtime.
     */
    public static void optimizeInstance(Object instance) {
        if (instance == null || instrumentation == null) return;

        Class<?> clazz = instance.getClass();

        // Analizamos todos los fields declarados y capturamos sus tipos concretos actuales
        Map<String, Class<?>> fieldTypes = new java.util.HashMap<>();
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object value = f.get(instance);
                if (value != null) {
                    fieldTypes.put(f.getName(), value.getClass());
                }
            } catch (IllegalAccessException e) {
                // ignoramos
            }
        }

        if (fieldTypes.isEmpty()) return;

        synchronized (instanceFieldTypes) {
            instanceFieldTypes.put(instance, fieldTypes);
        }

        // Marcamos que hay datos nuevos y retransformamos solo la clase del instance
        ConstantReceiverTransformer.triggerRetransform(clazz);

        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}