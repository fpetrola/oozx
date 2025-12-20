package com.fpetrola.oozx;

import com.fpetrola.oozx.inliner.BytecodeInliner;
import com.fpetrola.oozx.inliner.DummyMemory;
import com.fpetrola.oozx.inliner.InstructionAnalyzer;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BytecodeInlinerTestBase {
  // Guardar referencia al inliner para acceder al bytecode generado
  private BytecodeInliner lastInliner;

  public static Ld getLd1() {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("A");
    return new Ld(target, source, new Plain8BitRegister("F"));
  }

  protected static Xor getXor1() {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("C");
    return new Xor(target, source, new Plain8BitRegister("F"));
  }

  /**
   * Test helper genérico que genera bytecode, descompila y retorna el código fuente
   */
  protected String testBytecodeInlineOf(TargetSourceInstruction instruction) throws IOException {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(instruction);

    lastInliner = new BytecodeInliner(analyzer);
    String generatedClass = lastInliner.inlineInstruction(instruction);

    return getDecompiledSource(generatedClass);
  }

  /**
    * Test helper que genera una clase con múltiples instrucciones
    */
   protected String testBytecodeMultipleInstructionsOf(String className, Map<Integer, Instruction> instructions) throws IOException {
     lastInliner = new BytecodeInliner(new InstructionAnalyzer());
     String generatedClass = lastInliner.inlineMultipleInstructions(className, instructions);

     Class<?> aClass = lastInliner.generateAndLoadMultipleInstructions(className, instructions);

     return getDecompiledSource(generatedClass);
   }

  /**
   * Obtiene el código fuente descompilado de la clase generada usando Decompiler
   */
  private String getDecompiledSource(String generatedClass) throws IOException {
    try {
      if (lastInliner == null) {
        throw new IOException("lastInliner no fue inicializado");
      }

      byte[] bytecode = BytecodeInliner.generatedBytecodes.get(generatedClass);

      Path tempDir = Paths.get("target/decompiled-temp");
      Files.createDirectories(tempDir);
      Path classFile = tempDir.resolve(generatedClass + ".class");
      Files.write(classFile, bytecode);

      com.fpetrola.z80.bytecode.decompile.Decompiler decompiler = new com.fpetrola.z80.bytecode.decompile.Decompiler();
      decompiler.addClass(bytecode, classFile.toFile());
      String decompiled = decompiler.decompile();

      if (decompiled == null || decompiled.trim().isEmpty()) {
        throw new IOException("Decompiler no pudo descompilar la clase");
      }

      return decompiled;
    } catch (Exception e) {
      throw new IOException("Error descompilando bytecode generado: " + e.getMessage(), e);
    }
  }

  /**
   * Compara el código fuente descompilado con el esperado
   */
  protected void assertSourceEquals(String actual, String expectedSource) {
    assertEquals(expectedSource.trim(), actual.trim(), "Source code does not match:\n\n" + "expected:\n" + expectedSource + "\n\n" + "actual:\n" + actual);
  }
}
