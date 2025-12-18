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
import org.junit.Test;

import java.io.IOException;
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
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new DummyMemory());
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
    DummyMemory memory = new DummyMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("B");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdImrM16RB() {
              int var1 = this.read16(super.PC);
              super.memory.write(var1, super.B);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeInline4() throws IOException {
    DummyMemory memory = new DummyMemory();
    var target = new IndirectMemory16BitReference(new Memory16BitReference(memory, new Plain16BitRegister("PC"), 3), memory);
    var source = new Plain8BitRegister("C");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));

    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdImr16M16RC() {
              int var1 = this.read16(super.PC);
              super.memory.write16BitsReverse(super.C, var1);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeInline5() throws IOException {
    DummyMemory memory = new DummyMemory();
    var target = new IndirectMemory16BitReference(new Plain16BitRegister("IX"), memory);
    var source = new Plain8BitRegister("D");
    var ld = new Ld(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(ld);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class LdBytecode extends Z80UnRolled {
           public void executeLdImr16IxD() {
              super.memory.write16BitsReverse(super.D, super.IX);
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
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
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
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new DummyMemory());
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
    DummyMemory memory = new DummyMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    var xor = new Xor(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(xor);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class XorBytecode extends Z80UnRolled {
           public void executeXorImrM16RC() {
              int var1 = this.read16(super.PC);
              int var2 = super.memory.read(var1, 0);
              int var3 = super.xorTableAluOperation.execute2ValuesAndCarry(var2, super.C, super.F);
              super.memory.write(var1, var3);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeOrInline2() throws IOException {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new DummyMemory());
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
    DummyMemory memory = new DummyMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    var or = new Or(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(or);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class OrBytecode extends Z80UnRolled {
           public void executeOrImrM16RC() {
              int var1 = this.read16(super.PC);
              int var2 = super.memory.read(var1, 0);
              int var3 = super.orTableAluOperation.execute2ValuesAndCarry(var2, super.C, super.F);
              super.memory.write(var1, var3);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeAndInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
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
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new DummyMemory());
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
    DummyMemory memory = new DummyMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3), memory);
    var source = new Plain8BitRegister("C");
    var and = new And(target, source, new Plain8BitRegister("F"));
    String actualSource = testBytecodeInlineOf(and);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class AndBytecode extends Z80UnRolled {
           public void executeAndImrM16RC() {
              int var1 = this.read16(super.PC);
              int var2 = super.memory.read(var1, 0);
              int var3 = super.andTableAluOperation.execute2ValuesAndCarry(var2, super.C, super.F);
              super.memory.write(var1, var3);
           }
        }""";
    assertSourceEquals(actualSource, expectedSource);
  }

  @Test
  public void testBytecodeSubInline1() throws IOException {
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
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
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
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
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
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
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
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
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
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
    var target = new MemoryPlusRegister8BitReference(new Plain16BitRegister("IX"), new DummyMemory(), new Plain16BitRegister("PC"), 2);
    var source = new Plain8BitRegister("E");
    var add = new Add(target, source, new Plain8BitRegister("F"));

    Map<Integer, Instruction> instructions = new TreeMap<>(Map.of(10, ld, 20, xor, 30, add));
    String actualSource = testBytecodeMultipleInstructionsOf("MultiInstructionBytecodetestBytecodeMultipleInstructionsSwitch", instructions);

    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        
        public class MultiInstructionBytecodetestBytecodeMultipleInstructionsSwitch extends Z80UnRolled {
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
    Map<Integer, Instruction> instructions = new TreeMap<>();

    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();

    Instruction[] opcodeLookupTable = opcodesTables.getOpcodeLookupTable();
    // Opcodes significativos para pruebas: registros, memoria, ALU, 16-bit, etc
    int[] testOpcodes = {
        0x02,   // LD (BC), A
        0x0A,   // LD A, (BC)
        0x12,   // LD (DE), A
        0x1A,   // LD A, (DE)
        0x22,   // LD (nn), HL
        0x32,   // LD (nn), A
        0x40,   // LD B, B
        0x46,   // LD B, (HL)
        0x78,   // LD A, B
        0x7C,   // LD A, H
        0x80,   // ADD A, B
        0x86,   // ADD A, (HL)
        0x88,   // ADC A, B
        0x8E,   // ADC A, (HL)
        0x90,   // SUB A, B
        0x96,   // SUB A, (HL)
        0xA0,   // AND A, B
        0xA6,   // AND A, (HL)
        0xA8,   // XOR A, B
        0xAE,   // XOR A, (HL)
        0xB0,   // OR A, B
        0xB6,   // OR A, (HL)
        0xB8,   // CP A, B
        0xBE    // CP A, (HL)
    };

    for (int i = 0; i < opcodeLookupTable.length; i++) {
      Instruction instruction = opcodeLookupTable[i];
      if (instruction != null) {
        for (int opcode : testOpcodes) {
          if (i == opcode) {
            instructions.put(i, instruction);
            break;
          }
        }
      }
    }

    String actualSource = testBytecodeMultipleInstructionsOf("MultiInstructionBytecode", instructions);
    String expectedSource = """
        import com.fpetrola.oozx.Z80UnRolled;
        import com.fpetrola.z80.instructions.impl.Adc.AdcTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Add.AddTableAluOperation;
        import com.fpetrola.z80.instructions.impl.And.AndTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Cp.CpTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Or.OrTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Sub.SubTableAluOperation;
        import com.fpetrola.z80.instructions.impl.Xor.XorTableAluOperation;
        
        public class MultiInstructionBytecode extends Z80UnRolled {
           public void executeLdImrBcA() {
              int var1 = this.getBC();
              super.memory.write(var1, super.A);
           }
        
           public void executeLdAImrBc() {
              int var1 = this.getBC();
              int var2 = super.memory.read(var1, 0);
              super.A = var2;
           }
        
           public void executeLdImrDeA() {
              int var1 = this.getDE();
              super.memory.write(var1, super.A);
           }
        
           public void executeLdAImrDe() {
              int var1 = this.getDE();
              int var2 = super.memory.read(var1, 0);
              super.A = var2;
           }
        
           public void executeLdImr16M16RHl() {
              int var1 = this.read16(super.PC);
              int var2 = this.getHL();
              super.memory.write16BitsReverse(var2, var1);
           }
        
           public void executeLdImrM16RA() {
              int var1 = this.read16(super.PC);
              super.memory.write(var1, super.A);
           }
        
           public void executeLdBB() {
              super.B = super.B;
           }
        
           public void executeLdBImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              super.B = var2;
           }
        
           public void executeLdAB() {
              super.A = super.B;
           }
        
           public void executeLdAH() {
              super.A = super.H;
           }
        
           public void executeAddAB() {
              AddTableAluOperation var1 = super.addTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAddAImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              AddTableAluOperation var3 = super.addTableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(super.A, var2, super.F);
              super.A = var4;
              super.F = var3.F;
           }
        
           public void executeAdcAB() {
              AdcTableAluOperation var1 = super.adcTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAdcAImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              AdcTableAluOperation var3 = super.adcTableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(super.A, var2, super.F);
              super.A = var4;
              super.F = var3.F;
           }
        
           public void executeSubAB() {
              SubTableAluOperation var1 = super.subTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeSubAImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              SubTableAluOperation var3 = super.subTableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(super.A, var2, super.F);
              super.A = var4;
              super.F = var3.F;
           }
        
           public void executeAndAB() {
              AndTableAluOperation var1 = super.andTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeAndAImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              AndTableAluOperation var3 = super.andTableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(super.A, var2, super.F);
              super.A = var4;
              super.F = var3.F;
           }
        
           public void executeXorAB() {
              XorTableAluOperation var1 = super.xorTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeXorAImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              XorTableAluOperation var3 = super.xorTableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(super.A, var2, super.F);
              super.A = var4;
              super.F = var3.F;
           }
        
           public void executeOrAB() {
              OrTableAluOperation var1 = super.orTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeOrAImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              OrTableAluOperation var3 = super.orTableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(super.A, var2, super.F);
              super.A = var4;
              super.F = var3.F;
           }
        
           public void executeCpAB() {
              CpTableAluOperation var1 = super.cpTableAluOperation;
              int var2 = var1.execute2ValuesAndCarry(super.A, super.B, super.F);
              super.A = var2;
              super.F = var1.F;
           }
        
           public void executeCpAImrHl() {
              int var1 = this.getHL();
              int var2 = super.memory.read(var1, 0);
              CpTableAluOperation var3 = super.cpTableAluOperation;
              int var4 = var3.execute2ValuesAndCarry(super.A, var2, super.F);
              super.A = var4;
              super.F = var3.F;
           }
        
           public int execute(int opcode) {
              switch(opcode) {
              case 2:
                 this.executeLdImrBcA();
                 break;
              case 10:
                 this.executeLdAImrBc();
                 break;
              case 18:
                 this.executeLdImrDeA();
                 break;
              case 26:
                 this.executeLdAImrDe();
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
              case 70:
                 this.executeLdBImrHl();
                 break;
              case 120:
                 this.executeLdAB();
                 break;
              case 124:
                 this.executeLdAH();
                 break;
              case 128:
                 this.executeAddAB();
                 break;
              case 134:
                 this.executeAddAImrHl();
                 break;
              case 136:
                 this.executeAdcAB();
                 break;
              case 142:
                 this.executeAdcAImrHl();
                 break;
              case 144:
                 this.executeSubAB();
                 break;
              case 150:
                 this.executeSubAImrHl();
                 break;
              case 160:
                 this.executeAndAB();
                 break;
              case 166:
                 this.executeAndAImrHl();
                 break;
              case 168:
                 this.executeXorAB();
                 break;
              case 174:
                 this.executeXorAImrHl();
                 break;
              case 176:
                 this.executeOrAB();
                 break;
              case 182:
                 this.executeOrAImrHl();
                 break;
              case 184:
                 this.executeCpAB();
                 break;
              case 190:
                 this.executeCpAImrHl();
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
