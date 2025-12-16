package com.fpetrola.oozx.t2;

import org.junit.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test para ClassClonerWithSootUp que verifica que se genera correctamente
 * el bytecode clonado descompilando y validando el código fuente generado.
 */
public class ClassClonerWithSootUpTest extends TestHelper {
  @Test
  public void testClassClonerGeneratesValidBytecode() throws IOException {
    String decompiledSource = cloneAndDecompile(IndirectMemory8BitReference.class.getName());

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

  @Test
  public void testClassClonerGeneratesValidBytecodeForIndirectMemory16BitReference() throws IOException {
    String decompiledSource = cloneAndDecompile(IndirectMemory16BitReference.class.getName());

    assertEquals("""
        package com.fpetrola.oozx.t2;
        
        import com.fpetrola.z80.memory.Memory;
        import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
        
        public class IndirectMemory16BitReferenceClone {
           ImmutableOpcodeReference target;
           Memory memory;
        
           IndirectMemory16BitReferenceClone(ImmutableOpcodeReference var1, Memory var2) {
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
              return this.memory.read16Bits(var1);
           }
        
           void write(int var1) {
              int var2 = this.target.read();
              this.memory.write16Bits(var1, var2);
           }
        }
        """, decompiledSource);

  }

  @Test
  public void testClassClonerGeneratesValidBytecodeForMemory8BitReference() throws IOException {
    String decompiledSource = cloneAndDecompile(Memory8BitReference.class.getName());

    assertEquals("""
        package com.fpetrola.oozx.t2;
        
        import com.fpetrola.z80.memory.Memory;
        import com.fpetrola.z80.registers.Register;
        
        public class Memory8BitReferenceClone {
           Memory memory;
           int delta;
           Register pc;
        
           Memory8BitReferenceClone(Memory var1, Register var2, int var3) {
              this.memory = var1;
              this.pc = var2;
              this.delta = var3;
           }
        
           int getLength() {
              return 1;
           }
        
           int read() {
              int var1 = this.pc.read() + this.delta & '\\uffff';
              return this.memory.read(var1, 0);
           }
        
           void write(int var1) {
              int var2 = this.pc.read();
              this.memory.write(var2, var1);
           }
        }
        """, decompiledSource);

  }

  @Test
  public void testClassClonerGeneratesValidBytecodeForMemoryPlusRegister8BitReference() throws IOException {
    String decompiledSource = cloneAndDecompile(MemoryPlusRegister8BitReference.class.getName());

    assertEquals("""
        package com.fpetrola.oozx.t2;
        
        import com.fpetrola.z80.memory.Memory;
        import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
        import com.fpetrola.z80.registers.Register;
        
        public class MemoryPlusRegister8BitReferenceClone {
           Memory memory;
           ImmutableOpcodeReference target;
           int valueDelta;
           Register pc;
        
           MemoryPlusRegister8BitReferenceClone(ImmutableOpcodeReference var1, Memory var2, Register var3, int var4) {
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
              return this.memory.read(var3, 0);
           }
        
           void write(int var1) {
              int var2 = this.target.read();
              byte var3 = this.fetchRelative();
              int var4 = var2 + var3 & '\\uffff';
              this.memory.write(var4, var1);
           }
        }
        """, decompiledSource);

  }
}
