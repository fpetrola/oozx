package com.fpetrola.oozx;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Utilidad para escribir clases generadas dinámicamente a archivos .class
 * 
 * Nota: Cojen/maker genera clases en memoria. Para guardarlas a disco,
 * necesitaríamos acceso al bytecode internamente. Como alternativa,
 * se puede usar javap para descompilar la clase generada.
 */
public class BytecodeWriter {

  /**
   * Intenta guardar el bytecode de una clase a un archivo.
   * Nota: Esto solo funciona si la clase ha sido cargada desde el filesystem.
   * Para clases generadas dinámicamente por cojen/maker, se puede usar javap.
   * 
   * @param generatedClass La clase generada
   * @param outputDir Directorio de salida
   * @throws IOException Si hay error al escribir
   */
  public static void writeClassToFile(Class<?> generatedClass, Path outputDir) throws IOException {
    String className = generatedClass.getName();
    String classFileName = className.replace('.', '/') + ".class";
    Path outputPath = outputDir.resolve(classFileName);

    // Crear directorio
    Files.createDirectories(outputPath.getParent());

    // Intentar extraer bytecode
    byte[] bytecode = extractBytecodeFromDynamicClass(generatedClass);

    if (bytecode != null && bytecode.length > 0) {
      Files.write(outputPath, bytecode);
      System.out.println("✓ Bytecode guardado en: " + outputPath);
      System.out.println("  Tamaño: " + bytecode.length + " bytes");
    } else {
      // Fallback: escribir información de la clase
      writeFallbackClassInfo(generatedClass, outputPath);
    }
  }

  /**
   * Intenta extraer bytecode de una clase generada dinámicamente
   */
  public static byte[] extractBytecodeFromDynamicClass(Class<?> clazz) {
    try {
      // Intentar obtener el bytecode a través del ClassLoader
      String resourcePath = clazz.getName().replace('.', '/') + ".class";
      var resourceStream = clazz.getClassLoader().getResourceAsStream(resourcePath);
      
      if (resourceStream != null) {
        return resourceStream.readAllBytes();
      }

      // Si no está en resources, intentar desde el contexto
      resourceStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(resourcePath);
      
      if (resourceStream != null) {
        return resourceStream.readAllBytes();
      }
    } catch (Exception e) {
      // Ignorar
    }

    return null;
  }

  /**
   * Escribe información alternativa de la clase (no es bytecode, pero es útil)
   */
  private static void writeFallbackClassInfo(Class<?> clazz, Path outputPath) throws IOException {
    StringBuilder info = new StringBuilder();
    info.append("// Generated class by BytecodeInliner\n");
    info.append("// Name: ").append(clazz.getName()).append("\n");
    info.append("// Superclass: ").append(clazz.getSuperclass().getSimpleName()).append("\n");
    info.append("// Modifiers: ").append(clazz.getModifiers()).append("\n\n");

    info.append("// Fields:\n");
    for (var field : clazz.getDeclaredFields()) {
      info.append("//   ").append(field.getType().getSimpleName()).append(" ")
          .append(field.getName()).append("\n");
    }

    info.append("\n// Methods:\n");
    for (var method : clazz.getDeclaredMethods()) {
      info.append("//   ").append(method.getReturnType().getSimpleName()).append(" ")
          .append(method.getName()).append("()\n");
    }

    // Guardar como archivo de texto
    Path infoPath = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".class", ".class-info.txt"));
    Files.write(infoPath, info.toString().getBytes());
    System.out.println("✓ Información de clase guardada en: " + infoPath);
  }

  /**
   * Genera un comando javap para descompilar la clase generada.
   * Útil para ver el bytecode de clases generadas dinámicamente.
   */
  public static void printJavapCommand(Class<?> clazz) {
    String className = clazz.getName();
    System.out.println("\n// Para ver el bytecode generado, ejecuta:");
    System.out.println("javap -v -p -c -classpath target/test-classes " + className);
    System.out.println("");
  }
}
