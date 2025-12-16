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
        
        public class MemoryPlusRegister8BitReferenceClone {
           MyAbstractMemory memory;
           Plain16BitRegister target;
           int valueDelta;
           Plain16BitRegister pc;
        
           MemoryPlusRegister8BitReferenceClone(Plain16BitRegister var1, MyAbstractMemory var2, Plain16BitRegister var3, int var4) {
              this.target = var1;
              this.memory = var2;
              this.pc = var3;
              this.valueDelta = var4;
           }
        
           byte fetchRelative() {
              int var1 = this.pc.read() + this.valueDelta & '\\uffff';
              return (byte)this.memory.read(var1, 0);
           }
        
           int getLength() {
              return 1;
           }
        
           int read() {
              int var1 = this.target.read();
              byte var2 = this.fetchRelative();
              int var3 = var1 + var2 & '\\uffff';
              this.address = var3;
              int var4 = this.memory.read(this.address, 0);
              this.value = var4;
              return this.value;
           }
        
           void write(int var1) {
              int var2 = this.target.read();
              byte var3 = this.fetchRelative();
              int var4 = var2 + var3 & '\\uffff';
              this.address = var4;
              this.value = var1;
              this.memory.write(this.address, var1);
           }
        }
        """, decompiledSource);
  }

  private String inlineAndDecompile(Object instance) {
    return "";
  }
}
