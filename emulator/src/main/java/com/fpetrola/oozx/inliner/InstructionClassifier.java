package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

import java.util.Map;

/**
 * Clasifica instrucciones por tipo y característica.
 * Identifica instrucciones prefijadas, operaciones ALU, instrucciones no soportadas, etc.
 */
public class InstructionClassifier {

  /**
   * Verifica si un opcode es un opcode prefijado (ej: 0xCB00 significa prefijo CB con siguiente byte 0x00)
   */
  public boolean isPrefixedOpcode(Integer opcode, Map<Integer, Instruction> instructions) {
    // Un opcode prefijado tiene más de 1 byte y existe un prefijo correspondiente
    if (opcode > 0xFF) {
      int prefixByte = (opcode >> 8) & 0xFF;
      // Buscar si existe el prefijo en las instrucciones
      for (Integer key : instructions.keySet()) {
        if (key == prefixByte && instructions.get(key) instanceof DefaultFetchNextOpcodeInstruction) {
          return true;
        }
      }
    }
    return false;
  }

  /**
    * Verifica si una instrucción no puede ser inlineada (ej: LdAI, LdAR, LD R/I, IN, OUT, EX)
    * Estas instrucciones usan registros especiales (I, R) o referencias complejas que requieren lógica especial
    */
  public boolean isUnsupportedInstruction(Instruction instruction) {
    if (instruction == null)
      return false;
    String className = instruction.getClass().getSimpleName();

    // Filtrar instrucciones específicas con I y R, y instrucciones complejas
    if (className.equals("LdAI") || className.equals("LdAR") || className.equals("DAA") ||
        className.equals("In") || className.equals("Out") || className.equals("Ex")) {
      return true;
    }

    // Filtrar Ld que involucran registros I o R
    if (instruction instanceof Ld ld) {
      try {
        OpcodeReference target = ld.getTarget();
        ImmutableOpcodeReference source = ld.getSource();

        // Verificar si target es R o I
        if (target instanceof Register reg) {
          String regName = reg.getName();
          if ("R".equals(regName) || "I".equals(regName)) {
            return true;
          }
        }

        // Verificar si source es R o I
        if (source instanceof Register reg) {
          String regName = reg.getName();
          if ("R".equals(regName) || "I".equals(regName)) {
            return true;
          }
        }
      } catch (Exception e) {
        // Si hay error accediendo a target/source, permitir procesar la instrucción
      }
    }

    return false;
  }

  /**
    * Verifica si una instrucción tiene una operación ALU asociada
    */
  public boolean isAluOperation(TargetSourceInstruction instruction) {
    // Buscar por reflection si la clase tiene un inner class que implemente AluOperation
    try {
      Class<?> instructionClass = instruction.getClass();
      for (Class<?> innerClass : instructionClass.getDeclaredClasses()) {
        if (AluOperation.class.isAssignableFrom(innerClass)) {
          return true;
        }
      }
    } catch (Exception e) {
      // Ignorar excepciones
    }
    return false;
  }

  /**
   * Verifica si una instrucción es una instrucción PUSH
   */
  public boolean isPushInstruction(Instruction instruction) {
    return instruction instanceof Push;
  }
  }
