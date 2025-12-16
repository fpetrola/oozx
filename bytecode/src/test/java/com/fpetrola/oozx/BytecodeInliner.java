package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.nio.file.Path;
import java.util.*;
import java.util.LinkedHashMap;

/**
 * Genera clases Java en bytecode directamente usando cojen/maker,
 * extrayendo e inlineando código de instrucciones de forma dinámica.
 */
public class BytecodeInliner {
  public static final String FLAG = "F";
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
     * Genera una clase con múltiples métodos execute a partir de varias instrucciones
     */
    public String inlineMultipleInstructions(String className, Map<Integer, TargetSourceInstruction<?>> instructions) {
      className = className.replace("-", "_");
      
      ClassMaker cm = createBaseClass(className);
      
      // Guardar los nombres de métodos generados y sus opcodes asociados
      Map<Integer, String> opcodeToMethodName = new LinkedHashMap<>();
      
      // Agregar un método execute para cada instrucción
      for (Map.Entry<Integer, TargetSourceInstruction<?>> entry : instructions.entrySet()) {
        Integer opcode = entry.getKey();
        TargetSourceInstruction<?> instruction = entry.getValue();
        
        // Solo procesar si la instrucción tiene un source que es un Register
        if (!(instruction.getSource() instanceof Register)) {
          continue;
        }
        
        analyzer.analyze(instruction);
        String operationName = instruction.getClass().getSimpleName();
        OpcodeReference target = analyzer.getTarget();
        String methodName = generateUniquMethodName(instruction, operationName, target);
        addExecuteMethod(cm, instruction, operationName, target);
        opcodeToMethodName.put(opcode, methodName);
      }
      
      // Agregar método switch que dispache por opcode
      addDispatchMethodWithOpcodes(cm, opcodeToMethodName);
      
      return finializeClass(className, cm);
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

    ClassMaker cm = createBaseClass(className);
    OpcodeReference target = analyzer.getTarget();

    // Add execute method with inlined code
    addExecuteMethod(cm, instruction, operationName, target);

    return finializeClass(className, cm);
  }

  /**
   * Crea la clase base que extiende Z80UnRolled
   */
  private ClassMaker createBaseClass(String className) {
    ClassMaker cm = ClassMaker.beginExternal(className);
    cm.public_();
    cm.extend(Z80UnRolled.class);
    return cm;
  }

  /**
   * Compila la clase y guarda el bytecode
   */
  private String finializeClass(String className, ClassMaker cm) {
    byte[] bytecodeBytes = cm.finishBytes();
    lastGeneratedBytecode = bytecodeBytes;
    generatedBytecodes.put(className, bytecodeBytes);
    return className;
  }

  private void addAluOperationField(ClassMaker cm, TargetSourceInstruction instruction) {
    if (isAluOperation(instruction)) {
      // Add flag field if not already added
      if (!analyzer.getRequiredVariables().containsKey(FLAG)) {
        cm.addField(Register.class, FLAG).private_();
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


  /**
    * Agrega un método execute(int opcode) que despacha a los métodos específicos usando opcodes reales
    */
   private void addDispatchMethodWithOpcodes(ClassMaker cm, Map<Integer, String> opcodeToMethodName) {
     MethodMaker mm = cm.addMethod(int.class, "execute", int.class);
     mm.public_();
     
     // Crear labels para cada case y el default
     int numCases = opcodeToMethodName.size();
     Label[] caseLabels = new Label[numCases];
     for (int i = 0; i < numCases; i++) {
       caseLabels[i] = mm.label();
     }
     Label defaultLabel = mm.label();
     Label endLabel = mm.label();
     
     // Crear array de casos a partir de los opcodes
     int[] cases = new int[numCases];
     String[] methodNames = new String[numCases];
     int idx = 0;
     for (Map.Entry<Integer, String> entry : opcodeToMethodName.entrySet()) {
       cases[idx] = entry.getKey();
       methodNames[idx] = entry.getValue();
       idx++;
     }
     
     // Obtener variable del parámetro opcode y asignarle el nombre
     Variable opcodeVar = mm.param(0);
     opcodeVar.name("opcode");
     
     // Generar switch statement
     opcodeVar.switch_(defaultLabel, cases, caseLabels);
     
     // Generar código para cada case
     for (int i = 0; i < numCases; i++) {
       caseLabels[i].here();
       mm.invoke(methodNames[i]);
       mm.goto_(endLabel);
     }
     
     // Default case: throw exception
     defaultLabel.here();
     mm.return_(-1);

     // End label: fin del switch
     endLabel.here();
     mm.return_(0);
   }

  private void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
    // Generar nombre de método único basado en la instrucción y sus referencias
    String methodName = generateUniquMethodName(instruction, operationName, target);

    MethodMaker mm = cm.addMethod(void.class, methodName);
    mm.public_();
    generateExecute(mm, instruction, target);
    mm.return_();
  }

  /**
   * Genera un nombre único para el método execute basado en el tipo de instrucción,
   * target, source y referencias involucradas
   */
  private String generateUniquMethodName(TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
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
   * Genera un sufijo basado en el tipo de referencia (Register, Memory, etc.)
   */
  private String getReferenceSuffix(ImmutableOpcodeReference reference) {
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
    }

    return "";
  }

  /**
   * Capitaliza la primera letra de una cadena
   */
  private String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
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
     ImmutableOpcodeReference source = ld.getSource();
     // Solo procesar si el source es un Register
     if (source instanceof Register) {
       String sourceRegName = ((Register) source).getName();
       generateAluExecute(mm, ld, target, sourceRegName);
     }
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
     } else if (target instanceof IndirectMemory16BitReference indMem16) {
       Variable address = resolveIndirectMemory16BitAddress(mm, indMem16);
       Variable memory = mm.field("memory");
       executeAluOperation16Bit(mm, instruction, memory, address, sourceRegName);
     }
   }

