package com.fpetrola.oozx;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.impl.Cp;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
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
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para BytecodeInliner que verifica que se generan correctamente
 * clases bytecode compiladas dinámicamente que extiendan Z80UnRolled.
 */
public class BytecodeInlinerTest extends BytecodeInlinerTestBase {

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
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("B");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));

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
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("B");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));
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
  public void testBytecodeInline4() throws IOException {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory16BitReference(new Memory16BitReference(memory, new Plain16BitRegister("PC"), 3), memory);
    var source = new Plain8BitRegister("C");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));

    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdImr16M16RC() {
              int var1 = super.PC + 3 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 4 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              int var6 = super.memory.read16Bits(var5);
              super.memory.write16BitsReverse(var6, super.C);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeInline5() throws IOException {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory16BitReference(new Plain16BitRegister("IX"), memory);
    var source = new Plain8BitRegister("D");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdImr16IxD() {
              int var1 = super.memory.read16Bits(super.IX);
              super.memory.write16BitsReverse(var1, super.D);
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
              int var5 = super.xorTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("C");
    var or = new Or(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(or);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class OrBytecode extends Z80UnRolled {
           public void executeOrMprfIxC() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.orTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeXorInline2() throws IOException {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("C");
    var xor = new Xor(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(xor);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class XorBytecode extends Z80UnRolled {
           public void executeXorImrIyC() {
              int var1 = super.memory.read(super.IY, 0);
              int var2 = super.xorTableAluOperation.execute2ValuesAndCarry(var1, super.C, super.F);
              super.memory.write(super.IY, var2);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeXorInline3() throws IOException {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    var xor = new Xor(target, source, new Plain8BitRegister("F"));
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
              int var7 = super.xorTableAluOperation.execute2ValuesAndCarry(var6, super.C, super.F);
              super.memory.write(var5, var7);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline2() throws IOException {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("C");
    var or = new Or(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(or);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class OrBytecode extends Z80UnRolled {
           public void executeOrImrIyC() {
              int var1 = super.memory.read(super.IY, 0);
              int var2 = super.orTableAluOperation.execute2ValuesAndCarry(var1, super.C, super.F);
              super.memory.write(super.IY, var2);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline3() throws IOException {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    var or = new Or(target, source, new Plain8BitRegister("F"));
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
              int var7 = super.orTableAluOperation.execute2ValuesAndCarry(var6, super.C, super.F);
              super.memory.write(var5, var7);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAndInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("C");
    var and = new And(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(and);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AndBytecode extends Z80UnRolled {
           public void executeAndMprfIxC() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.andTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAndInline2() throws IOException {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("C");
    var and = new And(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(and);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AndBytecode extends Z80UnRolled {
           public void executeAndImrIyC() {
              int var1 = super.memory.read(super.IY, 0);
              int var2 = super.andTableAluOperation.execute2ValuesAndCarry(var1, super.C, super.F);
              super.memory.write(super.IY, var2);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAndInline3() throws IOException {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    var and = new And(target, source, new Plain8BitRegister("F"));
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
              int var7 = super.andTableAluOperation.execute2ValuesAndCarry(var6, super.C, super.F);
              super.memory.write(var5, var7);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeSubInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("B");
    var sub = new Sub(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(sub);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class SubBytecode extends Z80UnRolled {
           public void executeSubMprfIxB() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.subTableAluOperation.execute2ValuesAndCarry(var4, super.B, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeCpInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("D");
    var cp = new Cp(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(cp);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class CpBytecode extends Z80UnRolled {
           public void executeCpMprfIxD() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.cpTableAluOperation.execute2ValuesAndCarry(var4, super.D, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAddInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("E");
    var add = new Add(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(add);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AddBytecode extends Z80UnRolled {
           public void executeAddMprfIxE() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.addTableAluOperation.execute2ValuesAndCarry(var4, super.E, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAdcInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("H");
    var adc = new Adc(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(adc);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AdcBytecode extends Z80UnRolled {
           public void executeAdcMprfIxH() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.adcTableAluOperation.execute2ValuesAndCarry(var4, super.H, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeSbcInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("L");
    var sbc = new Sbc(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(sbc);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class SbcBytecode extends Z80UnRolled {
           public void executeSbcMprfIxL() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.sbcTableAluOperation.execute2ValuesAndCarry(var4, super.L, super.F);
              super.memory.write(var3, var5);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeMultipleInstructionsSwitch() throws IOException {
    var ld = getLd1();
    var xor = getXor1();
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("E");
    var add = new Add(target, source, new Plain8BitRegister("F"));

    Map<Integer, TargetSourceInstruction<?>> instructions = new TreeMap<>(Map.of(10, ld, 20, xor, 30, add));
    String actualSource = testBytecodeMultipleInstructionsOf("MultiInstructionBytecode", instructions);

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
              int var5 = super.xorTableAluOperation.execute2ValuesAndCarry(var4, super.C, super.F);
              super.memory.write(var3, var5);
           }
        
           public void executeAddMprfIxE() {
              int var1 = super.PC + 2 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.IX + var2 & '\\uffff';
              int var4 = super.memory.read(var3, 0);
              int var5 = super.addTableAluOperation.execute2ValuesAndCarry(var4, super.E, super.F);
              super.memory.write(var3, var5);
           }
        
           public int execute(int opcode) {
              switch(opcode) {
              case 10:
                 this.executeLdMprfIxA();
                 break;
              case 20:
                 this.executeXorMprfIxC();
                 break;
              case 30:
                 this.executeAddMprfIxE();
                 break;
              default:
                 return -1;
              }
        
              return 0;
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }


  @Test
  public void testBytecodeTableOpcodesSwitch() throws IOException {
    Map<Integer, TargetSourceInstruction<?>> instructions = new TreeMap<>();

    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();

    Instruction[] opcodeLookupTable = opcodesTables.getOpcodeLookupTable();
    for (int i = 0; i < opcodeLookupTable.length; i++) {
      Instruction instruction = opcodeLookupTable[i];
      if (instruction instanceof TargetSourceInstruction<?>)
        if (i % 1 == 0)
          instructions.put(i, (TargetSourceInstruction<?>) instruction);
    }

    String actualSource = testBytecodeMultipleInstructionsOf("MultiInstructionBytecode", instructions);
    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        import com.fpetrola.z80.instructions.impl.Adc.AdcTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Add.AddTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Add16.Add16TableAluOperation;
        import com.fpetrola.z80.instructions.impl.And.AndTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Cp.CpTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Or.OrTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Sbc.SbcTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Sub.SubTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Xor.XorTableAluOperation;
        
        public class MultiInstructionBytecode extends Z80UnRolled {
           public void executeLdImrBcA() {
              int var1 = this.getBC();
              super.memory.write(var1, super.A);
           }
        
           public void executeExImrBcAfx() {
              // $FF: Couldn't be decompiled
           }
        
           public void executeAdd16HlBc() {
              int var1 = this.getBC();
              int var2 = this.getHL();
              Add16TableAluOperation var3 = super.add16TableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(var2, var1, super.F);
              this.setHL(var4);
              super.F = var3.F;
           }
        
           public void executeLdImrDeA() {
              int var1 = this.getDE();
              super.memory.write(var1, super.A);
           }
        
           public void executeAdd16HlDe() {
              int var1 = this.getDE();
              int var2 = this.getHL();
              Add16TableAluOperation var3 = super.add16TableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(var2, var1, super.F);
              this.setHL(var4);
              super.F = var3.F;
           }
        
           public void executeLdImr16M16RHl() {
              int var1 = super.PC + 1 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 2 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              int var6 = this.getHL();
              int var7 = super.memory.read16Bits(var5);
              super.memory.write16BitsReverse(var7, var6);
           }
        
           public void executeAdd16HlHl() {
              int var1 = this.getHL();
              int var2 = this.getHL();
              Add16TableAluOperation var3 = super.add16TableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(var2, var1, super.F);
              this.setHL(var4);
              super.F = var3.F;
           }
        
           public void executeLdImrM16RA() {
              int var1 = super.PC + 1 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 2 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              super.memory.write(var5, super.A);
           }
        
           public void executeAdd16HlSp() {
              int var1 = this.getHL();
              Add16TableAluOperation var2 = super.add16TableAluOperation;
              int var3 = var2.execute2ValuesAndCarry(var1, super.SP, super.F);
              this.setHL(var3);
              super.F = var2.F;
           }
        
           public void executeLdBB() {
              super.B = super.B;
           }
        
           public void executeLdBC() {
              super.B = super.C;
           }
        
           public void executeLdBD() {
              super.B = super.D;
           }
        
           public void executeLdBE() {
              super.B = super.E;
           }
        
           public void executeLdBH() {
              super.B = super.H;
           }
        
           public void executeLdBL() {
              super.B = super.L;
           }
        
           public void executeLdBA() {
              super.B = super.A;
           }
        
           public void executeLdCB() {
              super.C = super.B;
           }
        
           public void executeLdCC() {
              super.C = super.C;
           }
        
           public void executeLdCD() {
              super.C = super.D;
           }
        
           public void executeLdCE() {
              super.C = super.E;
           }
        
           public void executeLdCH() {
              super.C = super.H;
           }
        
           public void executeLdCL() {
              super.C = super.L;
           }
        
           public void executeLdCA() {
              super.C = super.A;
           }
        
           public void executeLdDB() {
              super.D = super.B;
           }
        
           public void executeLdDC() {
              super.D = super.C;
           }
        
           public void executeLdDD() {
              super.D = super.D;
           }
        
           public void executeLdDE() {
              super.D = super.E;
           }
        
           public void executeLdDH() {
              super.D = super.H;
           }
        
           public void executeLdDL() {
              super.D = super.L;
           }
        
           public void executeLdDA() {
              super.D = super.A;
           }
        
           public void executeLdEB() {
              super.E = super.B;
           }
        
           public void executeLdEC() {
              super.E = super.C;
           }
        
           public void executeLdED() {
              super.E = super.D;
           }
        
           public void executeLdEE() {
              super.E = super.E;
           }
        
           public void executeLdEH() {
              super.E = super.H;
           }
        
           public void executeLdEL() {
              super.E = super.L;
           }
        
           public void executeLdEA() {
              super.E = super.A;
           }
        
           public void executeLdHB() {
              super.H = super.B;
           }
        
           public void executeLdHC() {
              super.H = super.C;
           }
        
           public void executeLdHD() {
              super.H = super.D;
           }
        
           public void executeLdHE() {
              super.H = super.E;
           }
        
           public void executeLdHH() {
              super.H = super.H;
           }
        
           public void executeLdHL() {
              super.H = super.L;
           }
        
           public void executeLdHA() {
              super.H = super.A;
           }
        
           public void executeLdLB() {
              super.L = super.B;
           }
        
           public void executeLdLC() {
              super.L = super.C;
           }
        
           public void executeLdLD() {
              super.L = super.D;
           }
        
           public void executeLdLE() {
              super.L = super.E;
           }
        
           public void executeLdLH() {
              super.L = super.H;
           }
        
           public void executeLdLL() {
              super.L = super.L;
           }
        
           public void executeLdLA() {
              super.L = super.A;
           }
        
           public void executeLdImrHlB() {
              int var1 = this.getHL();
              super.memory.write(var1, super.B);
           }
        
           public void executeLdImrHlC() {
              int var1 = this.getHL();
              super.memory.write(var1, super.C);
           }
        
           public void executeLdImrHlD() {
              int var1 = this.getHL();
              super.memory.write(var1, super.D);
           }
        
           public void executeLdImrHlE() {
              int var1 = this.getHL();
              super.memory.write(var1, super.E);
           }
        
           public void executeLdImrHlH() {
              int var1 = this.getHL();
              super.memory.write(var1, super.H);
           }
        
           public void executeLdImrHlL() {
              int var1 = this.getHL();
              super.memory.write(var1, super.L);
           }
        
           public void executeLdImrHlA() {
              int var1 = this.getHL();
              super.memory.write(var1, super.A);
           }
        
           public void executeLdAB() {
              super.A = super.B;
           }
        
           public void executeLdAC() {
              super.A = super.C;
           }
        
           public void executeLdAD() {
              super.A = super.D;
           }
        
           public void executeLdAE() {
              super.A = super.E;
           }
        
           public void executeLdAH() {
              super.A = super.H;
           }
        
           public void executeLdAL() {
              super.A = super.L;
           }
        
           public void executeLdAA() {
              super.A = super.A;
           }
        
           public void executeAddAB() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAddAC() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAddAD() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAddAE() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAddAH() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAddAL() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAddAA() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAB() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAC() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAD() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAE() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAH() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAL() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAA() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAB() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAC() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAD() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAE() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAH() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAL() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAA() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSbcAB() {
              SbcTableAluOperation var1 = super.sbcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSbcAC() {
              SbcTableAluOperation var1 = super.sbcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSbcAD() {
              SbcTableAluOperation var1 = super.sbcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSbcAE() {
              SbcTableAluOperation var1 = super.sbcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSbcAH() {
              SbcTableAluOperation var1 = super.sbcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSbcAL() {
              SbcTableAluOperation var1 = super.sbcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSbcAA() {
              SbcTableAluOperation var1 = super.sbcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAB() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAC() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAD() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAE() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAH() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAL() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAA() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAB() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAC() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAD() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAE() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAH() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAL() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAA() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAB() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAC() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAD() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAE() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAH() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAL() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAA() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAB() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAC() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.C, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAD() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAE() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.E, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAH() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAL() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.L, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAA() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.A, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOutA() {
           }
        
           public void executeExHl() {
           }
        
           public void executeExHl() {
           }
        
           public void executeLdSpHl() {
              int var1 = this.getHL();
              super.SP = var1;
           }
        
           public int execute(int opcode) {
              switch(opcode) {
              case 2:
                 this.executeLdImrBcA();
                 break;
              case 3:
              case 4:
              case 5:
              case 6:
              case 7:
              case 10:
              case 11:
              case 12:
              case 13:
              case 14:
              case 15:
              case 16:
              case 17:
              case 19:
              case 20:
              case 21:
              case 22:
              case 23:
              case 24:
              case 26:
              case 27:
              case 28:
              case 29:
              case 30:
              case 31:
              case 32:
              case 33:
              case 35:
              case 36:
              case 37:
              case 38:
              case 39:
              case 40:
              case 42:
              case 43:
              case 44:
              case 45:
              case 46:
              case 47:
              case 48:
              case 49:
              case 51:
              case 52:
              case 53:
              case 54:
              case 55:
              case 56:
              case 58:
              case 59:
              case 60:
              case 61:
              case 62:
              case 63:
              case 70:
              case 78:
              case 86:
              case 94:
              case 102:
              case 110:
              case 118:
              case 126:
              case 134:
              case 142:
              case 150:
              case 158:
              case 166:
              case 174:
              case 182:
              case 190:
              case 192:
              case 193:
              case 194:
              case 195:
              case 196:
              case 197:
              case 198:
              case 199:
              case 200:
              case 201:
              case 202:
              case 203:
              case 204:
              case 205:
              case 206:
              case 207:
              case 208:
              case 209:
              case 210:
              case 212:
              case 213:
              case 214:
              case 215:
              case 216:
              case 217:
              case 218:
              case 219:
              case 220:
              case 221:
              case 222:
              case 223:
              case 224:
              case 225:
              case 226:
              case 228:
              case 229:
              case 230:
              case 231:
              case 232:
              case 233:
              case 234:
              case 236:
              case 237:
              case 238:
              case 239:
              case 240:
              case 241:
              case 242:
              case 243:
              case 244:
              case 245:
              case 246:
              case 247:
              case 248:
              default:
                 return -1;
              case 8:
                 this.executeExImrBcAfx();
                 break;
              case 9:
                 this.executeAdd16HlBc();
                 break;
              case 18:
                 this.executeLdImrDeA();
                 break;
              case 25:
                 this.executeAdd16HlDe();
                 break;
              case 34:
                 this.executeLdImr16M16RHl();
                 break;
              case 41:
                 this.executeAdd16HlHl();
                 break;
              case 50:
                 this.executeLdImrM16RA();
                 break;
              case 57:
                 this.executeAdd16HlSp();
                 break;
              case 64:
                 this.executeLdBB();
                 break;
              case 65:
                 this.executeLdBC();
                 break;
              case 66:
                 this.executeLdBD();
                 break;
              case 67:
                 this.executeLdBE();
                 break;
              case 68:
                 this.executeLdBH();
                 break;
              case 69:
                 this.executeLdBL();
                 break;
              case 71:
                 this.executeLdBA();
                 break;
              case 72:
                 this.executeLdCB();
                 break;
              case 73:
                 this.executeLdCC();
                 break;
              case 74:
                 this.executeLdCD();
                 break;
              case 75:
                 this.executeLdCE();
                 break;
              case 76:
                 this.executeLdCH();
                 break;
              case 77:
                 this.executeLdCL();
                 break;
              case 79:
                 this.executeLdCA();
                 break;
              case 80:
                 this.executeLdDB();
                 break;
              case 81:
                 this.executeLdDC();
                 break;
              case 82:
                 this.executeLdDD();
                 break;
              case 83:
                 this.executeLdDE();
                 break;
              case 84:
                 this.executeLdDH();
                 break;
              case 85:
                 this.executeLdDL();
                 break;
              case 87:
                 this.executeLdDA();
                 break;
              case 88:
                 this.executeLdEB();
                 break;
              case 89:
                 this.executeLdEC();
                 break;
              case 90:
                 this.executeLdED();
                 break;
              case 91:
                 this.executeLdEE();
                 break;
              case 92:
                 this.executeLdEH();
                 break;
              case 93:
                 this.executeLdEL();
                 break;
              case 95:
                 this.executeLdEA();
                 break;
              case 96:
                 this.executeLdHB();
                 break;
              case 97:
                 this.executeLdHC();
                 break;
              case 98:
                 this.executeLdHD();
                 break;
              case 99:
                 this.executeLdHE();
                 break;
              case 100:
                 this.executeLdHH();
                 break;
              case 101:
                 this.executeLdHL();
                 break;
              case 103:
                 this.executeLdHA();
                 break;
              case 104:
                 this.executeLdLB();
                 break;
              case 105:
                 this.executeLdLC();
                 break;
              case 106:
                 this.executeLdLD();
                 break;
              case 107:
                 this.executeLdLE();
                 break;
              case 108:
                 this.executeLdLH();
                 break;
              case 109:
                 this.executeLdLL();
                 break;
              case 111:
                 this.executeLdLA();
                 break;
              case 112:
                 this.executeLdImrHlB();
                 break;
              case 113:
                 this.executeLdImrHlC();
                 break;
              case 114:
                 this.executeLdImrHlD();
                 break;
              case 115:
                 this.executeLdImrHlE();
                 break;
              case 116:
                 this.executeLdImrHlH();
                 break;
              case 117:
                 this.executeLdImrHlL();
                 break;
              case 119:
                 this.executeLdImrHlA();
                 break;
              case 120:
                 this.executeLdAB();
                 break;
              case 121:
                 this.executeLdAC();
                 break;
              case 122:
                 this.executeLdAD();
                 break;
              case 123:
                 this.executeLdAE();
                 break;
              case 124:
                 this.executeLdAH();
                 break;
              case 125:
                 this.executeLdAL();
                 break;
              case 127:
                 this.executeLdAA();
                 break;
              case 128:
                 this.executeAddAB();
                 break;
              case 129:
                 this.executeAddAC();
                 break;
              case 130:
                 this.executeAddAD();
                 break;
              case 131:
                 this.executeAddAE();
                 break;
              case 132:
                 this.executeAddAH();
                 break;
              case 133:
                 this.executeAddAL();
                 break;
              case 135:
                 this.executeAddAA();
                 break;
              case 136:
                 this.executeAdcAB();
                 break;
              case 137:
                 this.executeAdcAC();
                 break;
              case 138:
                 this.executeAdcAD();
                 break;
              case 139:
                 this.executeAdcAE();
                 break;
              case 140:
                 this.executeAdcAH();
                 break;
              case 141:
                 this.executeAdcAL();
                 break;
              case 143:
                 this.executeAdcAA();
                 break;
              case 144:
                 this.executeSubAB();
                 break;
              case 145:
                 this.executeSubAC();
                 break;
              case 146:
                 this.executeSubAD();
                 break;
              case 147:
                 this.executeSubAE();
                 break;
              case 148:
                 this.executeSubAH();
                 break;
              case 149:
                 this.executeSubAL();
                 break;
              case 151:
                 this.executeSubAA();
                 break;
              case 152:
                 this.executeSbcAB();
                 break;
              case 153:
                 this.executeSbcAC();
                 break;
              case 154:
                 this.executeSbcAD();
                 break;
              case 155:
                 this.executeSbcAE();
                 break;
              case 156:
                 this.executeSbcAH();
                 break;
              case 157:
                 this.executeSbcAL();
                 break;
              case 159:
                 this.executeSbcAA();
                 break;
              case 160:
                 this.executeAndAB();
                 break;
              case 161:
                 this.executeAndAC();
                 break;
              case 162:
                 this.executeAndAD();
                 break;
              case 163:
                 this.executeAndAE();
                 break;
              case 164:
                 this.executeAndAH();
                 break;
              case 165:
                 this.executeAndAL();
                 break;
              case 167:
                 this.executeAndAA();
                 break;
              case 168:
                 this.executeXorAB();
                 break;
              case 169:
                 this.executeXorAC();
                 break;
              case 170:
                 this.executeXorAD();
                 break;
              case 171:
                 this.executeXorAE();
                 break;
              case 172:
                 this.executeXorAH();
                 break;
              case 173:
                 this.executeXorAL();
                 break;
              case 175:
                 this.executeXorAA();
                 break;
              case 176:
                 this.executeOrAB();
                 break;
              case 177:
                 this.executeOrAC();
                 break;
              case 178:
                 this.executeOrAD();
                 break;
              case 179:
                 this.executeOrAE();
                 break;
              case 180:
                 this.executeOrAH();
                 break;
              case 181:
                 this.executeOrAL();
                 break;
              case 183:
                 this.executeOrAA();
                 break;
              case 184:
                 this.executeCpAB();
                 break;
              case 185:
                 this.executeCpAC();
                 break;
              case 186:
                 this.executeCpAD();
                 break;
              case 187:
                 this.executeCpAE();
                 break;
              case 188:
                 this.executeCpAH();
                 break;
              case 189:
                 this.executeCpAL();
                 break;
              case 191:
                 this.executeCpAA();
                 break;
              case 211:
                 this.executeOutA();
                 break;
              case 227:
                 this.executeExHl();
                 break;
              case 235:
                 this.executeExHl();
                 break;
              case 249:
                 this.executeLdSpHl();
              }
        
              return 0;
           }
        }""";

    assertSourceEquals(actualSource, expectedSource);
  }


}
