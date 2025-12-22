package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Ex;
import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.DefaultTargetInstruction;
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.oozx.inliner.strategies.OpcodeReferenceStrategyFactory;

/**
 * Genera nombres únicos y descriptivos para los métodos generados en el bytecode.
 * Los nombres reflejan el tipo de instrucción y los tipos de operandos (registros, memoria, etc.)
 */
public class MethodNameGenerator {

  /**
   * Genera un nombre único para un método execute basado en la instrucción y sus referencias
   */
  public String generateUniquMethodName(TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
     StringBuilder methodName = new StringBuilder("execute").append(operationName);

    // Agregar información del target
    methodName.append(getReferenceSuffix(target));

    // Agregar información de source
    if (instruction instanceof TargetSourceInstruction tsi) {
      ImmutableOpcodeReference source = tsi.getSource();
      methodName.append(getReferenceSuffix(source));
    }

    return methodName.toString();
  }

  /**
   * Genera nombre para instrucciones unarias (Inc, Dec, etc.)
   */
  public String generateUnaryMethodName(ParameterizedUnaryAluInstruction instruction, String operationName) {
    StringBuilder methodName = new StringBuilder("execute").append(operationName);

    // Obtener el target mediante reflexión - buscar en la jerarquía de clases
    try {
      OpcodeReference target = getTargetFromUnaryInstruction(instruction);
      if (target != null) {
        methodName.append(getReferenceSuffix(target));
      }
    } catch (Exception e) {
      // Si no puede acceder al target por reflexión, usar nombre genérico
      System.err.println("Warning: No se pudo obtener target para " + operationName + ": " + e.getMessage());
    }

    return methodName.toString();
  }

  /**
   * Obtiene el target de una instrucción unaria buscando en la jerarquía de clases
   */
  public OpcodeReference getTargetFromUnaryInstruction(ParameterizedUnaryAluInstruction instruction) throws Exception {
    Class<?> clazz = instruction.getClass();

    // Buscar el campo 'target' en la jerarquía de clases
    while (clazz != null && clazz != Object.class) {
      try {
        java.lang.reflect.Field targetField = clazz.getDeclaredField("target");
        targetField.setAccessible(true);
        return (OpcodeReference) targetField.get(instruction);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }

    return null;
  }

  /**
   * Genera un sufijo basado en el tipo de referencia (Register, Memory, etc.)
   */
  public String getReferenceSuffix(ImmutableOpcodeReference reference) {
    var strategy = OpcodeReferenceStrategyFactory.create(reference);
    return strategy.generateNameSuffix();
  }

  /**
   * Genera un nombre único para un método PUSH basado en el registro destino
   */
  public String generatePushMethodName(Push instruction, String operationName) {
    StringBuilder methodName = new StringBuilder("execute").append(operationName);
    
    // Obtener el target (que debe ser un Register)
    try {
      OpcodeReference target = instruction.getTarget();
      methodName.append(getReferenceSuffix(target));
    } catch (Exception e) {
      System.err.println("Warning: No se pudo obtener target para PUSH: " + e.getMessage());
    }
    
    return methodName.toString();
  }

  /**
   * Genera nombre para cualquier instrucción, considerando su tipo.
   * Este es el método centralizado para generación de nombres.
   */
  public String generateMethodName(Instruction instruction, String operationName) {
    // Caso 1: Instrucciones de flag (SCF, CCF)
    if (instruction instanceof SCF || instruction instanceof CCF) {
      return "execute" + operationName.toLowerCase();
    }
    
    // Caso 2: TargetSourceInstructions
    if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
      OpcodeReference target = ((DefaultTargetInstruction) targetSourceInstruction).getTarget();
      return generateUniquMethodName(targetSourceInstruction, operationName, target);
    }
    
    // Caso 3: Push
    if (instruction instanceof Push pushInstr) {
      OpcodeReference target = pushInstr.getTarget();
      return new StringBuilder("execute").append(operationName)
        .append(getReferenceSuffix(target)).toString();
    }
    
    // Caso 4: Pop
    if (instruction instanceof Pop popInstr) {
      OpcodeReference target = popInstr.getTarget();
      return new StringBuilder("execute").append(operationName)
        .append(getReferenceSuffix(target)).toString();
    }
    
    // Caso 5: BitOperation (RES, SET, BIT)
    if (instruction instanceof BitOperation bitOp) {
      OpcodeReference target = bitOp.getTarget();
      return new StringBuilder("execute").append(operationName)
        .append("Bit").append(bitOp.getN())
        .append(getReferenceSuffix(target)).toString();
    }
    
    // Caso 6: DefaultTargetInstruction
    if (instruction instanceof DefaultTargetInstruction defaultTargetInstr) {
      OpcodeReference target = defaultTargetInstr.getTarget();
      return new StringBuilder("execute").append(operationName)
        .append(getReferenceSuffix(target)).toString();
    }
    
    // Caso 7: Fallback genérico
    return "execute" + operationName;
  }

  /**
   * Capitaliza la primera letra de una cadena
   */
  public String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }
  }
