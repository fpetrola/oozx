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
        
        import com.fpetrola.oozx.MyAbstractMemory;
        import com.fpetrola.oozx.t2.Plain16BitRegister;
        import com.fpetrola.z80.memory.Memory;
        
        public class MemoryPlusRegister8BitReferenceIX2 extends UnrolledRegisterBank {
           Memory memory;
        
           int read() {
              int var1 = this.PC + this.valueDelta & '\\uffff';
              byte var2 = (byte) this.memory.read(var1, 0);
              int var3 = this.IX;
              int var4 = var2 + var3 & '\\uffff';
              return this.memory.read(var4, 0);
           }
        
           void write(int var1) {
              int var2 = this.PC + this.valueDelta & '\\uffff';
              byte var3 = (byte) this.memory.read(var2, 0);
              int var4 = this.IX;
              int var5 = var3 + var4 & '\\uffff';
              this.memory.write(var5, var1);
           }
        }
        """, decompiledSource);
  }

  @SuppressWarnings("unchecked")
  private String inlineAndDecompile(Object instance) throws IOException {
    return "";
  }
}
