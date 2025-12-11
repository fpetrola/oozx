package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.bytecode.Decompiler;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para BytecodeInliner que verifica que se generan correctamente
 * clases bytecode compiladas dinámicamente.
 */
public class BytecodeInlinerTest {

  @Test
  public void testBytecodeInline1() throws IOException {
    var ld = getLd1();
    String actualSource = testBytecodeInlineOf(ld);

    // Verificar código fuente generado
    String expectedSource = """
        public class LdBytecode {
            private int A;
            private int IX;
            private Memory memory;
            private int pc;
        
            public void execute() {
                int var1 = this.pc + 2 & '\\uffff';
                int var2 = this.memory.read(var1, 0);
                int var3 = this.IX + var2 & '\\uffff';
                this.memory.write(var3, this.A);
            }
        }
        """;
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeInline2() throws IOException {
    var ld = getLd2();
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.z80.memory.Memory;
        
        public class LdBytecode {
           private int B;
           private int IY;
           private Memory memory;
        
           public LdBytecode(Memory memory) {
              this.memory = memory;
           }
        
           public void execute() {
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeInline3() throws IOException {
    var ld = getLd3();
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        public class LdBytecode {
                 private int B;
                 private int IY;
                 private Memory memory;
                 private int pc;
                 public LdBytecode(Memory memory, int pc) {
                 this.memory = memory;
                 this.pc = pc;
                 }
                 public void execute() {
                 }
                 }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeGeneratesValidClass() throws Exception {
    var ld = getLd1();
    String actualSource = testBytecodeInlineOf(ld);

    // Verificar que se generó código válido
    assertNotNull(actualSource);
    assertFalse(actualSource.isEmpty());
    assertTrue(actualSource.contains("public class LdBytecode"));
  }

  @Test
  public void testBytecodeXorInline1() throws IOException {
    var xor = getXor1();
    String actualSource = testBytecodeInlineOf(xor);

    String expectedSource = """
        public class XorBytecode {
        private int C;
        private int IX;
        private Memory memory;
        private int pc;
        public XorBytecode(Memory memory, int pc) {
        this.memory = memory;
        this.pc = pc;
        }
        public void execute() {
        }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline1() throws IOException {
    var or = getOr1();
    String actualSource = testBytecodeInlineOf(or);

    String expectedSource = """
        public class OrBytecode {
        private int C;
        private int IX;
        private Memory memory;
        private int pc;
        public OrBytecode(Memory memory, int pc) {
        this.memory = memory;
        this.pc = pc;
        }
        public void execute() {
        }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  // Guardar referencia al inliner para acceder al bytecode generado
  private BytecodeInliner lastInliner;

  /**
   * Test helper que genera bytecode, descompila y retorna el código fuente
   */
  private String testBytecodeInlineOf(Ld ld) throws IOException {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(ld);

    Path sourcePath = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    Path bytecodeOutputDir = Paths.get("target/generated-classes");
    lastInliner = new BytecodeInliner(analyzer, sourcePath, bytecodeOutputDir);
    String generatedClass = lastInliner.inlineLd(ld);

    return getDecompiledSource(generatedClass);
  }

  private String testBytecodeInlineOf(Xor xor) throws IOException {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(xor);

    Path sourcePath = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    Path bytecodeOutputDir = Paths.get("target/generated-classes");
    lastInliner = new BytecodeInliner(analyzer, sourcePath, bytecodeOutputDir);
    String generatedClass = lastInliner.inlineXor(xor);

    return getDecompiledSource(generatedClass);
  }

  private String testBytecodeInlineOf(Or or) throws IOException {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(or);

    Path sourcePath = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    Path bytecodeOutputDir = Paths.get("target/generated-classes");
    lastInliner = new BytecodeInliner(analyzer, sourcePath, bytecodeOutputDir);
    String generatedClass = lastInliner.inlineOr(or);

    return getDecompiledSource(generatedClass);
  }

  /**
   * Guarda el bytecode generado a un archivo .class o crea un fallback
   */
  private void saveGeneratedClass(Class<?> generatedClass) {
    try {
      Path outputDir = Paths.get("target/generated-classes");
      BytecodeWriter.writeClassToFile(generatedClass, outputDir);
      BytecodeWriter.printJavapCommand(generatedClass);
    } catch (IOException e) {
      System.err.println("Error guardando bytecode: " + e.getMessage());
    }
  }

  /**
   * Obtiene el código fuente descompilado de la clase generada usando Decompiler
   * Utiliza el bytecode capturado directamente de ClassMaker.finishBytes()
   */
  private String getDecompiledSource(String generatedClass) throws IOException {
    try {
      // Obtener el bytecode del último inliner (generado por ClassMaker.finishBytes())
      if (lastInliner == null) {
        throw new IOException("lastInliner no fue inicializado");
      }

      byte[] bytecode = BytecodeInliner.generatedBytecodes.get(generatedClass);

      // Crear un archivo temporal para el bytecode
      Path tempDir = Paths.get("target/decompiled-temp");
      Files.createDirectories(tempDir);
      String className = "Test1";
      Path classFile = tempDir.resolve(className + ".class");
      Files.write(classFile, bytecode);

      // Usar Decompiler para descompilar el bytecode
      Decompiler decompiler = new Decompiler();
      decompiler.addClass(bytecode, classFile.toFile());
      String decompiled = decompiler.decompile();

      if (decompiled == null || decompiled.trim().isEmpty()) {
        throw new IOException("Decompiler no pudo descompilar la clase");
      }

      return decompiled;
    } catch (Exception e) {
      // Lanzar excepción para que el test falle si no se puede descompilar
      throw new IOException("Error descompilando bytecode generado: " + e.getMessage(), e);
    }
  }

  /**
   * Compara el código fuente descompilado con el esperado
   */
  private void assertSourceEquals(String actual, String expectedSource) {
    // Normalizar espacios en blanco para comparación flexible
    String actualNormalized = normalizeSource(actual);
    String expectedNormalized = normalizeSource(expectedSource);

    assertEquals(expectedNormalized, actualNormalized, "Código fuente no coincide:\n\n" +
                                                       "ESPERADO:\n" + expectedSource + "\n\n" +
                                                       "ACTUAL:\n" + actual);
  }

  /**
   * Normaliza el código fuente para comparación
   * - Elimina espacios en blanco al inicio/final
   * - Normaliza saltos de línea
   * - Reduce espacios múltiples a uno solo
   * - Elimina imports
   * - Normaliza nombres de clase generados (LdBytecode-X → LdBytecode)
   */
  private String normalizeSource(String source) {
    return source
        .replaceAll("\\r\\n", "\n")  // Normalizar saltos de línea
        .replaceAll("(?m)^import .*?;\\n", "") // Eliminar imports (multiline mode)
        .replaceAll("\\bLdBytecode-\\d+\\b", "LdBytecode") // Normalizar nombre de clase generado
        .replaceAll("\\bLdBytecode_\\d+", "LdBytecode") // Constructor normalizado
        .replaceAll("\\bXorBytecode-\\d+\\b", "XorBytecode") // Para XOR
        .replaceAll("\\bXorBytecode_\\d+", "XorBytecode") // Constructor XOR
        .replaceAll("\\bOrBytecode-\\d+\\b", "OrBytecode") // Para OR
        .replaceAll("\\bOrBytecode_\\d+", "OrBytecode") // Constructor OR
        .replaceAll("/\\*.*?\\*/", "") // Eliminar comentarios /* ... */
        .replaceAll("[ \\t]+", " ")   // Múltiples espacios a uno
        .replaceAll("\\n[ \\t]+", "\n") // Espacios después de saltos
        .replaceAll("[ \\t]+\\n", "\n") // Espacios antes de saltos
        .replaceAll("\\n+", "\n")     // Múltiples saltos a uno
        .trim();
  }


  // Helpers para crear instrucciones de prueba

  private static Ld getLd1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("A");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));
    return ld;
  }

  private static Ld getLd2() {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("B");
    return new Ld(target, source, new Plain8BitRegister("F"));
  }

  private static Ld getLd3() {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("B");
    return new Ld(target, source, new Plain8BitRegister("F"));
  }

  private static Xor getXor1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("C");
    var xor = new Xor(target, source, new Plain8BitRegister("F"));
    return xor;
  }

  private static Or getOr1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("C");
    var or = new Or(target, source, new Plain8BitRegister("F"));
    return or;
  }
}
