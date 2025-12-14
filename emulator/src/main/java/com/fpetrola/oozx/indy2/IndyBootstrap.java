// src/main/java/agent/IndyBootstrap.java
package com.fpetrola.oozx.indy2;

import java.lang.invoke.*;

public class IndyBootstrap {
    public static CallSite bootstrapVirtual(MethodHandles.Lookup lookup,
                                            String name,
                                            MethodType type,
                                            String ownerInternal,
                                            String methodName,
                                            String methodDesc) throws Throwable {

        Class<?> ownerClass = Class.forName(ownerInternal.replace('/', '.'), false, lookup.lookupClass().getClassLoader());

        // Obtenemos el MethodHandle del método virtual real
        MethodType originalType = MethodType.fromMethodDescriptorString(methodDesc, lookup.lookupClass().getClassLoader());
        MethodHandle mh = lookup.findVirtual(ownerClass, methodName, originalType);

        // Ajustamos al tipo del invokedynamic (que incluye el receiver explícitamente)
        mh = mh.asType(type);

        return new ConstantCallSite(mh);
    }
}