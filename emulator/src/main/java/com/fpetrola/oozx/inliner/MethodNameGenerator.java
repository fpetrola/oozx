package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

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
    if (reference instanceof Register) {
      Register reg = (Register) reference;
      // Para registros, usar el nombre del registro en formato camelCase
      return capitalizeFirstLetter(reg.getName());
    } else if (reference instanceof MemoryPlusRegister8BitReference) {
      MemoryPlusRegister8BitReference mprf = (MemoryPlusRegister8BitReference) reference;
      // MPRF: MemoryPlusRegister8BitReference
      StringBuilder suffix = new StringBuilder("Mprf");
      ImmutableOpcodeReference innerTarget = mprf.getTarget();
      if (innerTarget instanceof Register) {
        Register reg = (Register) innerTarget;
        suffix.append(capitalizeFirstLetter(reg.getName()));
      }
      return suffix.toString();
    } else if (reference instanceof IndirectMemory8BitReference) {
      IndirectMemory8BitReference imr = (IndirectMemory8BitReference) reference;
      // IMR: IndirectMemory8BitReference
      StringBuilder suffix = new StringBuilder("Imr");
      ImmutableOpcodeReference innerTarget = imr.getTarget();
      if (innerTarget instanceof Register) {
        Register reg = (Register) innerTarget;
        suffix.append(capitalizeFirstLetter(reg.getName()));
      } else if (innerTarget instanceof Memory16BitReference) {
        suffix.append("M16R");
      }
      return suffix.toString();
    } else if (reference instanceof IndirectMemory16BitReference) {
      IndirectMemory16BitReference imr16 = (IndirectMemory16BitReference) reference;
      // IMR16: IndirectMemory16BitReference
      StringBuilder suffix = new StringBuilder("Imr16");
      ImmutableOpcodeReference innerTarget = imr16.getTarget();
      if (innerTarget instanceof Register) {
        Register reg = (Register) innerTarget;
        suffix.append(capitalizeFirstLetter(reg.getName()));
      } else if (innerTarget instanceof Memory16BitReference) {
        suffix.append("M16R");
      }
      return suffix.toString();
    } else if (reference instanceof Memory16BitReference) {
      // M16R: Memory16BitReference
      return "M16R";
    } else if (reference instanceof Memory8BitReference) {
      // M16R: Memory16BitReference
      return "M8R";
    }

    return "";
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
