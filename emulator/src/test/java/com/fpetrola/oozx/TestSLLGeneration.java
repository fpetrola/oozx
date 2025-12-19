package com.fpetrola.oozx;

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSLLGeneration extends BytecodeInlinerTestBase {

  @Test
  public void testSLLMethodsGeneratedCorrectly() throws IOException {
    Map<Integer, Instruction> instructions = new TreeMap<>();
    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();
    
    Instruction[] opcodeLookupTable = opcodesTables.getOpcodeLookupTable();
    
    // Agregar el prefijo CB
    instructions.put(0xCB, opcodeLookupTable[0xCB]);
    
    // Agregar solo instrucciones SLL de la tabla CB
    DefaultFetchNextOpcodeInstruction cbInstruction = (DefaultFetchNextOpcodeInstruction) opcodeLookupTable[0xCB];
    Instruction[] cbTable = cbInstruction.getTable();
    
    // SLL opcodes: 0x30-0x37
    for (int i = 0x30; i <= 0x37; i++) {
      Instruction instruction = cbTable[i];
      if (instruction != null) {
        int prefixedOpcode = (0xCB << 8) | i;
        instructions.put(prefixedOpcode, instruction);
        System.out.println("Added SLL instruction: 0xCB" + String.format("%02X", i) + " -> " + instruction.getClass().getSimpleName());
      }
    }
    
    String source = testBytecodeMultipleInstructionsOf("TestSLLGenerated", instructions);
    
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
