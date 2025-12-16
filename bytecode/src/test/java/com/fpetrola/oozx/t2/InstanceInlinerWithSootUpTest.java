package com.fpetrola.oozx.t2;

import com.fpetrola.oozx.MyAbstractMemory;
import org.junit.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstanceInlinerWithSootUpTest {

  @Test
  public void testClassClonerGeneratesValidBytecode() throws IOException {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );

    String decompiledSource = inlineAndDecompile(target);

    assertEquals("""
        package com.fpetrola.oozx.t2;
        
        import com.fpetrola.z80.memory.Memory;
        import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
        import com.fpetrola.z80.registers.Register;
        
        public class MemoryPlusRegister8BitReferenceClone {
           Memory memory;
           int IX;
           int valueDelta;
           int PC;
        
           MemoryPlusRegister8BitReferenceClone(Plain16BitRegister var1, MyAbstractMemory var2, Plain16BitRegister var3, int var4) {
              this.IX = var1.read();
              this.memory = var2;
              this.PC = var3.read();
              this.valueDelta = var4;
           }
        
           byte fetchRelative() {
              int var1 = this.PC + this.valueDelta & '\\uffff';
              return (byte)this.memory.read(var1, 0);
           }
        
           int getLength() {
              return 1;
           }
        
           int read() {
              int var1 = this.IX;
              byte var2 = this.fetchRelative();
              int var3 = var1 + var2 & '\\uffff';
              return this.memory.read(var3, 0);
           }
        
           void write(int var1) {
              int var2 = this.IX;
              byte var3 = this.fetchRelative();
              int var4 = var2 + var3 & '\\uffff';
              this.memory.write(var4, var1);
           }
        }
        """, decompiledSource);
  }

  private String inlineAndDecompile(Object instance) {
    return "";
  }
}
