package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para BytecodeInliner que verifica que se generan correctamente
 * clases bytecode compiladas dinámicamente.
 */
public class BytecodeInlinerTest {

  @Test
  public void testBytecodeInline1() {
    var ld = getLd1();
    Class<?> generatedClass = testBytecodeInlineOf(ld);
    
    // Verificar que se generó la clase
    assertNotNull(generatedClass);
    assertTrue(generatedClass.getName().contains("Bytecode"));
    
    // Verificar campos
    verifyFields(generatedClass, new String[]{"A", "IX", "memory", "pc"});
    
    // Verificar constructor
    verifyConstructor(generatedClass, 2); // Memory y Register
    
    // Verificar método execute
    verifyMethodExists(generatedClass, "execute");
  }

  @Test
  public void testBytecodeInline2() {
    var ld = getLd2();
    Class<?> generatedClass = testBytecodeInlineOf(ld);
    
    assertNotNull(generatedClass);
    assertTrue(generatedClass.getName().contains("Bytecode"));
    
    // Verificar campos
    verifyFields(generatedClass, new String[]{"B", "IY", "memory"});
    
    // Verificar constructor
    verifyConstructor(generatedClass, 1); // Solo Memory
    
    // Verificar método execute
    verifyMethodExists(generatedClass, "execute");
  }

  @Test
  public void testBytecodeInline3() {
    var ld = getLd3();
    Class<?> generatedClass = testBytecodeInlineOf(ld);
    
    assertNotNull(generatedClass);
    assertTrue(generatedClass.getName().contains("Bytecode"));
    
    // Verificar campos
    verifyFields(generatedClass, new String[]{"B", "IY", "memory", "pc"});
    
    // Verificar constructor
    verifyConstructor(generatedClass, 2); // Memory y Register
    
    // Verificar método execute
    verifyMethodExists(generatedClass, "execute");
  }

  @Test
  public void testBytecodeGeneratesValidClass() throws Exception {
    var ld = getLd1();
    Class<?> generatedClass = testBytecodeInlineOf(ld);
    
    // Verificar que se puede acceder a constructores
    assertNotNull(generatedClass.getConstructors());
    assertTrue(generatedClass.getConstructors().length > 0);
    
    // Verificar que se puede obtener el método execute
    var executeMethod = generatedClass.getDeclaredMethod("execute");
    assertNotNull(executeMethod);
  }

  @Test
  public void testBytecodeXorInline1() {
    var xor = getXor1();
    Class<?> generatedClass = testBytecodeInlineOf(xor);
    
    assertNotNull(generatedClass);
    assertTrue(generatedClass.getName().contains("XorBytecode"));
    verifyFields(generatedClass, new String[]{"C", "IX", "memory", "pc"});
    verifyMethodExists(generatedClass, "execute");
  }

  @Test
  public void testBytecodeOrInline1() {
    var or = getOr1();
    Class<?> generatedClass = testBytecodeInlineOf(or);
    
    assertNotNull(generatedClass);
    assertTrue(generatedClass.getName().contains("OrBytecode"));
    verifyFields(generatedClass, new String[]{"C", "IX", "memory", "pc"});
    verifyMethodExists(generatedClass, "execute");
  }

  /**
   * Verifica que el método execute existe y es público
   */
  private void verifyMethodExists(Class<?> generatedClass, String methodName) {
    try {
      Method method = generatedClass.getDeclaredMethod(methodName);
      assertTrue(method.getReturnType() == void.class);
    } catch (NoSuchMethodException e) {
      fail("Método " + methodName + " no encontrado en clase generada");
    }
  }

  /**
   * Verifica que existen los campos esperados
   */
  private void verifyFields(Class<?> generatedClass, String[] expectedFields) {
    Field[] fields = generatedClass.getDeclaredFields();
    for (String expectedField : expectedFields) {
      boolean found = false;
      for (Field field : fields) {
        if (field.getName().equals(expectedField)) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Campo " + expectedField + " no encontrado");
    }
  }

  /**
   * Verifica que el constructor existe con el número esperado de parámetros
   */
  private void verifyConstructor(Class<?> generatedClass, int expectedParamCount) {
    var constructors = generatedClass.getDeclaredConstructors();
    boolean found = false;
    for (var constructor : constructors) {
      if (constructor.getParameterCount() == expectedParamCount) {
        found = true;
        break;
      }
    }
    assertTrue(found, "Constructor con " + expectedParamCount + " parámetros no encontrado");
  }

  /**
   * Test helper que genera bytecode y retorna la clase compilada
   */
  private Class<?> testBytecodeInlineOf(Ld ld) {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(ld);

    Path sourcePath = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    Path bytecodeOutputDir = Paths.get("target/generated-classes");
    var inliner = new BytecodeInliner(analyzer, sourcePath, bytecodeOutputDir);
    Class<?> generatedClass = inliner.inlineLd(ld);

    // Guardar información de la clase
    saveGeneratedClass(generatedClass);

    return generatedClass;
  }

  private Class<?> testBytecodeInlineOf(Xor xor) {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(xor);

    Path sourcePath = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    Path bytecodeOutputDir = Paths.get("target/generated-classes");
    var inliner = new BytecodeInliner(analyzer, sourcePath, bytecodeOutputDir);
    Class<?> generatedClass = inliner.inlineXor(xor);

    saveGeneratedClass(generatedClass);
    return generatedClass;
  }

  private Class<?> testBytecodeInlineOf(Or or) {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(or);

    Path sourcePath = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    Path bytecodeOutputDir = Paths.get("target/generated-classes");
    var inliner = new BytecodeInliner(analyzer, sourcePath, bytecodeOutputDir);
    Class<?> generatedClass = inliner.inlineOr(or);

    saveGeneratedClass(generatedClass);
    return generatedClass;
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
