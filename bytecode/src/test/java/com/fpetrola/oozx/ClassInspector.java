package com.fpetrola.oozx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utilidad para inspeccionar y mostrar el bytecode de clases generadas
 * usando javap.
 */
public class ClassInspector {

  /**
   * Muestra el bytecode de una clase usando javap
   */
  public static void showBytecode(Class<?> clazz) throws IOException, InterruptedException {
    String className = clazz.getName();
    System.out.println("\n" + "=".repeat(80));
    System.out.println("Bytecode para: " + className);
    System.out.println("=".repeat(80) + "\n");

    ProcessBuilder pb = new ProcessBuilder(
        "javap",
        "-v",           // verbose
        "-p",           // show private members
        "-c",           // show bytecode
        "-classpath", "target/test-classes",
        className
    );

    pb.redirectErrorStream(true);
    Process process = pb.start();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
    }

    process.waitFor();
    System.out.println("\n" + "=".repeat(80) + "\n");
  }

  /**
   * Muestra información simplificada de la clase (sin usar javap)
   */
  public static void showClassInfo(Class<?> clazz) {
    System.out.println("\n=== Información de Clase ===");
    System.out.println("Nombre: " + clazz.getName());
    System.out.println("Superclase: " + clazz.getSuperclass().getSimpleName());
    System.out.println("Modificadores: " + clazz.getModifiers());

    System.out.println("\nCampos:");
    for (var field : clazz.getDeclaredFields()) {
      System.out.println("  - " + field.getType().getSimpleName() + " " + field.getName());
    }

    System.out.println("\nConstructores:");
    for (var ctor : clazz.getDeclaredConstructors()) {
      System.out.print("  - " + ctor.getName() + "(");
      var params = ctor.getParameterTypes();
      for (int i = 0; i < params.length; i++) {
        System.out.print(params[i].getSimpleName());
        if (i < params.length - 1) System.out.print(", ");
      }
      System.out.println(")");
    }

    System.out.println("\nMétodos:");
    for (var method : clazz.getDeclaredMethods()) {
      System.out.println("  - " + method.getReturnType().getSimpleName() + " " + method.getName() + "()");
    }
    System.out.println("");
  }
}
