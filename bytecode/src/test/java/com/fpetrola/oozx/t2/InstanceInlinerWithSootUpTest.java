package com.fpetrola.oozx.t2;

import com.fpetrola.oozx.MyAbstractMemory;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.UnrolledRegisterBank;
import com.fpetrola.z80.registers.UnrolledRegisterBankFactory;
import org.junit.Test;

import java.io.IOException;

import static com.fpetrola.z80.registers.RegisterName.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstanceInlinerWithSootUpTest {

  @Test
  public void testClassClonerGeneratesValidBytecode() throws IOException {
    RegisterBank bank = new UnrolledRegisterBankFactory().createBank();
    var target = new MemoryPlusRegister8BitReference(bank.get(IX), new MyAbstractMemory(), bank.get(PC), 2);

    String decompiledSource = inlineAndDecompile(target);

    assertEquals("""
        import com.fpetrola.z80.memory.Memory;
        import com.fpetrola.z80.registers.UnrolledRegisterBank;
        
        public class MemoryPlusRegister8BitReferenceIX2 extends UnrolledRegisterBank {
           Memory memory;
        
           int read() {
              int var1 = this.PC + 2 & '\\uffff';
              byte var2 = (byte)this.memory.read(var1, 0);
              int var3 = this.IX;
              int var4 = var2 + var3 & '\\uffff';
              return this.memory.read(var4, 0);
           }
        
           void write(int var1) {
              int var2 = this.PC + 2 & '\\uffff';
              byte var3 = (byte)this.memory.read(var2, 0);
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
