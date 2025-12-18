package com.fpetrola.oozx.t2;

import com.fpetrola.z80.bytecode.decompile.Decompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestHelper {
  public byte[] generateClonedBytecode(String originalClassName) throws IOException {
    // Guardar args en ClassClonerWithSootUp (usa reflection para ejecutar)
    try {
      String[] args = new String[]{originalClassName};
      ClassClonerWithSootUp.main(args);

      // Leer el bytecode generado
      Path classFilePath = Paths.get("Test3.class");
      if (!Files.exists(classFilePath)) {
        throw new IOException("No se generó el archivo Test3.class");
      }

      byte[] bytecode = Files.readAllBytes(classFilePath);

      // Limpiar archivo temporal
      Files.deleteIfExists(classFilePath);

      return bytecode;
    } catch (Exception e) {
      throw new IOException("Error generando bytecode clonado: " + e.getMessage(), e);
    }
  }

  /**
   * Descompila el bytecode usando el Decompiler
   */
  public String decompileBytecode(byte[] bytecode, String className) throws IOException {
    try {
      Path tempDir = Paths.get("target/decompiled-temp");
      Files.createDirectories(tempDir);

      Path classFile = tempDir.resolve(className + ".class");
      Files.write(classFile, bytecode);

      Decompiler decompiler = new Decompiler();
      decompiler.addClass(bytecode, classFile.toFile());
      String decompiled = decompiler.decompile();

      if (decompiled == null || decompiled.trim().isEmpty()) {
        throw new IOException("Decompiler no pudo descompilar la clase");
      }

      return decompiled;
    } catch (Exception e) {
      throw new IOException("Error descompilando bytecode: " + e.getMessage(), e);
    }
  }

  protected String cloneAndDecompile(String originalClassName) throws IOException {
    String clonedClassName = originalClassName + "Clone";
    byte[] clonedBytecode = generateClonedBytecode(originalClassName);
    return decompileBytecode(clonedBytecode, clonedClassName);
  }
}
