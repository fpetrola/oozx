package com.fpetrola.oozx;

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.impl.SLL;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import org.junit.Test;

import java.lang.reflect.Field;

public class TestSLLTarget {

  @Test
  public void inspectSLLInstructions() throws Exception {
    OOZ80 ooz80 = Helper.createOOZ80();
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    TableBasedOpCodeDecoder opcodesTables = instructionFetcher.multiOpcodeFetcher.getOpcodesTables();
    
    DefaultFetchNextOpcodeInstruction cbInstruction = (DefaultFetchNextOpcodeInstruction) opcodesTables.getOpcodeLookupTable()[0xCB];
    Object[] cbTable = cbInstruction.getTable();
    
    System.out.println("\n=== Inspecting SLL Instructions ===");
    
    for (int i = 0x30; i <= 0x37; i++) {
      Object instr = cbTable[i];
      if (instr instanceof ParameterizedUnaryAluInstruction) {
        ParameterizedUnaryAluInstruction unaryInstr = (ParameterizedUnaryAluInstruction) instr;
        
        // Get target using reflection, iterating up the hierarchy
        OpcodeReference target = null;
        Class<?> clazz = unaryInstr.getClass();
        while (clazz != null && clazz != Object.class && target == null) {
          try {
            Field targetField = clazz.getDeclaredField("target");
            targetField.setAccessible(true);
            target = (OpcodeReference) targetField.get(unaryInstr);
            System.out.println("  Found 'target' in " + clazz.getSimpleName());
          } catch (NoSuchFieldException e) {
            clazz = clazz.getSuperclass();
          }
        }
        
        if (target == null) {
          System.out.println("  Warning: Could not find target field via reflection");
        }
        
        System.out.println("Instruction 0xCB" + String.format("%02X", i) + " (" + instr.getClass().getSimpleName() + ")");
        System.out.println("  Target: " + (target != null ? target.getClass().getSimpleName() : "NULL"));
        if (target != null) {
          System.out.println("  Target details: " + target);
        }
      }
    }
  }
}
