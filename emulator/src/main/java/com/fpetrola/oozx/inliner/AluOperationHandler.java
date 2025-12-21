package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja todas las operaciones ALU (Arithmetic Logic Unit),
 * incluyendo ejecución de operaciones binarias y unarias con flags.
 */
public class AluOperationHandler {

  public static final String FLAG = "F";
  private final RegisterValueResolver registerValueResolver;

  public AluOperationHandler(RegisterValueResolver registerValueResolver) {
    this.registerValueResolver = registerValueResolver;
  }

  /**
   * Obtiene la clase específica de la operación ALU para una instrucción
   */
  public Class<?> getAluOperationClass(TargetSourceInstruction instruction) {
    try {
      Class<?> instructionClass = instruction.getClass();
      String instructionName = instructionClass.getSimpleName();
      
      // Mapeo de nombres especiales
      String innerClassName = switch(instructionName) {
        case "RRC" -> "RRCAluOperation";
        case "SRA" -> "SRAAluOperation";
        default -> instructionName + "TableAluOperation";
      };

      // Buscar el inner class declarado
      for (Class<?> innerClass : instructionClass.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals(innerClassName)) {
          return innerClass;
        }
      }
    } catch (Exception e) {
      // Ignorar
    }
    return Object.class;
  }

  /**
   * Obtiene el nombre del campo ALU operation para una instrucción
   */
  public String getAluOperationFieldName(String instructionClassName) {
    // Mapeo especial para instrucciones que tienen names diferentes
    return switch(instructionClassName) {
      case "Inc" -> "inc8TableAluOperation";
      case "Dec" -> "dec8TableAluOperation";
      case "RRC" -> "rRCAluOperation";
      case "SRA" -> "sRAAluOperation";
      case "SLL" -> "sLLAluOperation";
      default -> instructionClassName.toLowerCase() + "TableAluOperation";
    };
  }

  /**
   * Ejecuta una operación ALU: lee valor (de memoria o registro para LD), aplica operación y escribe resultado
   */
  public void executeAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                  Variable memory, Variable address, String sourceRegName) {
    Variable source = registerValueResolver.resolveRegisterValueByName(mm, sourceRegName);

    // Para LD, escribir directamente sin variable intermedia
    if (instruction instanceof Ld) {
      memory.invoke("write", address, source);
      return;
    }

    // Si la instrucción tiene una operación ALU, usarla
    if (isAluOperation(instruction)) {
      executeWithAluOperation(mm, instruction, memory, address, sourceRegName);
      return;
    }

    // Para XOR/OR: leer, aplicar operación y escribir
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable result = mm.var(int.class);

    if (instruction instanceof Xor) {
      result.set(source.xor(value));
    } else if (instruction instanceof Or) {
      result.set(source.or(value));
    }

    // Escribir el resultado
    memory.invoke("write", address, result);
  }

  /**
   * Ejecuta una operación ALU para valores de 16 bits: lee valor (de memoria), aplica operación y escribe resultado
   */
  public void executeAluOperation16Bit(MethodMaker mm, TargetSourceInstruction instruction,
                                       Variable memory, Variable address, String sourceRegName) {
    Variable source = registerValueResolver.resolveRegisterValueByName(mm, sourceRegName);

    // Leer valor de 16 bits desde la dirección
    Variable value = address;

    // Para LD, escribir directamente el valor leído
    if (instruction instanceof Ld) {
      memory.invoke("write16BitsReverse", source, value);
      return;
    }

    // Para XOR/OR: aplicar operación y escribir
    Variable result = mm.var(int.class);

    if (instruction instanceof Xor) {
      result.set(source.xor(value));
    } else if (instruction instanceof Or) {
      result.set(source.or(value));
    }

    // Escribir el resultado (16 bits)
    memory.invoke("write16BitsReverse", result, address);
  }

  /**
   * Ejecuta operación ALU con un valor leído de memoria
   */
  public void executeWithAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                      Variable memory, Variable address, String sourceRegName) {
    // Leer valor de memoria
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable source = registerValueResolver.resolveRegisterValueByName(mm, sourceRegName);

    // Obtener la operación ALU
    String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());
    Variable aluOp = mm.field(fieldName);

    // Ejecutar la operación ALU: execute2ValuesAndCarry(value, source, flag)
    Variable result = mm.var(int.class);
    Variable flag = mm.field(FLAG);
    result.set(aluOp.invoke("execute2ValuesAndCarry", value, source, flag));

    // Escribir el resultado
    memory.invoke("write", address, result);
  }

  /**
   * Ejecuta la operación ALU unaria y actualiza los flags
   * Retorna el resultado y actualiza el campo de flags
   */
  public Variable executeUnaryAluOperation(MethodMaker mm, ParameterizedUnaryAluInstruction instruction,
                                           Variable value) {
    String operationName = instruction.getClass().getSimpleName();
    String fieldName = getAluOperationFieldName(operationName);
    
    Variable aluOp = mm.field(fieldName);
    Variable flag = mm.field(FLAG);

    Variable flagField = mm.field(FLAG);

    aluOp.field(FLAG).set(flagField);

    // Ejecutar: result = aluOperation.execute2ValuesAndCarry(value, 0, flag)
    Variable result = mm.var(int.class);
    result.set(aluOp.invoke("execute2ValuesAndCarry", value, 0, flag));
    
    // Actualizar flags
    flagField.set(aluOp.field(FLAG));
    
    return result;
  }

  /**
   * Verifica si una instrucción es una operación ALU
   */
  private boolean isAluOperation(TargetSourceInstruction instruction) {
    // Por ahora, asumir que cualquier instrucción que no sea Ld es una operación ALU
    // Esta lógica podría necesitar ser más sofisticada
    return !(instruction instanceof Ld);
  }
}
