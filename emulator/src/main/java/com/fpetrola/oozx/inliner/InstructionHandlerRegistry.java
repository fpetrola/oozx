package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.impl.Dec16;
import com.fpetrola.z80.instructions.impl.Inc16;
import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.impl.Ex;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.DefaultTargetInstruction;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.instructions.types.BitOperation;
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
                   String operationName, InstructionProcessingContext context);
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
    // Handler para instrucciones de bit (BIT, RES, SET) - DEBE IR ANTES de DefaultTargetInstruction
    // porque BitOperation extiende DefaultTargetFlagInstruction que extiende DefaultTargetInstruction
    registerHandler(BitOperation.class, (cm, instr, mm, opName, genMethods) -> {
      var bitOp = (BitOperation) instr;
      try {
        new BitOperationHandler(registerValueResolver, memoryAccessHandler)
          .executeBitOperation(mm, bitOp);
        return true;
      } catch (UnsupportedOperationException e) {
        // Si la operación no es soportada, retorna false
        return false;
      } catch (Exception e) {
        return false;
      }
    });
    
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

    // Handler para EX (Exchange/Swap)
    registerHandler(Ex.class, (cm, instr, mm, opName, genMethods) -> {
      var ex = (Ex) instr;
      try {
        new ExOperationHandler(registerValueResolver, memoryAccessHandler).executeEx(mm, ex);
        return true;
      } catch (UnsupportedOperationException e) {
        // Si el tipo de intercambio no es soportado, retorna false
        return false;
      }
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
   * Registra un handler con una condición de filtrado específica.
   * Permite registrar handlers condicionales para subtipos de instrucciones.
   * Ejemplo: registrar un handler solo para Push con ciertos registros
   */
  public void registerConditionalHandler(Class<?> baseClass, 
                                        java.util.function.Predicate<Instruction> condition,
                                        InstructionHandler handler) {
    // Wrapped handler que verifica la condición antes de procesar
    InstructionHandler wrappedHandler = (cm, instr, mm, opName, genMethods) -> {
      if (condition.test(instr)) {
        return handler.handle(cm, instr, mm, opName, genMethods);
      }
      return false;
    };
    registerHandler(baseClass, wrappedHandler);
  }

  /**
    * Obtiene el handler para una instrucción, si existe.
    * Busca en orden de especificidad (más específico primero).
    */
  public InstructionHandler getHandler(Instruction instruction) {
    // Búsqueda directa
    InstructionHandler handler = handlers.get(instruction.getClass());
    if (handler != null) {
      return handler;
    }
    
    // Búsqueda por jerarquía, ordenando por especificidad (subclases primero)
    // Esto asegura que BitOperation se evalúe antes que DefaultTargetInstruction
    InstructionHandler bestMatch = null;
    Class<?> bestMatchClass = null;
    int bestMatchDepth = -1;
    
    for (Map.Entry<Class<?>, InstructionHandler> entry : handlers.entrySet()) {
      if (entry.getKey().isAssignableFrom(instruction.getClass())) {
        // Calcular la especificidad (mayor especificidad = más específico)
        int depth = getInheritanceDepth(instruction.getClass(), entry.getKey());
        if (depth > bestMatchDepth) {
          bestMatchDepth = depth;
          bestMatch = entry.getValue();
          bestMatchClass = entry.getKey();
        }
      }
    }
    
    if (bestMatch != null) {
      return bestMatch;
    }
    return null;
  }
  
  /**
   * Calcula la especificidad de un handler para una instrucción.
   * Mayor valor = más específico.
   * Esto es importante porque queremos handlers más específicos (como BitOperation)
   * antes que sus supertypes (como DefaultTargetInstruction).
   */
  private int getInheritanceDepth(Class<?> child, Class<?> parent) {
    // Primero, buscar en la jerarquía de clases
    int depth = 0;
    Class<?> current = child;
    while (current != null) {
      if (current == parent) {
        // Menor profundidad = más cercano en la jerarquía = más específico
        // Invertimos el valor: 1000 - depth para que los más cercanos tengan valores mayores
        return 1000 - depth;  // Valor alto para jerarquía de clases
      }
      current = current.getSuperclass();
      depth++;
    }
    
    // Si no se encontró en la jerarquía de clases, buscar en interfaces
    depth = 0;
    current = child;
    while (current != null) {
      for (Class<?> iface : current.getInterfaces()) {
        if (iface == parent) {
          return 500 - depth;  // Valor medio para interfaces
        }
        if (parent.isAssignableFrom(iface)) {
          return 400 - depth;
        }
      }
      current = current.getSuperclass();
      depth++;
    }
    
    return 0;  // No encontrado
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
                         String operationName, InstructionProcessingContext context) {
   var handler = getHandler(instruction);
   if (handler == null) return false;
   
   try {
     return handler.handle(cm, instruction, mm, operationName, context);
   } catch (Exception e) {
     System.err.println("Error al procesar instrucción " + instruction.getClass().getSimpleName() + ": " + e.getMessage());
     e.printStackTrace();
     return false;
   }
  }
}
