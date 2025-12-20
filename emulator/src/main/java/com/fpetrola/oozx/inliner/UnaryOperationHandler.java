package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja la generación de bytecode para instrucciones unarias (Inc, Dec, etc.).
 * Responsable de compilar operaciones unarias en registros o en memoria.
 */
public class UnaryOperationHandler {

  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;
  private final AluOperationHandler aluOperationHandler;
  private final MethodNameGenerator nameGenerator;

  public UnaryOperationHandler(RegisterValueResolver registerValueResolver,
                               MemoryAccessHandler memoryAccessHandler,
                               AluOperationHandler aluOperationHandler,
                               MethodNameGenerator nameGenerator) {
    this.registerValueResolver = registerValueResolver;
    this.memoryAccessHandler = memoryAccessHandler;
    this.aluOperationHandler = aluOperationHandler;
    this.nameGenerator = nameGenerator;
  }

  /**
   * Genera código de ejecución para instrucciones unarias (Inc, Dec, etc.)
   */
  public void generateUnaryExecute(MethodMaker mm, ParameterizedUnaryAluInstruction instruction) {
    try {
      OpcodeReference target = nameGenerator.getTargetFromUnaryInstruction(instruction);
      if (target == null) {
        throw new RuntimeException("No se pudo obtener target de la instrucción unaria");
      }

      Class<?> aluOperationClass = getAluOperationClassFromUnaryInstruction(instruction);

      if (target instanceof Register targetReg) {
        String targetRegName = targetReg.getName();
        executeUnaryRegisterOperation(mm, instruction, targetRegName, aluOperationClass);
      } else if (target instanceof IndirectMemory8BitReference indMem) {
        Variable address = memoryAccessHandler.resolveIndirectMemoryAddress(mm, indMem);
        if (address == null) {
          throw new RuntimeException("No se pudo resolver dirección para IndirectMemory8BitReference en " + instruction.getClass().getSimpleName());
        }
        Variable memory = mm.field("memory");
        executeUnaryMemoryOperation(mm, instruction, memory, address, aluOperationClass);
      } else if (target instanceof MemoryPlusRegister8BitReference memRef) {
        MemoryAccessHandler.MemoryPlusRegisterContext ctx = memoryAccessHandler.readOffsetAndCalculateAddress(mm, memRef);
        executeUnaryMemoryOperation(mm, instruction, ctx.memory, ctx.address, aluOperationClass);
      } else {
        throw new RuntimeException("Tipo de target no soportado para instrucción unaria: " + target.getClass().getSimpleName() + " en " + instruction.getClass().getSimpleName());
      }
    } catch (Exception e) {
      throw new RuntimeException("Error generando código para instrucción unaria (" + instruction.getClass().getSimpleName() + "): " + e.getMessage(), e);
    }
  }

  /**
   * Obtiene la clase de la operación ALU desde una instrucción unaria mediante reflexión
   */
  private Class<?> getAluOperationClassFromUnaryInstruction(ParameterizedUnaryAluInstruction instruction) throws Exception {
    Class<?> clazz = instruction.getClass();

    while (clazz != null && clazz != Object.class) {
      try {
        java.lang.reflect.Field aluOpField = clazz.getDeclaredField("aluOperation");
        aluOpField.setAccessible(true);
        Object aluOp = aluOpField.get(instruction);
        return aluOp.getClass();
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }

    throw new RuntimeException("No se pudo obtener aluOperation");
  }

  /**
   * Ejecuta operación unaria en un registro: reg = alu.execute(reg, flag)
   */
  private void executeUnaryRegisterOperation(MethodMaker mm, ParameterizedUnaryAluInstruction instruction,
                                            String targetRegName, Class<?> aluOperationClass) {
    Variable value = registerValueResolver.resolveRegisterValueByName(mm, targetRegName);
    Variable result = aluOperationHandler.executeUnaryAluOperation(mm, instruction, value);
    registerValueResolver.assignRegisterValue(mm, targetRegName, result);
  }

  /**
   * Ejecuta operación unaria en memoria: (addr) = alu.execute((addr), flag)
   */
  private void executeUnaryMemoryOperation(MethodMaker mm, ParameterizedUnaryAluInstruction instruction,
                                          Variable memory, Variable address, Class<?> aluOperationClass) {
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable result = aluOperationHandler.executeUnaryAluOperation(mm, instruction, value);
    memory.invoke("write", address, result);
  }
}
