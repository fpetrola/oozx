package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.nio.file.Path;
import java.util.*;

/**
 * Genera clases Java en bytecode directamente usando cojen/maker,
 * extrayendo e inlineando código de instrucciones de forma dinámica.
 */
public class BytecodeInliner {
  private final InstructionAnalyzer analyzer;
  private byte[] lastGeneratedBytecode;
  public static Map<String, byte[]> generatedBytecodes = new HashMap<>();

  public BytecodeInliner(InstructionAnalyzer analyzer, Path bytecodeOutputDir) {
    this.analyzer = analyzer;
  }

  public String inlineInstruction(TargetSourceInstruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedClass(instruction, operationName);
  }

  /**
   * Retorna el bytecode de la última clase generada
   */
  public byte[] getLastGeneratedBytecode() {
    return lastGeneratedBytecode;
  }

  private String generateInlinedClass(TargetSourceInstruction instruction, String operationName) {
    String className = getClassName(instruction, operationName);
    className = className.replace("-", "_");

    // Crear class maker - cojen generará un nombre único basado en el className
    ClassMaker cm = ClassMaker.beginExternal(className);
    cm.public_();
    
    // Extender de Z80UnRolled
    cm.extend(Z80UnRolled.class);

    // Get analyzed variables in order
    Map<String, InstructionAnalyzer.VariableInfo> requiredVars = analyzer.getRequiredVariables();
    OpcodeReference target = analyzer.getTarget();

    // No agregar fields - se heredan de Z80UnRolled
    // addFieldsInOrder(cm, requiredVars, target);
    // addAluOperationField(cm, instruction);

    // No agregar constructor (se omite siempre)
    // addConstructor(cm, className, instruction, target);

    // Add execute method with inlined code
    addExecuteMethod(cm, instruction, operationName, target);

    // Compilar la clase y obtener el bytecode directamente
    byte[] bytecodeBytes = cm.finishBytes();

    lastGeneratedBytecode = bytecodeBytes;

    generatedBytecodes.put(className, bytecodeBytes);

    return className;
  }

