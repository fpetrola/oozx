package com.fpetrola.oozx.t2;

import com.fpetrola.oozx.MyAbstractMemory;
import org.junit.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test para ClassClonerWithSootUp que verifica que se genera correctamente
 * el bytecode clonado descompilando y validando el código fuente generado.
 */
public class InstanceInlinerWithSootUpTest extends TestHelper {

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
        
        public class IndirectMemory8BitReferenceClone {
           ImmutableOpcodeReference target;
           Memory memory;
        
           IndirectMemory8BitReferenceClone(ImmutableOpcodeReference var1, Memory var2) {
              this.target = var1;
              this.memory = var2;
           }
        
           int getLength() {
              return this.target.getLength();
           }
        
           Memory getMemory() {
              return this.memory;
           }
        
           ImmutableOpcodeReference getTarget() {
              return this.target;
           }
        
           int read() {
              int var1 = this.target.read();
              return this.memory.read(var1, 0);
           }
        
           void write(int var1) {
              int var2 = this.target.read();
              this.memory.write(var2, var1);
           }
        }
        """, decompiledSource);
  }

  private String inlineAndDecompile(Object instance) {
    return "";
  }
}
