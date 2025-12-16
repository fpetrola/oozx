package com.fpetrola.oozx.t2;

import com.fpetrola.z80.bytecode.Decompiler;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test para ClassClonerWithSootUp que verifica que se genera correctamente
 * el bytecode clonado descompilando y validando el código fuente generado.
 */
public class ClassClonerWithSootUpTest {

  @Test
  public void testClassClonerGeneratesValidBytecode() throws IOException {
    String originalClassName = IndirectMemory8BitReference.class.getName();
    String clonedClassName = originalClassName + "Clone";

    // Generar bytecode clonado
    byte[] clonedBytecode = generateClonedBytecode(originalClassName);

    assertNotNull(clonedBytecode, "El bytecode clonado no debe ser nulo");
    assertTrue(clonedBytecode.length > 0, "El bytecode clonado debe tener contenido");

    // Descompilar y verificar
    String decompiledSource = decompileBytecode(clonedBytecode, clonedClassName);

    assertEquals("""
        package com.fpetrola.oozx.t2;
        
        import com.fpetrola.z80.memory.Memory;
        import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
        
        public class IndirectMemory8BitReferenceClone {
           ImmutableOpcodeReference target;
           int address;
           Memory memory;
        
           ImmutableOpcodeReference getTarget() {
              return this.target;
           }
        
           int read() {
              address = target.read();
              return memory.read(address, 0);
           }
        
           void write(int var1) {
              address = target.read();
              memory.write(address, var1);
           }
        
           IndirectMemory8BitReferenceClone(ImmutableOpcodeReference var1, Memory var2) {
              this.target = var1;
              this.memory = var2;
           }
        
           int getLength() {
              return target.getLength();
           }
        
           Memory getMemory() {
              return this.memory;
           }
        }
        """, decompiledSource);

  }


  private byte[] generateClonedBytecode(String originalClassName) throws IOException {
    // Guardar args en ClassClonerWithSootUp (usa reflection para ejecutar)
    try {
      // Ejecutar la lógica de ClassClonerWithSootUp
      ClassClonerWithSootUp cloner = new ClassClonerWithSootUp();

      // Simular lo que hace el main
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
  private String decompileBytecode(byte[] bytecode, String className) throws IOException {
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
}
