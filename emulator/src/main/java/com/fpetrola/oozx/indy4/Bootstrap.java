package com.fpetrola.oozx.indy4;

import java.lang.invoke.*;

public class Bootstrap {
    public static CallSite bootstrap(MethodHandles.Lookup lookup,
                                     String name,
                                     MethodType type,
                                     Class<?> concreteType) throws Throwable {

        MethodType targetType = type.dropParameterTypes(0, 1);
        MethodHandle mh = lookup.findVirtual(concreteType, name, targetType);
        return new ConstantCallSite(mh);
    }
}