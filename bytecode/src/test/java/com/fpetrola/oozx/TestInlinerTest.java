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

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInlinerTest {

  @Test
  public void testInline1() {
    var ld = getLd1();
    testInlineOf(ld, """
        public class Ld1 extends TargetSourceInstruction<ImmutableOpcodeReference> {
            int A;
            int IX;
            Memory memory;
            Register pc;
        
            public Ld1(Memory memory, Register pc) {
                this.memory= memory;
                this.pc= pc;
            }
        
            public void execute() {
                  int dd = (byte) memory.read((pc.read() + 2) & 0xFFFF, 0);
                  memory.write((IX + dd) & 0xFFFF, A);
            }
        }
        """);
  }

  @Test
  public void testInline2() {
    var ld = getLd2();
    testInlineOf(ld, """
        public class Ld2 extends TargetSourceInstruction<ImmutableOpcodeReference> {
            int B;
            int IY;
            Memory memory;
        
            public Ld2(Memory memory) {
                this.memory= memory;
            }
        
            public void execute() {
                memory.write(IY, B);
            }
        }
        """);
  }


  @Test
  public void testInline3() {
    var ld = getLd3();
    testInlineOf(ld, """
        public class Ld3 extends TargetSourceInstruction<ImmutableOpcodeReference> {
            int B;
            int IY;
            Memory memory;
        
             Register pc;
        
            public Ld3(Memory memory, Register pc) {
                this.memory= memory;
                this.pc= pc;
            }
        
            public void execute() {
                int address= memory.read16Bits((pc.read() + 3) & 0xFFFF);
                memory.write(address, B);
            }
        }
        """);
  }
//
//  @Test
//  public void testXorInline1() {
//    var xor = getXor1();
//    testInlineOf(xor, """
//        public class Xor1 extends TargetSourceInstruction<ImmutableOpcodeReference> {
//            int C;
//            int IX;
//            Memory memory;
//            Register pc;
//
//            public Xor1(Memory memory, Register pc) {
//                this.memory= memory;
//                this.pc= pc;
//            }
//
//            public void execute() {
//                  int dd = (byte) memory.read((pc.read() + 2) & 0xFFFF, 0);
//                  int value = memory.read((IX + dd) & 0xFFFF, 0);
//                  value1 ^= (value2);
//            }
//        }
//        """);
//  }
//
//  @Test
//  public void testXorInline2() {
//    var xor = getXor2();
//    testInlineOf(xor, """
//        public class Xor2 extends TargetSourceInstruction<ImmutableOpcodeReference> {
//            int D;
//            int IY;
//            Memory memory;
//
//            public Xor2(Memory memory) {
//                this.memory= memory;
//            }
//
//            public void execute() {
//                int value = memory.read(IY, 0);
//                value1 ^= (value2);
//            }
//        }
//        """);
//  }
//
//  @Test
//  public void testXorInline3() {
//    var xor = getXor3();
//    testInlineOf(xor, """
//        public class Xor3 extends TargetSourceInstruction<ImmutableOpcodeReference> {
//            int E;
//            int IX;
//            Memory memory;
//
//             Register pc;
//
//            public Xor3(Memory memory, Register pc) {
//                this.memory= memory;
//                this.pc= pc;
//            }
//
//            public void execute() {
//                int address= memory.read16Bits((pc.read() + 3) & 0xFFFF);
//                int value = memory.read(address, 0);
//                value1 ^= (value2);
//            }
//        }
//        """);
//  }
//
//  @Test
//  public void testOrInline1() {
//    var or = getOr1();
//    testInlineOf(or, """
//        public class Or1 extends TargetSourceInstruction<ImmutableOpcodeReference> {
//            int C;
//            int IX;
//            Memory memory;
//            Register pc;
//
//            public Or1(Memory memory, Register pc) {
//                this.memory= memory;
//                this.pc= pc;
//            }
//
//            public void execute() {
//                  int dd = (byte) memory.read((pc.read() + 2) & 0xFFFF, 0);
//                  int value = memory.read((IX + dd) & 0xFFFF, 0);
//                  value1 |= (value2);
//            }
//        }
//        """);
//  }
//
//  @Test
//  public void testOrInline2() {
//    var or = getOr2();
//    testInlineOf(or, """
//        public class Or2 extends TargetSourceInstruction<ImmutableOpcodeReference> {
//            int D;
//            int IY;
//            Memory memory;
//
//            public Or2(Memory memory) {
//                this.memory= memory;
//            }
//
//            public void execute() {
//                int value = memory.read(IY, 0);
//                value1 |= (value2);
//            }
//        }
//        """);
//  }
//
//  @Test
//  public void testOrInline3() {
//    var or = getOr3();
//    testInlineOf(or, """
//        public class Or3 extends TargetSourceInstruction<ImmutableOpcodeReference> {
//            int E;
//            int IX;
//            Memory memory;
//
//             Register pc;
//
//            public Or3(Memory memory, Register pc) {
//                this.memory= memory;
//                this.pc= pc;
//            }
//
//            public void execute() {
//                int address= memory.read16Bits((pc.read() + 3) & 0xFFFF);
//                int value = memory.read(address, 0);
//                value1 |= (value2);
//            }
//        }
//        """);
//  }

  private void testInlineOf(Ld ld, String expected) {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(ld);

    Path path = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    var inliner = new CodeInliner(analyzer, path);
    var cu = inliner.inlineLd(ld);

    String inlinedCode = cu.toString();

    assertEquals(expected, inlinedCode);
  }

  private void testInlineOf(Xor xor, String expected) {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(xor);

    Path path = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    var inliner = new CodeInliner(analyzer, path);
    var cu = inliner.inlineXor(xor);

    String inlinedCode = cu.toString();

    assertEquals(expected, inlinedCode);
  }

  private void testInlineOf(Or or, String expected) {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(or);

    Path path = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    var inliner = new CodeInliner(analyzer, path);
    var cu = inliner.inlineOr(or);

    String inlinedCode = cu.toString();

    assertEquals(expected, inlinedCode);
  }

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

  private static Xor getXor2() {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("D");
    return new Xor(target, source, new Plain8BitRegister("F"));
  }

  private static Xor getXor3() {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IX"), 3), memory);
    var source = new Plain8BitRegister("E");
    return new Xor(target, source, new Plain8BitRegister("F"));
  }

  private static Or getOr1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("C");
    var or = new Or(target, source, new Plain8BitRegister("F"));
    return or;
  }

  private static Or getOr2() {
    var target = new IndirectMemory8BitReference(new Plain16BitRegister("IY"), new MyAbstractMemory());
    var source = new Plain8BitRegister("D");
    return new Or(target, source, new Plain8BitRegister("F"));
  }

  private static Or getOr3() {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("IX"), 3), memory);
    var source = new Plain8BitRegister("E");
    return new Or(target, source, new Plain8BitRegister("F"));
  }
}
