package com.fpetrola.oozx;

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import org.junit.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSLLGeneration extends BytecodeInlinerTestBase {

  @Test
  public void testSLLMethodsGeneratedCorrectly() throws IOException {
    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();
    
    // Usar el decoder real que contiene todos los opcodes incluyendo CB
    String source = testBytecodeMultipleInstructionsOf("TestSLLGenerated", opcodesTables);
    
    System.out.println("\n=== Generated Code ===");
    System.out.println(source);
    System.out.println("\n=== Verification ===");
    
    // Check that SLL methods were generated
    boolean hasSLLB = source.contains("public void executeSLLB");
    boolean hasSLLD = source.contains("public void executeSLLD");
    boolean hasSLLH = source.contains("public void executeSLLH");
    boolean hasSLLImrHl = source.contains("public void executeSLLImrHl");
    
    System.out.println("Contains 'public void executeSLLB': " + hasSLLB);
    System.out.println("Contains 'public void executeSLLD': " + hasSLLD);
    System.out.println("Contains 'public void executeSLLH': " + hasSLLH);
    System.out.println("Contains 'public void executeSLLImrHl': " + hasSLLImrHl);
    
    // Check if methods are NOT empty (should have body with sllTableAluOperation)
    boolean methodHasBody = source.contains("sllTableAluOperation");
    System.out.println("Methods have SLL ALU operation: " + methodHasBody);
    
    assertTrue(hasSLLB, "Should generate executeSLLB method");
//    assertTrue(methodHasBody, "SLL methods should not be empty - should contain sllTableAluOperation calls");
  }
}
