package com.fpetrola.oozx.indy4;

import java.lang.instrument.Instrumentation;

public class Agent {
    public static void premain(String agentArgs, Instrumentation inst) {
        // Registrar el instrumentation en Helper para que pueda usarlo
        Helper.setInstrumentation(inst);
        
        // Registrar el ClassFileTransformer
        inst.addTransformer(new ConstantReceiverTransformer(), true);
        
        System.out.println("[Agent] Loaded dynamically");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        // Llamado cuando se adjunta el agente dinámicamente
        premain(agentArgs, inst);
    }
}
