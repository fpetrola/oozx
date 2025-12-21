package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.impl.Dec16;
import com.fpetrola.z80.instructions.impl.Inc16;
import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.DefaultTargetInstruction;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registro centralizado de handlers para diferentes tipos de instrucciones.
 * Permite agregar nuevas instrucciones sin modificar múltiples archivos.
 * 
 * Ejemplo de uso:
 *   registry.registerHandler(MyInstruction.class, (cm, instr, mm, opName, genMethods) -> {
 *     // generar bytecode
 *     return true;
 *   });
 */
public class InstructionHandlerRegistry {

  @FunctionalInterface
  public interface InstructionHandler {
    /**
     * Genera bytecode para la instrucción
     * @return true si fue procesada, false si no puede procesar
     */
    boolean handle(ClassMaker cm, Instruction instruction, MethodMaker mm, 
                   String operationName, Set<String> generatedMethods);
  }

  private final Map<Class<?>, InstructionHandler> handlers = new HashMap<>();
  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;

  public InstructionHandlerRegistry(RegisterValueResolver registerValueResolver,
                                    MemoryAccessHandler memoryAccessHandler) {
    this.registerValueResolver = registerValueResolver;
    this.memoryAccessHandler = memoryAccessHandler;
    registerDefaultHandlers();
  }

  /**
   * Registra los handlers para instrucciones comunes
   */
  private void registerDefaultHandlers() {
    // Handler para PUSH
    registerHandler(Push.class, (cm, instr, mm, opName, genMethods) -> {
      var push = (Push) instr;
      var target = push.getTarget();
      if (target instanceof Register targetReg) {
        new PushOperationHandler(registerValueResolver).executePushWithRegister(mm, targetReg.getName());
        return true;
      }
      return false;
    });

    // Handler para SCF (Set Carry Flag)
    registerHandler(SCF.class, (cm, instr, mm, opName, genMethods) -> {
      var scf = (SCF) instr;
      new FlagOperationHandler(registerValueResolver, memoryAccessHandler).executeFlagOperation(mm, scf);
      return true;
    });

    // Handler para CCF (Complement Carry Flag)
    registerHandler(CCF.class, (cm, instr, mm, opName, genMethods) -> {
      var ccf = (CCF) instr;
      new FlagOperationHandler(registerValueResolver, memoryAccessHandler).executeFlagOperation(mm, ccf);
      return true;
    });

    // Handler para POP
    registerHandler(Pop.class, (cm, instr, mm, opName, genMethods) -> {
      var pop = (Pop) instr;
      var target = pop.getTarget();
      if (target instanceof Register targetReg) {
        new PopOperationHandler(registerValueResolver).executePopWithRegister(mm, targetReg.getName());
        return true;
      }
      return false;
    });

    // Handler genérico para instrucciones de un solo target (Dec16, Inc16, etc.)
    registerHandler(DefaultTargetInstruction.class, (cm, instr, mm, opName, genMethods) -> {
      var defaultTargetInstr = (DefaultTargetInstruction) instr;
      try {
        new DefaultTargetInstructionHandler(registerValueResolver)
          .executeDefaultTargetInstruction(mm, defaultTargetInstr, opName);
        return true;
      } catch (UnsupportedOperationException e) {
        // Si la operación no es soportada, retorna false para omitirla
        return false;
      }
    });
  }

  /**
   * Registra un handler para un tipo de instrucción
   */
  public void registerHandler(Class<?> instructionClass, InstructionHandler handler) {
    handlers.put(instructionClass, handler);
  }

  /**
   * Obtiene el handler para una instrucción, si existe
   */
  public InstructionHandler getHandler(Instruction instruction) {
    // Búsqueda directa
    InstructionHandler handler = handlers.get(instruction.getClass());
    if (handler != null) return handler;
    
    // Búsqueda por jerarquía (para instrucciones que extienden DefaultTargetInstruction)
    for (Map.Entry<Class<?>, InstructionHandler> entry : handlers.entrySet()) {
      if (entry.getKey().isAssignableFrom(instruction.getClass())) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Verifica si hay un handler registrado para esta instrucción
   */
  public boolean hasHandler(Instruction instruction) {
    return getHandler(instruction) != null;
  }

  /**
   * Intenta procesar la instrucción con el handler registrado
   */
  public boolean tryHandle(ClassMaker cm, Instruction instruction, MethodMaker mm,
                          String operationName, Set<String> generatedMethods) {
    var handler = getHandler(instruction);
    if (handler == null) return false;
    
    try {
      return handler.handle(cm, instruction, mm, operationName, generatedMethods);
    } catch (Exception e) {
      System.err.println("Error al procesar instrucción " + instruction.getClass().getSimpleName() + ": " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
}
