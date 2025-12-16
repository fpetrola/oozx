// src/main/java/agent/IndyBootstrap.java
package com.fpetrola.oozx.indy;

import java.lang.invoke.*;

public class IndyBootstrap {

    /**
     * Bootstrap method para invokedynamic que reemplaza invokevirtual.
     *
     * @param lookup   Lookup del callsite
     * @param name     Nombre del método
     * @param type     Tipo del método (incluye receiver como primer parámetro)
     * @param owner    Clase owner en formato interno (ej: java/lang/String)
     * @param methodName Nombre del método
     * @param methodDesc Descriptor del método
     * @return CallSite constante con el MethodHandle correcto
     */
    public static CallSite bootstrapVirtual(MethodHandles.Lookup lookup,
                                            String name,
                                            MethodType type,
                                            String owner,
                                            String methodName,
                                            String methodDesc) throws Throwable {

        // Resolvemos el método virtual real
        Class<?> ownerClass = Class.forName(owner.replace('/', '.'), false, lookup.lookupClass().getClassLoader());
        MethodHandle mh = lookup.unreflect(ownerClass.getDeclaredMethod(methodName,
                type.dropParameterTypes(0, 1).parameterArray())); // quita receiver del tipo

        // Ajustamos para que coincida exactamente con el tipo del callsite (incluye receiver)
        mh = mh.asType(type);

        // Usamos ConstantCallSite para máximo rendimiento (JVM lo optimiza como invokevirtual)
        return new ConstantCallSite(mh);
    }
}