package com.fpetrola.oozx;

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.registers.UnrolledRegisterBankFactory;
import org.junit.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCBPrefixGeneration extends BytecodeInlinerTestBase {

  @Test
  public void testGeneratedCodeContainsCBPrefix() throws IOException {
    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();
    
    // Usar el decoder real que contiene todos los opcodes incluyendo CB
    String source = testBytecodeMultipleInstructionsOf("TestCBPrefixGenerated", opcodesTables);
    
    System.out.println("\n=== Generated Code with CB Prefix Support ===");
    System.out.println(source);
    System.out.println("\n=== Verification ===");
    System.out.println("Contains 'executeCBPrefix': " + source.contains("executeCBPrefix"));
    System.out.println("Contains case 203 (opcode 0xCB): " + source.contains("case 203"));
    System.out.println("Contains switch for CB (nextOpcode): " + source.contains("switch(nextOpcode)"));
    
    // Assertions to verify the generated code
    assertTrue(source.contains("executeCBPrefix"), "Generated code should contain executeCBPrefix method");
    assertTrue(source.contains("case 203"), "Generated code should contain case 203 (0xCB opcode)");
    assertTrue(source.contains("switch(nextOpcode)"), "Generated code should contain switch statement for nextOpcode");
  }

  @Test
  public void testCreateZ80UnrolledIncludesCBInstructions() {
    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();
    
    // This now includes CB prefix instructions
    Z80UnRolled z80 = new UnrolledRegisterBankFactory().createZ80Unrolled(opcodesTables);
    
    // Verify the instance was created successfully
    assertNotNull(z80, "Z80UnRolled instance should be created");
    assertTrue(z80.getClass().getName().startsWith("InstructionsSwitch"), 
      "Generated class should have InstructionsSwitch name");
    
    System.out.println("\n=== Z80UnRolled Instance Created ===");
    System.out.println("Class: " + z80.getClass().getName());
    System.out.println("Methods: " + z80.getClass().getDeclaredMethods().length);
    
    // Verify it has methods for CB prefix handling
    boolean hasCBPrefixMethod = false;
    for (var method : z80.getClass().getDeclaredMethods()) {
      if (method.getName().contains("CB") || method.getName().contains("Prefix")) {
        hasCBPrefixMethod = true;
        System.out.println("Found CB/Prefix method: " + method.getName());
      }
    }
    assertTrue(hasCBPrefixMethod, "Generated class should have CB prefix handler methods");
  }
}
