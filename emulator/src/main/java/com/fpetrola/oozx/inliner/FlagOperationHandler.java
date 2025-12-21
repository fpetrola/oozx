package com.fpetrola.oozx.inliner;

import com.fpetrola.z80.instructions.impl.SCF;
import com.fpetrola.z80.instructions.impl.CCF;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

/**
 * Maneja la generación de bytecode para instrucciones que manipulan flags.
 * Incluye SCF (Set Carry Flag) y CCF (Complement Carry Flag).
 */
public class FlagOperationHandler {

  private final RegisterValueResolver registerValueResolver;
  private final MemoryAccessHandler memoryAccessHandler;

  public FlagOperationHandler(RegisterValueResolver registerValueResolver,
                              MemoryAccessHandler memoryAccessHandler) {
    this.registerValueResolver = registerValueResolver;
    this.memoryAccessHandler = memoryAccessHandler;
  }

  /**
   * Maneja instrucciones de flag operations (SCF, CCF)
   */
  public void executeFlagOperation(MethodMaker mm, DefaultTargetFlagInstruction instruction) {
    if (instruction instanceof SCF) {
      executeSCF(mm, (SCF) instruction);
    } else if (instruction instanceof CCF) {
      executeCCF(mm, (CCF) instruction);
    } else {
      throw new UnsupportedOperationException(
          "Tipo de instrucción de flag no soportado: " + instruction.getClass().getSimpleName());
    }
  }

  /**
   * Ejecuta SCF (Set Carry Flag)
   * SCF sets carry flag and preserves other flags
   * 
   * SCF: aluOp.execute2ValuesAndCarry(A, F_value, F_value)
   * The AluOperation updates the flag internally
   */
  private void executeSCF(MethodMaker mm, SCF instruction) {
    // Leer el valor actual del registro A (target) y F (flag)
    Register target = (Register) instruction.getTarget();
    Register flag = instruction.getFlag();
    
    Variable aValue = registerValueResolver.resolveRegisterValueByName(mm, target.getName());
    Variable fValue = registerValueResolver.resolveRegisterValueByName(mm, flag.getName());
    
    // Obtener el campo de la operación ALU
    Variable scfTableAluOp = mm.field("scfTableAluOperation");
    
    // Ejecutar la operación: result = aluOp.execute2ValuesAndCarry(aValue, fValue, fValue)
    // Note: The AluOperation will internally update its F field which we then read back
    Variable result = mm.var(int.class);
    result.set(scfTableAluOp.invoke("execute2ValuesAndCarry", aValue, fValue, fValue));
    
    // Actualizar F con el resultado
    registerValueResolver.assignRegisterValue(mm, flag.getName(), result);
  }

  /**
   * Ejecuta CCF (Complement Carry Flag)
   * CCF inverts the carry flag
   * 
   * CCF: aluOp.execute2ValuesAndCarry(A, F_value, F_value)
   * The AluOperation updates the flag internally
   */
  private void executeCCF(MethodMaker mm, CCF instruction) {
    // Leer el valor actual del registro A (target) y F (flag)
    Register target = (Register) instruction.getTarget();
    Register flag = instruction.getFlag();
    
    Variable aValue = registerValueResolver.resolveRegisterValueByName(mm, target.getName());
    Variable fValue = registerValueResolver.resolveRegisterValueByName(mm, flag.getName());
    
    // Obtener el campo de la operación ALU
    Variable ccfTableAluOp = mm.field("ccfTableAluOperation");
    
    // Ejecutar la operación: result = aluOp.execute2ValuesAndCarry(aValue, fValue, fValue)
    Variable result = mm.var(int.class);
    result.set(ccfTableAluOp.invoke("execute2ValuesAndCarry", aValue, fValue, fValue));
    
    // Actualizar F con el resultado
    registerValueResolver.assignRegisterValue(mm, flag.getName(), result);
  }

  }

