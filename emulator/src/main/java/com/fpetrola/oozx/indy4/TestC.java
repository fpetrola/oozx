/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  agent.IndyBootstrap
 *  com.fpetrola.oozx.indy2.Animal
 *  com.fpetrola.oozx.indy2.Dog
 */
package com.fpetrola.oozx.indy4;

import com.fpetrola.oozx.indy2.*;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestC {

  public static void main(String[] args) {
    // Cargar agente dinámicamente
    loadAgentDynamically();
    
    Animal a = new Dog();
    Animal b = new Cat();
    Test1 test = new Test1(a, b);

    Helper.optimizeInstance(test);  // ¡Magia!

    test.method1(); // Ambas llamadas: a.speak() → Dog.speak() directo
    //                 b.speak() → Cat.speak() directo
    // Totalmente inlineable por la JVM
  }

  private static void loadAgentDynamically() {
    try {
      // Obtener el PID del proceso actual
      String jvmPid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
      System.out.println("[TestC] JVM PID: " + jvmPid);

      // Localizar el JAR del agente (compilado primero)
      File agentJar = findAgentJar();
      if (agentJar == null || !agentJar.exists()) {
        System.err.println("[TestC] ERROR: No se encontró el agente JAR");
        return;
      }

      System.out.println("[TestC] Cargando agente: " + agentJar.getAbsolutePath());

      // Adjuntar el agente dinámicamente
      VirtualMachine vm = VirtualMachine.attach(jvmPid);
      try {
        vm.loadAgent(agentJar.getAbsolutePath());
        System.out.println("[TestC] Agente cargado exitosamente");
      } finally {
        vm.detach();
      }
    } catch (Exception e) {
      System.err.println("[TestC] Error cargando agente dinámicamente");
      e.printStackTrace();
    }
  }

  private static File findAgentJar() {
    // Buscar el JAR del agente con el manifest correcto
    String[] possiblePaths = {
        "emulator/target/emulator-0.0.1-SNAPSHOT-agent.jar",
        "target/emulator-0.0.1-SNAPSHOT-agent.jar",
        "emulator/target/classes",  // Fallback: usa el directorio compilado
        "target/classes"
    };

    for (String path : possiblePaths) {
      File f = new File(path);
      if (f.exists()) {
        System.out.println("[TestC] Encontrado: " + path);
        return f;
      }
    }

    return null;
  }
}
