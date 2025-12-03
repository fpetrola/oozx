package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
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


  private void testInlineOf(Ld ld, String expected) {
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(ld);

    Path path = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    var inliner = new CodeInliner(analyzer, path);
    var cu = inliner.inlineLd(ld);

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
    var target = new IndirectMemory8BitReference(new Memory16BitReference(memory, new Plain16BitRegister("PC"), 3), memory);
    var source = new Plain8BitRegister("B");
    return new Ld(target, source, new Plain8BitRegister("F"));
  }
}
