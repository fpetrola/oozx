package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.impl.Cp;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para BytecodeInliner que verifica que se generan correctamente
 * clases bytecode compiladas dinámicamente que extiendan Z80UnRolled.
 */
public class BytecodeInlinerTest {

  @Test
  public void testBytecodeInline1() throws IOException {
    var ld = getLd1();
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdMprfIxA() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              super.memory.write(var3, super.A);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeInline2() throws IOException {
    var ld = getLd2();
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdImrIyB() {
              super.memory.write(super.IY, super.B);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeInline3() throws IOException {
    var ld = getLd3();
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdImrM16RB() {
              int var1 = super.PC + 3 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 4 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              super.memory.write(var5, super.B);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeGeneratesValidClass() throws Exception {
    var ld = getLd1();
    String actualSource = testBytecodeInlineOf(ld);

    assertNotNull(actualSource);
    assertFalse(actualSource.isEmpty());
    assertTrue(actualSource.contains("extends Z80UnRolled"));
  }

  @Test
  public void testBytecodeXorInline1() throws IOException {
    var xor = getXor1();
    String actualSource = testBytecodeInlineOf(xor);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class XorBytecode extends Z80UnRolled {
           public void executeXorMprfIxC() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.xorTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline1() throws IOException {
    var or = getOr1();
    String actualSource = testBytecodeInlineOf(or);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class OrBytecode extends Z80UnRolled {
           public void executeOrMprfIxC() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.orTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeXorInline2() throws IOException {
    var xor = getXor2();
    String actualSource = testBytecodeInlineOf(xor);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class XorBytecode extends Z80UnRolled {
           public void executeXorImrIyC() {
              int var1 = super.memory.read(super.IY, 0);
              int var2 = super.xorTableAluOperation.execute2ValuesAndCarry(var1, super.C, super.flag);
              super.memory.write(super.IY, var2);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeXorInline3() throws IOException {
    var xor = getXor3();
    String actualSource = testBytecodeInlineOf(xor);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class XorBytecode extends Z80UnRolled {
           public void executeXorImrM16RC() {
              int var1 = super.PC + 3 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 4 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              int var6 = super.memory.read(var5, 0);
              int var7 = super.xorTableAluOperation.execute2ValuesAndCarry(var6, super.C, super.flag);
              super.memory.write(var5, var7);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline2() throws IOException {
    var or = getOr2();
    String actualSource = testBytecodeInlineOf(or);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class OrBytecode extends Z80UnRolled {
           public void executeOrImrIyC() {
              int var1 = super.memory.read(super.IY, 0);
              int var2 = super.orTableAluOperation.execute2ValuesAndCarry(var1, super.C, super.flag);
              super.memory.write(super.IY, var2);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline3() throws IOException {
    var or = getOr3();
    String actualSource = testBytecodeInlineOf(or);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class OrBytecode extends Z80UnRolled {
           public void executeOrImrM16RC() {
              int var1 = super.PC + 3 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 4 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              int var6 = super.memory.read(var5, 0);
              int var7 = super.orTableAluOperation.execute2ValuesAndCarry(var6, super.C, super.flag);
              super.memory.write(var5, var7);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAndInline1() throws IOException {
    var and = getAnd1();
    String actualSource = testBytecodeInlineOf(and);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AndBytecode extends Z80UnRolled {
           public void executeAndMprfIxC() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.andTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAndInline2() throws IOException {
    var and = getAnd2();
    String actualSource = testBytecodeInlineOf(and);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AndBytecode extends Z80UnRolled {
           public void executeAndImrIyC() {
              int var1 = super.memory.read(super.IY, 0);
              int var2 = super.andTableAluOperation.execute2ValuesAndCarry(var1, super.C, super.flag);
              super.memory.write(super.IY, var2);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAndInline3() throws IOException {
    var and = getAnd3();
    String actualSource = testBytecodeInlineOf(and);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AndBytecode extends Z80UnRolled {
           public void executeAndImrM16RC() {
              int var1 = super.PC + 3 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 4 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              int var6 = super.memory.read(var5, 0);
              int var7 = super.andTableAluOperation.execute2ValuesAndCarry(var6, super.C, super.flag);
              super.memory.write(var5, var7);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeSubInline1() throws IOException {
    var sub = getSub1();
    String actualSource = testBytecodeInlineOf(sub);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class SubBytecode extends Z80UnRolled {
           public void executeSubMprfIxB() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.subTableAluOperation.execute2ValuesAndCarry(var4, super.B, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeCpInline1() throws IOException {
    var cp = getCp1();
    String actualSource = testBytecodeInlineOf(cp);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class CpBytecode extends Z80UnRolled {
           public void executeCpMprfIxD() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.cpTableAluOperation.execute2ValuesAndCarry(var4, super.D, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAddInline1() throws IOException {
    var add = getAdd1();
    String actualSource = testBytecodeInlineOf(add);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AddBytecode extends Z80UnRolled {
           public void executeAddMprfIxE() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.addTableAluOperation.execute2ValuesAndCarry(var4, super.E, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAdcInline1() throws IOException {
    var adc = getAdc1();
    String actualSource = testBytecodeInlineOf(adc);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AdcBytecode extends Z80UnRolled {
           public void executeAdcMprfIxH() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.adcTableAluOperation.execute2ValuesAndCarry(var4, super.H, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeSbcInline1() throws IOException {
    var sbc = getSbc1();
    String actualSource = testBytecodeInlineOf(sbc);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class SbcBytecode extends Z80UnRolled {
           public void executeSbcMprfIxL() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.sbcTableAluOperation.execute2ValuesAndCarry(var4, super.L, super.flag);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeMultipleInstructionsSwitch() throws IOException {
    var ld = getLd1();
    var xor = getXor1();
    var add = getAdd1();

    String actualSource = testBytecodeMultipleInstructionsOf("MultiInstructionBytecode", List.of(ld, xor, add));

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class MultiInstructionBytecode extends Z80UnRolled {
           public void executeLdMprfIxA() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              super.memory.write(var3, super.A);
           }
        
           public void executeXorMprfIxC() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.xorTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.flag);
              super.memory.write(var3, var5);
           }
        
           public void executeAddMprfIxE() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.addTableAluOperation.execute2ValuesAndCarry(var4, super.E, super.flag);
              super.memory.write(var3, var5);
           }
        
           public void execute(int opcode) {
              switch(opcode) {
              case 0:
                 this.executeLdMprfIxA();
                 break;
              case 1:
                 this.executeXorMprfIxC();
                 break;
              case 2:
                 this.executeAddMprfIxE();
                 break;
              default:
                 StringBuilder var2 = new StringBuilder();
                 var2.append("Invalid instruction index: ");
                 var2.append(opcode);
                 String var3 = var2.toString();
                 throw new IllegalArgumentException(var3);
              }
        
           }
        }
        """;
    assertSourceEquals(actualSource, expectedSource);
  }


  // Guardar referencia al inliner para acceder al bytecode generado
  private BytecodeInliner lastInliner;

  /**
   * Test helper genérico que genera bytecode, descompila y retorna el código fuente
   */
  private String testBytecodeInlineOf(TargetSourceInstruction instruction) throws IOException {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(instruction);

    lastInliner = setupInliner(analyzer);
    String generatedClass = lastInliner.inlineInstruction(instruction);

    return getDecompiledSource(generatedClass);
  }

  /**
   * Test helper que genera una clase con múltiples instrucciones
   */
  private String testBytecodeMultipleInstructionsOf(String className, List<TargetSourceInstruction> instructions) throws IOException {
    var analyzer = new InstructionAnalyzer();
    lastInliner = setupInliner(analyzer);
    String generatedClass = lastInliner.inlineMultipleInstructions(className, instructions);

    return getDecompiledSource(generatedClass);
  }

  /**
   * Configura un BytecodeInliner con el analyzer y directorio de salida
   */
  private BytecodeInliner setupInliner(InstructionAnalyzer analyzer) {
    Path bytecodeOutputDir = Paths.get("target/generated-classes");
    return new BytecodeInliner(analyzer, bytecodeOutputDir);
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

      Decompiler decompiler = new Decompiler();
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
  private void assertSourceEquals(String actual, String expectedSource) {
    assertEquals(expectedSource.trim(), actual.trim(), "Source code does not match:\n\n" +
                                                       "expected:\n" + expectedSource + "\n\n" +
                                                       "actual:\n" + actual);
  }

  // ============ Helpers para crear instrucciones de prueba ============

  private static Ld getLd1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("A");
    return new Ld(target, source, new Plain8BitRegister("F"));
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
    return new Xor(target, source, new Plain8BitRegister("F"));
  }

  private static Or getOr1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("C");
    return new Or(target, source, new Plain8BitRegister("F"));
  }

  private static Xor getXor2() {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("C");
    return new Xor(target, source, new Plain8BitRegister("F"));
  }

  private static Xor getXor3() {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    return new Xor(target, source, new Plain8BitRegister("F"));
  }

  private static Or getOr2() {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("C");
    return new Or(target, source, new Plain8BitRegister("F"));
  }

  private static Or getOr3() {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    return new Or(target, source, new Plain8BitRegister("F"));
  }

  private static And getAnd1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("C");
    return new And(target, source, new Plain8BitRegister("F"));
  }

  private static And getAnd2() {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("C");
    return new And(target, source, new Plain8BitRegister("F"));
  }

  private static And getAnd3() {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    return new And(target, source, new Plain8BitRegister("F"));
  }

  private static Sub getSub1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("B");
    return new Sub(target, source, new Plain8BitRegister("F"));
  }

  private static Cp getCp1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("D");
    return new Cp(target, source, new Plain8BitRegister("F"));
  }

  private static Add getAdd1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("E");
    return new Add(target, source, new Plain8BitRegister("F"));
  }

  private static Adc getAdc1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("H");
    return new Adc(target, source, new Plain8BitRegister("F"));
  }

  private static Sbc getSbc1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("L");
    return new Sbc(target, source, new Plain8BitRegister("F"));
  }

  private static Dec getDec1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    return new Dec(target, new Plain8BitRegister("F"));
  }

  private static Inc getInc1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    return new Inc(target, new Plain8BitRegister("F"));
  }

  private static Neg getNeg1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    return new Neg(target, new Plain8BitRegister("F"));
  }
}