  private void addAluOperationField(ClassMaker cm, TargetSourceInstruction instruction) {
    if (isAluOperation(instruction)) {
      // Add flag field if not already added
      if (!analyzer.getRequiredVariables().containsKey("flag")) {
        cm.addField(Register.class, "flag").private_();
      }

      // Add ALU operation field with the correct type
      String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());
      Class<?> aluOperationClass = getAluOperationClass(instruction);
      cm.addField(aluOperationClass, fieldName).private_();
    }
  }

  private Class<?> getAluOperationClass(TargetSourceInstruction instruction) {
    try {
      Class<?> instructionClass = instruction.getClass();
      String innerClassName = instructionClass.getSimpleName() + "TableAluOperation";

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
   * Extrae el bytecode de una clase compilada usando el ClassLoader
   */
  private byte[] extractBytecodeFromCompiledClass(Class<?> clazz) {
    try {
      String resourcePath = clazz.getName().replace('.', '/') + ".class";
      var resourceStream = clazz.getClassLoader().getResourceAsStream(resourcePath);

      if (resourceStream != null) {
        return resourceStream.readAllBytes();
      }
    } catch (Exception e) {
      // Ignorar
    }

    return null;
  }

   private void addFieldsInOrder(ClassMaker cm, Map<String, InstructionAnalyzer.VariableInfo> vars, OpcodeReference target) {
    Set<String> excluded = Set.of("F", "Q");

    // 1. Variables en orden manteniendo el de inserción
    for (String name : vars.keySet()) {
      InstructionAnalyzer.VariableInfo var = vars.get(name);

      // Saltar si es excluido o si es pc/memory (los hacemos aparte)
      if (excluded.contains(name) || "memory".equals(name) || "pc".equals(name)) {
        continue;
      }

      Class<?> fieldType = resolveType(var.type);
      cm.addField(fieldType, var.name).private_();
    }

    // 2. Memory
    if (vars.containsKey("memory")) {
      Class<?> fieldType = Memory.class;
      cm.addField(fieldType, "memory").private_();
    }

    // 3. PC
    if (vars.containsKey("pc")) {
      cm.addField(int.class, "pc").private_();
    }
  }


  private void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
    // Generar nombre de método único basado en la clase generada
    // El nombre será algo como "executeXor1" o "executeLd2"
    String methodName = "execute" + operationName;
    
    MethodMaker mm = cm.addMethod(void.class, methodName);
    mm.public_();
    generateExecute(mm, instruction, target);
    mm.return_();
  }

  /**
   * Lee una dirección de 16 bits desde (pc + delta) en formato little-endian
   */
  private Variable readAddress16Bit(MethodMaker mm, Memory16BitReference mem16Ref) {
    Variable memory = mm.field("memory");
    Variable pc = mm.field("PC");

    // Leer dirección de 16 bits desde (pc + delta)
    Variable addr1 = mm.var(int.class);
    addr1.set(memory.invoke("read", pc.add(mem16Ref.getDelta()).and(0xFFFF), 0));
    Variable addr2 = mm.var(int.class);
    addr2.set(memory.invoke("read", pc.add(mem16Ref.getDelta() + 1).and(0xFFFF), 0));

    // Combinar en dirección de 16 bits (little-endian)
    Variable address = mm.var(int.class);
    address.set(addr1.or(addr2.shl(8)));
    return address;
  }

  private void generateExecute(MethodMaker mm, TargetSourceInstruction ld, OpcodeReference target) {
    String sourceRegName = ((Register) ld.getSource()).getName();
    generateAluExecute(mm, ld, target, sourceRegName);
  }

  /**
   * Genera código de ejecución para instrucciones ALU (LD, XOR, OR, etc.)
   * que leen un valor de memoria (o registro para LD), aplican la operación y escriben el resultado.
   */
  private void generateAluExecute(MethodMaker mm, TargetSourceInstruction instruction,
                                  OpcodeReference target, String sourceRegName) {
    if (target instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryPlusRegisterContext ctx = readOffsetAndCalculateAddress(mm, memRef);
      executeAluOperation(mm, instruction, ctx.memory, ctx.address, sourceRegName);
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      Variable address = resolveIndirectMemoryAddress(mm, indMem);
      Variable memory = mm.field("memory");
      executeAluOperation(mm, instruction, memory, address, sourceRegName);
    }
  }

  /**
   * Resuelve la dirección para IndirectMemory8BitReference
   */
  private Variable resolveIndirectMemoryAddress(MethodMaker mm, IndirectMemory8BitReference target) {
    ImmutableOpcodeReference innerTarget = target.getTarget();

    if (innerTarget instanceof Register reg) {
      return mm.field(reg.getName());
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  /**
   * Ejecuta una operación ALU: lee valor (de memoria o registro para LD), aplica operación y escribe resultado
   */
  private void executeAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                   Variable memory, Variable address, String sourceRegName) {
    Variable source = mm.field(sourceRegName);

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

  private void executeWithAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                       Variable memory, Variable address, String sourceRegName) {
    // Leer valor de memoria
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable source = mm.field(sourceRegName);

    // Obtener la operación ALU
    String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());
    Variable aluOp = mm.field(fieldName);

    // Ejecutar la operación ALU: execute2ValuesAndCarry(value, source, flag)
    Variable result = mm.var(int.class);
    Variable flag = mm.field("flag");
    result.set(aluOp.invoke("execute2ValuesAndCarry", value, source, flag));

    // Escribir el resultado
    memory.invoke("write", address, result);
  }

  /**
   * Lee el byte offset (dd) desde memoria en (pc + valueDelta) y calcula la dirección
   * destino como (targetReg + dd) & 0xFFFF. Retorna el contexto con memoria y dirección.
   */
  private MemoryPlusRegisterContext readOffsetAndCalculateAddress(MethodMaker mm, MemoryPlusRegister8BitReference memRef) {
    // 1. Leer el byte offset (dd) desde memoria en (pc + valueDelta)
    Variable dd = mm.var(int.class);
    Variable memory = mm.field("memory");
    Variable pc = mm.field("PC");

    // Cálculo: (pc + valueDelta) & 0xFFFF
    Variable pcPlusDelta = pc.add(memRef.getValueDelta());
    Variable addressDelta = pcPlusDelta.and(0xFFFF);
    dd.set(memory.invoke("read", addressDelta, 0));

    // 2. Calcular dirección destino: (targetReg + dd) & 0xFFFF
    Variable targetReg = mm.field("IX");
    Variable regPlusDd = targetReg.add(dd);
    Variable address = mm.var(int.class);
    address.set(regPlusDd.and(0xFFFF));

    return new MemoryPlusRegisterContext(memory, address);
  }

  /**
   * Contexto para operaciones de MemoryPlusRegister8BitReference
   */
  private static class MemoryPlusRegisterContext {
    final Variable memory;
    final Variable address;

    MemoryPlusRegisterContext(Variable memory, Variable address) {
      this.memory = memory;
      this.address = address;
    }
  }

  private boolean isAluOperation(TargetSourceInstruction instruction) {
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

  private String getAluOperationFieldName(String instructionClassName) {
    return instructionClassName.toLowerCase() + "TableAluOperation";
  }

  private String getClassName(TargetSourceInstruction instruction, String operationName) {
    // Generar nombre sin sufijo (o agregar sufijo si necesitas múltiples variantes)
    return operationName + "Bytecode";
  }

  private String getSourceExpression(ImmutableOpcodeReference source) {
    if (source instanceof Register reg) {
      return reg.getName();
    }
    return "source.read()";
  }

  private String getRegisterName(ImmutableOpcodeReference ref) {
    if (ref instanceof Register reg) {
      return reg.getName();
    }
    return "register";
  }


  /**
   * Resuelve nombres de tipo a clases Java.
   */
  private Class<?> resolveType(String typeName) {
    return switch (typeName) {
      case "int" -> int.class;
      case "long" -> long.class;
      case "byte" -> byte.class;
      case "short" -> short.class;
      case "boolean" -> boolean.class;
      case "char" -> char.class;
      case "float" -> float.class;
      case "double" -> double.class;
      case "Register" -> Register.class;
      case "Memory" -> Memory.class;
      case "Plain8BitRegister" -> int.class;
      case "Plain16BitRegister" -> int.class;
      default -> Object.class;
    };
  }
}