  /**
    * Resuelve la dirección para IndirectMemory8BitReference
    */
   private Variable resolveIndirectMemoryAddress(MethodMaker mm, IndirectMemory8BitReference target) {
     ImmutableOpcodeReference innerTarget = target.getTarget();

     if (innerTarget instanceof Register reg) {
       return resolveRegisterValue(mm, reg);
     } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
       return readAddress16Bit(mm, mem16Ref);
     }
     return null;
   }

  /**
    * Resuelve la dirección para IndirectMemory16BitReference
    */
   private Variable resolveIndirectMemory16BitAddress(MethodMaker mm, IndirectMemory16BitReference target) {
     ImmutableOpcodeReference innerTarget = target.getTarget();

     if (innerTarget instanceof Register reg) {
       return resolveRegisterValue(mm, reg);
     } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
       return readAddress16Bit(mm, mem16Ref);
     }
     return null;
   }

  /**
   * Resuelve el valor de un registro, manejando registros de 16 bits construidos a partir de 8 bits
   */
   private Variable resolveRegisterValue(MethodMaker mm, Register reg) {
     String regName = reg.getName();
     return resolveRegisterValueByName(mm, regName);
   }

  /**
   * Resuelve el valor de un registro por su nombre, manejando registros de 16 bits usando los getters de UnrolledRegisterBank
   */
    private Variable resolveRegisterValueByName(MethodMaker mm, String regName) {
      // Si el registro es de 16 bits sin underscore (BC, DE, HL, AF), usar los getters de UnrolledRegisterBank
      if (regName.length() == 2 && !regName.startsWith("_")) {
        String getterMethodName = "get" + regName;  // getBC, getDE, getHL, getAF
        Variable result = mm.var(int.class);
        result.set(mm.invoke(getterMethodName));
        return result;
      }
      
      // Para otros registros (A, F, I, R, IX, IY, SP, PC, etc.), acceder directamente
      return mm.field(regName);
    }

  /**
    * Ejecuta una operación ALU: lee valor (de memoria o registro para LD), aplica operación y escribe resultado
    */
   private void executeAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                    Variable memory, Variable address, String sourceRegName) {
     Variable source = resolveRegisterValueByName(mm, sourceRegName);

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
    private void executeAluOperation16Bit(MethodMaker mm, TargetSourceInstruction instruction,
                                          Variable memory, Variable address, String sourceRegName) {
      Variable source = resolveRegisterValueByName(mm, sourceRegName);

      // Leer valor de 16 bits desde la dirección
      Variable value = mm.var(int.class);
      value.set(memory.invoke("read16Bits", address));

      // Para LD, escribir directamente el valor leído
      if (instruction instanceof Ld) {
        memory.invoke("write16BitsReverse", value, source);
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

   private void executeWithAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                        Variable memory, Variable address, String sourceRegName) {
     // Leer valor de memoria
     Variable value = mm.var(int.class);
     value.set(memory.invoke("read", address, 0));
     Variable source = resolveRegisterValueByName(mm, sourceRegName);

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
   * Lee el byte offset (dd) desde memoria en (pc + valueDelta) y calcula la dirección
   * destino como (targetReg + dd) & 0xFFFF. Retorna el contexto con memoria y dirección.
   */
  private MemoryPlusRegisterContext readOffsetAndCalculateAddress(MethodMaker mm, MemoryPlusRegister8BitReference memRef) {
    Variable pcPlusDelta = mm.field("PC").add(memRef.getValueDelta()).and(0xFFFF);
    Variable dd = mm.var(int.class);
    Variable memory = mm.field("memory");
    dd.set(memory.invoke("read", pcPlusDelta, 0));

    // 2. Calcular dirección destino: (targetReg + dd) & 0xFFFF
    // Obtener el nombre del registro de forma genérica (puede ser IX, IY, etc.)
    ImmutableOpcodeReference target = memRef.getTarget();
    String registerName = getRegisterName(target);
    Variable targetReg = mm.field(registerName);
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
