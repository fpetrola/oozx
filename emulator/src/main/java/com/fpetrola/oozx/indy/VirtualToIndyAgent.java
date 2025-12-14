// src/main/java/agent/VirtualToIndyAgent.java
package com.fpetrola.oozx.indy;

import java.lang.instrument.Instrumentation;

public class VirtualToIndyAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new IndyVirtualTransformer());
        System.out.println("[VirtualToIndyAgent] Agente activado: transformando invokevirtual -> invokedynamic");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}