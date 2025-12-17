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
        if (i % 2 == 0)
          instructions.put(i, (TargetSourceInstruction<?>) instruction);
    }

    String actualSource = testBytecodeMultipleInstructionsOf("MultiInstructionBytecode", instructions);
    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        import com.fpetrola.z80.instructions.impl.Adc.AdcTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Add.AddTableAluOperation;
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
        
           public void executeLdImrDeA() {
              int var1 = this.getDE();
              super.memory.write(var1, super.A);
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
        
           public void executeLdImrM16RA() {
              int var1 = super.PC + 1 & '\\uffff';
              int var2 = super.memory.read(var1, 0);
              int var3 = super.PC + 2 & '\\uffff';
              int var4 = super.memory.read(var3, 0) << 8;
              int var5 = var2 | var4;
              super.memory.write(var5, super.A);
           }
        
           public void executeLdBB() {
              super.B = super.B;
           }
        
           public void executeLdBD() {
              super.B = super.D;
           }
        
           public void executeLdBH() {
              super.B = super.H;
           }
        
           public void executeLdCB() {
              super.C = super.B;
           }
        
           public void executeLdCD() {
              super.C = super.D;
           }
        
           public void executeLdCH() {
              super.C = super.H;
           }
        
           public void executeLdDB() {
              super.D = super.B;
           }
        
           public void executeLdDD() {
              super.D = super.D;
           }
        
           public void executeLdDH() {
              super.D = super.H;
           }
        
           public void executeLdEB() {
              super.E = super.B;
           }
        
           public void executeLdED() {
              super.E = super.D;
           }
        
           public void executeLdEH() {
              super.E = super.H;
           }
        
           public void executeLdHB() {
              super.H = super.B;
           }
        
           public void executeLdHD() {
              super.H = super.D;
           }
        
           public void executeLdHH() {
              super.H = super.H;
           }
        
           public void executeLdLB() {
              super.L = super.B;
           }
        
           public void executeLdLD() {
              super.L = super.D;
           }
        
           public void executeLdLH() {
              super.L = super.H;
           }
        
           public void executeLdImrHlB() {
              int var1 = this.getHL();
              super.memory.write(var1, super.B);
           }
        
           public void executeLdImrHlD() {
              int var1 = this.getHL();
              super.memory.write(var1, super.D);
           }
        
           public void executeLdImrHlH() {
              int var1 = this.getHL();
              super.memory.write(var1, super.H);
           }
        
           public void executeLdAB() {
              super.A = super.B;
           }
        
           public void executeLdAD() {
              super.A = super.D;
           }
        
           public void executeLdAH() {
              super.A = super.H;
           }
        
           public void executeAddAB() {
              int var1 = super.addTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              AddTableAluOperation var2 = super.addTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAddAD() {
              int var1 = super.addTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              AddTableAluOperation var2 = super.addTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAddAH() {
              int var1 = super.addTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              AddTableAluOperation var2 = super.addTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAdcAB() {
              int var1 = super.adcTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              AdcTableAluOperation var2 = super.adcTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAdcAD() {
              int var1 = super.adcTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              AdcTableAluOperation var2 = super.adcTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAdcAH() {
              int var1 = super.adcTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              AdcTableAluOperation var2 = super.adcTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeSubAB() {
              int var1 = super.subTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              SubTableAluOperation var2 = super.subTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeSubAD() {
              int var1 = super.subTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              SubTableAluOperation var2 = super.subTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeSubAH() {
              int var1 = super.subTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              SubTableAluOperation var2 = super.subTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeSbcAB() {
              int var1 = super.sbcTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              SbcTableAluOperation var2 = super.sbcTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeSbcAD() {
              int var1 = super.sbcTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              SbcTableAluOperation var2 = super.sbcTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeSbcAH() {
              int var1 = super.sbcTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              SbcTableAluOperation var2 = super.sbcTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAndAB() {
              int var1 = super.andTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              AndTableAluOperation var2 = super.andTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAndAD() {
              int var1 = super.andTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              AndTableAluOperation var2 = super.andTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeAndAH() {
              int var1 = super.andTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              AndTableAluOperation var2 = super.andTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeXorAB() {
              int var1 = super.xorTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              XorTableAluOperation var2 = super.xorTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeXorAD() {
              int var1 = super.xorTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              XorTableAluOperation var2 = super.xorTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeXorAH() {
              int var1 = super.xorTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              XorTableAluOperation var2 = super.xorTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeOrAB() {
              int var1 = super.orTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              OrTableAluOperation var2 = super.orTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeOrAD() {
              int var1 = super.orTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              OrTableAluOperation var2 = super.orTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeOrAH() {
              int var1 = super.orTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              OrTableAluOperation var2 = super.orTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeCpAB() {
              int var1 = super.cpTableAluOperation.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var1;
              CpTableAluOperation var2 = super.cpTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeCpAD() {
              int var1 = super.cpTableAluOperation.execute2ValuesAndCarry(super.A, super.D, super.F);
              super.A = var1;
              CpTableAluOperation var2 = super.cpTableAluOperation;
              super.F = var2.F;
           }
        
           public void executeCpAH() {
              int var1 = super.cpTableAluOperation.execute2ValuesAndCarry(super.A, super.H, super.F);
              super.A = var1;
              CpTableAluOperation var2 = super.cpTableAluOperation;
              super.F = var2.F;
           }
        
           public int execute(int opcode) {
              switch(opcode) {
              case 2:
                 this.executeLdImrBcA();
                 break;
              case 8:
                 this.executeExImrBcAfx();
                 break;
              case 18:
                 this.executeLdImrDeA();
                 break;
              case 34:
                 this.executeLdImr16M16RHl();
                 break;
              case 50:
                 this.executeLdImrM16RA();
                 break;
              case 64:
                 this.executeLdBB();
                 break;
              case 66:
                 this.executeLdBD();
                 break;
              case 68:
                 this.executeLdBH();
                 break;
              case 72:
                 this.executeLdCB();
                 break;
              case 74:
                 this.executeLdCD();
                 break;
              case 76:
                 this.executeLdCH();
                 break;
              case 80:
                 this.executeLdDB();
                 break;
              case 82:
                 this.executeLdDD();
                 break;
              case 84:
                 this.executeLdDH();
                 break;
              case 88:
                 this.executeLdEB();
                 break;
              case 90:
                 this.executeLdED();
                 break;
              case 92:
                 this.executeLdEH();
                 break;
              case 96:
                 this.executeLdHB();
                 break;
              case 98:
                 this.executeLdHD();
                 break;
              case 100:
                 this.executeLdHH();
                 break;
              case 104:
                 this.executeLdLB();
                 break;
              case 106:
                 this.executeLdLD();
                 break;
              case 108:
                 this.executeLdLH();
                 break;
              case 112:
                 this.executeLdImrHlB();
                 break;
              case 114:
                 this.executeLdImrHlD();
                 break;
              case 116:
                 this.executeLdImrHlH();
                 break;
              case 120:
                 this.executeLdAB();
                 break;
              case 122:
                 this.executeLdAD();
                 break;
              case 124:
                 this.executeLdAH();
                 break;
              case 128:
                 this.executeAddAB();
                 break;
              case 130:
                 this.executeAddAD();
                 break;
              case 132:
                 this.executeAddAH();
                 break;
              case 136:
                 this.executeAdcAB();
                 break;
              case 138:
                 this.executeAdcAD();
                 break;
              case 140:
                 this.executeAdcAH();
                 break;
              case 144:
                 this.executeSubAB();
                 break;
              case 146:
                 this.executeSubAD();
                 break;
              case 148:
                 this.executeSubAH();
                 break;
              case 152:
                 this.executeSbcAB();
                 break;
              case 154:
                 this.executeSbcAD();
                 break;
              case 156:
                 this.executeSbcAH();
                 break;
              case 160:
                 this.executeAndAB();
                 break;
              case 162:
                 this.executeAndAD();
                 break;
              case 164:
                 this.executeAndAH();
                 break;
              case 168:
                 this.executeXorAB();
                 break;
              case 170:
                 this.executeXorAD();
                 break;
              case 172:
                 this.executeXorAH();
                 break;
              case 176:
                 this.executeOrAB();
                 break;
              case 178:
                 this.executeOrAD();
                 break;
              case 180:
                 this.executeOrAH();
                 break;
              case 184:
                 this.executeCpAB();
                 break;
              case 186:
                 this.executeCpAD();
                 break;
              case 188:
                 this.executeCpAH();
                 break;
              default:
                 return -1;
              }
        
              return 0;
           }
        }""";

    assertSourceEquals(actualSource, expectedSource);
  }


}
